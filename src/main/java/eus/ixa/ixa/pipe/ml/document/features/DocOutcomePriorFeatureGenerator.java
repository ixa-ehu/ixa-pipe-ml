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
package eus.ixa.ixa.pipe.ml.document.features;

import java.util.List;
import java.util.Map;

import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.featuregen.FeatureGeneratorResourceProvider;

/**
 * The definition feature maps the underlying distribution of outcomes.
 */
public class DocOutcomePriorFeatureGenerator extends DocumentCustomFeatureGenerator {

  public static final String OUTCOME_PRIOR_FEATURE = "def";

  @Override
  public void createFeatures(final List<String> features, final String[] tokens) {
    features.add(OUTCOME_PRIOR_FEATURE);
  }

  @Override
  public void clearFeatureData() {
  }

  @Override
  public void init(final Map<String, String> arg0,
      final FeatureGeneratorResourceProvider arg1)
      throws InvalidFormatException {
  }
}
