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

import eus.ixa.ixa.pipe.ml.DocumentClassifierTrainer;
import eus.ixa.ixa.pipe.ml.document.DocSample;
import eus.ixa.ixa.pipe.ml.document.DocumentClassifier;
import eus.ixa.ixa.pipe.ml.document.DocumentClassifierDetailedEvaluationListener;
import eus.ixa.ixa.pipe.ml.document.DocumentClassifierEvaluationMonitor;
import eus.ixa.ixa.pipe.ml.document.DocumentClassifierEvaluator;
import eus.ixa.ixa.pipe.ml.document.DocumentClassifierME;
import eus.ixa.ixa.pipe.ml.document.DocumentClassifierModel;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelSample;
import opennlp.tools.doccat.DoccatEvaluationMonitor;
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
  /**
   * An instance of the probabilistic {@link DocumentClassifierME}.
   */
  private final DocumentClassifier docClassifier;

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

    final String clearFeatures = props.getProperty("clearFeatures");
    final String modelName = props.getProperty("model");
    final String testSet = props.getProperty("testset");
    this.testSamples = DocumentClassifierTrainer.getDocumentStream(testSet,
        clearFeatures);
    DocumentClassifierModel model = new DocumentClassifierModel(
        new FileInputStream(modelName));
    this.docClassifier = new DocumentClassifierME(model);
  }

  public final void evaluate() throws IOException {

    final DocumentClassifierEvaluator evaluator = new DocumentClassifierEvaluator(
        this.docClassifier);
    evaluator.evaluate(this.testSamples);
    System.out.println();
    System.out.println("Word Accuracy: " + evaluator.getAccuracy());
  }
  
  public final void detailEvaluate() throws IOException {
    final List<EvaluationMonitor<DocSample>> listeners = new LinkedList<EvaluationMonitor<DocSample>>();
    final DocumentClassifierDetailedEvaluationListener listener = new DocumentClassifierDetailedEvaluationListener();
    listeners.add(listener);
    final DocumentClassifierEvaluator evaluator = new DocumentClassifierEvaluator(this.docClassifier,  listeners.toArray(new DocumentClassifierEvaluationMonitor[listeners.size()]));
    evaluator.evaluate(testSamples);
    System.out.println();
    listener.writeReport();
  }

}
