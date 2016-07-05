/*
 * Copyright 2016 Rodrigo Agerri

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
package eus.ixa.ixa.pipe.ml.eval;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.eval.EvaluationMonitor;
import eus.ixa.ixa.pipe.ml.SequenceLabelerTrainer;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelSample;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelSampleTypeFilter;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabeler;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerDetailedFMeasureListener;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerEvaluationErrorListener;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerEvaluationMonitor;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerEvaluator;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerME;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerModel;
import eus.ixa.ixa.pipe.ml.utils.Flags;

/**
 * Evaluation class mostly using {@link SequenceLabelerEvaluator}.
 *
 * @author ragerri
 * @version 2016-07-04
 */
public class SequenceLabelerEvaluate {

  /**
   * The reference corpus to evaluate against.
   */
  private ObjectStream<SequenceLabelSample> testSamples;
  private ObjectStream<SequenceLabelSample> trainSamples;
  /**
   * The corpus format: conll02, conll03, lemmatizer, tabulated.
   */
  private String corpusFormat;
  private boolean unknownAccuracy = false;
  /**
   * An instance of the probabilistic {@link SequenceLabelerME}.
   */
  private SequenceLabeler sequenceLabeler;
  /**
   * The models to use for every language. The keys of the hash are the
   * language codes, the values the models.
   */
  private static ConcurrentHashMap<String, SequenceLabelerModel> seqModels =
      new ConcurrentHashMap<String, SequenceLabelerModel>();

  /**
   * Construct an evaluator. It takes from the properties a model,
   * a testset and the format of the testset. Every other parameter
   * set in the training, e.g., beamsize, decoding, etc., is serialized
   * in the model.
   * @param props the properties parameter
   * @throws IOException the io exception
   */
  public SequenceLabelerEvaluate(final Properties props) throws IOException {
    
    String lang = props.getProperty("language");
    String clearFeatures = props.getProperty("clearFeatures");
    String model = props.getProperty("model");
    String testSet = props.getProperty("testset");
    corpusFormat = props.getProperty("corpusFormat");
    String seqTypes = props.getProperty("types");
    String trainSet = props.getProperty("unknownAccuracy");
    testSamples = SequenceLabelerTrainer.getSequenceStream(testSet, clearFeatures, corpusFormat);
    if (!trainSet.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG)) {
      unknownAccuracy = true;
      trainSamples = SequenceLabelerTrainer.getSequenceStream(trainSet, clearFeatures, corpusFormat);
    }
    if (seqTypes != Flags.DEFAULT_SEQUENCE_TYPES) {
      String[] neTypes = seqTypes.split(",");
      testSamples = new SequenceLabelSampleTypeFilter(neTypes, testSamples);
    }
    seqModels.putIfAbsent(lang, new SequenceLabelerModel(new FileInputStream(model)));
    sequenceLabeler = new SequenceLabelerME(seqModels.get(lang));
  }

  /**
   * Evaluate and print precision, recall and F measure.
   * @throws IOException if test corpus not loaded
   */
  public final void evaluate() throws IOException {
    SequenceLabelerEvaluator evaluator = new SequenceLabelerEvaluator(sequenceLabeler);
    evaluator.evaluate(testSamples);
    System.out.println(evaluator.getFMeasure());
  }
  
  public final void evaluateAccuracy() throws IOException {
    if (unknownAccuracy) {
      SequenceLabelerEvaluator evaluator = new SequenceLabelerEvaluator(trainSamples, corpusFormat, sequenceLabeler);
      evaluator.evaluate(testSamples);
      System.out.println();
      System.out.println("Word Accuracy: " + evaluator.getWordAccuracy());
      System.out.println("Sentence Accuracy: " + evaluator.getSentenceAccuracy());
      System.out.println("Unknown Word Accuracy: " + evaluator.getUnknownWordAccuracy());
      System.out.println("Known Word Accuracy: " + evaluator.getKnownAccuracy());
    } else {
      SequenceLabelerEvaluator evaluator = new SequenceLabelerEvaluator(sequenceLabeler);
      evaluator.evaluate(testSamples);
      System.out.println();
      System.out.println("Word Accuracy: " + evaluator.getWordAccuracy());
      System.out.println("Sentence accuracy: " + evaluator.getSentenceAccuracy());
    }
  }
  /**
   * Evaluate and print the precision, recall and F measure per
   * sequence class.
   *
   * @throws IOException if test corpus not loaded
   */
  public final void detailEvaluate() throws IOException {
    List<EvaluationMonitor<SequenceLabelSample>> listeners = new LinkedList<EvaluationMonitor<SequenceLabelSample>>();
    SequenceLabelerDetailedFMeasureListener detailedFListener = new SequenceLabelerDetailedFMeasureListener();
    listeners.add(detailedFListener);
    SequenceLabelerEvaluator evaluator = new SequenceLabelerEvaluator(sequenceLabeler,
        listeners.toArray(new SequenceLabelerEvaluationMonitor[listeners.size()]));
    evaluator.evaluate(testSamples);
    System.out.println(detailedFListener.toString());
  }
  /**
   * Evaluate and print every error.
   * @throws IOException if test corpus not loaded
   */
  public final void evalError() throws IOException {
    List<EvaluationMonitor<SequenceLabelSample>> listeners = new LinkedList<EvaluationMonitor<SequenceLabelSample>>();
    listeners.add(new SequenceLabelerEvaluationErrorListener());
    SequenceLabelerEvaluator evaluator = new SequenceLabelerEvaluator(sequenceLabeler,
        listeners.toArray(new SequenceLabelerEvaluationMonitor[listeners.size()]));
    evaluator.evaluate(testSamples);
    System.out.println(evaluator.getFMeasure());
  }

}
