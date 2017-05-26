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


package eus.ixa.ixa.pipe.ml.features;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * The {@link DocumentFeatureAggregator} aggregates a set of
 * {@link DocumentFeatureGenerator}s and calls them to generate the features.
 */
public class DocumentFeatureAggregator implements DocumentFeatureGenerator {

  /**
   * Contains all aggregated {@link DocumentFeatureGenerator}s.
   */
  private Collection<DocumentFeatureGenerator> generators;

  /**
   * Initializes the current instance.
   *
   * @param generators array of generators, null values are not permitted
   */
  public DocumentFeatureAggregator(DocumentFeatureGenerator... generators) {

    for (DocumentFeatureGenerator generator : generators) {
      if (generator == null)
        throw new IllegalArgumentException("null values in generators are not permitted!");
    }

    this.generators = new ArrayList<DocumentFeatureGenerator>(generators.length);

    Collections.addAll(this.generators, generators);

    this.generators = Collections.unmodifiableCollection(this.generators);
  }

  public DocumentFeatureAggregator(Collection<DocumentFeatureGenerator> generators) {
    this(generators.toArray(new DocumentFeatureGenerator[generators.size()]));
  }

  /**
   * Calls the {@link DocumentFeatureGenerator#clearFeatureData()} method
   * on all aggregated {@link AdaptiveFeatureGenerator}s.
   */
  public void clearFeatureData() {

    for (DocumentFeatureGenerator generator : generators) {
      generator.clearFeatureData();
    }
  }

  /**
   * Calls the {@link DocumentFeatureGenerator#createFeatures(List, String[])}
   * method on all aggregated {@link AdaptiveFeatureGenerator}s.
   */
  public void createFeatures(List<String> features, String[] tokens) {

    for (DocumentFeatureGenerator generator : generators) {
      generator.createFeatures(features, tokens);
    }
  }

  /**
   * Retrieves a {@link Collections} of all aggregated
   * {@link AdaptiveFeatureGenerator}s.
   *
   * @return all aggregated generators
   */
  public Collection<DocumentFeatureGenerator> getGenerators() {
    return generators;
  }
}