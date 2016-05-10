package eus.ixa.ixa.pipe.ml;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import opennlp.tools.cmdline.TerminateToolException;
import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.TrainingParameters;
import opennlp.tools.util.model.ArtifactSerializer;
import eus.ixa.ixa.pipe.ml.features.XMLFeatureDescriptor;
import eus.ixa.ixa.pipe.ml.parse.AncoraHeadRules.AncoraHeadRulesSerializer;
import eus.ixa.ixa.pipe.ml.parse.HeadRules;
import eus.ixa.ixa.pipe.ml.parse.Parse;
import eus.ixa.ixa.pipe.ml.parse.ParseSampleStream;
import eus.ixa.ixa.pipe.ml.parse.ParserEvaluator;
import eus.ixa.ixa.pipe.ml.parse.ParserFactory;
import eus.ixa.ixa.pipe.ml.parse.ParserModel;
import eus.ixa.ixa.pipe.ml.parse.PennTreebankHeadRules.PennTreebankHeadRulesSerializer;
import eus.ixa.ixa.pipe.ml.parse.ShiftReduceParser;
import eus.ixa.ixa.pipe.ml.resources.LoadModelResources;
import eus.ixa.ixa.pipe.ml.sequence.BilouCodec;
import eus.ixa.ixa.pipe.ml.sequence.BioCodec;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerCodec;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerFactory;
import eus.ixa.ixa.pipe.ml.utils.Flags;
import eus.ixa.ixa.pipe.ml.utils.IOUtils;

public class ShiftReduceParserTrainer {

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
   * The head rules.
   */
  private HeadRules rules;
  /**
   * beamsize value needs to be established in any class extending this one.
   */
  private int beamSize;
  /**
   * features needs to be implemented by any class extending this one.
   */
  private ParserFactory parserFactory;
  /**
   * The sequence encoding of the named entity spans, e.g., BIO or BILOU.
   */
  private String sequenceCodec;
  /**
   * The sequence labeler factory for the tagger.
   */
  private SequenceLabelerFactory taggerFactory;
  /**
   * The sequence labeler factory for the chunker.
   */
  private SequenceLabelerFactory chunkerFactory;

  /**
   * Construct a trainer with training and test data, and with options for
   * language, beamsize for decoding, sequence codec and corpus format (conll or
   * opennlp).
   * 
   * @param params
   *          the training parameters
   * @param taggerParams
   *          the tagger parameters
   * @param chunkerParams
   *          the chunker parameters
   * @throws IOException
   *           io exception
   */
  public ShiftReduceParserTrainer(final TrainingParameters params,
      final TrainingParameters taggerParams,
      final TrainingParameters chunkerParams) throws IOException {

    this.lang = Flags.getLanguage(params);
    this.trainData = params.getSettings().get("TrainSet");
    this.testData = params.getSettings().get("TestSet");
    trainSamples = getParseStream(trainData);
    testSamples = getParseStream(testData);
    rules = getHeadRules(params);
    this.beamSize = Flags.getBeamsize(params);
    createParserFactory(params);
    setTaggerFactory(createSequenceLabelerFactory(taggerParams));
    setChunkerFactory(createSequenceLabelerFactory(chunkerParams));
  }

  public void createParserFactory(TrainingParameters params) throws IOException {
    Dictionary autoDict = ShiftReduceParser.buildDictionary(trainSamples,
        rules, params);
    Map<String, Object> resources = LoadModelResources
        .loadParseResources(params);
    setParserFactory(ParserFactory.create(ParserFactory.class.getName(),
        autoDict, resources));
  }

  public SequenceLabelerFactory createSequenceLabelerFactory(
      TrainingParameters params) throws IOException {
    String seqCodec = getSequenceCodec();
    SequenceLabelerCodec<String> sequenceCodec = SequenceLabelerFactory
        .instantiateSequenceCodec(seqCodec);
    String featureDescription = XMLFeatureDescriptor
        .createXMLFeatureDescriptor(params);
    System.err.println(featureDescription);
    byte[] featureGeneratorBytes = featureDescription.getBytes(Charset
        .forName("UTF-8"));
    Map<String, Object> resources = LoadModelResources
        .loadSequenceResources(params);
    return SequenceLabelerFactory.create(
        SequenceLabelerFactory.class.getName(), featureGeneratorBytes,
        resources, sequenceCodec);
  }

  public final ParserModel train(final TrainingParameters params, final TrainingParameters taggerParams, final TrainingParameters chunkerParams) {
    if (getParserFactory() == null) {
      throw new IllegalStateException(
          "The ParserFactory must be instantiated!!");
    }
    if (getTaggerFactory() == null) {
      throw new IllegalStateException("The TaggerFactory must be instantiated!");
    }
    ParserModel trainedModel = null;
    ParserEvaluator parserEvaluator = null;
    try {
      trainedModel = ShiftReduceParser.train(lang, trainSamples, rules,
          params, parserFactory, taggerParams, taggerFactory, chunkerParams, chunkerFactory);
      ShiftReduceParser parser = new ShiftReduceParser(trainedModel);
      parserEvaluator = new ParserEvaluator(parser);
      parserEvaluator.evaluate(testSamples);
    } catch (IOException e) {
      System.err.println("IO error while loading traing and test sets!");
      e.printStackTrace();
      System.exit(1);
    }
    System.out.println("Final Result: \n" + parserEvaluator.getFMeasure());
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
  public static ObjectStream<Parse> getParseStream(final String inputData)
      throws IOException {
    ObjectStream<String> parseStream = IOUtils
        .readFileIntoMarkableStreamFactory(inputData);
    ObjectStream<Parse> samples = new ParseSampleStream(parseStream);
    return samples;
  }

  @SuppressWarnings("rawtypes")
  public static HeadRules getHeadRules(TrainingParameters params)
      throws IOException {

    ArtifactSerializer headRulesSerializer = null;
    if (Flags.getLanguage(params).equalsIgnoreCase("en")) {
      headRulesSerializer = new PennTreebankHeadRulesSerializer();
    } else if (Flags.getLanguage(params).equalsIgnoreCase("es")) {
      headRulesSerializer = new AncoraHeadRulesSerializer();
    } else {
      System.err.println("HeadRules not suported for language "
          + Flags.getLanguage(params) + "!!");
    }
    Object headRulesObject = headRulesSerializer.create(new FileInputStream(
        Flags.getHeadRulesFile(params)));
    if (headRulesObject instanceof HeadRules) {
      return (HeadRules) headRulesObject;
    } else {
      throw new TerminateToolException(-1,
          "HeadRules Artifact Serializer must create an object of type HeadRules!");
    }
  }

  /**
   * Get the features which are implemented in each of the trainers extending
   * this class.
   * 
   * @return the features
   */
  public final SequenceLabelerFactory getTaggerFactory() {
    return taggerFactory;
  }

  public final SequenceLabelerFactory setTaggerFactory(
      SequenceLabelerFactory tokenNameFinderFactory) {
    this.taggerFactory = tokenNameFinderFactory;
    return taggerFactory;
  }

  /**
   * Get the features which are implemented in each of the trainers extending
   * this class.
   * 
   * @return the features
   */
  public final SequenceLabelerFactory getChunkerFactory() {
    return chunkerFactory;
  }

  public final SequenceLabelerFactory setChunkerFactory(
      SequenceLabelerFactory tokenNameFinderFactory) {
    this.chunkerFactory = tokenNameFinderFactory;
    return chunkerFactory;
  }

  /**
   * Get the features which are implemented in each of the trainers extending
   * this class.
   * 
   * @return the features
   */
  public final ParserFactory getParserFactory() {
    return parserFactory;
  }

  public final ParserFactory setParserFactory(ParserFactory parserFactory) {
    this.parserFactory = parserFactory;
    return parserFactory;
  }

  public final int getBeamSize() {
    return beamSize;
  }

  /**
   * Get the Sequence codec.
   * 
   * @return the sequence codec
   */
  public final String getSequenceCodec() {
    String seqCodec = null;
    if ("BIO".equals(sequenceCodec)) {
      seqCodec = BioCodec.class.getName();
    } else if ("BILOU".equals(sequenceCodec)) {
      seqCodec = BilouCodec.class.getName();
    }
    return seqCodec;
  }

  /**
   * Set the sequence codec.
   * 
   * @param aSeqCodec
   *          the sequence codec to be set
   */
  public final void setSequenceCodec(final String aSeqCodec) {
    this.sequenceCodec = aSeqCodec;
  }

}
