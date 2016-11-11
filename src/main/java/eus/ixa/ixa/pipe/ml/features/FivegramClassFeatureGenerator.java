/*
 * Copyright 2014 Rodrigo Agerri

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

import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.featuregen.CustomFeatureGenerator;
import opennlp.tools.util.featuregen.FeatureGeneratorResourceProvider;

/**
 * Adds fivegram features based on tokens and token class using
 * {@code TokenClassFeatureGenerator}.
 *
 * @author ragerri
 *
 */
public class FivegramClassFeatureGenerator extends CustomFeatureGenerator {

  @Override
  public void createFeatures(final List<String> features, final String[] tokens,
      final int index, final String[] previousOutcomes) {
    final String wc = TokenClassFeatureGenerator
        .tokenShapeFeature(tokens[index]);
    // fivegram features
    if (index > 3) {
      features.add("ppppw,pppw,ppw,pw,w=" + tokens[index - 4] + ","
          + tokens[index - 3] + "," + tokens[index - 2] + ","
          + tokens[index - 1] + "," + tokens[index]);
      final String pwc = TokenClassFeatureGenerator
          .tokenShapeFeature(tokens[index - 1]);
      final String ppwc = TokenClassFeatureGenerator
          .tokenShapeFeature(tokens[index - 2]);
      final String pppwc = TokenClassFeatureGenerator
          .tokenShapeFeature(tokens[index - 3]);
      final String ppppwc = TokenClassFeatureGenerator
          .tokenShapeFeature(tokens[index - 4]);
      features.add("pppwc,ppwc,pwc,wc=" + ppppwc + "," + pppwc + "," + ppwc
          + "," + pwc + "," + wc);
    }
    if (index + 4 < tokens.length) {
      features.add("w,nw,nnw,nnnw,nnnnw=" + tokens[index] + ","
          + tokens[index + 1] + "," + tokens[index + 2] + ","
          + tokens[index + 3] + "," + tokens[index + 4]);
      final String nwc = TokenClassFeatureGenerator
          .tokenShapeFeature(tokens[index + 1]);
      final String nnwc = TokenClassFeatureGenerator
          .tokenShapeFeature(tokens[index + 2]);
      final String nnnwc = TokenClassFeatureGenerator
          .tokenShapeFeature(tokens[index + 3]);
      final String nnnnwc = TokenClassFeatureGenerator
          .tokenShapeFeature(tokens[index + 4]);
      features.add("wc,nwc,nnwc,nnnwc=" + wc + "," + nwc + "," + nnwc + ","
          + nnnwc + "," + nnnnwc);
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
