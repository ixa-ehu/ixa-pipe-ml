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

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.featuregen.CustomFeatureGenerator;
import opennlp.tools.util.featuregen.FeatureGeneratorResourceProvider;

/**
 * A baseline context generator for the POS Tagger.
 * @author ragerri
 * @version 2016-05-12
 */
public class POSBaselineContextGenerator extends CustomFeatureGenerator {

  /**
   * Default prefix length.
   */
  private static final int PREFIX_LENGTH = 4;
  /**
   * Default suffix length.
   */
  private static final int SUFFIX_LENGTH = 7;
  /**
   * Has capital regexp.
   */
  private static Pattern hasCap = Pattern.compile("[A-Z]");
  /**
   * Has number regexp.
   */
  private static Pattern hasNum = Pattern.compile("[0-9]");

  public POSBaselineContextGenerator() {
  }
  
  /**
   * Obtain prefixes for each token.
   * @param lex
   *          the current word
   * @return the prefixes
   */
  private static String[] getPrefixes(final String lex) {
    String[] prefs = new String[PREFIX_LENGTH];
    for (int li = 2, ll = PREFIX_LENGTH; li < ll; li++) {
      prefs[li] = lex.substring(0, Math.min(li + 1, lex.length()));
    }
    return prefs;
  }

  /**
   * Obtain suffixes for each token.
   * @param lex
   *          the word
   * @return the suffixes
   */
  private static String[] getSuffixes(final String lex) {
    String[] suffs = new String[SUFFIX_LENGTH];
    for (int li = 0, ll = SUFFIX_LENGTH; li < ll; li++) {
      suffs[li] = lex.substring(Math.max(lex.length() - li - 1, 0));
    }
    return suffs;
  }

  public void createFeatures(List<String> features, String[] tokens, int index,
      String[] previousOutcomes) {
    
    //words in a five word window
    String w_1, w0, w1;
    //previous predictions
    String p_2, p_1;
    
    if (index < 2) {
      p_2 = "bos";
    } else {
      p_2 = previousOutcomes[index - 2];
    }
    if (index < 1) {
      w_1 = "bos";
      p_1 = "bos";
    } else {
      w_1 = tokens[index - 1];
      p_1 = previousOutcomes[index - 1];
    }
    
    w0 = tokens[index];
    
    if (index + 1 >= tokens.length) {
      w1 = "eos";
    } else {
      w1 = tokens[index + 1];
    }
    
    features.add("w_1=" + w_1);
    features.add("w0=" + w0);
    features.add("w1=" + w1);
    features.add("w1,w0=" + w1 + "," + w0);
    features.add("w0,w1=" + w0 + "," + w1);
    features.add("p_1,p_2=" + p_1 + "," + p_2);
    
    addTokenShapeFeatures(features, w0);
  }
  
  private void addTokenShapeFeatures(List<String> features, String lex) {
    // do some basic suffix analysis
    String[] suffs = getSuffixes(lex);
    for (int i = 0; i < suffs.length; i++) {
      features.add("suf=" + suffs[i]);
    }
    String[] prefs = getPrefixes(lex);
    for (int i = 0; i < prefs.length; i++) {
      features.add("pre=" + prefs[i]);
    }
    // see if the word has any special characters
    if (lex.indexOf('-') != -1) {
      features.add("h");
    }
    if (hasCap.matcher(lex).find()) {
      features.add("c");
    }
    if (hasNum.matcher(lex).find()) {
      features.add("d");
    }
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
  }

}

