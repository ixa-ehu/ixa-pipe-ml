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

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

/**
 * Interface for classes which classifies documents.
 */
public interface DocumentClassifier {

  /**
   * Classifies the given text, provided in separate tokens.
   * @param text the tokens of text to classify
   * @return the best label found
   */
  String classify(String[] text);
  
  /**
   * Classifies the given text, provided in separate tokens.
   * @param text the tokens of text to classify
   * @return per category probabilities
   */
  double[] classifyProb(String[] text);

  /**
   * Get the best label from previously generated outcome probabilities.
   *
   * @param outcome a vector of outcome probabilities
   * @return the best label in a String
   */
  String getBestCategory(double[] outcome);

  /**
   * Get the index of a certain label.
   *
   * @param label the label
   * @return an index
   */
  int getIndex(String label);

  /**
   * Get the category at a given index.
   *
   * @param index the index
   * @return a label
   */
  String getLabel(int index);

  /**
   * get the number of found labels.
   *
   * @return the number of labels
   */
  int getNumberOfLabels();

  /**
   * Get the label associated with the given probabilities.
   *
   * @param results the probabilities of each label
   * @return the name of the outcome
   */
  String getAllLabels(double[] results);

  /**
   * Returns a map in which the key is the label name and the value is the score.
   *
   * @param text the input text to classify
   * @return a map with the score as a key. The value is a Set of labels with the score.
   */
  Map<String, Double> scoreMap(String[] text);

  /**
   * Get a map of the scores sorted in ascending order together with their associated labels.
   * Many labels can have the same score, hence the Set as value.
   *
   * @param text the input text to classify
   * @return a map with the score as a key. The value is a Set of labels with their score.
   */
  SortedMap<Double, Set<String>> sortedScoreMap(String[] text);

}

