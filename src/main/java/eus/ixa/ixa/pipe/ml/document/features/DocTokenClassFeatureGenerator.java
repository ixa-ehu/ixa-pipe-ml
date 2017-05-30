/*
 *  Copyright 2014 Rodrigo Agerri

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
import java.util.regex.Pattern;

import eus.ixa.ixa.pipe.ml.utils.Flags;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.featuregen.FeatureGeneratorResourceProvider;
import opennlp.tools.util.featuregen.StringPattern;

/**
 * Generates a class name for the specified token. The classes are as follows
 * where the first matching class is used:
 * <ul>
 * <li>lc - lowercase alphabetic</li>
 * <li>2d - two digits</li>
 * <li>4d - four digits</li>
 * <li>an - alpha-numeric</li>
 * <li>dd - digits and dashes</li>
 * <li>ds - digits and slashes</li>
 * <li>dc - digits and commas</li>
 * <li>dp - digits and periods</li>
 * <li>num - digits</li>
 * <li>sc - single capital letter</li>
 * <li>ac - all capital letters</li>
 * <li>ic - initial capital letter</li>
 * <li>other - other</li>
 * </ul>
 */
public class DocTokenClassFeatureGenerator extends DocumentCustomFeatureGenerator {

  private boolean isLower;
  private boolean isWordAndClassFeature;
  private static Pattern capPeriod;
  static {
    capPeriod = Pattern.compile("^[A-Z]\\.$");
  }

  public DocTokenClassFeatureGenerator() {
  }

  @Override
  public void createFeatures(final List<String> features, final String[] tokens) {
    
    for (String token : tokens) {
      final String wordClass = tokenShapeFeature(token);
      features.add("wc=" + wordClass);

      if (this.isWordAndClassFeature) {
        if (this.isLower) {
          features.add("w&c=" + token.toLowerCase() + "," + wordClass);
        } else {
          features.add("w&c=" + token + "," + wordClass);
        }
      }
      if (Flags.DEBUG) {
        System.err.println("-> " + token.toLowerCase() + ": w&c="
            + token.toLowerCase() + "," + wordClass);
      }
    }
  }
    

  public static String tokenShapeFeature(final String token) {

    final StringPattern pattern = StringPattern.recognize(token);

    String feat;
    if (pattern.isAllLowerCaseLetter()) {
      feat = "lc";
    } else if (pattern.digits() == 2) {
      feat = "2d";
    } else if (pattern.digits() == 4) {
      feat = "4d";
    } else if (pattern.containsDigit()) {
      if (pattern.containsLetters()) {
        feat = "an";
      } else if (pattern.containsHyphen()) {
        feat = "dd";
      } else if (pattern.containsSlash()) {
        feat = "ds";
      } else if (pattern.containsComma()) {
        feat = "dc";
      } else if (pattern.containsPeriod()) {
        feat = "dp";
      } else {
        feat = "num";
      }
    } else if (pattern.isAllCapitalLetter() && token.length() == 1) {
      feat = "sc";
    } else if (pattern.isAllCapitalLetter()) {
      feat = "ac";
    } else if (capPeriod.matcher(token).find()) {
      feat = "cp";
    } else if (pattern.isInitialCapitalLetter()) {
      feat = "ic";
    } else {
      feat = "other";
    }
    return feat;
  }

  @Override
  public void clearFeatureData() {
  }

  @Override
  public void init(final Map<String, String> properties,
      final FeatureGeneratorResourceProvider resourceProvider)
      throws InvalidFormatException {
    processRangeOptions(properties);
  }

  /**
   * Process the options of which type of features are to be generated.
   * 
   * @param properties
   *          the properties map
   */
  private void processRangeOptions(final Map<String, String> properties) {
    final String featuresRange = properties.get("range");
    final String[] rangeArray = Flags
        .processTokenClassFeaturesRange(featuresRange);
    if (rangeArray[0].equalsIgnoreCase("lower")) {
      this.isLower = true;
    }
    if (rangeArray[1].equalsIgnoreCase("wac")) {
      this.isWordAndClassFeature = true;
    }
  }

}
