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

import java.util.ArrayList;
import java.util.List;

import opennlp.tools.util.featuregen.AdaptiveFeatureGenerator;
import opennlp.tools.util.featuregen.BigramNameFeatureGenerator;
import opennlp.tools.util.featuregen.CachedFeatureGenerator;
import opennlp.tools.util.featuregen.FeatureGeneratorUtil;
import opennlp.tools.util.featuregen.OutcomePriorFeatureGenerator;
import opennlp.tools.util.featuregen.PreviousMapFeatureGenerator;
import opennlp.tools.util.featuregen.TokenClassFeatureGenerator;
import opennlp.tools.util.featuregen.TokenFeatureGenerator;
import opennlp.tools.util.featuregen.WindowFeatureGenerator;

/**
 * Class for determining contextual features for a tag/chunk style named-entity
 * recognizer.
 */
public class DefaultSequenceLabelerContextGenerator
    implements SequenceLabelerContextGenerator {

  private AdaptiveFeatureGenerator[] featureGenerators;

  @Deprecated
  private static AdaptiveFeatureGenerator windowFeatures = new CachedFeatureGenerator(
      new AdaptiveFeatureGenerator[] {
          new WindowFeatureGenerator(new TokenFeatureGenerator(), 2, 2),
          new WindowFeatureGenerator(new TokenClassFeatureGenerator(true), 2,
              2),
          new OutcomePriorFeatureGenerator(), new PreviousMapFeatureGenerator(),
          new BigramNameFeatureGenerator() });

  /**
   * Creates a name context generator with the specified cache size.
   *
   * @param featureGenerators
   *          the array of feature generators
   */
  public DefaultSequenceLabelerContextGenerator(
      final AdaptiveFeatureGenerator... featureGenerators) {

    if (featureGenerators != null) {
      this.featureGenerators = featureGenerators;
    } else {
      // use defaults
      this.featureGenerators = new AdaptiveFeatureGenerator[] { windowFeatures,
          new PreviousMapFeatureGenerator() };
    }
  }

  @Override
  public void addFeatureGenerator(final AdaptiveFeatureGenerator generator) {
    final AdaptiveFeatureGenerator generators[] = this.featureGenerators;

    this.featureGenerators = new AdaptiveFeatureGenerator[this.featureGenerators.length
        + 1];

    System.arraycopy(generators, 0, this.featureGenerators, 0,
        generators.length);

    this.featureGenerators[this.featureGenerators.length - 1] = generator;
  }

  @Override
  public void updateAdaptiveData(final String[] tokens,
      final String[] outcomes) {

    if (tokens != null && outcomes != null
        && tokens.length != outcomes.length) {
      throw new IllegalArgumentException(
          "The tokens and outcome arrays MUST have the same size!");
    }

    for (final AdaptiveFeatureGenerator featureGenerator : this.featureGenerators) {
      featureGenerator.updateAdaptiveData(tokens, outcomes);
    }
  }

  @Override
  public void clearAdaptiveData() {
    for (final AdaptiveFeatureGenerator featureGenerator : this.featureGenerators) {
      featureGenerator.clearAdaptiveData();
    }
  }

  /**
   * Return the context for finding names at the specified index.
   * 
   * @param index
   *          The index of the token in the specified toks array for which the
   *          context should be constructed.
   * @param tokens
   *          The tokens of the sentence. The <code>toString</code> methods of
   *          these objects should return the token text.
   * @param preds
   *          The previous decisions made in the tagging of this sequence. Only
   *          indices less than i will be examined.
   * @param additionalContext
   *          Addition features which may be based on a context outside of the
   *          sentence.
   *
   * @return the context for finding names at the specified index.
   */
  @Override
  public String[] getContext(final int index, final String[] tokens,
      final String[] preds, final Object[] additionalContext) {
    final List<String> features = new ArrayList<String>();

    for (final AdaptiveFeatureGenerator featureGenerator : this.featureGenerators) {
      featureGenerator.createFeatures(features, tokens, index, preds);
    }

    // previous outcome features
    String po = BilouCodec.OTHER;
    String ppo = BilouCodec.OTHER;

    // TODO: These should be moved out here in its own feature generator!
    if (preds != null) {
      if (index > 1) {
        ppo = preds[index - 2];
      }

      if (index > 0) {
        po = preds[index - 1];
      }
      features.add("po=" + po);
      features.add("pow=" + po + "," + tokens[index]);
      features.add("powf=" + po + ","
          + FeatureGeneratorUtil.tokenFeature(tokens[index]));
      features.add("ppo=" + ppo);
    }

    return features.toArray(new String[features.size()]);
  }
}
