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

import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.featuregen.ArtifactToSerializerMapper;
import opennlp.tools.util.featuregen.CustomFeatureGenerator;
import opennlp.tools.util.featuregen.FeatureGeneratorResourceProvider;
import opennlp.tools.util.model.ArtifactSerializer;
import eus.ixa.ixa.pipe.ml.resources.SequenceModelResource;

/**
 * This feature generator can also be placed in a sliding window.
 * @author ragerri
 * @version 2016-04-07
 */
public class LemmaModelFeatureGenerator extends CustomFeatureGenerator implements ArtifactToSerializerMapper {
  
  private SequenceModelResource seqModelResource;
  private String[] currentSentence;
  private String[] currentLemmas;
  
  public LemmaModelFeatureGenerator() {
  }
  
  public void createFeatures(List<String> features, String[] tokens, int index,
      String[] previousOutcomes) {
    
    //cache annotations for each sentence
    if (currentSentence != tokens) {
      currentSentence = tokens;
      currentLemmas = seqModelResource.lemmatize(tokens);
    }
    String lemma = currentLemmas[index];
    features.add("lemmaModel=" + lemma);
    //System.err.println("-> Model Lemma: " + tokens[index] + " " + lemma);
  }
  
  @Override
  public void updateAdaptiveData(String[] tokens, String[] outcomes) {
  }

  @Override
  public void clearAdaptiveData() {
  }

  @Override
  public void init(Map<String, String> properties,
      FeatureGeneratorResourceProvider resourceProvider)
      throws InvalidFormatException {
    Object posResource = resourceProvider.getResource(properties.get("model"));
    if (!(posResource instanceof SequenceModelResource)) {
      throw new InvalidFormatException("Not a SequenceModelResource for key: " + properties.get("model"));
    }
    this.seqModelResource = (SequenceModelResource) posResource;
  }
  
  @Override
  public Map<String, ArtifactSerializer<?>> getArtifactSerializerMapping() {
    Map<String, ArtifactSerializer<?>> mapping = new HashMap<>();
    mapping.put("seqmodelserializer", new SequenceModelResource.SequenceModelResourceSerializer());
    return Collections.unmodifiableMap(mapping);
  }
}




