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

import eus.ixa.ixa.pipe.ml.DocumentClassifierTrainer;
import eus.ixa.ixa.pipe.ml.document.DocSample;
import eus.ixa.ixa.pipe.ml.document.DocumentClassifier;
import eus.ixa.ixa.pipe.ml.document.DocumentClassifierModel;
import eus.ixa.ixa.pipe.ml.utils.Flags;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.eval.EvaluationMonitor;

/**
 * Evaluation class mostly using {@link DocumentClassifierEvaluator}.
 *
 * @author ragerri
 * @version 2016-07-04
 */
public class DocumentClassifierEvaluate {

  /**
   * The reference corpus to evaluate against.
   */
  private ObjectStream<DocSample> testSamples;
  private ObjectStream<DocSample> trainSamples;
  /**
   * An instance of the probabilistic {@link DocumentClassifierME}.
   */
  private final DocumentClassifier docClassifier;
  /**
   * The models to use for every language. The keys of the hash are the language
   * codes, the values the models.
   */
  private static ConcurrentHashMap<String, DocumentClassifierModel> docModels = new ConcurrentHashMap<String, DocumentClassifierModel>();

  /**
   * Construct an evaluator. It takes from the properties a model, a testset.
   * Every other parameter set in the training is serialized in the model.
   * 
   * @param props
   *          the properties parameter
   * @throws IOException
   *           the io exception
   */
  public DocumentClassifierEvaluate(final Properties props) throws IOException {

    final String lang = props.getProperty("language");
    final String clearFeatures = props.getProperty("clearFeatures");
    final String model = props.getProperty("model");
    final String testSet = props.getProperty("testset");
    this.testSamples = DocumentClassifierTrainer.getDocumentStream(testSet, clearFeatures);
    if (!trainSet.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG)) {
      this.unknownAccuracy = true;
      this.trainSamples = SequenceLabelerTrainer.getSequenceStream(trainSet,
          clearFeatures, this.corpusFormat);
    }
    if (seqTypes != Flags.DEFAULT_SEQUENCE_TYPES) {
      final String[] neTypes = seqTypes.split(",");
      this.testSamples = new SequenceLabelSampleTypeFilter(neTypes,
          this.testSamples);
    }
    seqModels.putIfAbsent(lang,
        new SequenceLabelerModel(new FileInputStream(model)));
    this.docClassifier = new SequenceLabelerME(seqModels.get(lang));
  }

  /**
   * Evaluate and print precision, recall and F measure.
   * 
   * @throws IOException
   *           if test corpus not loaded
   */
  public final void evaluate() throws IOException {
    final SequenceLabelerEvaluator evaluator = new SequenceLabelerEvaluator(this.corpusFormat,
        this.docClassifier);
    evaluator.evaluate(this.testSamples);
    System.out.println(evaluator.getFMeasure());
  }

  public final void evaluateAccuracy() throws IOException {
    if (this.unknownAccuracy) {
      final SequenceLabelerEvaluator evaluator = new SequenceLabelerEvaluator(
          this.trainSamples, this.corpusFormat, this.docClassifier);
      evaluator.evaluate(this.testSamples);
      System.out.println();
      System.out.println("Word Accuracy: " + evaluator.getWordAccuracy());
      System.out
          .println("Sentence Accuracy: " + evaluator.getSentenceAccuracy());
      System.out.println(
          "Unknown Word Accuracy: " + evaluator.getUnknownWordAccuracy());
      System.out
          .println("Known Word Accuracy: " + evaluator.getKnownAccuracy());
    } else {
      final SequenceLabelerEvaluator evaluator = new SequenceLabelerEvaluator(this.corpusFormat,
          this.docClassifier);
      evaluator.evaluate(this.testSamples);
      System.out.println();
      System.out.println("Word Accuracy: " + evaluator.getWordAccuracy());
      System.out
          .println("Sentence accuracy: " + evaluator.getSentenceAccuracy());
    }
  }

  /**
   * Evaluate and print the precision, recall and F measure per sequence class.
   *
   * @throws IOException
   *           if test corpus not loaded
   */
  public final void detailEvaluate() throws IOException {
    final List<EvaluationMonitor<SequenceLabelSample>> listeners = new LinkedList<EvaluationMonitor<SequenceLabelSample>>();
    final SequenceLabelerDetailedFMeasureListener detailedFListener = new SequenceLabelerDetailedFMeasureListener();
    listeners.add(detailedFListener);
    final SequenceLabelerEvaluator evaluator = new SequenceLabelerEvaluator(this.corpusFormat,
        this.docClassifier, listeners
            .toArray(new SequenceLabelerEvaluationMonitor[listeners.size()]));
    evaluator.evaluate(this.testSamples);
    System.out.println(detailedFListener.toString());
  }

  /**
   * Evaluate and print every error.
   * 
   * @throws IOException
   *           if test corpus not loaded
   */
  public final void evalError() throws IOException {
    final List<EvaluationMonitor<SequenceLabelSample>> listeners = new LinkedList<EvaluationMonitor<SequenceLabelSample>>();
    listeners.add(new SequenceLabelerEvaluationErrorListener());
    final SequenceLabelerEvaluator evaluator = new SequenceLabelerEvaluator(this.corpusFormat,
        this.docClassifier, listeners
            .toArray(new SequenceLabelerEvaluationMonitor[listeners.size()]));
    evaluator.evaluate(this.testSamples);
    System.out.println(evaluator.getFMeasure());
  }

}
