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

package eus.ixa.ixa.pipe.ml.parse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerModel;
import opennlp.tools.ml.BeamSearch;
import opennlp.tools.ml.model.AbstractModel;
import opennlp.tools.ml.model.MaxentModel;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.model.ArtifactSerializer;
import opennlp.tools.util.model.BaseModel;
import opennlp.tools.util.model.UncloseableInputStream;

/**
 * ParserModel class. Based on opennlp.tools.parse.ParserModel.java.
 *
 * @author ragerri
 * @version 2016-05-03
 */
public class ParserModel extends BaseModel {

  private static final String COMPONENT_NAME = "shiftReducedParser";
  private static final String BUILD_MODEL_ENTRY_NAME = "build.model";
  private static final String CHECK_MODEL_ENTRY_NAME = "check.model";
  private static final String PARSER_TAGGER_MODEL_ENTRY_NAME = "parsertager.postagger";
  private static final String CHUNKER_TAGGER_MODEL_ENTRY_NAME = "parserchunker.chunker";
  private static final String HEAD_RULES_MODEL_ENTRY_NAME = "head-rules.headrules";

  public ParserModel(final String languageCode, final MaxentModel buildModel,
      final MaxentModel checkModel, final SequenceLabelerModel parserTagger,
      final SequenceLabelerModel chunkerTagger, final int beamSize,
      final HeadRules headRules,
      final Map<String, String> manifestInfoEntries) {

    super(COMPONENT_NAME, languageCode, manifestInfoEntries);

    // adding beamsize to manifest
    final Properties manifest = (Properties) this.artifactMap
        .get(MANIFEST_ENTRY);
    manifest.put(BeamSearch.BEAM_SIZE_PARAMETER, Integer.toString(beamSize));

    this.artifactMap.put(BUILD_MODEL_ENTRY_NAME, buildModel);
    this.artifactMap.put(CHECK_MODEL_ENTRY_NAME, checkModel);
    this.artifactMap.put(PARSER_TAGGER_MODEL_ENTRY_NAME, parserTagger);
    this.artifactMap.put(CHUNKER_TAGGER_MODEL_ENTRY_NAME, chunkerTagger);
    this.artifactMap.put(HEAD_RULES_MODEL_ENTRY_NAME, headRules);
    checkArtifactMap();
  }

  public ParserModel(final String languageCode, final MaxentModel buildModel,
      final MaxentModel checkModel, final SequenceLabelerModel parserTagger,
      final SequenceLabelerModel chunkerTagger, final int beamSize,
      final HeadRules headRules) {
    this(languageCode, buildModel, checkModel, parserTagger, chunkerTagger,
        beamSize, headRules, null);
  }

  public ParserModel(final InputStream in)
      throws IOException, InvalidFormatException {
    super(COMPONENT_NAME, in);
  }

  public ParserModel(final File modelFile)
      throws IOException, InvalidFormatException {
    super(COMPONENT_NAME, modelFile);
  }

  public ParserModel(final URL modelURL)
      throws IOException, InvalidFormatException {
    super(COMPONENT_NAME, modelURL);
  }

  @SuppressWarnings("rawtypes")
  @Override
  protected void createArtifactSerializers(
      final Map<String, ArtifactSerializer> serializers) {
    super.createArtifactSerializers(serializers);

    if (getLanguage().equalsIgnoreCase("es")) {
      serializers.put("headrules",
          new AncoraHeadRules.AncoraHeadRulesSerializer());
    } else {
      serializers.put("headrules",
          new PennTreebankHeadRules.PennTreebankHeadRulesSerializer());
    }
    serializers.put("postagger", new SequenceLabelerModelSerializer());
    serializers.put("chunker", new SequenceLabelerModelSerializer());
  }

  public MaxentModel getBuildModel() {
    return (MaxentModel) this.artifactMap.get(BUILD_MODEL_ENTRY_NAME);
  }

  public MaxentModel getCheckModel() {
    return (MaxentModel) this.artifactMap.get(CHECK_MODEL_ENTRY_NAME);
  }

  public SequenceLabelerModel getParserTaggerModel() {
    return (SequenceLabelerModel) this.artifactMap
        .get(PARSER_TAGGER_MODEL_ENTRY_NAME);
  }

  public SequenceLabelerModel getParserChunkerModel() {
    return (SequenceLabelerModel) this.artifactMap
        .get(CHUNKER_TAGGER_MODEL_ENTRY_NAME);
  }

  public HeadRules getHeadRules() {
    return (HeadRules) this.artifactMap.get(HEAD_RULES_MODEL_ENTRY_NAME);
  }

  public int getBeamSize() {
    final Properties manifest = (Properties) this.artifactMap
        .get(MANIFEST_ENTRY);
    final String beamSizeString = manifest
        .getProperty(BeamSearch.BEAM_SIZE_PARAMETER);

    int beamSize = ShiftReduceParser.DEFAULT_BEAMSIZE;
    if (beamSizeString != null) {
      beamSize = Integer.parseInt(beamSizeString);
    }
    return beamSize;
  }

  // TODO: Update model methods should make sure properties are copied correctly
  // ...
  public ParserModel updateBuildModel(final MaxentModel buildModel) {
    return new ParserModel(getLanguage(), buildModel, getCheckModel(),
        getParserTaggerModel(), getParserChunkerModel(), getBeamSize(),
        getHeadRules());
  }

  public ParserModel updateCheckModel(final MaxentModel checkModel) {
    return new ParserModel(getLanguage(), getBuildModel(), checkModel,
        getParserTaggerModel(), getParserChunkerModel(), getBeamSize(),
        getHeadRules());
  }

  public ParserModel updateTaggerModel(final SequenceLabelerModel taggerModel) {
    return new ParserModel(getLanguage(), getBuildModel(), getCheckModel(),
        taggerModel, getParserChunkerModel(), getBeamSize(), getHeadRules());
  }

  public ParserModel updateChunkerModel(final SequenceLabelerModel chunkModel) {
    return new ParserModel(getLanguage(), getBuildModel(), getCheckModel(),
        getParserTaggerModel(), chunkModel, getBeamSize(), getHeadRules());
  }

  @Override
  protected void validateArtifactMap() throws InvalidFormatException {
    super.validateArtifactMap();

    if (!(this.artifactMap
        .get(BUILD_MODEL_ENTRY_NAME) instanceof AbstractModel)) {
      throw new InvalidFormatException("Missing the build model!");
    }

    if (!(this.artifactMap
        .get(CHECK_MODEL_ENTRY_NAME) instanceof AbstractModel)) {
      throw new InvalidFormatException("Missing the check model!");
    }

    if (!(this.artifactMap
        .get(PARSER_TAGGER_MODEL_ENTRY_NAME) instanceof SequenceLabelerModel)) {
      throw new InvalidFormatException("Missing the tagger model!");
    }

    if (!(this.artifactMap.get(
        CHUNKER_TAGGER_MODEL_ENTRY_NAME) instanceof SequenceLabelerModel)) {
      throw new InvalidFormatException("Missing the chunker model!");
    }

    if (!(this.artifactMap
        .get(HEAD_RULES_MODEL_ENTRY_NAME) instanceof HeadRules)) {
      throw new InvalidFormatException("Missing the head rules!");
    }
  }

  private static class SequenceLabelerModelSerializer
      implements ArtifactSerializer<SequenceLabelerModel> {

    @Override
    public SequenceLabelerModel create(final InputStream in)
        throws IOException, InvalidFormatException {
      final SequenceLabelerModel posModel = new SequenceLabelerModel(
          new UncloseableInputStream(in));
      return posModel;
    }

    @Override
    public void serialize(final SequenceLabelerModel artifact,
        final OutputStream out) throws IOException {
      artifact.serialize(out);
    }
  }

}
