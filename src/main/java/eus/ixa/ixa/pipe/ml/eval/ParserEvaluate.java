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

import opennlp.tools.util.ObjectStream;
import eus.ixa.ixa.pipe.ml.ShiftReduceParserTrainer;
import eus.ixa.ixa.pipe.ml.parse.Parse;
import eus.ixa.ixa.pipe.ml.parse.ParserEvaluator;
import eus.ixa.ixa.pipe.ml.parse.ParserModel;
import eus.ixa.ixa.pipe.ml.parse.ShiftReduceParser;

/**
 * Evaluation class mostly using ParserEvaluator.
 *
 * @author ragerri
 * @version 2015-02-24
 */
public class ParserEvaluate {

  /**
   * ObjectStream of the test data.
   */
  private ObjectStream<Parse> testSamples;
  ShiftReduceParser parser;
  
  /**
   * The models to use for every language. The keys of the hash are the
   * language codes, the values the models.
   */
  private static ConcurrentHashMap<String, ParserModel> parseModels =
      new ConcurrentHashMap<String, ParserModel>();
 
  /**
   * Construct an evaluator. It takes from the properties a model,
   * a testset and the format of the testset. Every other parameter
   * set in the training, e.g., beamsize, decoding, etc., is serialized
   * in the model.
   * @param props the properties parameter
   * @throws IOException the io exception
   */
  public ParserEvaluate(final Properties props) throws IOException {
    
    String lang = props.getProperty("language");
    String model = props.getProperty("model");
    String testSet = props.getProperty("testset");
    
    testSamples = ShiftReduceParserTrainer.getParseStream(testSet);
    parseModels.putIfAbsent(lang, new ParserModel(new FileInputStream(model)));
    parser = new ShiftReduceParser(parseModels.get(lang));
  }

  /**
   * Evaluate and print precision, recall and F measure.
   * @throws IOException if test corpus not loaded
   */
  public final void evaluate() throws IOException {
    ParserEvaluator evaluator = new ParserEvaluator(parser);
    evaluator.evaluate(testSamples);
    System.out.println(evaluator.getFMeasure());
  }

}

