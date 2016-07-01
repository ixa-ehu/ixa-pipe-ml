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
import opennlp.tools.util.eval.Evaluator;
import opennlp.tools.util.eval.FMeasure;
import eus.ixa.ixa.pipe.ml.eval.Accuracy;
import eus.ixa.ixa.pipe.ml.utils.Span;

/**
 * The {@link SequenceLabelerEvaluator} measures the F1 performance
 * of the given {@link SequenceLabeler} with the provided
 * reference {@link SequenceLabelSample}s.
 *
 * @see Evaluator
 * @see SequenceLabeler
 * @see SequenceLabelSample
 */
public class SequenceLabelerEvaluator extends Evaluator<SequenceLabelSample> {

  private FMeasure fmeasure = new FMeasure();
  private Accuracy wordAccuracy = new Accuracy();
  private Accuracy sentenceAccuracy = new Accuracy();
  private Accuracy unknownWords = new Accuracy();
  private Accuracy knownWords = new Accuracy();


  /**
   * The {@link SequenceLabeler} used to create the predicted
   * {@link SequenceLabelSample} objects.
   */
  private SequenceLabeler sequenceLabeler;

  /**
   * Initializes the current instance with the given
   * {@link SequenceLabeler}.
   *
   * @param nameFinder the {@link SequenceLabeler} to evaluate.
   * @param listeners evaluation sample listeners
   */
  public SequenceLabelerEvaluator(SequenceLabeler nameFinder, SequenceLabelerEvaluationMonitor ... listeners) {
    super(listeners);
    this.sequenceLabeler = nameFinder;
  }

  /**
   * Evaluates the given reference {@link SequenceLabelSample} object.
   *
   * This is done by finding the sequences with the
   * {@link SequenceLabeler} in the sentence from the reference
   * {@link SequenceLabelSample}. The found sequences are then used to
   * calculate and update the scores.
   *
   * @param reference the reference {@link SequenceLabelSample}.
   *
   * @return the predicted {@link SequenceLabelSample}.
   */
  @Override
  protected SequenceLabelSample processSample(SequenceLabelSample reference) {

    if (reference.isClearAdaptiveDataSet()) {
      sequenceLabeler.clearAdaptiveData();
    }
    String[] referenceTokens = reference.getTokens();
    Span[] predictedNames = sequenceLabeler.tag(referenceTokens);
    Span[] references = reference.getSequences();
    // OPENNLP-396 When evaluating with a file in the old format
    // the type of the span is null, but must be set to default to match
    // the output of the name finder.
    for (int i = 0; i < references.length; i++) {
      if (references[i].getType() == null) {
        references[i] = new Span(references[i].getStart(), references[i].getEnd(), "default");
      }
    }
    //TODO add here training data, use two methods or the same one with a boolean?
    //if trainingData != null... then evaluate known/unknown
    //otherwise do not go there in the updateScores method
    updateScores(references, predictedNames);
    fmeasure.updateScores(references, predictedNames);
    return new SequenceLabelSample(reference.getTokens(), predictedNames, reference.isClearAdaptiveDataSet());
  }
  
  public FMeasure getFMeasure() {
    return fmeasure;
  }  

  /**
   * Retrieves the word accuracy.
   *
   * This is defined as:
   * word accuracy = correctly detected tags / total words
   *
   * @return the word accuracy
   */
  public double getWordAccuracy() {
    return wordAccuracy.mean();
  }
  
  public double getSentenceAccuracy() {
    return sentenceAccuracy.mean();
  }
  
  /**
   * Retrieves the total number of words considered
   * in the evaluation.
   *
   * @return the word count
   */
  public long getWordCount() {
    return wordAccuracy.count();
  }
  
  public void updateScores(final Object[] references, final Object[] predictions) {
    int fails = 0;
    for (int i = 0; i < references.length; i++) {
      if (references[i].equals(predictions[i])) {
        wordAccuracy.add(1);
      }
      else {
        wordAccuracy.add(0);
        fails++;
      }
    }
    if (fails > 0) {
      sentenceAccuracy.add(0);
    } else {
      sentenceAccuracy.add(1);
    }
  }

}

