/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eus.ixa.ixa.pipe.ml.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import eus.ixa.ixa.pipe.ml.utils.Span;
import opennlp.tools.util.eval.Evaluator;
import opennlp.tools.util.eval.FMeasure;

/**
 * Class for ParserEvaluator. This ParserEvaluator behaves like EVALB with no
 * exceptions, e.g, without removing punctuation tags, or equality between ADVP
 * and PRT (as in COLLINS convention). To follow parsing evaluation conventions
 * (Bikel, Collins, Charniak, etc.) as in EVALB, options are to be added to the
 * {@code ParserEvaluatorTool}.
 *
 */
public class ParserEvaluator extends Evaluator<Parse> {

  /**
   * fmeasure.
   */
  private final FMeasure fmeasure = new FMeasure();
  /**
   * The parser to evaluate.
   */
  private final ShiftReduceParser parser;

  /**
   * Construct a parser with some evaluation monitors.
   * 
   * @param aParser
   *          the parser
   * @param monitors
   *          the evaluation monitors
   */
  public ParserEvaluator(final ShiftReduceParser aParser,
      final ParserEvaluationMonitor... monitors) {
    super(monitors);
    this.parser = aParser;
  }

  /**
   * Obtain {@code Span}s for every parse in the sentence.
   * 
   * @param parse
   *          the parse from which to obtain the spans
   * @return an array containing every span for the parse
   */
  private static Span[] getConstituencySpans(final Parse parse) {

    final Stack<Parse> stack = new Stack<Parse>();

    if (parse.getChildCount() > 0) {
      for (final Parse child : parse.getChildren()) {
        stack.push(child);
      }
    }
    final List<Span> consts = new ArrayList<Span>();

    while (!stack.isEmpty()) {

      final Parse constSpan = stack.pop();

      if (!constSpan.isPosTag()) {
        final Span span = constSpan.getSpan();
        consts
            .add(new Span(span.getStart(), span.getEnd(), constSpan.getType()));

        for (final Parse child : constSpan.getChildren()) {
          stack.push(child);
        }
      }
    }

    return consts.toArray(new Span[consts.size()]);
  }

  /*
   * (non-Javadoc)
   * 
   * @see opennlp.tools.util.eval.Evaluator#processSample(java.lang.Object)
   */
  @Override
  protected final Parse processSample(final Parse reference) {

    final String sentenceText = reference.getText();

    final Parse[] predictions = ShiftReduceParser.parseLine(sentenceText,
        this.parser, 1);

    Parse prediction = null;
    if (predictions.length > 0) {
      prediction = predictions[0];
      // System.err.println("-> Prediction: " + prediction.getType());
    }

    this.fmeasure.updateScores(getConstituencySpans(reference),
        getConstituencySpans(prediction));

    return prediction;
  }

  /**
   * It returns the fmeasure result.
   * 
   * @return the fmeasure value
   */
  public final FMeasure getFMeasure() {
    return this.fmeasure;
  }

  /**
   * Main method to show the example of running the evaluator. Moved to a test
   * case soon, hopefully.
   * 
   * @param args
   *          arguments
   */
  // TODO: Move this to a test case!
  public static void main(final String[] args) {

    final String goldParseString = "(TOP (S (NP (NNS Sales) (NNS executives)) (VP (VBD were) (VP (VBG examing) (NP (DT the) (NNS figures)) (PP (IN with) (NP (JJ great) (NN care))) ))  (NP (NN yesterday)) (. .) ))";
    final Span[] goldConsts = getConstituencySpans(
        Parse.parseParse(goldParseString));

    final String testParseString = "(TOP (S (NP (NNS Sales) (NNS executives)) (VP (VBD were) (VP (VBG examing) (NP (DT the) (NNS figures)) (PP (IN with) (NP (JJ great) (NN care) (NN yesterday))) ))  (. .) ))";
    final Span[] testConsts = getConstituencySpans(
        Parse.parseParse(testParseString));

    final FMeasure measure = new FMeasure();
    measure.updateScores(goldConsts, testConsts);

    // Expected output:
    // Precision: 0.42857142857142855
    // Recall: 0.375
    // F-Measure: 0.39999999999999997

    System.out.println(measure.toString());
  }
}