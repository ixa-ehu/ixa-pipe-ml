/*
 *  Copyright 2016 Rodrigo Agerri

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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eus.ixa.ixa.pipe.ml.document.DocumentClassifierModel;
import eus.ixa.ixa.pipe.ml.lemma.DictionaryLemmatizer;
import eus.ixa.ixa.pipe.ml.resources.Dictionary;
import eus.ixa.ixa.pipe.ml.resources.POSDictionary;
import eus.ixa.ixa.pipe.ml.resources.SequenceModelResource;
import eus.ixa.ixa.pipe.ml.resources.WordCluster;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerModel;
import eus.ixa.ixa.pipe.ml.utils.Flags;
import eus.ixa.ixa.pipe.ml.utils.IOUtils;
import eus.ixa.ixa.pipe.ml.utils.StringUtils;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.TrainingParameters;
import opennlp.tools.util.model.ArtifactSerializer;

public class DocumentModelResources {

  private DocumentModelResources() {
  }

  /**
   * Load the external resources such as gazetters and clustering lexicons.
   * 
   * @param params
   *          the training parameters
   * @return the map contanining and id and the resource
   * @throws IOException
   *           if io error
   */
  public static Map<String, Object> loadDocumentResources(
      final TrainingParameters params) throws IOException {
    final Map<String, Object> resources = new HashMap<String, Object>();
    @SuppressWarnings("rawtypes")
    final Map<String, ArtifactSerializer> artifactSerializers = DocumentClassifierModel
        .createArtifactSerializers();

    if (Flags.isBrownFeatures(params)) {
      final String ClusterLexiconPath = Flags.getBrownFeatures(params);
      final String serializerId = "brownserializer";
      final List<File> ClusterLexiconFiles = Flags
          .getClusterLexiconFiles(ClusterLexiconPath);
      for (final File ClusterLexiconFile : ClusterLexiconFiles) {
        final String brownFilePath = ClusterLexiconFile.getCanonicalPath();
        artifactSerializers.put(serializerId,
            new WordCluster.WordClusterSerializer());
        loadResource(serializerId, artifactSerializers, brownFilePath,
            resources);
      }
    }
    if (Flags.isClarkFeatures(params)) {
      final String clarkClusterPath = Flags.getClarkFeatures(params);
      final String serializerId = "clarkserializer";
      final List<File> clarkClusterFiles = Flags
          .getClusterLexiconFiles(clarkClusterPath);
      for (final File clarkClusterFile : clarkClusterFiles) {
        final String clarkFilePath = clarkClusterFile.getCanonicalPath();
        artifactSerializers.put(serializerId,
            new WordCluster.WordClusterSerializer());
        loadResource(serializerId, artifactSerializers, clarkFilePath,
            resources);
      }
    }
    if (Flags.isWord2VecClusterFeatures(params)) {
      final String ClusterLexiconPath = Flags
          .getWord2VecClusterFeatures(params);
      final String serializerId = "word2vecserializer";
      final List<File> ClusterLexiconFiles = Flags
          .getClusterLexiconFiles(ClusterLexiconPath);
      for (final File ClusterLexiconFile : ClusterLexiconFiles) {
        final String word2vecFilePath = ClusterLexiconFile.getCanonicalPath();
        artifactSerializers.put(serializerId,
            new WordCluster.WordClusterSerializer());
        loadResource(serializerId, artifactSerializers, word2vecFilePath,
            resources);
      }
    }
    if (Flags.isDictionaryPolarityFeatures(params)) {
      final String dictDir = Flags.getDictionaryPolarityFeatures(params);
      final String serializerId = "polaritydictionaryserializer";
      final List<File> fileList = StringUtils.getFilesInDir(new File(dictDir));
      for (final File dictFile : fileList) {
        final String dictionaryPath = dictFile.getCanonicalPath();
        artifactSerializers.put(serializerId,
            new Dictionary.DictionarySerializer());
        loadResource(serializerId, artifactSerializers, dictionaryPath,
            resources);
      }
    }
    if (Flags.isFrequentWordFeatures(params)) {
      final String dictDir = Flags.getFrequentWordFeatures(params);
      final String serializerId = "dictionaryserializer";
      final List<File> fileList = StringUtils.getFilesInDir(new File(dictDir));
      for (final File dictFile : fileList) {
        final String dictionaryPath = dictFile.getCanonicalPath();
        artifactSerializers.put(serializerId,
            new Dictionary.DictionarySerializer());
        loadResource(serializerId, artifactSerializers, dictionaryPath,
            resources);
      }
    }
    if (Flags.isTargetFeatures(params)) {
      final String morphoResourcesPath = Flags.getTargetFeatures(params);
      final String posSerializerId = "otemodelserializer";
      artifactSerializers.put(posSerializerId,
          new SequenceModelResource.SequenceModelResourceSerializer());
      loadResource(posSerializerId, artifactSerializers, morphoResourcesPath,
          resources);
    }
    if (Flags.isPOSTagModelFeatures(params)) {
      final String morphoResourcesPath = Flags.getPOSTagModelFeatures(params);
      final String posSerializerId = "seqmodelserializer";
      artifactSerializers.put(posSerializerId,
          new SequenceModelResource.SequenceModelResourceSerializer());
      loadResource(posSerializerId, artifactSerializers, morphoResourcesPath,
          resources);
    }
    if (Flags.isPOSDictionaryFeatures(params)) {
      final String posDictPath = Flags.getPOSDictionaryFeatures(params);
      final String posSerializerId = "posdictserializer";
      artifactSerializers.put(posSerializerId,
          new POSDictionary.POSDictionarySerializer());
      loadResource(posSerializerId, artifactSerializers, posDictPath,
          resources);
    }
    if (Flags.isLemmaModelFeatures(params)) {
      final String lemmaModelPath = Flags.getLemmaModelFeatures(params);
      final String lemmaSerializerId = "seqmodelserializer";
      artifactSerializers.put(lemmaSerializerId,
          new SequenceModelResource.SequenceModelResourceSerializer());
      loadResource(lemmaSerializerId, artifactSerializers, lemmaModelPath,
          resources);
    }
    if (Flags.isLemmaDictionaryFeatures(params)) {
      final String lemmaDictPath = Flags.getLemmaDictionaryFeatures(params);
      final String[] lemmaDictResources = Flags
          .getLemmaDictionaryResources(lemmaDictPath);
      final String posSerializerId = "seqmodelserializer";
      final String lemmaDictSerializerId = "lemmadictserializer";
      artifactSerializers.put(posSerializerId,
          new SequenceModelResource.SequenceModelResourceSerializer());
      loadResource(posSerializerId, artifactSerializers, lemmaDictResources[0],
          resources);
      artifactSerializers.put(lemmaDictSerializerId,
          new DictionaryLemmatizer.DictionaryLemmatizerSerializer());
      loadResource(lemmaDictSerializerId, artifactSerializers,
          lemmaDictResources[1], resources);
    }
    return resources;
  }

  /**
   * Load the external resources such as gazetters and clustering lexicons.
   * 
   * @param params
   *          the training parameters
   * @return the map contanining and id and the resource
   * @throws IOException
   *           if io error
   */
  public static Map<String, Object> loadParseResources(
      final TrainingParameters params) throws IOException {
    final Map<String, Object> resources = new HashMap<String, Object>();
    @SuppressWarnings("rawtypes")
    final Map<String, ArtifactSerializer> artifactSerializers = SequenceLabelerModel
        .createArtifactSerializers();

    if (Flags.isBrownFeatures(params)) {
      final String ClusterLexiconPath = Flags.getBrownFeatures(params);
      final String serializerId = "brownserializer";
      final List<File> ClusterLexiconFiles = Flags
          .getClusterLexiconFiles(ClusterLexiconPath);
      for (final File ClusterLexiconFile : ClusterLexiconFiles) {
        final String brownFilePath = ClusterLexiconFile.getCanonicalPath();
        artifactSerializers.put(serializerId,
            new WordCluster.WordClusterSerializer());
        loadResource(serializerId, artifactSerializers, brownFilePath,
            resources);
      }
    }
    if (Flags.isClarkFeatures(params)) {
      final String clarkClusterPath = Flags.getClarkFeatures(params);
      final String serializerId = "clarkserializer";
      final List<File> clarkClusterFiles = Flags
          .getClusterLexiconFiles(clarkClusterPath);
      for (final File clarkClusterFile : clarkClusterFiles) {
        final String clarkFilePath = clarkClusterFile.getCanonicalPath();
        artifactSerializers.put(serializerId,
            new WordCluster.WordClusterSerializer());
        loadResource(serializerId, artifactSerializers, clarkFilePath,
            resources);
      }
    }
    if (Flags.isWord2VecClusterFeatures(params)) {
      final String ClusterLexiconPath = Flags
          .getWord2VecClusterFeatures(params);
      final String serializerId = "word2vecserializer";
      final List<File> ClusterLexiconFiles = Flags
          .getClusterLexiconFiles(ClusterLexiconPath);
      for (final File ClusterLexiconFile : ClusterLexiconFiles) {
        final String word2vecFilePath = ClusterLexiconFile.getCanonicalPath();
        artifactSerializers.put(serializerId,
            new WordCluster.WordClusterSerializer());
        loadResource(serializerId, artifactSerializers, word2vecFilePath,
            resources);
      }
    }
    if (Flags.isPOSTagModelFeatures(params)) {
      final String morphoResourcesPath = Flags.getPOSTagModelFeatures(params);
      final String posSerializerId = "seqmodelserializer";
      artifactSerializers.put(posSerializerId,
          new SequenceModelResource.SequenceModelResourceSerializer());
      loadResource(posSerializerId, artifactSerializers, morphoResourcesPath,
          resources);
    }
    if (Flags.isLemmaModelFeatures(params)) {
      final String lemmaModelPath = Flags.getLemmaModelFeatures(params);
      final String lemmaSerializerId = "seqmodelserializer";
      artifactSerializers.put(lemmaSerializerId,
          new SequenceModelResource.SequenceModelResourceSerializer());
      loadResource(lemmaSerializerId, artifactSerializers, lemmaModelPath,
          resources);
    }
    if (Flags.isLemmaDictionaryFeatures(params)) {
      final String lemmaDictPath = Flags.getLemmaDictionaryFeatures(params);
      final String[] lemmaDictResources = Flags
          .getLemmaDictionaryResources(lemmaDictPath);
      final String posSerializerId = "seqmodelserializer";
      final String lemmaDictSerializerId = "lemmadictserializer";
      artifactSerializers.put(posSerializerId,
          new SequenceModelResource.SequenceModelResourceSerializer());
      loadResource(posSerializerId, artifactSerializers, lemmaDictResources[0],
          resources);
      artifactSerializers.put(lemmaDictSerializerId,
          new DictionaryLemmatizer.DictionaryLemmatizerSerializer());
      loadResource(lemmaDictSerializerId, artifactSerializers,
          lemmaDictResources[1], resources);
    }
    return resources;
  }

  /**
   * Load a resource by resourceId.
   * 
   * @param serializerId
   *          the serializer id
   * @param artifactSerializers
   *          the serializers in which to put the resource
   * @param resourcePath
   *          the canonical path of the resource
   * @param resources
   *          the map in which to put the resource
   */
  public static void loadResource(final String serializerId,
      @SuppressWarnings("rawtypes") final Map<String, ArtifactSerializer> artifactSerializers,
      final String resourcePath, final Map<String, Object> resources) {

    final File resourceFile = new File(resourcePath);
    if (resourceFile != null) {
      final String resourceId = IOUtils
          .normalizeLexiconName(resourceFile.getName());
      final ArtifactSerializer<?> serializer = artifactSerializers
          .get(serializerId);
      final InputStream resourceIn = IOUtils.openFromFile(resourceFile);
      try {
        resources.put(resourceId, serializer.create(resourceIn));
      } catch (final InvalidFormatException e) {
        e.printStackTrace();
      } catch (final IOException e) {
        e.printStackTrace();
      } finally {
        try {
          resourceIn.close();
        } catch (final IOException e) {
        }
      }
    }
  }

}
