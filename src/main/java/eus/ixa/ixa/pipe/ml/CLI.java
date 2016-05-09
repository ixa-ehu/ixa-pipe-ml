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
import java.io.IOException;
import java.util.Properties;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;
import opennlp.tools.cmdline.CmdLineUtil;
import opennlp.tools.util.TrainingParameters;

import com.google.common.io.Files;

import eus.ixa.ixa.pipe.ml.eval.CrossValidator;
import eus.ixa.ixa.pipe.ml.eval.Evaluate;
import eus.ixa.ixa.pipe.ml.parse.ParserModel;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerModel;
import eus.ixa.ixa.pipe.ml.utils.Flags;
import eus.ixa.ixa.pipe.ml.utils.IOUtils;

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
   * Get dynamically the version of ixa-pipe-ml by looking at the MANIFEST
   * file.
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
  private ArgumentParser argParser = ArgumentParsers.newArgumentParser(
      "ixa-pipe-ml-" + version + "-exec.jar").description(
      "ixa-pipe-ml-" + version
          + " is a Machine Learning component to train and evaluate models for various IXA pipes tasks.\n");
  /**
   * Sub parser instance.
   */
  private Subparsers subParsers = argParser.addSubparsers().help(
      "sub-command help");
  /**
   * The parser that manages the Sequence Labeler training sub-command.
   */
  private Subparser seqTrainerParser;
  /**
   * The subparser that manages the Constituent Parser training sub-command.
   */
  private Subparser parserTrainerParser;
  /**
   * The parser that manages the evaluation sub-command.
   */
  private Subparser evalParser;
  /**
   * The parser that manages the cross validation sub-command.
   */
  private Subparser crossValidateParser;
 
  public static final String SEQ_TRAINER_NAME = "sequenceTrainer";
  public static final String PARSE_TRAINER_NAME = "parserTrainer";
  public static final String EVAL_PARSER_NAME = "eval";
  public static final String CROSS_PARSER_NAME = "cross";
  /**
   * Construct a CLI object with the sub-parsers to manage the command
   * line parameters.
   */
  public CLI() {
    
    seqTrainerParser = subParsers.addParser(SEQ_TRAINER_NAME).help("Sequence Labeler training CLI");
    loadSeqLabelerTrainingParameters();
    parserTrainerParser = subParsers.addParser(PARSE_TRAINER_NAME).help("Constituent Parser training CLI");
    loadParserTrainingParameters();
    evalParser = subParsers.addParser(EVAL_PARSER_NAME).help("Evaluation CLI");
    loadEvalParameters();
    crossValidateParser = subParsers.addParser(CROSS_PARSER_NAME).help("Cross validation CLI");
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

    CLI cmdLine = new CLI();
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
  public final void parseCLI(final String[] args) throws IOException {
    try {
      parsedArguments = argParser.parseArgs(args);
      System.err.println("CLI options: " + parsedArguments);
      if (args[0].equals(EVAL_PARSER_NAME)) {
        eval();
      } else if (args[0].equals(SEQ_TRAINER_NAME)) {
        seqTrain();
      } else if (args[0].equals(PARSE_TRAINER_NAME)) {
        parserTrain();
      } else if (args[0].equals("CROSS_PARSER_NAME")) {
        crossValidate();
      }
    } catch (ArgumentParserException e) {
      argParser.handleError(e);
      System.out.println("Run java -jar target/ixa-pipe-ml-" + version
          + "-exec.jar (" + SEQ_TRAINER_NAME + "|" + PARSE_TRAINER_NAME + "|eval|cross) -help for details");
      System.exit(1);
    }
  }
  
  /**
   * Main access to the Sequence Labeler train functionalities.
   * 
   * @throws IOException
   *           input output exception if problems with corpora
   */
  public final void seqTrain() throws IOException {

    // load training parameters file
    String paramFile = parsedArguments.getString("params");
    TrainingParameters params = IOUtils
        .loadTrainingParameters(paramFile);
    String outModel = null;
    if (params.getSettings().get("OutputModel") == null || params.getSettings().get("OutputModel").length() == 0) {
      outModel = Files.getNameWithoutExtension(paramFile) + ".bin";
      params.put("OutputModel", outModel);
    }
    else {
      outModel = Flags.getModel(params);
    }
    SequenceLabelerTrainer nercTrainer = new SequenceLabelerTrainer(params);
    SequenceLabelerModel trainedModel = nercTrainer.train(params);
    CmdLineUtil.writeModel("ixa-pipe-ml", new File(outModel), trainedModel);
  }
  
  /**
   * Main access to the ShiftReduceParser train functionalities.
   * 
   * @throws IOException
   *           input output exception if problems with corpora
   */
  public final void parserTrain() throws IOException {

    // load training parameters file
    String paramFile = parsedArguments.getString("params");
    String taggerParamsFile = parsedArguments.getString("taggerParams");
    String chunkerParamsFile = parsedArguments.getString("chunkerParams");
    TrainingParameters params = IOUtils
        .loadTrainingParameters(paramFile);
    TrainingParameters taggerParams = IOUtils.loadTrainingParameters(taggerParamsFile);
    TrainingParameters chunkerParams = IOUtils.loadTrainingParameters(chunkerParamsFile);
    String outModel = null;
    if (params.getSettings().get("OutputModel") == null || params.getSettings().get("OutputModel").length() == 0) {
      outModel = Files.getNameWithoutExtension(paramFile) + ".bin";
      params.put("OutputModel", outModel);
    }
    else {
      outModel = Flags.getModel(params);
    }
    ShiftReduceParserTrainer parserTrainer = new ShiftReduceParserTrainer(params, taggerParams, chunkerParams);
    ParserModel trainedModel = parserTrainer.train(params);
    CmdLineUtil.writeModel("ixa-pipe-ml", new File(outModel), trainedModel);
  }

  /**
   * Main evaluation entry point.
   * 
   * @throws IOException
   *           throws exception if test set not available
   */
  public final void eval() throws IOException {

    String lang = parsedArguments.getString("language");
    String model = parsedArguments.getString("model");
    String testset = parsedArguments.getString("testset");
    String corpusFormat = parsedArguments.getString("corpusFormat");
    String netypes = parsedArguments.getString("types");
    String clearFeatures = parsedArguments.getString("clearFeatures");
    Properties props = setEvalProperties(lang, model, testset, corpusFormat, netypes, clearFeatures);
    
      Evaluate evaluator = new Evaluate(props);
      if (parsedArguments.getString("evalReport") != null) {
        if (parsedArguments.getString("evalReport").equalsIgnoreCase("brief")) {
          evaluator.evaluate();
        } else if (parsedArguments.getString("evalReport").equalsIgnoreCase(
            "error")) {
          evaluator.evalError();
        } else if (parsedArguments.getString("evalReport").equalsIgnoreCase(
            "detailed")) {
          evaluator.detailEvaluate();
        }
      } else {
        evaluator.detailEvaluate();
      }
  }
  
  /**
   * Main access to the cross validation.
   * 
   * @throws IOException
   *           input output exception if problems with corpora
   */
  public final void crossValidate() throws IOException {

    String paramFile = parsedArguments.getString("params");
    TrainingParameters params = IOUtils
        .loadTrainingParameters(paramFile);
    CrossValidator crossValidator = new CrossValidator(params);
    crossValidator.crossValidate(params);
  }
 
  /**
   * Create the main parameters available for training sequence labeling models.
   */
  private void loadSeqLabelerTrainingParameters() {
    seqTrainerParser.addArgument("-p", "--params")
        .required(true)
        .help("Load the training parameters file\n");
  }
  
  /**
   * Create the main parameters available for training ShiftReduceParse models.
   */
  private void loadParserTrainingParameters() {
    parserTrainerParser.addArgument("-p", "--params")
        .required(true)
        .help("Load the parsing training parameters file.\n");
    parserTrainerParser.addArgument("-t", "--taggerParams")
        .required(false)
        .help("Load the tagger training parameters file.\n");
    parserTrainerParser.addArgument("-c", "--chunkerParams")
        .required(false)
        .help("Load the chunker training parameters file.\n");
  }

  /**
   * Create the parameters available for evaluation.
   */
  private void loadEvalParameters() {
    evalParser.addArgument("-l", "--language")
        .required(true)
        .choices("de", "en", "es", "eu", "it", "nl")
        .help("Choose language.\n");
    evalParser.addArgument("-m", "--model")
        .required(false)
        .setDefault(Flags.DEFAULT_EVALUATE_MODEL)
        .help("Pass the model to evaluate as a parameter.\n");
    evalParser.addArgument("-t", "--testset")
        .required(true)
        .help("The test or reference corpus.\n");
    evalParser.addArgument("--clearFeatures")
        .required(false)
        .choices("yes", "no", "docstart")
        .setDefault(Flags.DEFAULT_FEATURE_FLAG)
        .help("Reset the adaptive features; defaults to 'no'.\n");
    evalParser.addArgument("-f","--corpusFormat")
        .required(false)
        .choices("conll02", "conll03", "lemmatizer", "tabulated")
        .setDefault(Flags.DEFAULT_EVAL_FORMAT)
        .help("Choose format of reference corpus; it defaults to conll02 format.\n");
    evalParser.addArgument("--evalReport")
        .required(false)
        .choices("brief", "detailed", "error")
        .help("Choose level of detail of evaluation report; it defaults to detailed evaluation.\n");
    evalParser.addArgument("--types")
        .required(false)
        .setDefault(Flags.DEFAULT_SEQUENCE_TYPES)
        .help("Choose which Sequence types used for evaluation; the argument must be a comma separated" +
        		" string; e.g., 'person,organization'.\n");
            
  }
  
  /**
   * Create the main parameters available for training NERC models.
   */
  private void loadCrossValidateParameters() {
    crossValidateParser.addArgument("-p", "--params").required(true)
        .help("Load the Cross validation parameters file\n");
  }
  
  
  /**
   * Set a Properties object with the CLI parameters for evaluation.
   * @param model the model parameter
   * @param testset the reference set
   * @param corpusFormat the format of the testset
   * @param netypes the ne types to use in the evaluation
   * @return the properties object
   */
  private Properties setEvalProperties(String language, String model, String testset, String corpusFormat, String netypes, String clearFeatures) {
    Properties evalProperties = new Properties();
    evalProperties.setProperty("language", language);
    evalProperties.setProperty("model", model);
    evalProperties.setProperty("testset", testset);
    evalProperties.setProperty("corpusFormat", corpusFormat);
    evalProperties.setProperty("types", netypes);
    evalProperties.setProperty("clearFeatures", clearFeatures);
    return evalProperties;
  }

}
