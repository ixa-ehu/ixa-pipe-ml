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

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import eus.ixa.ixa.pipe.ml.document.features.BagOfWordsFeatureGenerator;
import opennlp.tools.ml.EventTrainer;
import opennlp.tools.ml.TrainerFactory;
import opennlp.tools.ml.model.Event;
import opennlp.tools.ml.model.MaxentModel;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.TrainingParameters;

/**
 * Statistical implementation of {@link DocumentClassifier}.
 */
public class DocumentClassifierME implements DocumentClassifier {

  private MaxentModel model;
  private DocumentClassifierContextGenerator contextGenerator;

  /**
   * Initializes the current instance with a DocumentClassifier model. Default feature
   * generation is used.
   *
   * @param model the document classifier model
   */
  public DocumentClassifierME(DocumentClassifierModel model) {
    final DocumentClassifierFactory factory = model.getFactory();
    this.model = model.getDocumentClassifierModel();
    this.contextGenerator = factory.createContextGenerator();
    // TODO: We should deprecate this. And come up with a better solution!
    this.contextGenerator.addFeatureGenerator(new BagOfWordsFeatureGenerator());
  }
  
  public String classify(String[] text) {
    double[] outcome = classifyProb(text);
    return model.getBestOutcome(outcome);
  }

  /**
   * Classify the given text provided as tokens.
   *
   * @param text text tokens to classify
   */
  @Override
  public double[] classifyProb(String[] text) {
    return model.eval(
        contextGenerator.getContext(text));
  }
  
  public String getBestLabel(double[] outcome) {
    return model.getBestOutcome(outcome);
  }

  /**
   * Returns a map in which the key is the label name and the value is the score.
   *
   * @param text the input text to classify
   * @return the score map
   */
  @Override
  public Map<String, Double> scoreMap(String[] text) {
    Map<String, Double> probDist = new HashMap<>();

    double[] categorize = classifyProb(text);
    int catSize = getNumberOfLabels();
    for (int i = 0; i < catSize; i++) {
      String category = getLabel(i);
      probDist.put(category, categorize[getIndex(category)]);
    }
    return probDist;
  }

  /**
   * Returns a map with the score as a key in ascending order.
   * The value is a Set of categories with the score.
   * Many labels can have the same score, hence the Set as value.
   *
   * @param text the input text to classify
   * @return the sorted score map
   */
  @Override
  public SortedMap<Double, Set<String>> sortedScoreMap(String[] text) {
    SortedMap<Double, Set<String>> descendingMap = new TreeMap<>();
    double[] categorize = classifyProb(text);
    int catSize = getNumberOfLabels();
    for (int i = 0; i < catSize; i++) {
      String category = getLabel(i);
      double score = categorize[getIndex(category)];
      if (descendingMap.containsKey(score)) {
        descendingMap.get(score).add(category);
      } else {
        Set<String> newset = new HashSet<>();
        newset.add(category);
        descendingMap.put(score, newset);
      }
    }
    return descendingMap;
  }

  public int getIndex(String category) {
    return model.getIndex(category);
  }

  public String getLabel(int index) {
    return model.getOutcome(index);
  }

  public int getNumberOfLabels() {
    return model.getNumOutcomes();
  }

  public String getAllLabels(double[] results) {
    return model.getAllOutcomes(results);
  }
  
  /**
   * Forgets all adaptive data which was collected during previous calls to one
   * of the find methods.
   *
   * This method is typical called at the end of a document.
   */
  @Override
  public void clearFeatureData() {
    this.contextGenerator.clearFeatureData();
  }

  public static DocumentClassifierModel train(String languageCode, ObjectStream<DocSample> samples,
      TrainingParameters mlParams, DocumentClassifierFactory factory)
          throws IOException {

    Map<String, String> manifestInfoEntries = new HashMap<>();
    EventTrainer trainer = TrainerFactory.getEventTrainer(
        mlParams.getSettings(), manifestInfoEntries);
    final ObjectStream<Event> eventStream =  new DocumentClassifierEventStream(samples, factory.createContextGenerator());
    MaxentModel model = trainer.train(eventStream);
    return new DocumentClassifierModel(languageCode, model, factory.getFeatureGenerator(), factory.getResources(), manifestInfoEntries, factory);
  }
}
