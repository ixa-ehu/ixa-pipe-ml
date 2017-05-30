package eus.ixa.ixa.pipe.ml.document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import eus.ixa.ixa.pipe.ml.document.features.DocumentFeatureAggregator;
import eus.ixa.ixa.pipe.ml.document.features.DocumentFeatureGenerator;
import eus.ixa.ixa.pipe.ml.document.features.DocumentGeneratorFactory;
import opennlp.tools.util.BaseToolFactory;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.ext.ExtensionLoader;
import opennlp.tools.util.featuregen.FeatureGeneratorResourceProvider;

public class DocumentClassifierFactory extends BaseToolFactory {
  
  private byte[] featureGeneratorBytes;
  private Map<String, Object> resources;
  
  /**
   * Creates a {@link DocumentClassifierFactory} that provides the default
   * implementation of the resources.
   */
  public DocumentClassifierFactory() {
  }

  public DocumentClassifierFactory(final byte[] featureGeneratorBytes,
      final Map<String, Object> resources) {
    init(featureGeneratorBytes, resources);
  }

  void init(final byte[] featureGeneratorBytes,
      final Map<String, Object> resources) {
    this.featureGeneratorBytes = featureGeneratorBytes;
    this.resources = resources;
  }
  
//TODO call this method for default feature generation
  private static byte[] loadDefaultFeatureGeneratorBytes() {

    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (InputStream in = DocumentClassifierFactory.class.getResourceAsStream(
        "/documentClassifier/default-feature-descriptor.xml")) {

      if (in == null) {
        throw new IllegalStateException(
            "Classpath must contain default-feature-descriptor.xml file!");
      }

      final byte buf[] = new byte[1024];
      int len;
      while ((len = in.read(buf)) > 0) {
        bytes.write(buf, 0, len);
      }
    } catch (final IOException e) {
      throw new IllegalStateException(
          "Failed reading from default-feature-descriptor.xml file on classpath!");
    }
    return bytes.toByteArray();
  }
  
  protected Map<String, Object> getResources() {
    return this.resources;
  }

  protected byte[] getFeatureGenerator() {
    return this.featureGeneratorBytes;
  }
  
  public static DocumentClassifierFactory create(final String subclassName,
      final byte[] featureGeneratorBytes, final Map<String, Object> resources)
      throws InvalidFormatException {
    DocumentClassifierFactory theFactory;
    if (subclassName == null) {
      // will create the default factory
      theFactory = new DocumentClassifierFactory();
    } else {
      try {
        theFactory = ExtensionLoader
            .instantiateExtension(DocumentClassifierFactory.class, subclassName);
      } catch (final Exception e) {
        final String msg = "Could not instantiate the " + subclassName
            + ". The initialization throw an exception.";
        System.err.println(msg);
        e.printStackTrace();
        throw new InvalidFormatException(msg, e);
      }
    }
    theFactory.init(featureGeneratorBytes, resources);
    return theFactory;
  }
  
  @Override
  public void validateArtifactMap() throws InvalidFormatException {
    // no additional artifacts
  }
  
  public DocumentClassifierContextGenerator createContextGenerator() {

    final DocumentFeatureGenerator featureGenerator = createFeatureGenerators();
    if (featureGenerator == null) {
      throw new NullPointerException("featureGenerator must not be null");
    }
    return new DefaultDocumentClassifierContextGenerator(featureGenerator);
  }
  
  /**
   * Creates the {@link DocumentFeatureGenerator}. Usually this is a set of
   * generators contained in the {@link DocumentFeatureAggregator}.
   *
   * Note: The generators are created on every call to this method.
   *
   * @return the feature generator or null if there is no descriptor in the
   *         model
   */
  public DocumentFeatureGenerator createFeatureGenerators() {

    if (this.featureGeneratorBytes == null && this.artifactProvider != null) {
      this.featureGeneratorBytes = (byte[]) this.artifactProvider
          .getArtifact(DocumentClassifierModel.GENERATOR_DESCRIPTOR_ENTRY_NAME);
    }
    if (this.featureGeneratorBytes == null) {
      System.err.println(
          "WARNING: loading the default feature generator descriptor!!");
      this.featureGeneratorBytes = loadDefaultFeatureGeneratorBytes();
    }

    final InputStream descriptorIn = new ByteArrayInputStream(
        this.featureGeneratorBytes);
    DocumentFeatureGenerator generator = null;
    try {
      generator = DocumentGeneratorFactory.create(descriptorIn,
          new FeatureGeneratorResourceProvider() {

            @Override
            public Object getResource(final String key) {
              if (DocumentClassifierFactory.this.artifactProvider != null) {
                return DocumentClassifierFactory.this.artifactProvider
                    .getArtifact(key);
              } else {
                return DocumentClassifierFactory.this.resources.get(key);
              }
            }
          });
    } catch (final InvalidFormatException e) {
      // It is assumed that the creation of the feature generation does not
      // fail after it succeeded once during model loading.

      // But it might still be possible that such an exception is thrown,
      // in this case the caller should not be forced to handle the exception
      // and a Runtime Exception is thrown instead.

      // If the re-creation of the feature generation fails it is assumed
      // that this can only be caused by a programming mistake and therefore
      // throwing a Runtime Exception is reasonable

      throw new DocumentClassifierModel.FeatureGeneratorCreationError(e);
    } catch (final IOException e) {
      throw new IllegalStateException(
          "Reading from mem cannot result in an I/O error", e);
    }
    return generator;
  }

}
