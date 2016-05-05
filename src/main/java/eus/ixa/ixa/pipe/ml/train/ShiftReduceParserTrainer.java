package eus.ixa.ixa.pipe.ml.train;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import opennlp.tools.cmdline.TerminateToolException;
import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.TrainingParameters;
import opennlp.tools.util.ext.ExtensionLoader;
import opennlp.tools.util.model.ArtifactSerializer;
import eus.ixa.ixa.pipe.ml.features.XMLFeatureDescriptor;
import eus.ixa.ixa.pipe.ml.formats.CoNLL02Format;
import eus.ixa.ixa.pipe.ml.formats.CoNLL03Format;
import eus.ixa.ixa.pipe.ml.formats.LemmatizerFormat;
import eus.ixa.ixa.pipe.ml.formats.TabulatedFormat;
import eus.ixa.ixa.pipe.ml.parse.AncoraHeadRules;
import eus.ixa.ixa.pipe.ml.parse.AncoraHeadRules.AncoraHeadRulesSerializer;
import eus.ixa.ixa.pipe.ml.parse.HeadRules;
import eus.ixa.ixa.pipe.ml.parse.Parse;
import eus.ixa.ixa.pipe.ml.parse.ParseSampleStream;
import eus.ixa.ixa.pipe.ml.parse.ParserFactory;
import eus.ixa.ixa.pipe.ml.parse.PennTreebankHeadRules;
import eus.ixa.ixa.pipe.ml.parse.PennTreebankHeadRules.PennTreebankHeadRulesSerializer;
import eus.ixa.ixa.pipe.ml.parse.ShiftReduceParser;
import eus.ixa.ixa.pipe.ml.resources.LoadModelResources;
import eus.ixa.ixa.pipe.ml.sequence.BilouCodec;
import eus.ixa.ixa.pipe.ml.sequence.BioCodec;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelSample;
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
   * beamsize value needs to be established in any class extending this one.
   */
  private int beamSize;
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
    this.trainData = params.getSettings().get("TrainSet");
    this.testData = params.getSettings().get("TestSet");
    trainSamples = getParseStream(trainData);
    testSamples = getParseStream(testData);
    this.beamSize = Flags.getBeamsize(params);
    createParserFactory(params);
  }
  
  public void createParserFactory(TrainingParameters params) throws IOException {
    String featureDescription = XMLFeatureDescriptor
        .createXMLFeatureDescriptor(params);
    System.err.println(featureDescription);
    HeadRules rules = getHeadRules(params);
    Dictionary autoDict = ShiftReduceParser.buildDictionary(trainSamples, rules, params);
    
    Map<String, Object> resources = LoadModelResources.loadSequenceResources(params);
    setParserFactory(ParserFactory.create(ParserFactory.class.getName(), autoDict, resources));
  }
  
  /**
   * Getting the stream with the right corpus format.
   * @param inputData
   *          the input data
   * @param clearFeatures clear the features
   * @param aCorpusFormat
   *          the corpus format
   * @return the stream from the several corpus formats
   * @throws IOException
   *           the io exception
   */
  public static ObjectStream<Parse> getParseStream(final String inputData) throws IOException {
    ObjectStream<String> parseStream = IOUtils.readFileIntoMarkableStreamFactory(inputData);
    ObjectStream<Parse> samples = new ParseSampleStream(parseStream);
    return samples;
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

  public final int getBeamSize() {
    return beamSize;
  }
  
  public static HeadRules getHeadRules(TrainingParameters params) throws IOException {

    ArtifactSerializer headRulesSerializer = null;

    if (params.getHeadRulesSerializerImpl() != null) {
      headRulesSerializer = ExtensionLoader.instantiateExtension(ArtifactSerializer.class,
              params.getHeadRulesSerializerImpl());
    }
    else {
      if (Flags.getLanguage(params).equalsIgnoreCase("en")) {
        headRulesSerializer = new PennTreebankHeadRulesSerializer();
      }
      else if (Flags.getLanguage(params).equalsIgnoreCase("es")) {
        headRulesSerializer = new AncoraHeadRulesSerializer();
      }
      else {
        // default for now, this case should probably cause an error ...
        headRulesSerializer = new opennlp.tools.parser.lang.en.HeadRules.HeadRulesSerializer();
      }
    }

    Object headRulesObject = headRulesSerializer.create(new FileInputStream(params.getHeadRules()));

    if (headRulesObject instanceof HeadRules) {
      return (HeadRules) headRulesObject;
    }
    else {
      throw new TerminateToolException(-1, "HeadRules Artifact Serializer must create an object of type HeadRules!");
    }
  }



}
