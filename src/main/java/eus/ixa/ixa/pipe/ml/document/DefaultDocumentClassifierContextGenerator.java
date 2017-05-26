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

import java.util.ArrayList;
import java.util.List;

/**
 * Class for determining contextual features for a tag/chunk style named-entity
 * recognizer.
 */
public class DefaultDocumentClassifierContextGenerator
    implements DocumentClassifierContextGenerator {

  private DocumentFeatureGenerator[] featureGenerators;

  /**
   * Creates a name context generator with the specified cache size.
   *
   * @param featureGenerators
   *          the array of feature generators
   */
  public DefaultDocumentClassifierContextGenerator(
      final DocumentFeatureGenerator... featureGenerators) {

    if (featureGenerators != null) {
      this.featureGenerators = featureGenerators;
    } else {
      // use defaults
      this.featureGenerators = new DocumentFeatureGenerator[] {
          new BagOfWordsFeatureGenerator()};
    }
  }

  @Override
  public void addFeatureGenerator(final DocumentFeatureGenerator generator) {
    final DocumentFeatureGenerator generators[] = this.featureGenerators;

    this.featureGenerators = new DocumentFeatureGenerator[this.featureGenerators.length
        + 1];

    System.arraycopy(generators, 0, this.featureGenerators, 0,
        generators.length);
    this.featureGenerators[this.featureGenerators.length - 1] = generator;
  }

  @Override
  public void clearFeatureData() {
    for (final DocumentFeatureGenerator featureGenerator : this.featureGenerators) {
      featureGenerator.clearFeatureData();
    }
  }

  public String[] getContext(final String[] tokens) {
    final List<String> features = new ArrayList<String>();
    
    for (final DocumentFeatureGenerator featureGenerator : this.featureGenerators) {
      featureGenerator.createFeatures(features, tokens);
    }
    return features.toArray(new String[features.size()]);
  }
}
