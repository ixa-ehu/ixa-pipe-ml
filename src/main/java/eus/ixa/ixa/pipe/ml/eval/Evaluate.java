/*
 * Copyright 2015 Rodrigo Agerri

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
import eus.ixa.ixa.pipe.ml.sequence.SequenceEvaluationErrorListener;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabeler;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerDetailedFMeasureListener;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerEvaluationMonitor;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerEvaluator;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerME;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerModel;
import eus.ixa.ixa.pipe.ml.sequence.SequenceSample;
import eus.ixa.ixa.pipe.ml.sequence.SequenceSampleTypeFilter;
import eus.ixa.ixa.pipe.ml.train.AbstractTrainer;
import eus.ixa.ixa.pipe.ml.utils.Flags;

/**
 * Evaluation class mostly using {@link SequenceLabelerEvaluator}.
 *
 * @author ragerri
 * @version 2015-02-24
 */
public class Evaluate {

  /**
   * The reference corpus to evaluate against.
   */
  private ObjectStream<SequenceSample> testSamples;
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
  public Evaluate(final Properties props) throws IOException {
    
    String lang = props.getProperty("language");
    String clearFeatures = props.getProperty("clearFeatures");
    String model = props.getProperty("model");
    String testSet = props.getProperty("testset");
    String corpusFormat = props.getProperty("corpusFormat");
    String seqTypes = props.getProperty("types");
    
    testSamples = AbstractTrainer.getSequenceStream(testSet, clearFeatures, corpusFormat);
    if (seqTypes != Flags.DEFAULT_SEQUENCE_TYPES) {
      String[] neTypes = seqTypes.split(",");
      testSamples = new SequenceSampleTypeFilter(neTypes, testSamples);
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
    //TODO split F-measure and wordAccuracy
    //System.out.println(evaluator.getWordAccuracy());
  }
  /**
   * Evaluate and print the precision, recall and F measure per
   * sequence class.
   *
   * @throws IOException if test corpus not loaded
   */
  public final void detailEvaluate() throws IOException {
    List<EvaluationMonitor<SequenceSample>> listeners = new LinkedList<EvaluationMonitor<SequenceSample>>();
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
    List<EvaluationMonitor<SequenceSample>> listeners = new LinkedList<EvaluationMonitor<SequenceSample>>();
    listeners.add(new SequenceEvaluationErrorListener());
    SequenceLabelerEvaluator evaluator = new SequenceLabelerEvaluator(sequenceLabeler,
        listeners.toArray(new SequenceLabelerEvaluationMonitor[listeners.size()]));
    evaluator.evaluate(testSamples);
    System.out.println(evaluator.getFMeasure());
  }

}
