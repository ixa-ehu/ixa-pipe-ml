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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Map;

import opennlp.tools.ml.model.AbstractModel;
import opennlp.tools.ml.model.MaxentModel;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.model.ArtifactSerializer;
import opennlp.tools.util.model.BaseModel;
import opennlp.tools.util.model.UncloseableInputStream;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerModel;

/**
 * ParserModel class. Based on opennlp.tools.parse.ParserModel.java.
 * @author ragerri
 * @version 2016-05-03
 */
public class ParserModel extends BaseModel {

  private static class SequenceLabelerModelSerializer implements
      ArtifactSerializer<SequenceLabelerModel> {

    public SequenceLabelerModel create(InputStream in) throws IOException,
        InvalidFormatException {
      SequenceLabelerModel posModel = new SequenceLabelerModel(
          new UncloseableInputStream(in));
      return posModel;
    }
    public void serialize(SequenceLabelerModel artifact, OutputStream out)
        throws IOException {
      artifact.serialize(out);
    }
  }

  public static class PennTreebankHeadRulesSerializer implements
      ArtifactSerializer<PennTreebankHeadRules> {

    public PennTreebankHeadRules create(InputStream in) throws IOException,
        InvalidFormatException {
      return new PennTreebankHeadRules(new BufferedReader(
          new InputStreamReader(in, "UTF-8")));
    }
    public void serialize(PennTreebankHeadRules artifact, OutputStream out)
        throws IOException {
      artifact.serialize(new OutputStreamWriter(out, "UTF-8"));
    }
  }

  public static class AncoraHeadRulesSerializer implements
      ArtifactSerializer<AncoraHeadRules> {

    public AncoraHeadRules create(InputStream in) throws IOException,
        InvalidFormatException {
      return new AncoraHeadRules(new BufferedReader(new InputStreamReader(in,
          "UTF-8")));
    }
    public void serialize(AncoraHeadRules artifact, OutputStream out)
        throws IOException {
      artifact.serialize(new OutputStreamWriter(out, "UTF-8"));
    }
  }

  private static final String COMPONENT_NAME = "shiftReducedParser";
  private static final String BUILD_MODEL_ENTRY_NAME = "build.model";
  private static final String CHECK_MODEL_ENTRY_NAME = "check.model";
  private static final String PARSER_TAGGER_MODEL_ENTRY_NAME = "parsertager.postagger";
  private static final String CHUNKER_TAGGER_MODEL_ENTRY_NAME = "parserchunker.chunker";
  private static final String HEAD_RULES_MODEL_ENTRY_NAME = "head-rules.headrules";

  public ParserModel(String languageCode, MaxentModel buildModel,
      MaxentModel checkModel, SequenceLabelerModel parserTagger,
      SequenceLabelerModel chunkerTagger, HeadRules headRules,
      Map<String, String> manifestInfoEntries) {

    super(COMPONENT_NAME, languageCode, manifestInfoEntries);
    artifactMap.put(BUILD_MODEL_ENTRY_NAME, buildModel);
    artifactMap.put(CHECK_MODEL_ENTRY_NAME, checkModel);
    artifactMap.put(PARSER_TAGGER_MODEL_ENTRY_NAME, parserTagger);
    artifactMap.put(CHUNKER_TAGGER_MODEL_ENTRY_NAME, chunkerTagger);
    artifactMap.put(HEAD_RULES_MODEL_ENTRY_NAME, headRules);
    checkArtifactMap();
  }

  public ParserModel(String languageCode, MaxentModel buildModel,
      MaxentModel checkModel, SequenceLabelerModel parserTagger,
      SequenceLabelerModel chunkerTagger, HeadRules headRules) {
    this(languageCode, buildModel, checkModel, parserTagger, chunkerTagger,
        headRules, null);
  }

  public ParserModel(InputStream in) throws IOException, InvalidFormatException {
    super(COMPONENT_NAME, in);
  }

  public ParserModel(File modelFile) throws IOException, InvalidFormatException {
    super(COMPONENT_NAME, modelFile);
  }

  public ParserModel(URL modelURL) throws IOException, InvalidFormatException {
    super(COMPONENT_NAME, modelURL);
  }

  @Override
  protected void createArtifactSerializers(
      Map<String, ArtifactSerializer> serializers) {
    super.createArtifactSerializers(serializers);

    if (getLanguage().equalsIgnoreCase("es")) {
      serializers.put("headrules", new AncoraHeadRulesSerializer());
    } else {
      serializers.put("headrules", new PennTreebankHeadRulesSerializer());
    }
    serializers.put("postagger", new SequenceLabelerModelSerializer());
    serializers.put("chunker", new SequenceLabelerModelSerializer());
  }

  public MaxentModel getBuildModel() {
    return (MaxentModel) artifactMap.get(BUILD_MODEL_ENTRY_NAME);
  }

  public MaxentModel getCheckModel() {
    return (MaxentModel) artifactMap.get(CHECK_MODEL_ENTRY_NAME);
  }

  public SequenceLabelerModel getParserTaggerModel() {
    return (SequenceLabelerModel) artifactMap
        .get(PARSER_TAGGER_MODEL_ENTRY_NAME);
  }

  public SequenceLabelerModel getParserChunkerModel() {
    return (SequenceLabelerModel) artifactMap
        .get(CHUNKER_TAGGER_MODEL_ENTRY_NAME);
  }

  public HeadRules getHeadRules() {
    return (HeadRules) artifactMap.get(HEAD_RULES_MODEL_ENTRY_NAME);
  }

  // TODO: Update model methods should make sure properties are copied correctly
  // ...
  public ParserModel updateBuildModel(MaxentModel buildModel) {
    return new ParserModel(getLanguage(), buildModel, getCheckModel(),
        getParserTaggerModel(), getParserChunkerModel(), getHeadRules());
  }

  public ParserModel updateCheckModel(MaxentModel checkModel) {
    return new ParserModel(getLanguage(), getBuildModel(), checkModel,
        getParserTaggerModel(), getParserChunkerModel(), getHeadRules());
  }

  public ParserModel updateTaggerModel(SequenceLabelerModel taggerModel) {
    return new ParserModel(getLanguage(), getBuildModel(), getCheckModel(),
        taggerModel, getParserChunkerModel(), getHeadRules());
  }

  public ParserModel updateChunkerModel(SequenceLabelerModel chunkModel) {
    return new ParserModel(getLanguage(), getBuildModel(), getCheckModel(),
        getParserTaggerModel(), chunkModel, getHeadRules());
  }

  @Override
  protected void validateArtifactMap() throws InvalidFormatException {
    super.validateArtifactMap();

    if (!(artifactMap.get(BUILD_MODEL_ENTRY_NAME) instanceof AbstractModel)) {
      throw new InvalidFormatException("Missing the build model!");
    }

    if (!(artifactMap.get(CHECK_MODEL_ENTRY_NAME) instanceof AbstractModel)) {
      throw new InvalidFormatException("Missing the check model!");
    }

    if (!(artifactMap.get(PARSER_TAGGER_MODEL_ENTRY_NAME) instanceof SequenceLabelerModel)) {
      throw new InvalidFormatException("Missing the tagger model!");
    }

    if (!(artifactMap.get(CHUNKER_TAGGER_MODEL_ENTRY_NAME) instanceof SequenceLabelerModel)) {
      throw new InvalidFormatException("Missing the chunker model!");
    }

    if (!(artifactMap.get(HEAD_RULES_MODEL_ENTRY_NAME) instanceof HeadRules)) {
      throw new InvalidFormatException("Missing the head rules!");
    }
  }
}
