package eus.ixa.ixa.pipe.ml;

import java.io.IOException;

import eus.ixa.ixa.pipe.ml.utils.Flags;
import eus.ixa.ixa.pipe.ml.utils.IOUtils;
import opennlp.tools.doccat.DoccatFactory;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.doccat.DocumentSampleStream;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.TrainingParameters;

/**
 * Trainer based on Apache OpenNLP Machine Learning API. This class creates a
 * feature set based on the features activated in the docClassicationTrainer.properties
 * file
 * 
 * @author vjramirez
 * @version 2017-04-17
 */

public class DocClassificationTrainer {

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
  private ObjectStream<DocumentSample> trainSamples;
  /**
   * features needs to be implemented by any class extending this one.
   */
  private DoccatFactory docClassFactory;

  /**
   * Construct a trainer with training and test data and language options.
   * 
   * @param params
   *          the training parameters
   * @throws IOException
   *           io exception
   */
  public DocClassificationTrainer(final TrainingParameters params)
      throws IOException {

    this.lang = Flags.getLanguage(params);
    this.trainData = params.getSettings().get("TrainSet");
    this.trainSamples = getDocumentStream(this.trainData);
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
    docClassFactory = new DoccatFactory();
  }

  public final DoccatModel train(final TrainingParameters params) {
    if (getDocumentClassificationFactory() == null) {
      throw new IllegalStateException(
          "The DocumentClassificationFactory must be instantiated!!");
    }
    DoccatModel trainedModel = null;
    try {
      trainedModel = DocumentCategorizerME.train(this.lang, trainSamples,
          params, docClassFactory);
    } catch (final IOException e) {
      System.err.println("IO error while loading traing and test sets!");
      e.printStackTrace();
      System.exit(1);
    }
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
  public static ObjectStream<DocumentSample> getDocumentStream(
      final String inputData) throws IOException {
    final ObjectStream<String> docStream = IOUtils
        .readFileIntoMarkableStreamFactory(inputData);
    ObjectStream<DocumentSample> sampleStream = new DocumentSampleStream(
        docStream);
    return sampleStream;
  }

  /**
   * Get the features which are implemented in each of the trainers extending
   * this class.
   * 
   * @return the features
   */
  public final DoccatFactory getDocumentClassificationFactory() {
    return this.docClassFactory;
  }

}
