package eus.ixa.ixa.pipe.ml.train;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.TrainingParameters;
import eus.ixa.ixa.pipe.ml.features.XMLFeatureDescriptor;
import eus.ixa.ixa.pipe.ml.parse.Parse;
import eus.ixa.ixa.pipe.ml.parse.ParserFactory;
import eus.ixa.ixa.pipe.ml.resources.LoadModelResources;
import eus.ixa.ixa.pipe.ml.sequence.BilouCodec;
import eus.ixa.ixa.pipe.ml.sequence.BioCodec;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerCodec;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerFactory;
import eus.ixa.ixa.pipe.ml.utils.Flags;

public class ParserTrainer {
  
  /**
   * The language.
   */
  private String lang;
  /**
   * String holding the training data.
   */
  private String trainData;
  /**
   * String holding the testData.
   */
  private String testData;
  /**
   * ObjectStream of the training data.
   */
  private ObjectStream<Parse> trainSamples;
  /**
   * ObjectStream of the test data.
   */
  private ObjectStream<Parse> testSamples;
  /**
   * beamsize value needs to be established in any class extending this one.
   */
  private int beamSize;
  /**
   * The sequence encoding of the named entity spans, e.g., BIO or BILOU.
   */
  private String sequenceCodec;
  /**
   * Reset the adaptive features every newline in the training data.
   */
  private String clearTrainingFeatures;
  /**
   * Reset the adaptive features every newline in the testing data.
   */
  private String clearEvaluationFeatures;
  /**
   * features needs to be implemented by any class extending this one.
   */
  private ParserFactory parserFactory;

  /**
   * Construct a trainer with training and test data, and with options for
   * language, beamsize for decoding, sequence codec and corpus format (conll or opennlp).
   * @param params the training parameters
   * @throws IOException
   *           io exception
   */
  public ShiftReduceParserTrainer(final TrainingParameters params) throws IOException {
    
    this.lang = Flags.getLanguage(params);
    this.clearTrainingFeatures = Flags.getClearTrainingFeatures(params);
    this.clearEvaluationFeatures = Flags.getClearEvaluationFeatures(params);
    this.trainData = params.getSettings().get("TrainSet");
    this.testData = params.getSettings().get("TestSet");
    trainSamples = getSequenceStream(trainData, clearTrainingFeatures);
    testSamples = getSequenceStream(testData, clearEvaluationFeatures);
    this.beamSize = Flags.getBeamsize(params);
    this.sequenceCodec = Flags.getSequenceCodec(params); 
    createParserFactory(params);
  }
  
  public void createParserFactory(TrainingParameters params) throws IOException {
    String seqCodec = getSequenceCodec();
    SequenceLabelerCodec<String> sequenceCodec = SequenceLabelerFactory
        .instantiateSequenceCodec(seqCodec);
    String featureDescription = XMLFeatureDescriptor
        .createXMLFeatureDescriptor(params);
    System.err.println(featureDescription);
    byte[] featureGeneratorBytes = featureDescription.getBytes(Charset
        .forName("UTF-8"));
    Map<String, Object> resources = LoadModelResources.loadResources(params, featureGeneratorBytes);
    setParserFactory(ParserFactory.create(
        ParserFactory.class.getName(), featureGeneratorBytes,
        resources, sequenceCodec));
  }
  
  /**
   * Get the features which are implemented in each of the trainers extending
   * this class.
   * @return the features
   */
  public final ParserFactory getParserFactory() {
    return parserFactory;
  }
  
  public final ParserFactory setParserFactory(ParserFactory parserFactory) {
    this.parserFactory = parserFactory;
    return parserFactory;
  }
    
  /**
   * Get the Sequence codec.
   * @return the sequence codec
   */
  public final String getSequenceCodec() {
    String seqCodec = null;
    if ("BIO".equals(sequenceCodec)) {
      seqCodec = BioCodec.class.getName();
    }
    else if ("BILOU".equals(sequenceCodec)) {
      seqCodec = BilouCodec.class.getName();
    }
    return seqCodec;
  }
  
  /**
   * Set the sequence codec.
   * @param aSeqCodec the sequence codec to be set
   */
  public final void setSequenceCodec(final String aSeqCodec) {
    this.sequenceCodec = aSeqCodec;
  }
  
  public final int getBeamSize() {
    return beamSize;
  }


}
