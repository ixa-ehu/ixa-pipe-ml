/*
 * Copyright 2015 Rodrigo Agerri

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
import eus.ixa.ixa.pipe.ml.resources.MFSResource;
import eus.ixa.ixa.pipe.ml.resources.SequenceModelResource;
import eus.ixa.ixa.pipe.ml.utils.Span;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.featuregen.ArtifactToSerializerMapper;
import opennlp.tools.util.featuregen.CustomFeatureGenerator;
import opennlp.tools.util.featuregen.FeatureGeneratorResourceProvider;
import opennlp.tools.util.model.ArtifactSerializer;

/**
 * Generates Ciaramita and Altun (2006) features for super sense tagging. Do not
 * use this SuperSenseFeatureGenerator with either the MorphoFeatureGenerator or
 * the MFSFeatureGenerator as both Morphological and MFS features are included
 * in this feature generator.
 *
 * @author ragerri
 * @version 2015-03-30
 */
public class SuperSenseFeatureGenerator extends CustomFeatureGenerator
    implements ArtifactToSerializerMapper {

  private SequenceModelResource posModelResource;
  private DictionaryLemmatizer lemmaDictResource;
  private MFSResource mfsDictResource;
  private String[] currentSentence;
  private Span[] currentTags;
  private List<String> currentLemmas;
  private List<String> currentMFSList;
  private final String startSymbol = null;
  private final String endSymbol = null;
  private Boolean isBio = true;

  public SuperSenseFeatureGenerator() {
  }

  @Override
  public void createFeatures(final List<String> features, final String[] tokens,
      final int index, final String[] previousOutcomes) {

    // cache results for each sentence
    if (this.currentSentence != tokens) {
      this.currentSentence = tokens;
      this.currentTags = this.posModelResource.seqToSpans(tokens);
      this.currentLemmas = this.lemmaDictResource.lemmatize(tokens,
          this.currentTags);
      if (this.isBio) {
        this.currentMFSList = this.mfsDictResource
            .getFirstSenseBio(this.currentLemmas, this.currentTags);
      } else {
        this.currentMFSList = this.mfsDictResource
            .getFirstSenseBilou(this.currentLemmas, this.currentTags);
      }
    }

    final String curLemma = this.currentLemmas.get(index);
    final String curTok = tokens[index];
    final String curPOS = this.currentTags[index].getType();
    final String curShape = WordShapeSuperSenseFeatureGenerator
        .normalize(tokens[index]);
    String firstSense = this.currentMFSList.get(index);
    String prevLabel = this.startSymbol;

    String prevShape = this.startSymbol;
    String prevPOS = this.startSymbol;
    String prevLemma = this.startSymbol;
    String nextShape = this.startSymbol;
    String nextPOS = this.endSymbol;
    String nextLemma = this.endSymbol;

    String prev2Shape = this.startSymbol;
    String prev2POS = this.startSymbol;
    String prev2Lemma = this.startSymbol;
    String next2Shape = this.startSymbol;
    String next2POS = this.endSymbol;
    String next2Lemma = this.endSymbol;

    if (index - 2 >= 0) {
      prev2Shape = WordShapeSuperSenseFeatureGenerator
          .normalize(tokens[index - 2]);
      prev2Lemma = this.currentLemmas.get(index - 2);
      prev2POS = this.currentTags[index - 2].getType();
    }
    if (index - 1 >= 0) {
      prevShape = WordShapeSuperSenseFeatureGenerator
          .normalize(tokens[index - 1]);
      prevLemma = this.currentLemmas.get(index - 1);
      prevPOS = this.currentTags[index - 1].getType();
      prevLabel = previousOutcomes[index - 1];
    }
    if (index + 1 < tokens.length) {
      nextShape = WordShapeSuperSenseFeatureGenerator
          .normalize(tokens[index + 1]);
      nextLemma = this.currentLemmas.get(index + 1);
      nextPOS = this.currentTags[index + 1].getType();
    }
    if (index + 2 < tokens.length) {
      next2Shape = WordShapeSuperSenseFeatureGenerator
          .normalize(tokens[index + 2]);
      next2Lemma = this.currentLemmas.get(index + 2);
      next2POS = this.currentTags[index + 2].getType();
    }

    features.add("bias");

    if (firstSense == null) {
      firstSense = "O";
    }
    features.add("firstSense=" + firstSense);
    features.add("firstSense,curTok=" + firstSense + "," + curLemma);

    if (prevLabel != this.startSymbol) {
      features.add("prevLabel=" + prevLabel);
    }

    if (curPOS.equals("NN") || curPOS.equals("NNS")) {
      features.add("curPOS_common");
    }
    if (curPOS.equals("NNP") || curPOS.equals("NNPS")) {
      features.add("curPOS_proper");
    }
    features.add("curTok=" + curLemma);
    features.add("curPOS=" + curPOS);
    features.add("curPOS_0" + curPOS.charAt(0));

    if (prevPOS != this.startSymbol) {
      features.add("prevTok=" + prevLemma);
      features.add("prevPOS=" + prevPOS);
      features.add("prevPOS_0=" + prevPOS.charAt(0));
    }

    if (nextPOS != this.endSymbol) {
      features.add("nextTok=" + nextLemma);
      features.add("nextPOS=" + nextPOS);
      features.add("nextPOS_0" + nextPOS.charAt(0));
    }

    if (prev2POS != this.startSymbol) {
      features.add("prev2Tok=" + prev2Lemma);
      features.add("prev2POS=" + prev2POS);
      features.add("prev2POS_0=" + prev2POS.charAt(0));
    }
    if (next2POS != this.startSymbol) {
      features.add("next2Tok=" + next2Lemma);
      features.add("next2POS=" + next2POS);
      features.add("next2POS_0=" + next2POS.charAt(0));
    }

    features.add("curShape=" + curShape);
    if (prevPOS != this.startSymbol) {
      features.add("prevShape=" + prevShape);
    }
    if (nextPOS != this.endSymbol) {
      features.add("nextShape=" + nextShape);
    }
    if (prev2POS != this.startSymbol) {
      features.add("prev2Shape=" + prev2Shape);
    }
    if (next2POS != this.endSymbol) {
      features.add("next2Shape=" + next2Shape);
    }

    // word shapes with no window
    final String firstCharCurTok = curTok.substring(0, 1);
    if (firstCharCurTok.toLowerCase().equals(firstCharCurTok)) {
      features.add("curTokLowercase");
    } else if (index == 0) {
      features.add("curTokUpperCaseFirstChar");
    } else {
      features.add("curTokUpperCaseOther");
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
          "Not a POSModelResource for key: " + properties.get("model"));
    }
    this.posModelResource = (SequenceModelResource) posResource;
    final Object lemmaResource = resourceProvider
        .getResource(properties.get("dict"));
    if (!(lemmaResource instanceof DictionaryLemmatizer)) {
      throw new InvalidFormatException(
          "Not a LemmaResource for key: " + properties.get("dict"));
    }
    this.lemmaDictResource = (DictionaryLemmatizer) lemmaResource;
    final Object mfsResource = resourceProvider
        .getResource(properties.get("mfs"));
    if (!(mfsResource instanceof MFSResource)) {
      throw new InvalidFormatException(
          "Not a MFSResource for key: " + properties.get("mfs"));
    }
    this.mfsDictResource = (MFSResource) mfsResource;
    if (properties.get("seqCodec").equalsIgnoreCase("bilou")) {
      this.isBio = false;
    } else {
      this.isBio = true;
    }
  }

  @Override
  public Map<String, ArtifactSerializer<?>> getArtifactSerializerMapping() {
    final Map<String, ArtifactSerializer<?>> mapping = new HashMap<>();
    mapping.put("posmodelserializer",
        new SequenceModelResource.SequenceModelResourceSerializer());
    mapping.put("lemmadictserializer",
        new DictionaryLemmatizer.DictionaryLemmatizerSerializer());
    mapping.put("mfsdictserializer", new MFSResource.MFSResourceSerializer());
    return Collections.unmodifiableMap(mapping);
  }
}
