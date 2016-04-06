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
import eus.ixa.ixa.pipe.ml.utils.Span;
import opennlp.tools.util.eval.Evaluator;
import opennlp.tools.util.eval.FMeasure;
import opennlp.tools.util.eval.Mean;

/**
 * The {@link SequenceLabelerEvaluator} measures the performance
 * of the given {@link SequenceLabeler} with the provided
 * reference {@link SequenceSample}s.
 *
 * @see Evaluator
 * @see SequenceLabeler
 * @see SequenceSample
 */
public class SequenceLabelerEvaluator extends Evaluator<SequenceSample> {

  private FMeasure fmeasure = new FMeasure();
  private Mean wordAccuracy = new Mean();


  /**
   * The {@link SequenceLabeler} used to create the predicted
   * {@link SequenceSample} objects.
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
   * Evaluates the given reference {@link SequenceSample} object.
   *
   * This is done by finding the sequneces with the
   * {@link SequenceLabeler} in the sentence from the reference
   * {@link SequenceSample}. The found sequences are then used to
   * calculate and update the scores.
   *
   * @param reference the reference {@link SequenceSample}.
   *
   * @return the predicted {@link SequenceSample}.
   */
  @Override
  protected SequenceSample processSample(SequenceSample reference) {

    if (reference.isClearAdaptiveDataSet()) {
      sequenceLabeler.clearAdaptiveData();
    }

    Span[] predictedNames = sequenceLabeler.tag(reference.getTokens());
    Span[] references = reference.getSequences();
    /*String[] predictedTags = StringUtils.getTagsFromSpan(predictedNames, reference.getTokens());
    String[] referenceTags = StringUtils.getTagsFromSpan(references, reference.getTokens());
    //TODO split word accuracy and F-measure depending on the task?
    for (int i = 0; i < referenceTags.length; i++) {
      if (referenceTags[i].equals(predictedTags[i])) {
        wordAccuracy.add(1);
      }
      else {
        wordAccuracy.add(0);
      }
    }*/
    // OPENNLP-396 When evaluating with a file in the old format
    // the type of the span is null, but must be set to default to match
    // the output of the name finder.
    for (int i = 0; i < references.length; i++) {
      if (references[i].getType() == null) {
        references[i] = new Span(references[i].getStart(), references[i].getEnd(), "default");
      }
    }
    fmeasure.updateScores(references, predictedNames);
    return new SequenceSample(reference.getTokens(), predictedNames, reference.isClearAdaptiveDataSet());
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
  
  /**
   * Retrieves the total number of words considered
   * in the evaluation.
   *
   * @return the word count
   */
  public long getWordCount() {
    return wordAccuracy.count();
  }
}

