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

import eus.ixa.ixa.pipe.ml.lemma.DictionaryLemmatizer;
import eus.ixa.ixa.pipe.ml.resources.SequenceModelResource;
import eus.ixa.ixa.pipe.ml.utils.Span;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.featuregen.ArtifactToSerializerMapper;
import opennlp.tools.util.featuregen.CustomFeatureGenerator;
import opennlp.tools.util.featuregen.FeatureGeneratorResourceProvider;
import opennlp.tools.util.model.ArtifactSerializer;

/**
 * Generate lemma features from a dictionary as feature of the current token.
 * This feature generator can also be placed in a sliding window.
 *
 * @author ragerri
 * @version 2016-04-07
 */
public class LemmaDictionaryFeatureGenerator extends CustomFeatureGenerator
    implements ArtifactToSerializerMapper {

  private SequenceModelResource posModelResource;
  private DictionaryLemmatizer lemmaDictResource;
  private String[] currentSentence;
  private Span[] currentTags;
  private List<String> currentLemmas;

  public LemmaDictionaryFeatureGenerator() {
  }

  @Override
  public void createFeatures(final List<String> features, final String[] tokens,
      final int index, final String[] previousOutcomes) {

    // cache annotation results for each sentence
    if (this.currentSentence != tokens) {
      this.currentSentence = tokens;
      this.currentTags = this.posModelResource.seqToSpans(tokens);
      this.currentLemmas = this.lemmaDictResource.lemmatize(tokens,
          this.currentTags);
    }
    final String lemma = this.currentLemmas.get(index);
    features.add("lemmaDict=" + lemma);
    // System.err.println("-> Dictionary Lemma: " + tokens[index] + " " +
    // lemma);
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
    final Object posResource = resourceProvider
        .getResource(properties.get("model"));
    if (!(posResource instanceof SequenceModelResource)) {
      throw new InvalidFormatException(
          "Not a POSModelResource for key: " + properties.get("model"));
    }
    this.posModelResource = (SequenceModelResource) posResource;
    final Object lemmaResource = resourceProvider
        .getResource(properties.get("dict"));
    if (!(lemmaResource instanceof DictionaryLemmatizer)) {
      throw new InvalidFormatException(
          "Not a DictionaryLemmatizer for key: " + properties.get("dict"));
    }
    this.lemmaDictResource = (DictionaryLemmatizer) lemmaResource;
  }

  @Override
  public Map<String, ArtifactSerializer<?>> getArtifactSerializerMapping() {
    final Map<String, ArtifactSerializer<?>> mapping = new HashMap<>();
    mapping.put("seqmodelserializer",
        new SequenceModelResource.SequenceModelResourceSerializer());
    mapping.put("lemmadictserializer",
        new DictionaryLemmatizer.DictionaryLemmatizerSerializer());
    return Collections.unmodifiableMap(mapping);
  }
}
