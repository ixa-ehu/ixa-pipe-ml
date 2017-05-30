package eus.ixa.ixa.pipe.ml;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import eus.ixa.ixa.pipe.ml.document.DocSample;
import eus.ixa.ixa.pipe.ml.document.DocSampleStream;
import eus.ixa.ixa.pipe.ml.document.DocumentClassifier;
import eus.ixa.ixa.pipe.ml.document.DocumentClassifierEvaluator;
import eus.ixa.ixa.pipe.ml.document.DocumentClassifierFactory;
import eus.ixa.ixa.pipe.ml.document.DocumentClassifierME;
import eus.ixa.ixa.pipe.ml.document.DocumentClassifierModel;
import eus.ixa.ixa.pipe.ml.document.features.DocumentFeatureDescriptor;
import eus.ixa.ixa.pipe.ml.document.features.DocumentModelResources;
import eus.ixa.ixa.pipe.ml.utils.Flags;
import eus.ixa.ixa.pipe.ml.utils.IOUtils;

import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.TrainingParameters;

/**
 * Trainer based on Apache OpenNLP Machine Learning API. This class creates a
 * feature set based on the features activated in the docClassicationTrainer.properties
 * file
 * 
 * @author ragerri
 * @version 2017-05-27
 */

public class DocumentClassifierTrainer {

  /**
   * The language.
   */
  private final String lang;
  /**
   * String holding the training data.
   */
  private final String trainData;
  /**
   * String pointing to the test data.
   */
  private final String testData;
  /**
   * ObjectStream of the training data.
   */
  private ObjectStream<DocSample> trainSamples;
  /**
   * ObjectStream of the test data.
   */
  private ObjectStream<DocSample> testSamples;
  /**
   * Reset the adaptive features every newline in the training data.
   */
  private final String clearTrainingFeatures;
  /**
   * Reset the adaptive features every newline in the testing data.
   */
  private final String clearEvaluationFeatures;
  /**
   * features needs to be implemented by any class extending this one.
   */
  private DocumentClassifierFactory docClassFactory;

  /**
   * Construct a trainer with training and test data and language options.
   * 
   * @param params
   *          the training parameters
   * @throws IOException
   *           io exception
   */
  public DocumentClassifierTrainer(final TrainingParameters params)
      throws IOException {

    this.lang = Flags.getLanguage(params);
    this.clearTrainingFeatures = Flags.getClearTrainingFeatures(params);
    this.clearEvaluationFeatures = Flags.getClearEvaluationFeatures(params);
    this.trainData = params.getSettings().get("TrainSet");
    this.testData = params.getSettings().get("TestSet");
    this.trainSamples = getDocumentStream(trainData, clearTrainingFeatures);
    this.testSamples = getDocumentStream(testData, clearEvaluationFeatures);
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
    setDocumentClassifierFactory(
        DocumentClassifierFactory.create(DocumentClassifierFactory.class.getName(),
            featureGeneratorBytes, resources));
  }

  public final DocumentClassifierModel train(final TrainingParameters params) {
    if (getDocumentClassificationFactory() == null) {
      throw new IllegalStateException(
          "The DocumentClassificationFactory must be instantiated!!");
    }
    DocumentClassifierModel trainedModel = null;
    DocumentClassifierEvaluator docEvaluator = null;
    try {
      trainedModel = DocumentClassifierME.train(this.lang, trainSamples,
          params, docClassFactory);
      final DocumentClassifier docClassifier = new DocumentClassifierME(trainedModel);
      docEvaluator = new DocumentClassifierEvaluator(docClassifier);
      docEvaluator.evaluate(testSamples);
    } catch (final IOException e) {
      System.err.println("IO error while loading traing and test sets!");
      e.printStackTrace();
      System.exit(1);
    }
    System.out.println("Final Result: \n" + docEvaluator.getAccuracy());
    return trainedModel;
  }

  /**
   * Getting the stream with the right corpus format.
   * 
   * @param inputData
   *          the input data
   * @return the stream from the several corpus formats
   * @throws IOException
   *           the io exception
   */
  public static ObjectStream<DocSample> getDocumentStream(
      final String inputData, String clearFeatures) throws IOException {
    final ObjectStream<String> docStream = IOUtils
        .readFileIntoMarkableStreamFactory(inputData);
    ObjectStream<DocSample> sampleStream = new DocSampleStream(clearFeatures,
        docStream);
    return sampleStream;
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
