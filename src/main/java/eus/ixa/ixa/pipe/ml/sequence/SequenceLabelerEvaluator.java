/*
 *Copyright 2016 Rodrigo Agerri

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

package eus.ixa.ixa.pipe.ml.sequence;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import eus.ixa.ixa.pipe.ml.utils.Flags;
import eus.ixa.ixa.pipe.ml.utils.Span;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.eval.Evaluator;
import opennlp.tools.util.eval.FMeasure;
import opennlp.tools.util.eval.Mean;

/**
 * The {@link SequenceLabelerEvaluator} measures the F1 performance of the given
 * {@link SequenceLabeler} with the provided reference
 * {@link SequenceLabelSample}s. Based on Apache OpenNLP evaluators.
 *
 * @see Evaluator
 * @see SequenceLabeler
 * @see SequenceLabelSample
 */
public class SequenceLabelerEvaluator extends Evaluator<SequenceLabelSample> {

  private final FMeasure fmeasure = new FMeasure();
  private final Mean wordAccuracy = new Mean();
  private final Mean sentenceAccuracy = new Mean();
  private final Mean unknownAccuracy = new Mean();
  private final Mean knownAccuracy = new Mean();
  private final Set<String> knownWords = new HashSet<>();

  /**
   * The {@link SequenceLabeler} used to create the predicted
   * {@link SequenceLabelSample} objects.
   */
  private final SequenceLabeler sequenceLabeler;
  private String corpusFormat = Flags.DEFAULT_EVAL_FORMAT;

  public SequenceLabelerEvaluator(final SequenceLabeler seqLabeler,
      final SequenceLabelerEvaluationMonitor... listeners) {
    super(listeners);
    this.sequenceLabeler = seqLabeler;
  }

  public SequenceLabelerEvaluator(
      final ObjectStream<SequenceLabelSample> trainSamples,
      final String aCorpusFormat, final SequenceLabeler seqLabeler,
      final SequenceLabelerEvaluationMonitor... listeners) {
    super(listeners);
    this.corpusFormat = aCorpusFormat;
    this.sequenceLabeler = seqLabeler;
    if (trainSamples != null) {
      getKnownWords(trainSamples);
    }
  }

  /**
   * Evaluates the given reference {@link SequenceLabelSample} object.
   *
   * This is done by finding the sequences with the {@link SequenceLabeler} in
   * the sentence from the reference {@link SequenceLabelSample}. The found
   * sequences are then used to calculate and update the scores.
   *
   * @param reference
   *          the reference {@link SequenceLabelSample}.
   *
   * @return the predicted {@link SequenceLabelSample}.
   */
  @Override
  protected SequenceLabelSample processSample(
      final SequenceLabelSample reference) {

    if (reference.isClearAdaptiveDataSet()) {
      this.sequenceLabeler.clearAdaptiveData();
    }
    final String[] referenceTokens = reference.getTokens();
    final Span[] predictedNames = this.sequenceLabeler.tag(referenceTokens);
    final Span[] references = reference.getSequences();
    // OPENNLP-396 When evaluating with a file in the old format
    // the type of the span is null, but must be set to default to match
    // the output of the name finder.
    for (int i = 0; i < references.length; i++) {
      if (references[i].getType() == null) {
        references[i] = new Span(references[i].getStart(),
            references[i].getEnd(), "default");
      }
    }
    if (this.corpusFormat.equalsIgnoreCase("lemmatizer")
        || this.corpusFormat.equalsIgnoreCase("tabulated")) {
      updateAccuracyScores(referenceTokens, references, predictedNames);
    }
    this.fmeasure.updateScores(references, predictedNames);
    return new SequenceLabelSample(referenceTokens, predictedNames,
        reference.isClearAdaptiveDataSet());
  }

  public FMeasure getFMeasure() {
    return this.fmeasure;
  }

  /**
   * Retrieves the word accuracy.
   *
   * This is defined as: word accuracy = correctly detected tags / total words
   *
   * @return the word accuracy
   */
  public double getWordAccuracy() {
    return this.wordAccuracy.mean();
  }

  public double getSentenceAccuracy() {
    return this.sentenceAccuracy.mean();
  }

  public double getUnknownWordAccuracy() {
    return this.unknownAccuracy.mean();
  }

  public double getKnownAccuracy() {
    return this.knownAccuracy.mean();
  }

  /**
   * Retrieves the total number of words considered in the evaluation.
   *
   * @return the word count
   */
  public long getWordCount() {
    return this.wordAccuracy.count();
  }

  public void updateAccuracyScores(final String[] referenceTokens,
      final Object[] references, final Object[] predictions) {

    int fails = 0;
    for (int i = 0; i < references.length; i++) {
      final boolean isKnown = checkWordInSeenData(referenceTokens[i]);
      if (references[i].equals(predictions[i])) {
        this.wordAccuracy.add(1);
        if (isKnown) {
          this.knownAccuracy.add(1);
        } else {
          this.unknownAccuracy.add(1);
        }
      } else {
        this.wordAccuracy.add(0);
        fails++;
        if (isKnown) {
          this.knownAccuracy.add(0);
        } else {
          this.unknownAccuracy.add(0);
        }
      }
    }
    if (fails > 0) {
      this.sentenceAccuracy.add(0);
    } else {
      this.sentenceAccuracy.add(1);
    }
  }

  private boolean checkWordInSeenData(final String referenceToken) {
    boolean isKnown = false;
    if (!this.knownWords.isEmpty()) {
      if (this.knownWords.contains(referenceToken)) {
        isKnown = true;
      } else {
        isKnown = false;
      }
    }
    return isKnown;
  }

  private void getKnownWords(
      final ObjectStream<SequenceLabelSample> trainSamples) {
    SequenceLabelSample sample;
    try {
      while ((sample = trainSamples.read()) != null) {
        for (final String token : sample.getTokens()) {
          this.knownWords.add(token);
        }
      }
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }

}
