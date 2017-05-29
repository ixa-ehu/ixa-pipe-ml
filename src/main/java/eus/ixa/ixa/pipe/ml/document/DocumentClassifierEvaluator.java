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

package eus.ixa.ixa.pipe.ml.document;

import opennlp.tools.util.eval.Evaluator;
import opennlp.tools.util.eval.Mean;

/**
 * The {@link DocumentClassifierEvaluator} measures the performance of
 * the given {@link DocumentClassifier} with the provided reference
 * {@link DocSample}s.
 *
 * @see DocumentClassifier
 * @see DocSample
 */
public class DocumentClassifierEvaluator extends Evaluator<DocSample> {

  private DocumentClassifier docClassifier;

  private Mean accuracy = new Mean();

  /**
   * Initializes the current instance.
   *
   * @param classifier the document classifier instance
   * @param listeners the listeners
   */
  public DocumentClassifierEvaluator(DocumentClassifier classifier,
      DocumentClassifierEvaluationMonitor ... listeners) {
    super(listeners);
    this.docClassifier = classifier;
  }

  /**
   * Evaluates the given reference {@link DocSample} object.
   *
   * This is done by categorizing the document from the provided
   * {@link DocSample}. The detected category is then used
   * to calculate and update the score.
   *
   * @param sample the reference {@link DocSample}.
   */
  public DocSample processSample(DocSample sample) {

    if (sample.isClearAdaptiveDataSet()) {
      this.docClassifier.clearFeatureData();
    }
    String[] document = sample.getTokens();
    System.err.println("-> Document length: " + document.length);
    String cat = docClassifier.classify(document);

    if (sample.getLabel().equals(cat)) {
      accuracy.add(1);
    }
    else {
      accuracy.add(0);
    }
    return new DocSample(cat, sample.getTokens(), sample.isClearAdaptiveDataSet());
  }

  /**
   * Retrieves the accuracy of provided {@link DocumentClassifier}.
   *
   * accuracy = correctly categorized documents / total documents
   *
   * @return the accuracy
   */
  public double getAccuracy() {
    return accuracy.mean();
  }

  public long getDocumentCount() {
    return accuracy.count();
  }

  /**
   * Represents this objects as human readable {@link String}.
   */
  @Override
  public String toString() {
    return "Accuracy: " + accuracy.mean() + "\n" +
        "Number of documents: " + accuracy.count();
  }
}
