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
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.featuregen.ArtifactToSerializerMapper;
import opennlp.tools.util.featuregen.FeatureGeneratorResourceProvider;
import opennlp.tools.util.model.ArtifactSerializer;

/**
 * This feature generator can also be placed in a sliding window.
 * 
 * @author ragerri
 * @version 2016-04-07
 */
public class DocLemmaModelFeatureGenerator extends DocumentCustomFeatureGenerator
    implements ArtifactToSerializerMapper {

  private SequenceModelResource seqModelResource;
  private String[] currentSentence;
  private String[] currentLemmas;

  public DocLemmaModelFeatureGenerator() {
  }

  @Override
  public void createFeatures(final List<String> features, final String[] tokens) {

    // cache annotations for each sentence
    if (this.currentSentence != tokens) {
      this.currentSentence = tokens;
      this.currentLemmas = this.seqModelResource.lemmatize(tokens);
    }
    for (int index = 0; index < tokens.length; index++) {
      final String lemma = this.currentLemmas[index];
      features.add("lemmaModel=" + lemma);
      // System.err.println("-> Model Lemma: " + tokens[index] + " " + lemma);
    }
  }

  @Override
  public void clearFeatureData() {
  }

  @Override
  public void init(final Map<String, String> properties,
      final FeatureGeneratorResourceProvider resourceProvider)
      throws InvalidFormatException {
    final Object posResource = resourceProvider
        .getResource(properties.get("model"));
    if (!(posResource instanceof SequenceModelResource)) {
      throw new InvalidFormatException(
          "Not a SequenceModelResource for key: " + properties.get("model"));
    }
    this.seqModelResource = (SequenceModelResource) posResource;
  }

  @Override
  public Map<String, ArtifactSerializer<?>> getArtifactSerializerMapping() {
    final Map<String, ArtifactSerializer<?>> mapping = new HashMap<>();
    mapping.put("seqmodelserializer",
        new SequenceModelResource.SequenceModelResourceSerializer());
    return Collections.unmodifiableMap(mapping);
  }
}
