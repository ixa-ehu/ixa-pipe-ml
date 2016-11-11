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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.featuregen.CustomFeatureGenerator;
import opennlp.tools.util.featuregen.FeatureGeneratorResourceProvider;

public class PreviousMapTokenFeatureGenerator extends CustomFeatureGenerator {

  private final Map<String, String> previousMap = new HashMap<String, String>();

  @Override
  public void createFeatures(final List<String> features, final String[] tokens,
      final int index, final String[] preds) {
    features.add(
        "w,pd=" + tokens[index] + "," + this.previousMap.get(tokens[index]));
  }

  /**
   * Generates previous decision features for the token based on contents of the
   * previous map.
   */
  @Override
  public void updateAdaptiveData(final String[] tokens,
      final String[] outcomes) {

    for (int i = 0; i < tokens.length; i++) {
      this.previousMap.put(tokens[i], outcomes[i]);
    }
  }

  /**
   * Clears the previous map.
   */
  @Override
  public void clearAdaptiveData() {
    this.previousMap.clear();
  }

  @Override
  public void init(final Map<String, String> properties,
      final FeatureGeneratorResourceProvider resourceProvider)
      throws InvalidFormatException {

  }

}
