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
 * The {@link SequenceLabelerModel} is the model used
 * by a learnable {@link SequenceLabeler}.
 *
 * @see SequenceLabelerME
 */
// TODO: Fix the model validation, on loading via constructors and input streams
public class SequenceLabelerModel extends BaseModel {

  public static class FeatureGeneratorCreationError extends RuntimeException {
    FeatureGeneratorCreationError(Throwable t) {
      super(t);
    }
  }

  private static class ByteArraySerializer implements ArtifactSerializer<byte[]> {

    public byte[] create(InputStream in) throws IOException,
        InvalidFormatException {

      return ModelUtil.read(in);
    }

    public void serialize(byte[] artifact, OutputStream out) throws IOException {
      out.write(artifact);
    }
  }

  private static final String COMPONENT_NAME = "SequenceLabelerME";
  private static final String MAXENT_MODEL_ENTRY_NAME = "sequenceLabeler.model";

  static final String GENERATOR_DESCRIPTOR_ENTRY_NAME = "generator.featuregen";

  static final String SEQUENCE_CODEC_CLASS_NAME_PARAMETER = "sequenceCodecImplName";

  public SequenceLabelerModel(String languageCode, SequenceClassificationModel<String> nameFinderModel,
      byte[] generatorDescriptor, Map<String, Object> resources, Map<String, String> manifestInfoEntries,
      SequenceCodec<String> seqCodec, SequenceLabelerFactory factory) {
    super(COMPONENT_NAME, languageCode, manifestInfoEntries, factory);

    init(nameFinderModel, generatorDescriptor, resources, manifestInfoEntries, seqCodec);

    if (!seqCodec.areOutcomesCompatible(nameFinderModel.getOutcomes())) {
      throw new IllegalArgumentException("Model not compatible with name finder!");
    }
  }

  public SequenceLabelerModel(String languageCode, MaxentModel nameFinderModel, int beamSize,
      byte[] generatorDescriptor, Map<String, Object> resources, Map<String, String> manifestInfoEntries,
      SequenceCodec<String> seqCodec, SequenceLabelerFactory factory) {
    super(COMPONENT_NAME, languageCode, manifestInfoEntries, factory);


    Properties manifest = (Properties) artifactMap.get(MANIFEST_ENTRY);
    manifest.put(BeamSearch.BEAM_SIZE_PARAMETER, Integer.toString(beamSize));

    init(nameFinderModel, generatorDescriptor, resources, manifestInfoEntries, seqCodec);

    if (!isModelValid(nameFinderModel)) {
      throw new IllegalArgumentException("Model not compatible with name finder!");
    }
  }

  // TODO: Extend this one with beam size!
  public SequenceLabelerModel(String languageCode, MaxentModel nameFinderModel,
      byte[] generatorDescriptor, Map<String, Object> resources, Map<String, String> manifestInfoEntries) {
    this(languageCode, nameFinderModel, SequenceLabelerME.DEFAULT_BEAM_SIZE,
        generatorDescriptor, resources, manifestInfoEntries, new BioCodec(), new SequenceLabelerFactory());
  }

  public SequenceLabelerModel(String languageCode, MaxentModel nameFinderModel,
      Map<String, Object> resources, Map<String, String> manifestInfoEntries) {
    this(languageCode, nameFinderModel, null, resources, manifestInfoEntries);
  }

  public SequenceLabelerModel(InputStream in) throws IOException, InvalidFormatException {
    super(COMPONENT_NAME, in);
  }

  public SequenceLabelerModel(File modelFile) throws IOException, InvalidFormatException {
    super(COMPONENT_NAME, modelFile);
  }

  public SequenceLabelerModel(URL modelURL) throws IOException, InvalidFormatException {
    super(COMPONENT_NAME, modelURL);
  }

  private void init(Object nameFinderModel,
      byte[] generatorDescriptor, Map<String, Object> resources, Map<String, String> manifestInfoEntries,
      SequenceCodec<String> seqCodec) {

    Properties manifest = (Properties) artifactMap.get(MANIFEST_ENTRY);
    manifest.put(SEQUENCE_CODEC_CLASS_NAME_PARAMETER, seqCodec.getClass().getName());

    artifactMap.put(MAXENT_MODEL_ENTRY_NAME, nameFinderModel);

    if (generatorDescriptor != null && generatorDescriptor.length > 0)
      artifactMap.put(GENERATOR_DESCRIPTOR_ENTRY_NAME, generatorDescriptor);

    if (resources != null) {
      // The resource map must not contain key which are already taken
      // like the name finder maxent model name
      if (resources.containsKey(MAXENT_MODEL_ENTRY_NAME) ||
          resources.containsKey(GENERATOR_DESCRIPTOR_ENTRY_NAME)) {
        throw new IllegalArgumentException();
      }

      // TODO: Add checks to not put resources where no serializer exists,
      // make that case fail here, should be done in the BaseModel
      artifactMap.putAll(resources);
    }
    checkArtifactMap();
  }

  public SequenceClassificationModel<String> getNameFinderSequenceModel() {

    Properties manifest = (Properties) artifactMap.get(MANIFEST_ENTRY);

    if (artifactMap.get(MAXENT_MODEL_ENTRY_NAME) instanceof MaxentModel) {
      String beamSizeString = manifest.getProperty(BeamSearch.BEAM_SIZE_PARAMETER);

      int beamSize = SequenceLabelerME.DEFAULT_BEAM_SIZE;
      if (beamSizeString != null) {
        beamSize = Integer.parseInt(beamSizeString);
      }

      return new BeamSearch<>(beamSize, (MaxentModel) artifactMap.get(MAXENT_MODEL_ENTRY_NAME));
    }
    else if (artifactMap.get(MAXENT_MODEL_ENTRY_NAME) instanceof SequenceClassificationModel) {
      return (SequenceClassificationModel) artifactMap.get(MAXENT_MODEL_ENTRY_NAME);
    }
    else {
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
  protected void createArtifactSerializers(Map<String, ArtifactSerializer> serializers) {
    super.createArtifactSerializers(serializers);

    serializers.put("featuregen", new ByteArraySerializer());
  }

  /**
   * Create the artifact serializers. The DefaultTrainer deals with any other Custom serializers.
   * @return the map containing the added serializers
   */
  public static Map<String, ArtifactSerializer> createArtifactSerializers()  {
    Map<String, ArtifactSerializer> serializers = BaseModel.createArtifactSerializers();

    serializers.put("featuregen", new ByteArraySerializer());
    return serializers;
  }

  boolean isModelValid(MaxentModel model) {

    String outcomes[] = new String[model.getNumOutcomes()];

    for (int i = 0; i < model.getNumOutcomes(); i++) {
      outcomes[i] = model.getOutcome(i);
    }

    return getFactory().createSequenceCodec().areOutcomesCompatible(outcomes);
  }

  @Override
  protected void validateArtifactMap() throws InvalidFormatException {
    super.validateArtifactMap();

    if (artifactMap.get(MAXENT_MODEL_ENTRY_NAME) instanceof MaxentModel ||
        artifactMap.get(MAXENT_MODEL_ENTRY_NAME) instanceof SequenceClassificationModel) {
      // TODO: Check should be performed on the possible outcomes!
//      MaxentModel model = (MaxentModel) artifactMap.get(MAXENT_MODEL_ENTRY_NAME);
//      isModelValid(model);
    }
    else {
      throw new InvalidFormatException("Token Name Finder model is incomplete!");
    }
  }
}

