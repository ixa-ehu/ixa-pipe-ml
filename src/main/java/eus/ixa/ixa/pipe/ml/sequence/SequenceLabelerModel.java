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

package eus.ixa.ixa.pipe.ml.sequence;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import opennlp.tools.ml.BeamSearch;
import opennlp.tools.ml.model.MaxentModel;
import opennlp.tools.ml.model.SequenceClassificationModel;
import opennlp.tools.util.BaseToolFactory;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.model.ArtifactSerializer;
import opennlp.tools.util.model.BaseModel;
import opennlp.tools.util.model.ModelUtil;

/**
 * The {@link SequenceLabelerModel} is the model used by a learnable
 * {@link SequenceLabeler}.
 *
 * @see SequenceLabelerME
 */
// TODO: Fix the model validation, on loading via constructors and input streams
public class SequenceLabelerModel extends BaseModel {

  public static class FeatureGeneratorCreationError extends RuntimeException {
    /**
     * Default serial version.
     */
    private static final long serialVersionUID = 1L;

    FeatureGeneratorCreationError(final Throwable t) {
      super(t);
    }
  }

  private static class ByteArraySerializer
      implements ArtifactSerializer<byte[]> {

    @Override
    public byte[] create(final InputStream in)
        throws IOException, InvalidFormatException {

      return ModelUtil.read(in);
    }

    @Override
    public void serialize(final byte[] artifact, final OutputStream out)
        throws IOException {
      out.write(artifact);
    }
  }

  private static final String COMPONENT_NAME = "SequenceLabelerME";
  private static final String MAXENT_MODEL_ENTRY_NAME = "sequenceLabeler.model";

  static final String GENERATOR_DESCRIPTOR_ENTRY_NAME = "generator.featuregen";

  static final String SEQUENCE_CODEC_CLASS_NAME_PARAMETER = "sequenceCodecImplName";

  public SequenceLabelerModel(final String languageCode,
      final SequenceClassificationModel<String> nameFinderModel,
      final byte[] generatorDescriptor, final Map<String, Object> resources,
      final Map<String, String> manifestInfoEntries,
      final SequenceLabelerCodec<String> seqCodec,
      final SequenceLabelerFactory factory) {
    super(COMPONENT_NAME, languageCode, manifestInfoEntries, factory);

    init(nameFinderModel, generatorDescriptor, resources, manifestInfoEntries,
        seqCodec);

    if (!seqCodec.areOutcomesCompatible(nameFinderModel.getOutcomes())) {
      throw new IllegalArgumentException(
          "Model not compatible with name finder!");
    }
  }

  public SequenceLabelerModel(final String languageCode,
      final MaxentModel nameFinderModel, final int beamSize,
      final byte[] generatorDescriptor, final Map<String, Object> resources,
      final Map<String, String> manifestInfoEntries,
      final SequenceLabelerCodec<String> seqCodec,
      final SequenceLabelerFactory factory) {
    super(COMPONENT_NAME, languageCode, manifestInfoEntries, factory);

    final Properties manifest = (Properties) this.artifactMap
        .get(MANIFEST_ENTRY);
    manifest.put(BeamSearch.BEAM_SIZE_PARAMETER, Integer.toString(beamSize));

    init(nameFinderModel, generatorDescriptor, resources, manifestInfoEntries,
        seqCodec);

    if (!isModelValid(nameFinderModel)) {
      throw new IllegalArgumentException(
          "Model not compatible with name finder!");
    }
  }

  // TODO: Extend this one with beam size!
  public SequenceLabelerModel(final String languageCode,
      final MaxentModel nameFinderModel, final byte[] generatorDescriptor,
      final Map<String, Object> resources,
      final Map<String, String> manifestInfoEntries) {
    this(languageCode, nameFinderModel, SequenceLabelerME.DEFAULT_BEAM_SIZE,
        generatorDescriptor, resources, manifestInfoEntries, new BioCodec(),
        new SequenceLabelerFactory());
  }

  public SequenceLabelerModel(final String languageCode,
      final MaxentModel nameFinderModel, final Map<String, Object> resources,
      final Map<String, String> manifestInfoEntries) {
    this(languageCode, nameFinderModel, null, resources, manifestInfoEntries);
  }

  public SequenceLabelerModel(final InputStream in)
      throws IOException, InvalidFormatException {
    super(COMPONENT_NAME, in);
  }

  public SequenceLabelerModel(final File modelFile)
      throws IOException, InvalidFormatException {
    super(COMPONENT_NAME, modelFile);
  }

  public SequenceLabelerModel(final URL modelURL)
      throws IOException, InvalidFormatException {
    super(COMPONENT_NAME, modelURL);
  }

  private void init(final Object nameFinderModel,
      final byte[] generatorDescriptor, final Map<String, Object> resources,
      final Map<String, String> manifestInfoEntries,
      final SequenceLabelerCodec<String> seqCodec) {

    final Properties manifest = (Properties) this.artifactMap
        .get(MANIFEST_ENTRY);
    manifest.put(SEQUENCE_CODEC_CLASS_NAME_PARAMETER,
        seqCodec.getClass().getName());

    this.artifactMap.put(MAXENT_MODEL_ENTRY_NAME, nameFinderModel);

    if (generatorDescriptor != null && generatorDescriptor.length > 0) {
      this.artifactMap.put(GENERATOR_DESCRIPTOR_ENTRY_NAME,
          generatorDescriptor);
    }

    if (resources != null) {
      // The resource map must not contain key which are already taken
      // like the name finder maxent model name
      if (resources.containsKey(MAXENT_MODEL_ENTRY_NAME)
          || resources.containsKey(GENERATOR_DESCRIPTOR_ENTRY_NAME)) {
        throw new IllegalArgumentException();
      }

      // TODO: Add checks to not put resources where no serializer exists,
      // make that case fail here, should be done in the BaseModel
      this.artifactMap.putAll(resources);
    }
    checkArtifactMap();
  }

  @SuppressWarnings({ "unchecked" })
  public SequenceClassificationModel<String> getSequenceLabelerModel() {

    final Properties manifest = (Properties) this.artifactMap
        .get(MANIFEST_ENTRY);

    if (this.artifactMap.get(MAXENT_MODEL_ENTRY_NAME) instanceof MaxentModel) {
      final String beamSizeString = manifest
          .getProperty(BeamSearch.BEAM_SIZE_PARAMETER);

      int beamSize = SequenceLabelerME.DEFAULT_BEAM_SIZE;
      if (beamSizeString != null) {
        beamSize = Integer.parseInt(beamSizeString);
      }

      return new BeamSearch<>(beamSize,
          (MaxentModel) this.artifactMap.get(MAXENT_MODEL_ENTRY_NAME));
    } else if (this.artifactMap
        .get(MAXENT_MODEL_ENTRY_NAME) instanceof SequenceClassificationModel) {
      return (SequenceClassificationModel<String>) this.artifactMap
          .get(MAXENT_MODEL_ENTRY_NAME);
    } else {
      return null;
    }
  }

  @Override
  protected Class<? extends BaseToolFactory> getDefaultFactory() {
    return SequenceLabelerFactory.class;
  }

  public SequenceLabelerFactory getFactory() {
    return (SequenceLabelerFactory) this.toolFactory;
  }

  @Override
  protected void createArtifactSerializers(
      @SuppressWarnings("rawtypes") final Map<String, ArtifactSerializer> serializers) {
    super.createArtifactSerializers(serializers);

    serializers.put("featuregen", new ByteArraySerializer());
  }

  /**
   * Create the artifact serializers. The DefaultTrainer deals with any other
   * Custom serializers.
   * 
   * @return the map containing the added serializers
   */
  @SuppressWarnings("rawtypes")
  public static Map<String, ArtifactSerializer> createArtifactSerializers() {
    final Map<String, ArtifactSerializer> serializers = BaseModel
        .createArtifactSerializers();

    serializers.put("featuregen", new ByteArraySerializer());
    return serializers;
  }

  boolean isModelValid(final MaxentModel model) {

    final String outcomes[] = new String[model.getNumOutcomes()];

    for (int i = 0; i < model.getNumOutcomes(); i++) {
      outcomes[i] = model.getOutcome(i);
    }

    return getFactory().createSequenceCodec().areOutcomesCompatible(outcomes);
  }

  @Override
  protected void validateArtifactMap() throws InvalidFormatException {
    super.validateArtifactMap();

    if (this.artifactMap.get(MAXENT_MODEL_ENTRY_NAME) instanceof MaxentModel
        || this.artifactMap.get(
            MAXENT_MODEL_ENTRY_NAME) instanceof SequenceClassificationModel) {
      // TODO: Check should be performed on the possible outcomes!
      // MaxentModel model = (MaxentModel)
      // artifactMap.get(MAXENT_MODEL_ENTRY_NAME);
      // isModelValid(model);
    } else {
      throw new InvalidFormatException(
          "Token Name Finder model is incomplete!");
    }
  }
}
