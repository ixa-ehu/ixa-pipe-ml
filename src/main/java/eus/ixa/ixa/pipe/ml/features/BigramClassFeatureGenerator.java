/*
 * Copyright 2016 Rodrigo Agerri

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
package eus.ixa.ixa.pipe.ml.features;

import java.util.List;
import java.util.Map;

import eus.ixa.ixa.pipe.ml.utils.Flags;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.featuregen.CustomFeatureGenerator;
import opennlp.tools.util.featuregen.FeatureGeneratorResourceProvider;

/**
 * Adds bigram features based on tokens and token class using
 * {@code TokenClassFeatureGenerator}.
 * 
 * @author ragerri
 * @version 2016-07-26
 */
public class BigramClassFeatureGenerator extends CustomFeatureGenerator {

  @Override
  public void createFeatures(final List<String> features, final String[] tokens,
      final int index, final String[] previousOutcomes) {
    final String wc = TokenClassFeatureGenerator
        .tokenShapeFeature(tokens[index]);
    // bi-gram features
    if (index > 0) {
      features.add("pw,w=" + tokens[index - 1] + "," + tokens[index]);
      final String pwc = TokenClassFeatureGenerator
          .tokenShapeFeature(tokens[index - 1]);
      features.add("pwc,wc=" + pwc + "," + wc);
      if (Flags.DEBUG) {
        System.err.println("-> " + tokens[index] + ": pw,w=" + tokens[index - 1]
            + "," + tokens[index]);
        System.err
            .println("-> " + tokens[index] + ": pwc,wc=" + pwc + "," + wc);
      }
    }
    if (index + 1 < tokens.length) {
      features.add("w,nw=" + tokens[index] + "," + tokens[index + 1]);
      final String nwc = TokenClassFeatureGenerator
          .tokenShapeFeature(tokens[index + 1]);
      features.add("wc,nc=" + wc + "," + nwc);
      if (Flags.DEBUG) {
        System.err.println("-> " + tokens[index] + ": w,nw=" + tokens[index]
            + "," + tokens[index + 1]);
        System.err.println("-> " + tokens[index] + ": wc,nc=" + wc + "," + nwc);
      }
    }
  }

  @Override
  public void clearAdaptiveData() {
  }

  @Override
  public void updateAdaptiveData(final String[] arg0, final String[] arg1) {
  }

  @Override
  public void init(final Map<String, String> arg0,
      final FeatureGeneratorResourceProvider arg1)
      throws InvalidFormatException {
  }
}
