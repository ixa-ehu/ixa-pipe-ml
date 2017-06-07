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

package eus.ixa.ixa.pipe.ml.document.features;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eus.ixa.ixa.pipe.ml.resources.SequenceModelResource;
import eus.ixa.ixa.pipe.ml.utils.Span;
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
public class DocTargetFeatureGenerator extends DocumentCustomFeatureGenerator
    implements ArtifactToSerializerMapper {

  private SequenceModelResource oteModelResource;
  private String[] currentSentence;
  private Span[] currentTargets;
  private boolean isCoarse = false;
  private boolean isFineGrained = false;

  public DocTargetFeatureGenerator() {
  }

  @Override
  public void createFeatures(final List<String> features, final String[] tokens) {

    // cache annotations for each sentence
    if (this.currentSentence != tokens) {
      this.currentSentence = tokens;
      this.currentTargets = this.oteModelResource.seqToSpans(tokens);
    }
    for (int index = 0; index < tokens.length; index++) {
      for (Span target : currentTargets) {
        if (target.contains(index)) {
          if (this.isCoarse) {
            final String type = target.getType();
            features.add("aspect=" + tokens[index] + "," + type.split("#")[0]);
          } else if (this.isFineGrained) {
            final String type = target.getType();
            final String aspect = type;
            features.add("aspect=" + tokens[index] + "," + aspect);
          } else {
            features.add("target=" + tokens[index]);
          }
        }
      }
    }
  }

  @Override
  public void clearFeatureData() {
  }

  @Override
  public void init(final Map<String, String> properties,
      final FeatureGeneratorResourceProvider resourceProvider)
      throws InvalidFormatException {
    final Object targetResource = resourceProvider
        .getResource(properties.get("model"));
    if (!(targetResource instanceof SequenceModelResource)) {
      throw new InvalidFormatException(
          "Not a SequenceModelResource for key: " + properties.get("model"));
    }
    this.oteModelResource = (SequenceModelResource) targetResource;
    processRangeOptions(properties);
  }

  /**
   * Process the options of which kind of features are to be generated.
   * 
   * @param properties
   *          the properties map
   */
  private void processRangeOptions(final Map<String, String> properties) {
    final String featuresRange = properties.get("range");
    if (featuresRange.equalsIgnoreCase("coarse")) {
      this.isCoarse = true;
    }
    if (featuresRange.equalsIgnoreCase("fine")) {
      this.isFineGrained = true;
    }
  }

  @Override
  public Map<String, ArtifactSerializer<?>> getArtifactSerializerMapping() {
    final Map<String, ArtifactSerializer<?>> mapping = new HashMap<>();
    mapping.put("otemodelserializer",
        new SequenceModelResource.SequenceModelResourceSerializer());
    return Collections.unmodifiableMap(mapping);
  }
}
