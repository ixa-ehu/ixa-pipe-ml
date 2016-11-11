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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eus.ixa.ixa.pipe.ml.resources.PredicateContext;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.featuregen.ArtifactToSerializerMapper;
import opennlp.tools.util.featuregen.CustomFeatureGenerator;
import opennlp.tools.util.featuregen.FeatureGeneratorResourceProvider;
import opennlp.tools.util.model.ArtifactSerializer;

public class PredicateContextFeatureGenerator extends CustomFeatureGenerator
    implements ArtifactToSerializerMapper {

  public PredicateContextFeatureGenerator() {
  }

  @Override
  public void createFeatures(final List<String> features, final String[] tokens,
      final int index, final String[] preds) {

    final String[] predicateContexts = tokens[index].split(":KK:");

    // token
    // System.err.println(predicateContexts[0]);
    features.add(predicateContexts[0]);
    // pred
    // System.err.println(predicateContexts[1]);
    features.add(predicateContexts[1]);
    // region
    // System.err.println(predicateContexts[2]);
    features.add(predicateContexts[2]);
    // ctxt
    // System.err.println(predicateContexts[3]);
    features.add(predicateContexts[3]);
  }

  @Override
  public void updateAdaptiveData(final String[] tokens,
      final String[] outcomes) {

  }

  @Override
  public void clearAdaptiveData() {

  }

  @Override
  public void init(final Map<String, String> properties,
      final FeatureGeneratorResourceProvider resourceProvider)
      throws InvalidFormatException {
    final Object dictResource = resourceProvider
        .getResource(properties.get("dict"));
    if (!(dictResource instanceof PredicateContext)) {
      throw new InvalidFormatException(
          "Not a PredicateContext resource for key: " + properties.get("dict"));
    }
  }

  @Override
  public Map<String, ArtifactSerializer<?>> getArtifactSerializerMapping() {
    final Map<String, ArtifactSerializer<?>> mapping = new HashMap<>();
    mapping.put("predicatecontextserializer",
        new PredicateContext.PredicateContextSerializer());
    return Collections.unmodifiableMap(mapping);
  }

}
