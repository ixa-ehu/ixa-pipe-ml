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

package eus.ixa.ixa.pipe.ml.sequence;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import eus.ixa.ixa.pipe.ml.utils.Span;
import opennlp.tools.util.eval.EvaluationMonitor;

/**
 * <b>Note:</b> Do not use this class, internal use only!
 */
public abstract class EvaluationErrorPrinter<T>
    implements EvaluationMonitor<T> {

  private final PrintStream printStream;

  protected EvaluationErrorPrinter(final OutputStream outputStream) {
    this.printStream = new PrintStream(outputStream);
  }

  // for the sentence detector
  protected void printError(final Span references[], final Span predictions[],
      final T referenceSample, final T predictedSample, final String sentence) {
    final List<Span> falseNegatives = new ArrayList<Span>();
    final List<Span> falsePositives = new ArrayList<Span>();

    findErrors(references, predictions, falseNegatives, falsePositives);

    if (falsePositives.size() + falseNegatives.size() > 0) {

      printSamples(referenceSample, predictedSample);

      printErrors(falsePositives, falseNegatives, sentence);

    }
  }

  // for namefinder, chunker...
  protected void printError(final String id, final Span references[],
      final Span predictions[], final T referenceSample,
      final T predictedSample, final String[] sentenceTokens) {
    final List<Span> falseNegatives = new ArrayList<Span>();
    final List<Span> falsePositives = new ArrayList<Span>();

    findErrors(references, predictions, falseNegatives, falsePositives);

    if (falsePositives.size() + falseNegatives.size() > 0) {

      if (id != null) {
        this.printStream.println("Id: {" + id + "}");
      }

      printSamples(referenceSample, predictedSample);

      printErrors(falsePositives, falseNegatives, sentenceTokens);

    }
  }

  protected void printError(final Span references[], final Span predictions[],
      final T referenceSample, final T predictedSample,
      final String[] sentenceTokens) {
    printError(null, references, predictions, referenceSample, predictedSample,
        sentenceTokens);
  }

  // for pos tagger
  protected void printError(final String references[],
      final String predictions[], final T referenceSample,
      final T predictedSample, final String[] sentenceTokens) {
    final List<String> filteredDoc = new ArrayList<String>();
    final List<String> filteredRefs = new ArrayList<String>();
    final List<String> filteredPreds = new ArrayList<String>();

    for (int i = 0; i < references.length; i++) {
      if (!references[i].equals(predictions[i])) {
        filteredDoc.add(sentenceTokens[i]);
        filteredRefs.add(references[i]);
        filteredPreds.add(predictions[i]);
      }
    }

    if (filteredDoc.size() > 0) {

      printSamples(referenceSample, predictedSample);

      printErrors(filteredDoc, filteredRefs, filteredPreds);

    }
  }

  // for others
  protected void printError(final T referenceSample, final T predictedSample) {
    printSamples(referenceSample, predictedSample);
    this.printStream.println();
  }

  /**
   * Auxiliary method to print tag errors
   *
   * @param filteredDoc
   *          the document tokens which were tagged wrong
   * @param filteredRefs
   *          the reference tags
   * @param filteredPreds
   *          the predicted tags
   */
  private void printErrors(final List<String> filteredDoc,
      final List<String> filteredRefs, final List<String> filteredPreds) {
    this.printStream.println("Errors: {");
    this.printStream.println("Tok: Ref | Pred");
    this.printStream.println("---------------");
    for (int i = 0; i < filteredDoc.size(); i++) {
      this.printStream.println(filteredDoc.get(i) + ": " + filteredRefs.get(i)
          + " | " + filteredPreds.get(i));
    }
    this.printStream.println("}\n");
  }

  /**
   * Auxiliary method to print span errors
   *
   * @param falsePositives
   *          false positives span
   * @param falseNegatives
   *          false negative span
   * @param doc
   *          the document text
   */
  private void printErrors(final List<Span> falsePositives,
      final List<Span> falseNegatives, final String doc) {
    this.printStream.println("False positives: {");
    for (final Span span : falsePositives) {
      this.printStream.println(span.getCoveredText(doc));
    }
    this.printStream.println("} False negatives: {");
    for (final Span span : falseNegatives) {
      this.printStream.println(span.getCoveredText(doc));
    }
    this.printStream.println("}\n");
  }

  /**
   * Auxiliary method to print span errors
   *
   * @param falsePositives
   *          false positives span
   * @param falseNegatives
   *          false negative span
   * @param toks
   *          the document tokens
   */
  private void printErrors(final List<Span> falsePositives,
      final List<Span> falseNegatives, final String[] toks) {
    this.printStream.println("False positives: {");
    this.printStream.println(print(falsePositives, toks));
    this.printStream.println("} False negatives: {");
    this.printStream.println(print(falseNegatives, toks));
    this.printStream.println("}\n");
  }

  /**
   * Auxiliary method to print spans
   *
   * @param spans
   *          the span list
   * @param toks
   *          the tokens array
   * @return the spans as string
   */
  private String print(final List<Span> spans, final String[] toks) {
    return Arrays.toString(
        Span.spansToStrings(spans.toArray(new Span[spans.size()]), toks));
  }

  /**
   * Auxiliary method to print expected and predicted samples.
   *
   * @param referenceSample
   *          the reference sample
   * @param predictedSample
   *          the predicted sample
   */
  private <S> void printSamples(final S referenceSample,
      final S predictedSample) {
    final String details = "Expected: {\n" + referenceSample
        + "}\nPredicted: {\n" + predictedSample + "}";
    this.printStream.println(details);
  }

  /**
   * Outputs falseNegatives and falsePositives spans from the references and
   * predictions list.
   *
   * @param references
   * @param predictions
   * @param falseNegatives
   *          [out] the false negatives list
   * @param falsePositives
   *          [out] the false positives list
   */
  private void findErrors(final Span references[], final Span predictions[],
      final List<Span> falseNegatives, final List<Span> falsePositives) {

    falseNegatives.addAll(Arrays.asList(references));
    falsePositives.addAll(Arrays.asList(predictions));

    for (final Span referenceName : references) {

      for (final Span prediction : predictions) {
        if (referenceName.equals(prediction)) {
          // got it, remove from fn and fp
          falseNegatives.remove(referenceName);
          falsePositives.remove(prediction);
        }
      }
    }
  }

  @Override
  public void correctlyClassified(final T reference, final T prediction) {
    // do nothing
  }

  @Override
  public abstract void missclassified(T reference, T prediction);

}
