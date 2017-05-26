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

/**
 * Generates ngram features for a document.
 * n-gram {@link DocumenCustomFeatureGenerator}
 */
public class NGramFeatureGenerator extends DocumentCustomFeatureGenerator {

  private Map<String, String> attributes; 

  public NGramFeatureGenerator() {
  }

  public void createFeatures(List<String> features, String[] text) {
    int minGram = Integer.parseInt(this.attributes.get("minLength"));
    int maxGram = Integer.parseInt(this.attributes.get("maxLength"));
    
    try {
      checkMinMaxNGrams(minGram, maxGram);
    } catch (InvalidFormatException e) {
      e.printStackTrace();
    }
    
    Objects.requireNonNull(text, "text must not be null");

    for (int i = 0; i <= text.length - minGram; i++) {
      String feature = "ng=";
      for (int y = 0; y < maxGram && i + y < text.length; y++) {
        feature = feature + ":" + text[i + y];
        int gramCount = y + 1;
        if (maxGram >= gramCount && gramCount >= minGram) {
          features.add(feature);
        }
      }
    }
  }
  
  private void checkMinMaxNGrams(int minGram, int maxGram) throws InvalidFormatException {
    if (minGram > 0 && maxGram > 0) {
      if (minGram >= maxGram) {
        throw new InvalidFormatException(
            "Minimum range value (minGram) should be less than or equal to maximum range value (maxGram)!");
      }
    } else {
      throw new InvalidFormatException("Both minimum range value (minGram) & maximum " +
          "range value (maxGram) should be greater than or equal to 1!");
    }
  }
  
  @Override
  public void clearFeatureData() {
  }

  @Override
  public void init(final Map<String, String> properties,
      final FeatureGeneratorResourceProvider resourceProvider)
      throws InvalidFormatException {
    this.attributes = properties;

  }
}
