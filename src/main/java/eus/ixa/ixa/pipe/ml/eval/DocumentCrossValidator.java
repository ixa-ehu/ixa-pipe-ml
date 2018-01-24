/*
 *  Copyright 2016 Rodrigo Agerri

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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import eus.ixa.ixa.pipe.ml.DocumentClassifierTrainer;
import eus.ixa.ixa.pipe.ml.document.DocClassifierCrossValidator;
import eus.ixa.ixa.pipe.ml.document.DocSample;
import eus.ixa.ixa.pipe.ml.document.DocumentClassifierFactory;
import eus.ixa.ixa.pipe.ml.document.features.DocumentFeatureDescriptor;
import eus.ixa.ixa.pipe.ml.document.features.DocumentModelResources;
import eus.ixa.ixa.pipe.ml.utils.Flags;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.TrainingParameters;

/**
 * Document Cross Validator.
 * @author ragerri
 * @version 2018-01-24
 */
public class DocumentCrossValidator {

  /**
   * The language.
   */
  private final String lang;
  /**
   * String holding the training data.
   */
  private final String trainData;
  /**
   * ObjectStream of the training data.
   */
  private ObjectStream<DocSample> trainSamples;
  /**
   * features needs to be implemented by any class extending this one.
   */
  private DocumentClassifierFactory docClassFactory;
  /**
   * The number of folds for cross validation.
   */
  private int folds;

  public DocumentCrossValidator(final TrainingParameters params) throws IOException {

    this.lang = Flags.getLanguage(params);
    final String clearFeatures = Flags.getClearTrainingFeatures(params);
    this.trainData = params.getSettings().get("TrainSet");
    this.trainSamples = DocumentClassifierTrainer.getDocumentStream(this.trainData,
        clearFeatures);
    this.folds = Flags.getFolds(params);
    createDocumentClassificationFactory(params);
  }

  /**
   * Create {@code createDocumentClassificationFactory} .
   *
   * @param params
   *          the parameter training file
   * @throws IOException
   *           if io error
   */
  public void createDocumentClassificationFactory(
      final TrainingParameters params) throws IOException {
    final String featureDescription = DocumentFeatureDescriptor
        .createDocumentFeatureDescriptor(params);
    System.err.println(featureDescription);
    final byte[] featureGeneratorBytes = featureDescription
        .getBytes(Charset.forName("UTF-8"));
    final Map<String, Object> resources = DocumentModelResources
        .loadDocumentResources(params);
    //TODO add default feature generator
    setDocumentClassifierFactory(
        DocumentClassifierFactory.create(DocumentClassifierFactory.class.getName(),
            featureGeneratorBytes, resources));
  }
  
  public final void crossValidate(final TrainingParameters params) {
    if (getDocumentClassificationFactory() == null) {
      throw new IllegalStateException(
          "The DocumentClassificationFactory must be instantiated!");
    }
    DocClassifierCrossValidator validator = null;
    try {
      validator = new DocClassifierCrossValidator(this.lang, params,
          this.docClassFactory);
      validator.evaluate(this.trainSamples, this.folds);
    } catch (final IOException e) {
      System.err.println("IO error while loading training set!");
      e.printStackTrace();
      System.exit(1);
    } finally {
      try {
        this.trainSamples.close();
      } catch (final IOException e) {
        System.err.println("IO error with the train samples!");
      }
    }
    System.out.println(validator.getDocumentAccuracy());
  }
  
  /**
   * Get the features which are implemented in each of the trainers extending
   * this class.
   * 
   * @return the features
   */
  public final DocumentClassifierFactory getDocumentClassificationFactory() {
    return this.docClassFactory;
  }
  
  public final DocumentClassifierFactory setDocumentClassifierFactory(
      final DocumentClassifierFactory tokenNameFinderFactory) {
    this.docClassFactory = tokenNameFinderFactory;
    return this.docClassFactory;
  }

}
