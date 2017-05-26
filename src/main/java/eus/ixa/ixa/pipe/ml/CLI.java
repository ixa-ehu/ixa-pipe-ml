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

package eus.ixa.ixa.pipe.ml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.google.common.io.Files;

import eus.ixa.ixa.pipe.ml.document.DocumentClassifierModel;
import eus.ixa.ixa.pipe.ml.eval.CrossValidator;
import eus.ixa.ixa.pipe.ml.eval.ParserEvaluate;
import eus.ixa.ixa.pipe.ml.eval.SequenceLabelerEvaluate;
import eus.ixa.ixa.pipe.ml.eval.TokenizerEvaluate;
import eus.ixa.ixa.pipe.ml.parse.ParserModel;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerModel;
import eus.ixa.ixa.pipe.ml.utils.Flags;
import eus.ixa.ixa.pipe.ml.utils.IOUtils;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;
import opennlp.tools.cmdline.CmdLineUtil;
import opennlp.tools.util.TrainingParameters;

/**
 * Main class of ixa-pipe-ml, the IXA pipes (ixa2.si.ehu.es/ixa-pipes) Machine
 * Learning library.
 *
 * @author ragerri
 * @version 2016-04-01
 *
 */
public class CLI {

  /**
   * Get dynamically the version of ixa-pipe-ml by looking at the MANIFEST file.
   */
  private final String version = CLI.class.getPackage()
      .getImplementationVersion();
  /**
   * Name space of the arguments provided at the CLI.
   */
  private Namespace parsedArguments = null;
  /**
   * Argument parser instance.
   */
  private final ArgumentParser argParser = ArgumentParsers
      .newArgumentParser("ixa-pipe-ml-" + this.version + "-exec.jar")
      .description("ixa-pipe-ml-" + this.version
          + " is a Machine Learning component to train and evaluate models for various IXA pipes tasks.\n");
  /**
   * Sub parser instance.
   */
  private final Subparsers subParsers = this.argParser.addSubparsers()
      .help("sub-command help");
  /**
   * The parser that manages the SequenceLabeler training sub-command.
   */
  private final Subparser seqTrainerParser;
  /**
   * The subparser that manages the Constituent Parser training sub-command.
   */
  private final Subparser parserTrainerParser;
  /**
   * The subparser that manages the Tokenizer evaluation.
   */
  private final Subparser tokevalParser;
  /**
   * The parser that manages the SequenceLabeler evaluation sub-command.
   */
  private final Subparser evalParser;
  /**
   * The parser to manage the parsing evaluation.
   */
  private final Subparser parsevalParser;
  /**
   * The parser that manages the DocumentClassification training sub-command.
   */
  private final Subparser docTrainerParser;
  /**
   * The parser that manages the cross validation sub-command.
   */
  private final Subparser crossValidateParser;

  private static final String SEQ_TRAINER_NAME = "sequenceTrainer";
  private static final String PARSE_TRAINER_NAME = "parserTrainer";
  private static final String TOKEVAL_PARSER_NAME = "tokeval";
  private static final String EVAL_PARSER_NAME = "eval";
  private static final String PARSEVAL_PARSER_NAME = "parseval";
  public static final String DOC_TRAINER_NAME = "docTrainer";
  private static final String CROSS_PARSER_NAME = "cross";

  /**
   * Construct a CLI object with the sub-parsers to manage the command line
   * parameters.
   */
  public CLI() {

    this.seqTrainerParser = this.subParsers.addParser(SEQ_TRAINER_NAME)
        .help("Sequence Labeler training CLI");
    loadSeqLabelerTrainingParameters();
    this.parserTrainerParser = this.subParsers.addParser(PARSE_TRAINER_NAME)
        .help("Constituent Parser training CLI");
    loadParserTrainingParameters();
    this.tokevalParser = this.subParsers.addParser(TOKEVAL_PARSER_NAME);
    loadTokevalParameters();
    this.evalParser = this.subParsers.addParser(EVAL_PARSER_NAME)
        .help("Evaluation CLI");
    loadEvalParameters();
    this.parsevalParser = this.subParsers.addParser(PARSEVAL_PARSER_NAME)
        .help("Parseval CLI");
    loadParsevalParameters();
    this.docTrainerParser = this.subParsers.addParser(DOC_TRAINER_NAME).help("Document Classification training CLI");
    loadDocTrainingParameters();
    this.crossValidateParser = this.subParsers.addParser(CROSS_PARSER_NAME)
        .help("Cross validation CLI");
    loadCrossValidateParameters();
  }

  /**
   * Main entry point of ixa-pipe-ml.
   *
   * @param args
   *          the arguments passed through the CLI
   * @throws IOException
   *           exception if input data not available
   */
  public static void main(final String[] args) throws IOException {

    final CLI cmdLine = new CLI();
    cmdLine.parseCLI(args);
  }

  /**
   * Parse the command interface parameters with the argParser.
   *
   * @param args
   *          the arguments passed through the CLI
   * @throws IOException
   *           exception if problems with the incoming data
   */
  private void parseCLI(final String[] args) throws IOException {
    try {
      this.parsedArguments = this.argParser.parseArgs(args);
      System.err.println("CLI options: " + this.parsedArguments);
        switch (args[0]) {
        case TOKEVAL_PARSER_NAME:
          tokeval();
          break;
        case EVAL_PARSER_NAME:
            eval();
            break;
        case PARSEVAL_PARSER_NAME:
            parseval();
            break;
        case SEQ_TRAINER_NAME:
            seqTrain();
            break;
        case PARSE_TRAINER_NAME:
            parserTrain();
            break;
        case DOC_TRAINER_NAME:
          docTrain();
        case CROSS_PARSER_NAME:
            crossValidate();
            break;
        }
    } catch (final ArgumentParserException e) {
      this.argParser.handleError(e);
      System.out.println("Run java -jar target/ixa-pipe-ml-" + this.version
          + "-exec.jar (" + SEQ_TRAINER_NAME + "|" + PARSE_TRAINER_NAME
          + "|" + DOC_TRAINER_NAME + "|tokeval|eval|parseval|cross) -help for details");
      System.exit(1);
    }
  }

  /**
   * Main access to the Sequence Labeler train functionalities.
   *
   * @throws IOException
   *           input output exception if problems with corpora
   */
  private void seqTrain() throws IOException {

    // load training parameters file
    final String paramFile = this.parsedArguments.getString("params");
    final TrainingParameters params = IOUtils.loadTrainingParameters(paramFile);
    String outModel = null;
    if (params.getSettings().get("OutputModel") == null
        || params.getSettings().get("OutputModel").length() == 0) {
      outModel = Files.getNameWithoutExtension(paramFile) + ".bin";
      params.put("OutputModel", outModel);
    } else {
      outModel = Flags.getModel(params);
    }
    final SequenceLabelerTrainer nercTrainer = new SequenceLabelerTrainer(
        params);
    final SequenceLabelerModel trainedModel = nercTrainer.train(params);
    CmdLineUtil.writeModel("ixa-pipe-ml", new File(outModel), trainedModel);
  }

  /**
   * Main access to the ShiftReduceParser train functionalities.
   *
   * @throws IOException
   *           input output exception if problems with corpora
   */
  private void parserTrain() throws IOException {

    // load training parameters file
    final String paramFile = this.parsedArguments.getString("params");
    final String taggerParamsFile = this.parsedArguments
        .getString("taggerParams");
    final String chunkerParamsFile = this.parsedArguments
        .getString("chunkerParams");
    final TrainingParameters params = IOUtils.loadTrainingParameters(paramFile);
    final TrainingParameters chunkerParams = IOUtils
        .loadTrainingParameters(chunkerParamsFile);
    ParserModel trainedModel;
    String outModel;
      if (params.getSettings().get("OutputModel") == null
        || params.getSettings().get("OutputModel").length() == 0) {
      outModel = Files.getNameWithoutExtension(paramFile) + ".bin";
      params.put("OutputModel", outModel);
    } else {
      outModel = Flags.getModel(params);
    }
    if (taggerParamsFile.endsWith(".bin")) {
      final InputStream posModel = new FileInputStream(taggerParamsFile);
      final ShiftReduceParserTrainer parserTrainer = new ShiftReduceParserTrainer(
          params, chunkerParams);
      trainedModel = parserTrainer.train(params, posModel, chunkerParams);
    } else {
      final TrainingParameters taggerParams = IOUtils
          .loadTrainingParameters(taggerParamsFile);
      final ShiftReduceParserTrainer parserTrainer = new ShiftReduceParserTrainer(
          params, taggerParams, chunkerParams);
      trainedModel = parserTrainer.train(params, taggerParams, chunkerParams);
    }
    CmdLineUtil.writeModel("ixa-pipe-ml", new File(outModel), trainedModel);
  }
  
  /* Main access to the Sequence Labeler train functionalities.
    *
    * @throws IOException
    *           input output exception if problems with corpora
    */
   private void docTrain() throws IOException {
      // load training parameters file
      final String paramFile = this.parsedArguments.getString("params");
      final TrainingParameters params = IOUtils.loadTrainingParameters(paramFile);
      String outModel = null;
      if (params.getSettings().get("OutputModel") == null
          || params.getSettings().get("OutputModel").length() == 0) {
        outModel = Files.getNameWithoutExtension(paramFile) + ".bin";
        params.put("OutputModel", outModel);
      } else {
        outModel = Flags.getModel(params);
      }
      final DocumentClassifierTrainer docTrainer = new DocumentClassifierTrainer(
          params);
      final DocumentClassifierModel trainedModel = docTrainer.train(params);
      CmdLineUtil.writeModel("ixa-pipe-ml", new File(outModel), trainedModel);
    }
    
  
  /**
   * Main evaluation entry point for sequence labelling.
   *
   * @throws IOException
   *           throws exception if test set not available
   */
  public final void tokeval() throws IOException {

    final String lang = this.parsedArguments.getString("language");
    final String testset = this.parsedArguments.getString("testset");
    final Properties props = setTokevalProperties(lang, testset);
    final TokenizerEvaluate evaluator = new TokenizerEvaluate(
        props);
    //evaluator.evaluateAccuracy();
  }

  /**
   * Main evaluation entry point for sequence labelling.
   *
   * @throws IOException
   *           throws exception if test set not available
   */
  private void eval() throws IOException {

    final String metric = this.parsedArguments.getString("metric");
    final String lang = this.parsedArguments.getString("language");
    final String model = this.parsedArguments.getString("model");
    final String testset = this.parsedArguments.getString("testset");
    final String corpusFormat = this.parsedArguments.getString("corpusFormat");
    final String netypes = this.parsedArguments.getString("types");
    final String clearFeatures = this.parsedArguments
        .getString("clearFeatures");
    final String unknownAccuracy = this.parsedArguments
        .getString("unknownAccuracy");
    final Properties props = setEvalProperties(lang, model, testset,
        corpusFormat, netypes, clearFeatures, unknownAccuracy);
    final SequenceLabelerEvaluate evaluator = new SequenceLabelerEvaluate(
        props);

    if (metric.equalsIgnoreCase("accuracy")) {
      evaluator.evaluateAccuracy();
    } else {
      if (this.parsedArguments.getString("evalReport") != null) {
        if (this.parsedArguments.getString("evalReport")
            .equalsIgnoreCase("brief")) {
          evaluator.evaluate();
        } else if (this.parsedArguments.getString("evalReport")
            .equalsIgnoreCase("error")) {
          evaluator.evalError();
        } else if (this.parsedArguments.getString("evalReport")
            .equalsIgnoreCase("detailed")) {
          evaluator.detailEvaluate();
        }
      } else {
        evaluator.detailEvaluate();
      }
    }
  }

  /**
   * Main evaluation entry point.
   *
   * @throws IOException
   *           throws exception if test set not available
   */
  private void parseval() throws IOException {

    final String lang = this.parsedArguments.getString("language");
    final String model = this.parsedArguments.getString("model");
    final String testset = this.parsedArguments.getString("testset");
    final Properties props = setParsevalProperties(lang, model, testset);

    final ParserEvaluate parserEvaluator = new ParserEvaluate(props);
    parserEvaluator.evaluate();

  }

  /**
   * Main access to the cross validation.
   *
   * @throws IOException
   *           input output exception if problems with corpora
   */
  private void crossValidate() throws IOException {

    final String paramFile = this.parsedArguments.getString("params");
    final TrainingParameters params = IOUtils.loadTrainingParameters(paramFile);
    final CrossValidator crossValidator = new CrossValidator(params);
    crossValidator.crossValidate(params);
  }

  /**
   * Create the main parameters available for training sequence labeling models.
   */
  private void loadSeqLabelerTrainingParameters() {
    this.seqTrainerParser.addArgument("-p", "--params").required(true)
        .help("Load the training parameters file\n");
  }

  /**
   * Create the main parameters available for training ShiftReduceParse models.
   */
  private void loadParserTrainingParameters() {
    this.parserTrainerParser.addArgument("-p", "--params").required(true)
        .help("Load the parsing training parameters file.\n");
    this.parserTrainerParser.addArgument("-t", "--taggerParams").required(true)
        .help("Load the tagger training parameters file or an already trained POS tagger model.\n");
    this.parserTrainerParser.addArgument("-c", "--chunkerParams")
        .required(true).help("Load the chunker training parameters file.\n");
  }
  
  /**
     * Create the main parameters available for training document classification models.
     */
    private void loadDocTrainingParameters() {
     this.docTrainerParser.addArgument("-p", "--params").required(true)
          .help("Load the training parameters file\n");
    }
  
  /**
   * Create the parameters available for evaluation.
   */
  private void loadTokevalParameters() {
    this.tokevalParser.addArgument("-l", "--language").required(true)
        .choices("ca", "de", "en", "es", "eu", "fr", "it")
        .help("Choose language.\n");
    this.tokevalParser.addArgument("-t", "--testset").required(true)
        .help("The test or reference corpus.\n");
  }


  /**
   * Create the parameters available for evaluation.
   */
  private void loadEvalParameters() {
    this.evalParser.addArgument("--metric").required(false)
        .choices("accuracy", "fmeasure").setDefault("fmeasure").help(
            "Choose evaluation metric for Sequence Labeler; it defaults to fmeasure.\n");
    this.evalParser.addArgument("-l", "--language").required(true)
        .choices("de", "en", "es", "eu", "it", "nl").help("Choose language.\n");
    this.evalParser.addArgument("-m", "--model").required(false)
        .setDefault(Flags.DEFAULT_EVALUATE_MODEL)
        .help("Pass the model to evaluate as a parameter.\n");
    this.evalParser.addArgument("-t", "--testset").required(true)
        .help("The test or reference corpus.\n");
    this.evalParser.addArgument("--clearFeatures").required(false)
        .choices("yes", "no", "docstart").setDefault(Flags.DEFAULT_FEATURE_FLAG)
        .help("Reset the adaptive features; defaults to 'no'.\n");
    this.evalParser.addArgument("-f", "--corpusFormat").required(false)
        .choices("conll02", "conll03", "lemmatizer", "tabulated")
        .setDefault(Flags.DEFAULT_EVAL_FORMAT).help(
            "Choose format of reference corpus; it defaults to conll02 format.\n");
    this.evalParser.addArgument("--evalReport").required(false)
        .choices("brief", "detailed", "error").help(
            "Choose level of detail of evaluation report; it defaults to detailed evaluation.\n");
    this.evalParser.addArgument("--types").required(false)
        .setDefault(Flags.DEFAULT_SEQUENCE_TYPES).help(
            "Choose which Sequence types used for evaluation; the argument must be a comma separated"
                + " string; e.g., 'person,organization'.\n");
    this.evalParser.addArgument("-u", "--unknownAccuracy").required(false)
        .setDefault(Flags.DEFAULT_FEATURE_FLAG).help(
            "Pass the model training set to evaluate unknown and known word accuracy.\n");
  }

  /**
   * Create the parameters available for evaluation.
   */
  private void loadParsevalParameters() {
    this.parsevalParser.addArgument("-l", "--language").required(true)
        .choices("ca", "de", "en", "es", "eu", "fr", "it")
        .help("Choose language.\n");
    this.parsevalParser.addArgument("-m", "--model").required(false)
        .setDefault(Flags.DEFAULT_EVALUATE_MODEL)
        .help("Pass the model to evaluate as a parameter.\n");
    this.parsevalParser.addArgument("-t", "--testset").required(true)
        .help("The test or reference corpus.\n");
    this.parsevalParser.addArgument("--clearFeatures").required(false)
        .choices("yes", "no", "docstart").setDefault(Flags.DEFAULT_FEATURE_FLAG)
        .help("Reset the adaptive features; defaults to 'no'.\n");
  }

  /**
   * Create the main parameters available for training NERC models.
   */
  private void loadCrossValidateParameters() {
    this.crossValidateParser.addArgument("-p", "--params").required(true)
        .help("Load the Cross validation parameters file\n");
  }

  /**
   * Set a Properties object with the CLI parameters for evaluation.
   * 
   * @param model
   *          the model parameter
   * @param testset
   *          the reference set
   * @param corpusFormat
   *          the format of the testset
   * @param netypes
   *          the ne types to use in the evaluation
   * @return the properties object
   */
  private Properties setEvalProperties(final String language,
      final String model, final String testset, final String corpusFormat,
      final String netypes, final String clearFeatures,
      final String unknownAccuracy) {
    final Properties evalProperties = new Properties();
    evalProperties.setProperty("language", language);
    evalProperties.setProperty("model", model);
    evalProperties.setProperty("testset", testset);
    evalProperties.setProperty("corpusFormat", corpusFormat);
    evalProperties.setProperty("types", netypes);
    evalProperties.setProperty("clearFeatures", clearFeatures);
    evalProperties.setProperty("unknownAccuracy", unknownAccuracy);
    return evalProperties;
  }

  /**
   * Set a Properties object with the CLI parameters for evaluation.
   * 
   * @param model
   *          the model parameter
   * @param testset
   *          the reference set
   * @return the properties object
   */
  private Properties setParsevalProperties(final String language,
      final String model, final String testset) {
    final Properties parsevalProperties = new Properties();
    parsevalProperties.setProperty("language", language);
    parsevalProperties.setProperty("model", model);
    parsevalProperties.setProperty("testset", testset);
    return parsevalProperties;
  }
  
  private Properties setTokevalProperties(final String language, final String testset) {
    final Properties parsevalProperties = new Properties();
    parsevalProperties.setProperty("language", language);
    parsevalProperties.setProperty("testset", testset);
    return parsevalProperties;
  }

}
