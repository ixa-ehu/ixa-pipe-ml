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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eus.ixa.ixa.pipe.ml.resources.WordCluster;
import eus.ixa.ixa.pipe.ml.utils.Flags;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.featuregen.ArtifactToSerializerMapper;
import opennlp.tools.util.featuregen.CustomFeatureGenerator;
import opennlp.tools.util.featuregen.FeatureGeneratorResourceProvider;
import opennlp.tools.util.model.ArtifactSerializer;

public class BrownTokenClassFeatureGenerator extends CustomFeatureGenerator
    implements ArtifactToSerializerMapper {

  private WordCluster brownLexicon;
  private Map<String, String> attributes;

  public BrownTokenClassFeatureGenerator() {
  }

  @Override
  public void createFeatures(final List<String> features, final String[] tokens,
      final int index, final String[] previousOutcomes) {

    final String tokenShape = TokenClassFeatureGenerator
        .tokenShapeFeature(tokens[index]);
    final List<String> wordClasses = BrownTokenClasses
        .getWordClasses(tokens[index], this.brownLexicon);

    for (int i = 0; i < wordClasses.size(); i++) {
      features.add("c," + this.attributes.get("dict") + "=" + tokenShape + ","
          + wordClasses.get(i));
      if (Flags.DEBUG) {
        System.err.println(
            "-> " + tokens[index] + ": + c," + this.attributes.get("dict") + "="
                + tokenShape + "," + wordClasses.get(i));
      }
    }
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
    if (!(dictResource instanceof WordCluster)) {
      throw new InvalidFormatException(
          "Not a ClusterLexicon resource for key: " + properties.get("dict"));
    }
    this.brownLexicon = (WordCluster) dictResource;
    this.attributes = properties;
  }

  @Override
  public Map<String, ArtifactSerializer<?>> getArtifactSerializerMapping() {
    final Map<String, ArtifactSerializer<?>> mapping = new HashMap<>();
    mapping.put("brownserializer", new WordCluster.WordClusterSerializer());
    return Collections.unmodifiableMap(mapping);
  }

}
