/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package eus.ixa.ixa.pipe.ml.document;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.featuregen.FeatureGeneratorResourceProvider;
import opennlp.tools.util.featuregen.StringPattern;

/**
 * Generates a feature for each word in a document.
 */
public class BagOfWordsFeatureGenerator extends DocumentCustomFeatureGenerator {

  private Map<String, String> attributes;
  
  public BagOfWordsFeatureGenerator() {
  }

  @Override
  public void createFeatures(List<String> features, String[] text) {
    Objects.requireNonNull(text, "text must not be null");

    
    for (String word : text) {
      features.add("bow=" + word);
      /*if (this.attributes.get("range").equalsIgnoreCase("lettersOnly")) {
        StringPattern pattern = StringPattern.recognize(word);
        if (pattern.isAllLetter())
          features.add("bow=" + word);
      }
      else {
        features.add("bow=" + word);
      }*/
    }
  }

  @Override
  public void clearFeatureData() {
  }

  @Override
  public void init(Map<String, String> properties,
      FeatureGeneratorResourceProvider resourceProvider)
      throws InvalidFormatException {
    this.attributes = properties;
  }
}
