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
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import eus.ixa.ixa.pipe.ml.DocumentClassifierTrainer;
import eus.ixa.ixa.pipe.ml.document.DocSample;
import eus.ixa.ixa.pipe.ml.document.DocumentClassifier;
import eus.ixa.ixa.pipe.ml.document.DocumentClassifierEvaluator;
import eus.ixa.ixa.pipe.ml.document.DocumentClassifierME;
import eus.ixa.ixa.pipe.ml.document.DocumentClassifierModel;
import opennlp.tools.util.ObjectStream;

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
    docModels.putIfAbsent(lang,
        new DocumentClassifierModel(new FileInputStream(model)));
    this.docClassifier = new DocumentClassifierME(docModels.get(lang));
  }

  public final void evaluate() throws IOException {
    
      final DocumentClassifierEvaluator evaluator = new DocumentClassifierEvaluator(this.docClassifier);
      evaluator.evaluate(this.testSamples);
      System.out.println();
      System.out.println("Word Accuracy: " + evaluator.getAccuracy());
  }

}
