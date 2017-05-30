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

import java.util.List;
import java.util.Map;

import eus.ixa.ixa.pipe.ml.utils.Flags;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.featuregen.FeatureGeneratorResourceProvider;

public class DocSuffixFeatureGenerator extends DocumentCustomFeatureGenerator {

  private Map<String, String> attributes;

  public String[] getSuffixes(final String lex) {
    final Integer start = Integer.parseInt(this.attributes.get("begin"));
    final Integer end = Integer.parseInt(this.attributes.get("end"));
    final String[] suffs = new String[end];
    for (int i = start, l = end; i < l; i++) {
      suffs[i] = lex.substring(Math.max(lex.length() - i - 1, 0));
    }
    return suffs;
  }

  @Override
  public void createFeatures(final List<String> features, final String[] tokens) {
    for (String token : tokens) {
      final String[] suffs = getSuffixes(token);
      for (final String suff : suffs) {
        features.add("suf=" + suff);
        if (Flags.DEBUG) {
          System.err.println("-> " + token + ": suf=" + suff);
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
    this.attributes = properties;
  }
}