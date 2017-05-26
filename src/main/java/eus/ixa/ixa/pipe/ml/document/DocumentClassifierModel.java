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

package eus.ixa.ixa.pipe.ml.document;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Map;

import opennlp.tools.ml.model.AbstractModel;
import opennlp.tools.ml.model.MaxentModel;
import opennlp.tools.util.BaseToolFactory;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.model.ArtifactSerializer;
import opennlp.tools.util.model.BaseModel;
import opennlp.tools.util.model.ModelUtil;

/**
 * A model for document classification.
 */
public class DocumentClassifierModel extends BaseModel {

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
  
  private static final String COMPONENT_NAME = "DocumentClassifierME";
  private static final String DOC_MODEL_ENTRY_NAME = "documentclassifier.model";
  static final String GENERATOR_DESCRIPTOR_ENTRY_NAME = "generator.featuregen";

  public DocumentClassifierModel(String languageCode, MaxentModel doccatModel, final byte[] generatorDescriptor,
      final Map<String, Object> resources,
      Map<String, String> manifestInfoEntries, DocumentClassifierFactory factory) {
    super(COMPONENT_NAME, languageCode, manifestInfoEntries, factory);
    init(doccatModel, generatorDescriptor, resources);
  }

  public DocumentClassifierModel(InputStream in) throws IOException {
    super(COMPONENT_NAME, in);
  }

  public DocumentClassifierModel(File modelFile) throws IOException {
    super(COMPONENT_NAME, modelFile);
  }

  public DocumentClassifierModel(URL modelURL) throws IOException {
    super(COMPONENT_NAME, modelURL);
  }
  
  private void init(final Object documentClassifierModel,
      final byte[] generatorDescriptor, final Map<String, Object> resources) {

    this.artifactMap.put(DOC_MODEL_ENTRY_NAME, documentClassifierModel);

    if (generatorDescriptor != null && generatorDescriptor.length > 0) {
      this.artifactMap.put(GENERATOR_DESCRIPTOR_ENTRY_NAME,
          generatorDescriptor);
    }

    if (resources != null) {
      // The resource map must not contain key which are already taken
      // like the name finder maxent model name
      if (resources.containsKey(DOC_MODEL_ENTRY_NAME)
          || resources.containsKey(GENERATOR_DESCRIPTOR_ENTRY_NAME)) {
        throw new IllegalArgumentException();
      }

      // TODO: Add checks to not put resources where no serializer exists,
      // make that case fail here, should be done in the BaseModel
      this.artifactMap.putAll(resources);
    }
    checkArtifactMap();
  }
  
  public MaxentModel getDocumentClassifierModel() {
    return (MaxentModel) artifactMap.get(DOC_MODEL_ENTRY_NAME);
  }

  @Override
  protected Class<? extends BaseToolFactory> getDefaultFactory() {
    return DocumentClassifierFactory.class;
  }
  
  public DocumentClassifierFactory getFactory() {
    return (DocumentClassifierFactory) this.toolFactory;
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

  @Override
  protected void validateArtifactMap() throws InvalidFormatException {
    super.validateArtifactMap();

    if (!(artifactMap.get(DOC_MODEL_ENTRY_NAME) instanceof AbstractModel)) {
      throw new InvalidFormatException("Doccat model is incomplete!");
    }
  }
}
