/*
 *Copyright 2016 Rodrigo Agerri

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package eus.ixa.ixa.pipe.ml;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import eus.ixa.ixa.pipe.ml.sequence.Sequence;
import eus.ixa.ixa.pipe.ml.sequence.SequenceFactory;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerME;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerModel;
import eus.ixa.ixa.pipe.ml.utils.Span;
import eus.ixa.ixa.pipe.ml.utils.StringUtils;

/**
 * Statistical Sequence Labeling based on Apache OpenNLP Machine Learning API.
 *
 * @author ragerri
 * @version 2016-04-01
 *
 */

public class StatisticalSequenceLabeler {

  /**
   * The models to use for every language. The keys of the hash are the
   * language codes, the values the models.
   */
  private static ConcurrentHashMap<String, SequenceLabelerModel> seqModels =
      new ConcurrentHashMap<String, SequenceLabelerModel>();
  /**
   * The sequence labeler.
   */
  private SequenceLabelerME sequenceLabeler;
  /**
   * The Sequence factory.
   */
  private SequenceFactory sequenceFactory;

  /**
   * Construct a probabilistic sequence labeler.
   * @param props the properties to be loaded
   */
  public StatisticalSequenceLabeler(final Properties props) {
    String lang = props.getProperty("language");
    String model = props.getProperty("model");
    SequenceLabelerModel seqModel = loadModel(lang, model);
    sequenceLabeler = new SequenceLabelerME(seqModel);
  }

  /**
   * Construct a StatisticalSequenceLabeler specifying the factory to
   * be used.
   *
   * @param props the properties
   * @param aSeqFactory the name factory to construct Name objects
   */
  public StatisticalSequenceLabeler(final Properties props, final SequenceFactory aSeqFactory) {

    String lang = props.getProperty("language");
    String model = props.getProperty("model");
    this.sequenceFactory = aSeqFactory;
    SequenceLabelerModel seqModel = loadModel(lang, model);
    sequenceLabeler = new SequenceLabelerME(seqModel);
  }

  /**
   * Get array of Spans from a list of tokens.
   * @param tokens the sentence tokens
   * @return the array of Sequence Spans
   */
  public final Span[] seqToSpans(final String[] tokens) {
    Span[] annotatedText = sequenceLabeler.tag(tokens);
    List<Span> probSpans = new ArrayList<Span>(Arrays.asList(annotatedText));
    return probSpans.toArray(new Span[probSpans.size()]);
  }
  
  public final Span[] lemmatizeToSpans(final String[] tokens) {
    Span[] seqSpans = sequenceLabeler.tag(tokens);
    StringUtils.decodeLemmasToSpans(tokens, seqSpans);
    return seqSpans;
  }

  /**
   * Produce a list of the {@link Sequence} objects classified by the
   * probabilistic model.
   *
   * Takes an array of tokens, calls seqToSpans function for probabilistic Sequence
   * Labeling and returns a List of {@link Sequence} objects containing the string, the
   * type and the {@link Span}.
   *
   * @param tokens
   *          an array of tokenized text
   * @return a List of sequences
   */
  public final List<Sequence> getSequences(final String[] tokens) {
    Span[] origSpans = sequenceLabeler.tag(tokens);
    Span[] seqSpans = SequenceLabelerME.dropOverlappingSpans(origSpans);
    List<Sequence> sequences = getSequencesFromSpans(tokens, seqSpans);
    return sequences;
  }
  
  public final List<Sequence> getLemmaSequences(final String[] tokens) {
    Span[] origSpans = sequenceLabeler.tag(tokens);
    Span[] seqSpans = SequenceLabelerME.dropOverlappingSpans(origSpans);
    List<Sequence> sequences = getLemmaSequencesFromSpans(tokens, seqSpans);
    return sequences;
  }
  
  /**
   * Creates a list of {@link Sequence} objects from spans and tokens.
   *
   * @param seqSpans the sequence spans of a sentence
   * @param tokens the tokens in the sentence
   * @return a list of {@link Sequence} objects
   */
  public final List<Sequence> getSequencesFromSpans(final String[] tokens, final Span[] seqSpans) {
    List<Sequence> sequences = new ArrayList<Sequence>();
    for (Span seqSpan : seqSpans) {
      String seqString = seqSpan.getCoveredText(tokens);
      String seqType = seqSpan.getType();
      Sequence sequence = sequenceFactory.createSequence(seqString, seqType, seqSpan);
      sequences.add(sequence);
    }
    return sequences;
  }
  
  public final List<Sequence> getLemmaSequencesFromSpans(final String[] tokens, Span[] seqSpans) {
    List<Sequence> sequences = new ArrayList<>();
    for (Span seqSpan : seqSpans) {
      String seqString = seqSpan.getCoveredText(tokens);
      String decodedLemma = StringUtils.decodeShortestEditScript(seqString.toLowerCase(), seqSpan.getType());
      seqSpan.setType(decodedLemma);
      Sequence sequence = sequenceFactory.createSequence(seqString, decodedLemma, seqSpan);
      sequences.add(sequence);
    }
    return sequences;
  }
  
  /**
   * Forgets all adaptive data which was collected during previous calls to one
   * of the find methods. This method is typically called at the end of a
   * document.
   *
   * From Apache OpenNLP documentation: "After every document clearAdaptiveData
   * must be called to clear the adaptive data in the feature generators. Not
   * calling clearAdaptiveData can lead to a sharp drop in the detection rate
   * after a few documents."
   */
  public final void clearAdaptiveData() {
    sequenceLabeler.clearAdaptiveData();
  }

  /**
   * Loads statically the probabilistic model. Every instance of this finder
   * will share the same model.
   *
   * @param lang the language
   * @param model the model to be loaded
   * @return the model as a {@link SequenceLabelerModel} object
   */
  private final SequenceLabelerModel loadModel(final String lang, final String model) {
    long lStartTime = new Date().getTime();
    try {
      synchronized (seqModels) {
        if (!seqModels.containsKey(lang)) {
          seqModels.put(lang, new SequenceLabelerModel(new FileInputStream(model)));
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    long lEndTime = new Date().getTime();
    long difference = lEndTime - lStartTime;
    System.err.println("IXA pipes Sequence model loaded in: " + difference
        + " miliseconds ... [DONE]");
    return seqModels.get(lang);
  }
}
