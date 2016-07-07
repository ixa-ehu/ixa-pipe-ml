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
import eus.ixa.ixa.pipe.ml.resources.PredicateContext;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerME;

public class PredicateContextFeatureGenerator extends CustomFeatureGenerator implements ArtifactToSerializerMapper {

  private PredicateContext predicateContext;
  public PredicateContextFeatureGenerator() {
  }
  
  public void createFeatures(List<String> features, String[] tokens, int index,
      String[] preds) {
    
    String w_1 = null;
    String w0 = null;
    String w1 = null;
    String predicate = null;
    String contextString = null;
    
    String[] decodeSequences = SequenceLabelerME.decodeSequences(preds);
    System.err.println("-> Predicate: " + decodeSequences[index]);
    System.err.println("-> Token: " + tokens[index]);
    
    if (decodeSequences[index].equalsIgnoreCase("B-V")) {
      predicate = tokens[index];
      features.add("region=1");
    }
    if (index < 1) {
      features.add("region=1");
    }
    if (index + 1 >= tokens.length) {
      features.add("region=1");
    }
    
    features.add("pred=" + predicate);
      /*for (int i = 0; i < predicateContexts.length; i++) {
       
        if (predicateContexts[i].equalsIgnoreCase("B-V")) {
          //pred feature for each token
          w0 = tokens[i];
          System.err.println("-> Predicate=" + w0);
          predIndex = i;
          features.add("region=" + "1");
          System.err.println("region=" + "1");
        }
        //predicate context
        if (predIndex - 1 >= 0) {
          w_1 = tokens[predIndex - 1];
          features.add("region=" + "1");
          System.err.println("region=1");
          if (i < predIndex - 1) {
            features.add("region=" + "0");
            System.err.println("region=0");
          }
        }
        w0 = tokens[predIndex];
        
        if (predIndex + 1 < predicateContexts.length) {
          w1 = tokens[predIndex + 1];
          features.add("region=" + "1");
          System.err.println("region=1");
          if (i >= predIndex + 1) {
            features.add("region=" + "0");
            System.err.println("region=0");
          }
        }
        features.add("pred=" + w0);
        System.err.println("pred=" + w0);
        contextString = w_1 + " " + w0 + " " + w1;
      }
      //context feature
      features.add("pred_cont=" + contextString.trim());
      System.err.println("pred_cont=" + contextString.trim());*/
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
    Object dictResource = resourceProvider.getResource(properties.get("dict"));
    if (!(dictResource instanceof PredicateContext)) {
      throw new InvalidFormatException("Not a PredicateContext resource for key: " + properties.get("dict"));
    }
    this.predicateContext = (PredicateContext) dictResource;
  }

  @Override
  public Map<String, ArtifactSerializer<?>> getArtifactSerializerMapping() {
    Map<String, ArtifactSerializer<?>> mapping = new HashMap<>();
    mapping.put("predicatecontextserializer", new PredicateContext.PredicateContextSerializer());
    return Collections.unmodifiableMap(mapping);
  }
  
}

