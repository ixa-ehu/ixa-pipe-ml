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
package eus.ixa.ixa.pipe.ml.document;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eus.ixa.ixa.pipe.ml.resources.WordCluster;
import eus.ixa.ixa.pipe.ml.utils.Flags;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.featuregen.ArtifactToSerializerMapper;
import opennlp.tools.util.featuregen.FeatureGeneratorResourceProvider;
import opennlp.tools.util.model.ArtifactSerializer;

public class DocClarkFeatureGenerator extends DocumentCustomFeatureGenerator
    implements ArtifactToSerializerMapper {

  private WordCluster clarkCluster;
  private Map<String, String> attributes;
  public static String unknownClarkClass = "O";

  public DocClarkFeatureGenerator() {
  }

  @Override
  public void createFeatures(final List<String> features, final String[] tokens) {

    for (String token : tokens) {
      final String wordClass = getWordClass(token.toLowerCase());
      features.add(this.attributes.get("dict") + "=" + wordClass);
      if (Flags.DEBUG) {
        System.err.println("-> " + token.toLowerCase() + ": "
            + this.attributes.get("dict") + "=" + wordClass);
      }
    }
  }

  public String getWordClass(final String token) {
    String clarkClass = this.clarkCluster.lookupToken(token);
    if (clarkClass == null) {
      clarkClass = unknownClarkClass;
    }
    return clarkClass;
  }

  @Override
  public void clearFeatureData() {
  }

  @Override
  public void init(final Map<String, String> properties,
      final FeatureGeneratorResourceProvider resourceProvider)
      throws InvalidFormatException {
    final Object dictResource = resourceProvider
        .getResource(properties.get("dict"));
    if (!(dictResource instanceof WordCluster)) {
      throw new InvalidFormatException(
          "Not a ClarkCluster resource for key: " + properties.get("dict"));
    }
    this.clarkCluster = (WordCluster) dictResource;
    this.attributes = properties;
  }

  @Override
  public Map<String, ArtifactSerializer<?>> getArtifactSerializerMapping() {
    final Map<String, ArtifactSerializer<?>> mapping = new HashMap<>();
    mapping.put("clarkserializer", new WordCluster.WordClusterSerializer());
    return Collections.unmodifiableMap(mapping);
  }

}

