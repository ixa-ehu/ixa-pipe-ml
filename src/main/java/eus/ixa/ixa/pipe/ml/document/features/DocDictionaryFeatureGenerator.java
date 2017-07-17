/*
 * Copyright 2017 Rodrigo Agerri

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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eus.ixa.ixa.pipe.ml.resources.Dictionary;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.featuregen.ArtifactToSerializerMapper;
import opennlp.tools.util.featuregen.FeatureGeneratorResourceProvider;
import opennlp.tools.util.model.ArtifactSerializer;

/**
 * This feature generator can also be placed in a sliding window.
 * 
 * @author ragerri
 * @version 2015-03-12
 */
public class DocDictionaryFeatureGenerator extends DocumentCustomFeatureGenerator
    implements ArtifactToSerializerMapper {

  private String[] currentSentence;
  private List<String> currentClasses;
  private Dictionary dictionary;
  private Map<String, String> attributes;
  private boolean isBilou = true;

  public DocDictionaryFeatureGenerator() {
  }

  @Override
  public void createFeatures(final List<String> features, final String[] tokens) {

    // cache annotations for each sentence
    if (this.currentSentence != tokens) {
      this.currentSentence = tokens;
      if (isBilou) {
        this.currentClasses = dictionary.getBilouDictionaryMatch(tokens);
      } else {
        this.currentClasses = dictionary.getBioDictionaryMatch(tokens);
      }
    }
    
    for (int index = 0; index < tokens.length; index++) {
      String aspect = currentClasses.get(index);
      features.add(this.attributes.get("dict") + "=" + aspect);
      features.add(this.attributes.get("dict") + "=" + aspect + ",w=" + tokens[index]);
      //System.err.println("-> Dictionary feature: " + aspect);
    }
  }

  @Override
  public void clearFeatureData() {
  }

  @Override
  public void init(final Map<String, String> properties,
      final FeatureGeneratorResourceProvider resourceProvider)
      throws InvalidFormatException {
    final Object aspectResource = resourceProvider
        .getResource(properties.get("dict"));
    if (!(aspectResource instanceof Dictionary)) {
      throw new InvalidFormatException(
          "Not Dictionary resource for key: " + properties.get("dict"));
    }
    this.dictionary = (Dictionary) aspectResource;
    this.attributes = properties;
    if (properties.get("seqCodec").equalsIgnoreCase("bio")) {
      this.isBilou = false;
    } else {
      this.isBilou = true;
    }
  }

  @Override
  public Map<String, ArtifactSerializer<?>> getArtifactSerializerMapping() {
    final Map<String, ArtifactSerializer<?>> mapping = new HashMap<>();
    mapping.put("dictionaryserializer", new Dictionary.DictionarySerializer());
    return Collections.unmodifiableMap(mapping);
  }
}
