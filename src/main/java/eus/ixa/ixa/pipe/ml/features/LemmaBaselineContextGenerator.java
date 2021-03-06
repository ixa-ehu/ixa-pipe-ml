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
import java.util.regex.Pattern;

import eus.ixa.ixa.pipe.ml.resources.SequenceModelResource;
import eus.ixa.ixa.pipe.ml.utils.Flags;
import eus.ixa.ixa.pipe.ml.utils.Span;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.featuregen.ArtifactToSerializerMapper;
import opennlp.tools.util.featuregen.CustomFeatureGenerator;
import opennlp.tools.util.featuregen.FeatureGeneratorResourceProvider;
import opennlp.tools.util.model.ArtifactSerializer;

/**
 * Implements a Baseline Feature Generator for Statistical Lemmatization.
 * 
 * @author ragerri
 * @version 2016-05-12
 */
public class LemmaBaselineContextGenerator extends CustomFeatureGenerator
    implements ArtifactToSerializerMapper {

  private Map<String, String> attributes;
  private SequenceModelResource posModelResource;
  private Span[] currentTags;
  private String[] currentSentence;

  /**
   * Has capital regexp.
   */
  private static Pattern hasCap = Pattern.compile("\\p{Upper}",
      Pattern.UNICODE_CHARACTER_CLASS);
  /**
   * Has number regexp.
   */
  private static Pattern hasNum = Pattern.compile("\\p{Digit}",
      Pattern.UNICODE_CHARACTER_CLASS);
  private boolean isPos;
  private boolean isPosClass;

  public LemmaBaselineContextGenerator() {
  }

  private String[] getPrefixes(final String lex) {
    final Integer start = Integer.parseInt(this.attributes.get("prefBegin"));
    final Integer end = Integer.parseInt(this.attributes.get("prefEnd"));
    final String[] prefs = new String[end];
    for (int li = start, ll = end; li < ll; li++) {
      prefs[li] = lex.substring(0, Math.min(li + 1, lex.length()));
    }
    return prefs;
  }

  private String[] getSuffixes(final String lex) {
    final Integer start = Integer.parseInt(this.attributes.get("sufBegin"));
    final Integer end = Integer.parseInt(this.attributes.get("sufEnd"));
    final String[] suffs = new String[end];
    for (int li = start, ll = end; li < ll; li++) {
      suffs[li] = lex.substring(Math.max(lex.length() - li - 1, 0));
    }
    return suffs;
  }

  @Override
  public void createFeatures(final List<String> features, final String[] tokens,
      final int index, final String[] previousOutcomes) {

    // cache annotation results for each sentence
    if (this.currentSentence != tokens) {
      this.currentSentence = tokens;
      this.currentTags = this.posModelResource.seqToSpans(tokens);
    }
    // previous prediction
    String p_1;
    // words
    String w0, w1;
    // pos tags
    final String posTag = this.currentTags[index].getType();
    final String posTagClass = posTag.substring(0, 1);

    if (index < 1) {
      p_1 = "bos";
    } else {
      p_1 = previousOutcomes[index - 1];
    }

    w0 = tokens[index];

    if (index + 1 >= tokens.length) {
      w1 = "eos";
    } else {
      w1 = tokens[index + 1];
    }

    // adding features
    features.add("p_1=" + p_1);
    features.add("w0=" + w0);
    features.add("w1=" + w1);
    addTokenShapeFeatures(features, w0);

    if (this.isPos) {
      features.add("t0=" + posTag);
    }
    if (this.isPosClass) {
      features.add("posTagClass=" + posTagClass);
      features.add("p_1,posTagClass=" + p_1 + "," + posTagClass);
    }
  }

  private void addTokenShapeFeatures(final List<String> features,
      final String lex) {
    final String[] suffs = getSuffixes(lex);
    for (final String suff : suffs) {
      features.add("suf=" + suff);
    }
    final String[] prefs = getPrefixes(lex);
    for (final String pref : prefs) {
      features.add("pre=" + pref);
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
          "Not a SequenceModelResource for key: " + properties.get("model"));
    }
    this.posModelResource = (SequenceModelResource) posResource;
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
    final String[] rangeArray = Flags
        .processLemmaBaselineFeaturesRange(featuresRange);
    if (rangeArray[0].equalsIgnoreCase("pos")) {
      this.isPos = true;
    }
    if (rangeArray[1].equalsIgnoreCase("posclass")) {
      this.isPosClass = true;
    }
    this.attributes = properties;
  }

  @Override
  public Map<String, ArtifactSerializer<?>> getArtifactSerializerMapping() {
    final Map<String, ArtifactSerializer<?>> mapping = new HashMap<>();
    mapping.put("seqmodelserializer",
        new SequenceModelResource.SequenceModelResourceSerializer());
    return Collections.unmodifiableMap(mapping);
  }

}
