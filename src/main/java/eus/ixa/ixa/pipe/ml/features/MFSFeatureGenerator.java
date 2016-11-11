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
import eus.ixa.ixa.pipe.ml.utils.Flags;
import eus.ixa.ixa.pipe.ml.utils.Span;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.featuregen.ArtifactToSerializerMapper;
import opennlp.tools.util.featuregen.CustomFeatureGenerator;
import opennlp.tools.util.featuregen.FeatureGeneratorResourceProvider;
import opennlp.tools.util.model.ArtifactSerializer;

/**
 * Generate pos tag, pos tag class, lemma and most frequent sense as feature of
 * the current token. This feature generator can also be placed in a sliding
 * window.
 *
 * @author ragerri
 * @version 2015-03-27
 */
public class MFSFeatureGenerator extends CustomFeatureGenerator
    implements ArtifactToSerializerMapper {

  private SequenceModelResource posModelResource;
  private DictionaryLemmatizer lemmaDictResource;
  private MFSResource mfsDictResource;
  private String[] currentSentence;
  private Span[] currentTags;
  private List<String> currentLemmas;
  private List<String> currentMFSList;
  private boolean isPos;
  private boolean isPosClass;
  private boolean isLemma;
  private boolean isMFS;
  private boolean isMonosemic;
  private boolean isBio;

  public MFSFeatureGenerator() {
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
      if (this.isBio) {
        this.currentMFSList = this.mfsDictResource
            .getFirstSenseBio(this.currentLemmas, this.currentTags);
      } else {
        this.currentMFSList = this.mfsDictResource
            .getFirstSenseBilou(this.currentLemmas, this.currentTags);
      }
    }
    final String posTag = this.currentTags[index].getType();

    if (this.isPos) {
      features.add("posTag=" + posTag);
    }
    if (this.isPosClass) {
      final String posTagClass = posTag.substring(0, 1);
      features.add("posTagClass=" + posTagClass);

    }
    if (this.isLemma) {
      final String lemma = this.currentLemmas.get(index);
      features.add("lemma=" + lemma);
    }
    if (this.isMFS) {
      final String mfs = this.currentMFSList.get(index);
      features.add("mfs=" + mfs);
      features.add("mfs,lemma=" + mfs + "," + this.currentLemmas.get(index));

    }
    if (this.isMonosemic) {

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
    processRangeOptions(properties);
    if (properties.get("seqCodec").equalsIgnoreCase("bilou")) {
      this.isBio = false;
    } else {
      this.isBio = true;
    }
  }

  /**
   * Process the options of which kind of features are to be generated.
   *
   * @param properties
   *          the properties map
   */
  private void processRangeOptions(final Map<String, String> properties) {
    final String featuresRange = properties.get("range");
    final String[] rangeArray = Flags.processMFSFeaturesRange(featuresRange);
    // options
    if (rangeArray[0].equalsIgnoreCase("pos")) {
      this.isPos = true;
    }
    if (rangeArray[1].equalsIgnoreCase("posclass")) {
      this.isPosClass = true;
    }
    if (rangeArray[2].equalsIgnoreCase("lemma")) {
      this.isLemma = true;
    }
    if (rangeArray[3].equalsIgnoreCase("mfs")) {
      this.isMFS = true;
    }
    if (rangeArray[4].equalsIgnoreCase("monosemic")) {
      this.isMonosemic = true;
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
