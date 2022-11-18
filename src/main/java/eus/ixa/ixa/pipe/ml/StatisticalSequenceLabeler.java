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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import eus.ixa.ixa.pipe.ml.sequence.SequenceLabel;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelFactory;
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
   * The models to use for every language. The keys of the hash are the language
   * codes, the values the models.
   */
  private final static ConcurrentHashMap<String, SequenceLabelerModel> seqModels = new ConcurrentHashMap<>();
  /**
   * The sequence labeler.
   */
  private final SequenceLabelerME sequenceLabeler;
  /**
   * The Sequence factory.
   */
  private SequenceLabelFactory sequenceFactory;

  /**
   * Construct a StatisticalSequenceLabeler specifying the factory to be used.
   *
   * @param props
   *          the properties
   */
  public StatisticalSequenceLabeler(final Properties props) {
    final String lang = props.getProperty("language");
    final String model = props.getProperty("model");
    Boolean useModelCache = Boolean.valueOf(props.getProperty("useModelCache", "true"));
    this.sequenceFactory = new SequenceLabelFactory();
    final SequenceLabelerModel seqModel = loadModel(lang, model, useModelCache);
    this.sequenceLabeler = new SequenceLabelerME(seqModel);
  }

  /**
   * Construct a StatisticalSequenceLabeler specifying the model to be used.
   * 
   * @param model
   *          the specific model to be used.
   * @param lang
   *          the language
   */
  public StatisticalSequenceLabeler(final String model, final String lang) {
    final SequenceLabelerModel seqModel = loadModel(lang, model, true);
    this.sequenceFactory = new SequenceLabelFactory();
    this.sequenceLabeler = new SequenceLabelerME(seqModel);
  }

  /**
   * Get array of Spans from a list of tokens.
   * 
   * @param tokens
   *          the sentence tokens
   * @return the array of Sequence Spans
   */
  public final Span[] seqToSpans(final String[] tokens) {
    final Span[] annotatedText = this.sequenceLabeler.tag(tokens);
    final List<Span> probSpans = new ArrayList<Span>(
        Arrays.asList(annotatedText));
    return probSpans.toArray(new Span[probSpans.size()]);
  }

  public final Span[] lemmatizeToSpans(final String[] tokens) {
    final Span[] seqSpans = this.sequenceLabeler.tag(tokens);
    StringUtils.decodeLemmasToSpans(tokens, seqSpans);
    return seqSpans;
  }

  public final String[] seqToStrings(final String[] tokens) {
    final String[] seqStrings = this.sequenceLabeler.tagToStrings(tokens);
    return this.sequenceLabeler
        .decodeSequences(seqStrings);
  }

  /**
   * Produce a list of the {@link SequenceLabel} objects classified by the
   * probabilistic model.
   *
   * Takes an array of tokens, calls seqToSpans function for probabilistic
   * Sequence Labeling and returns a List of {@link SequenceLabel} objects
   * containing the string, the type and the {@link Span}.
   *
   * @param tokens
   *          an array of tokenized text
   * @return a List of sequences
   */
  public final List<SequenceLabel> getSequences(final String[] tokens) {
    final Span[] origSpans = this.sequenceLabeler.tag(tokens);
    final Span[] seqSpans = SequenceLabelerME.dropOverlappingSpans(origSpans);
    return getSequencesFromSpans(tokens,
        seqSpans);
  }

  public final List<SequenceLabel> getLemmaSequences(final String[] tokens) {
    final Span[] origSpans = this.sequenceLabeler.tag(tokens);
    final Span[] seqSpans = SequenceLabelerME.dropOverlappingSpans(origSpans);
    return getLemmaSequencesFromSpans(tokens,
        seqSpans);
  }

  /**
   * Creates a list of {@link SequenceLabel} objects from spans and tokens.
   *
   * @param seqSpans
   *          the sequence spans of a sentence
   * @param tokens
   *          the tokens in the sentence
   * @return a list of {@link SequenceLabel} objects
   */
  public final List<SequenceLabel> getSequencesFromSpans(final String[] tokens,
      final Span[] seqSpans) {
    final List<SequenceLabel> sequences = new ArrayList<SequenceLabel>();
    for (final Span seqSpan : seqSpans) {
      final String seqString = seqSpan.getCoveredText(tokens);
      final String seqType = seqSpan.getType();
      final SequenceLabel sequence = this.sequenceFactory
          .createSequence(seqString, seqType, seqSpan);
      sequences.add(sequence);
    }
    return sequences;
  }

  public final List<SequenceLabel> getLemmaSequencesFromSpans(
      final String[] tokens, final Span[] seqSpans) {
    final List<SequenceLabel> sequences = new ArrayList<>();
    for (final Span seqSpan : seqSpans) {
      final String seqString = seqSpan.getCoveredText(tokens);
      final String decodedLemma = StringUtils
          .decodeShortestEditScript(seqString, seqSpan.getType());
      seqSpan.setType(decodedLemma);
      final SequenceLabel sequence = this.sequenceFactory
          .createSequence(seqString, decodedLemma, seqSpan);
      sequences.add(sequence);
    }
    return sequences;
  }

  /**
   * Produces a multidimensional array containing all the taggings possible for
   * a given sentence.
   * 
   * @param tokens
   *          the tokens
   * @return the array containing for each row the tags
   */
  public final Span[][] getAllTags(final String[] tokens) {
    return this.sequenceLabeler.tag(13, tokens);
  }

  /**
   * Takes a sentence with multiple tags alternatives for each word and produces
   * a lemma for each of the word-tag combinations.
   * 
   * @param tokens
   *          the sentence tokens
   * @param posTags
   *          the alternative postags
   * @return the ordered map containing all the possible tag#lemma values for
   *         token
   */
  public ListMultimap<String, String> getMultipleLemmas(final String[] tokens,
      final Span[][] posTags) {

    final ListMultimap<String, String> morphMap = ArrayListMultimap.create();
    for (final Span[] posTag : posTags) {
      final Span[] rowLemmas = this.sequenceLabeler.tag(tokens);
      final String[] decodedLemmas = StringUtils.decodeLemmas(tokens,
          rowLemmas);
      for (int j = 0; j < decodedLemmas.length; j++) {
        morphMap.put(tokens[j], posTag[j].getType() + "#" + decodedLemmas[j]);
      }
    }
    return morphMap;
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
    this.sequenceLabeler.clearAdaptiveData();
  }

  /**
   * Loads statically the probabilistic model. Every instance of this finder
   * will share the same model.
   *
   * @param lang
   *          the language
   * @param model
   *          the model to be loaded
   * @return the model as a {@link SequenceLabelerModel} object
   */
  private SequenceLabelerModel loadModel(final String lang,
      final String modelName, final Boolean useModelCache) {
    final long lStartTime = new Date().getTime();
    try {
      if (useModelCache) {
        synchronized (seqModels) {
          if (!seqModels.containsKey(lang + modelName)) {
            SequenceLabelerModel model = new SequenceLabelerModel(new FileInputStream(modelName));
            seqModels.put(lang + modelName, model);
          }
        }
      } else {
        synchronized (seqModels) {
          if (!seqModels.containsKey(lang + modelName)) {
            SequenceLabelerModel model = new SequenceLabelerModel(new FileInputStream(modelName));
            seqModels.put(lang + modelName, model);
          }
        }
      }
    } catch (final IOException e) {
      e.printStackTrace();
    }
    final long lEndTime = new Date().getTime();
    final long difference = lEndTime - lStartTime;
    System.err.println("IXA pipes Sequence model loaded in: " + difference
        + " miliseconds ... [DONE]");
    return seqModels.get(lang + modelName);
  }
}
