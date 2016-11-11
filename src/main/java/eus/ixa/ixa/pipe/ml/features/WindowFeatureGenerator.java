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
import java.util.List;

import opennlp.tools.util.featuregen.AdaptiveFeatureGenerator;
import opennlp.tools.util.featuregen.AggregatedFeatureGenerator;

/**
 * Generates previous and next features for a given
 * {@link AdaptiveFeatureGenerator}. The window size can be specified.
 *
 * Features: Current token is always included unchanged Previous tokens are
 * prefixed with p distance Next tokens are prefix with n distance
 */
public class WindowFeatureGenerator implements AdaptiveFeatureGenerator {

  public static final String PREV_PREFIX = "p";
  public static final String NEXT_PREFIX = "n";

  private final AdaptiveFeatureGenerator generator;

  private final int prevWindowSize;
  private final int nextWindowSize;

  /**
   * Initializes the current instance with the given parameters.
   *
   * @param generator
   *          feature generator to apply to the window.
   * @param prevWindowSize
   *          size of the window to the left of the current token.
   * @param nextWindowSize
   *          size of the window to the right of the current token.
   */
  public WindowFeatureGenerator(final AdaptiveFeatureGenerator generator,
      final int prevWindowSize, final int nextWindowSize) {
    if (prevWindowSize < 1 || nextWindowSize < 1) {
      throw new IllegalArgumentException("window parameter must be at least 1. "
          + "minLength=" + prevWindowSize + ", maxLength= " + nextWindowSize);
    }
    this.generator = generator;
    this.prevWindowSize = prevWindowSize;
    this.nextWindowSize = nextWindowSize;
  }

  /**
   * Initializes the current instance with the given parameters.
   *
   * @param prevWindowSize
   *          size of the window to the left of the current token
   * @param nextWindowSize
   *          size of the window to the right of the current token
   * @param generators
   *          the array of feature generators
   */
  public WindowFeatureGenerator(final int prevWindowSize,
      final int nextWindowSize, final AdaptiveFeatureGenerator... generators) {
    this(new AggregatedFeatureGenerator(generators), prevWindowSize,
        nextWindowSize);
  }

  /**
   * Initializes the current instance. The previous and next window size is 5.
   *
   * @param generator
   *          feature generator
   */
  public WindowFeatureGenerator(final AdaptiveFeatureGenerator generator) {
    this(generator, 6, 6);
  }

  /**
   * Initializes the current instance with the given parameters.
   *
   * @param generators
   *          array of feature generators
   */
  public WindowFeatureGenerator(final AdaptiveFeatureGenerator... generators) {
    this(new AggregatedFeatureGenerator(generators), 5, 5);
  }

  @Override
  public void createFeatures(final List<String> features, final String[] tokens,
      final int index, final String[] preds) {
    // current features
    this.generator.createFeatures(features, tokens, index, preds);

    // previous features
    for (int i = 1; i < this.prevWindowSize + 1; i++) {
      if (index - i >= 0) {

        final List<String> prevFeatures = new ArrayList<String>();

        this.generator.createFeatures(prevFeatures, tokens, index - i, preds);

        for (final String prevFeature : prevFeatures) {
          features.add(PREV_PREFIX + i + prevFeature);
        }
      }
    }

    // next features
    for (int i = 1; i < this.nextWindowSize + 1; i++) {
      if (i + index < tokens.length) {

        final List<String> nextFeatures = new ArrayList<String>();

        this.generator.createFeatures(nextFeatures, tokens, index + i, preds);

        for (final String nextFeature : nextFeatures) {
          features.add(NEXT_PREFIX + i + nextFeature);
        }
      }
    }
  }

  @Override
  public void updateAdaptiveData(final String[] tokens,
      final String[] outcomes) {
    this.generator.updateAdaptiveData(tokens, outcomes);
  }

  @Override
  public void clearAdaptiveData() {
    this.generator.clearAdaptiveData();
  }

  @Override
  public String toString() {
    return super.toString() + ": Prev window size: " + this.prevWindowSize
        + ", Next window size: " + this.nextWindowSize;
  }
}