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
package eus.ixa.ixa.pipe.ml.resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.TrainingParameters;
import opennlp.tools.util.model.ArtifactSerializer;
import eus.ixa.ixa.pipe.ml.lemma.DictionaryLemmatizer;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerModel;
import eus.ixa.ixa.pipe.ml.utils.Flags;
import eus.ixa.ixa.pipe.ml.utils.IOUtils;
import eus.ixa.ixa.pipe.ml.utils.StringUtils;

public class LoadModelResources {
  
  private LoadModelResources() {
  }
  
  /**
   * Load the external resources such as gazetters and clustering lexicons.
   * @param params the training parameters
   * @return the map contanining and id and the resource
   * @throws IOException if io error
   */
  public static Map<String, Object> loadSequenceResources(TrainingParameters params) throws IOException {
    Map<String, Object> resources = new HashMap<String, Object>();
    @SuppressWarnings("rawtypes")
    Map<String, ArtifactSerializer> artifactSerializers = SequenceLabelerModel.createArtifactSerializers();
    
    if (Flags.isBrownFeatures(params)) {
      String ClusterLexiconPath = Flags.getBrownFeatures(params);
      String serializerId = "brownserializer";
      List<File> ClusterLexiconFiles = Flags.getClusterLexiconFiles(ClusterLexiconPath);
      for (File ClusterLexiconFile : ClusterLexiconFiles) {
        String brownFilePath = ClusterLexiconFile.getCanonicalPath();
        artifactSerializers.put(serializerId, new WordCluster.WordClusterSerializer());
        loadResource(serializerId, artifactSerializers, brownFilePath, resources);
      }
    }
    if (Flags.isClarkFeatures(params)) {
      String clarkClusterPath = Flags.getClarkFeatures(params);
      String serializerId = "clarkserializer";
      List<File> clarkClusterFiles = Flags.getClusterLexiconFiles(clarkClusterPath);
      for (File clarkClusterFile: clarkClusterFiles) {
        String clarkFilePath = clarkClusterFile.getCanonicalPath();
        artifactSerializers.put(serializerId, new WordCluster.WordClusterSerializer());
        loadResource(serializerId, artifactSerializers, clarkFilePath, resources);
      }
    }
    if (Flags.isWord2VecClusterFeatures(params)) {
      String ClusterLexiconPath = Flags.getWord2VecClusterFeatures(params);
      String serializerId = "word2vecserializer";
      List<File> ClusterLexiconFiles = Flags.getClusterLexiconFiles(ClusterLexiconPath);
      for (File ClusterLexiconFile : ClusterLexiconFiles) {
        String word2vecFilePath = ClusterLexiconFile.getCanonicalPath();
        artifactSerializers.put(serializerId, new WordCluster.WordClusterSerializer());
        loadResource(serializerId, artifactSerializers, word2vecFilePath, resources);
      }
    }
    if (Flags.isDictionaryFeatures(params)) {
      String dictDir = Flags.getDictionaryFeatures(params);
      String serializerId = "dictionaryserializer";
      List<File> fileList = StringUtils.getFilesInDir(new File(dictDir));
      for (File dictFile : fileList) {
        String dictionaryPath = dictFile.getCanonicalPath();
        artifactSerializers.put(serializerId, new Dictionary.DictionarySerializer());
        loadResource(serializerId, artifactSerializers, dictionaryPath, resources);
      }
    }
    if (Flags.isPOSTagModelFeatures(params)) {
      String morphoResourcesPath = Flags.getPOSTagModelFeatures(params);
      String posSerializerId = "seqmodelserializer";
      artifactSerializers.put(posSerializerId, new SequenceModelResource.SequenceModelResourceSerializer());
      loadResource(posSerializerId, artifactSerializers, morphoResourcesPath, resources);
    }
    if (Flags.isPOSDictionaryFeatures(params)) {
      String posDictPath = Flags.getPOSDictionaryFeatures(params);
      String posSerializerId = "posdictserializer";
      artifactSerializers.put(posSerializerId, new POSDictionary.POSDictionarySerializer());
      loadResource(posSerializerId, artifactSerializers, posDictPath, resources);
    }
    if (Flags.isLemmaModelFeatures(params)) {
      String lemmaModelPath = Flags.getLemmaModelFeatures(params);
      String lemmaSerializerId = "seqmodelserializer";
      artifactSerializers.put(lemmaSerializerId, new SequenceModelResource.SequenceModelResourceSerializer());
      loadResource(lemmaSerializerId, artifactSerializers, lemmaModelPath, resources);
    }
    if (Flags.isLemmaDictionaryFeatures(params)) {
      String lemmaDictPath = Flags.getLemmaDictionaryFeatures(params);
      String[] lemmaDictResources = Flags.getLemmaDictionaryResources(lemmaDictPath);
      String posSerializerId = "seqmodelserializer";
      String lemmaDictSerializerId = "lemmadictserializer";
      artifactSerializers.put(posSerializerId, new SequenceModelResource.SequenceModelResourceSerializer());
      loadResource(posSerializerId, artifactSerializers, lemmaDictResources[0], resources);
      artifactSerializers.put(lemmaDictSerializerId, new DictionaryLemmatizer.DictionaryLemmatizerSerializer());
      loadResource(lemmaDictSerializerId, artifactSerializers, lemmaDictResources[1], resources);
    }
    if (Flags.isSuperSenseFeatures(params)) {
      String mfsResourcesPath = Flags.getSuperSenseFeatures(params);
      String[] mfsResources = Flags.getSuperSenseResources(mfsResourcesPath);
      String posSerializerId = "seqmodelserializer";
      String lemmaSerializerId = "lemmadictserializer";
      String mfsSerializerId = "mfsserializer";
      artifactSerializers.put(posSerializerId, new SequenceModelResource.SequenceModelResourceSerializer());
      loadResource(posSerializerId, artifactSerializers, mfsResources[0], resources);
      artifactSerializers.put(lemmaSerializerId, new DictionaryLemmatizer.DictionaryLemmatizerSerializer());
      loadResource(lemmaSerializerId, artifactSerializers, mfsResources[1], resources);
      artifactSerializers.put(mfsSerializerId, new MFSResource.MFSResourceSerializer());
      loadResource(mfsSerializerId, artifactSerializers, mfsResources[2], resources);
    }
    if (Flags.isMFSFeatures(params)) {
      String mfsResourcesPath = Flags.getMFSFeatures(params);
      String[] mfsResources = Flags.getMFSResources(mfsResourcesPath);
      String posSerializerId = "seqmodelserializer";
      String lemmaSerializerId = "lemmadictserializer";
      String mfsSerializerId = "mfsserializer";
      artifactSerializers.put(posSerializerId, new SequenceModelResource.SequenceModelResourceSerializer());
      loadResource(posSerializerId, artifactSerializers, mfsResources[0], resources);
      artifactSerializers.put(lemmaSerializerId, new DictionaryLemmatizer.DictionaryLemmatizerSerializer());
      loadResource(lemmaSerializerId, artifactSerializers, mfsResources[1], resources);
      artifactSerializers.put(mfsSerializerId, new MFSResource.MFSResourceSerializer());
      loadResource(mfsSerializerId, artifactSerializers, mfsResources[2], resources);
    }
    if (Flags.isLemmaBaselineFeatures(params)) {
      String lemmaBaselineFeatures = Flags.getLemmaBaselineFeatures(params);
      String posSerializerId = "seqmodelserializer";
      artifactSerializers.put(posSerializerId, new SequenceModelResource.SequenceModelResourceSerializer());
      loadResource(posSerializerId, artifactSerializers, lemmaBaselineFeatures, resources);
    }
    if (Flags.isChunkBaselineFeatures(params)) {
      String chunkBaselineFeatures = Flags.getChunkBaselineFeatures(params);
      String posSerializerId = "seqmodelserializer";
      artifactSerializers.put(posSerializerId, new SequenceModelResource.SequenceModelResourceSerializer());
      loadResource(posSerializerId, artifactSerializers, chunkBaselineFeatures, resources);
    }
    if (Flags.isPredicateContextFeatures(params)) {
      String predicateContextFeatures = Flags.getPredicateContextFeatures(params);
      String posSerializerId = "predicatecontextserializer";
      artifactSerializers.put(posSerializerId, new PredicateContext.PredicateContextSerializer());
      loadResource(posSerializerId, artifactSerializers, predicateContextFeatures, resources);
    }
    return resources;
  }
  
  /**
   * Load the external resources such as gazetters and clustering lexicons.
   * @param params the training parameters
   * @return the map contanining and id and the resource
   * @throws IOException if io error
   */
  public static Map<String, Object> loadParseResources(TrainingParameters params) throws IOException {
    Map<String, Object> resources = new HashMap<String, Object>();
    @SuppressWarnings("rawtypes")
    Map<String, ArtifactSerializer> artifactSerializers = SequenceLabelerModel.createArtifactSerializers();
    
    if (Flags.isBrownFeatures(params)) {
      String ClusterLexiconPath = Flags.getBrownFeatures(params);
      String serializerId = "brownserializer";
      List<File> ClusterLexiconFiles = Flags.getClusterLexiconFiles(ClusterLexiconPath);
      for (File ClusterLexiconFile : ClusterLexiconFiles) {
        String brownFilePath = ClusterLexiconFile.getCanonicalPath();
        artifactSerializers.put(serializerId, new WordCluster.WordClusterSerializer());
        loadResource(serializerId, artifactSerializers, brownFilePath, resources);
      }
    }
    if (Flags.isClarkFeatures(params)) {
      String clarkClusterPath = Flags.getClarkFeatures(params);
      String serializerId = "clarkserializer";
      List<File> clarkClusterFiles = Flags.getClusterLexiconFiles(clarkClusterPath);
      for (File clarkClusterFile: clarkClusterFiles) {
        String clarkFilePath = clarkClusterFile.getCanonicalPath();
        artifactSerializers.put(serializerId, new WordCluster.WordClusterSerializer());
        loadResource(serializerId, artifactSerializers, clarkFilePath, resources);
      }
    }
    if (Flags.isWord2VecClusterFeatures(params)) {
      String ClusterLexiconPath = Flags.getWord2VecClusterFeatures(params);
      String serializerId = "word2vecserializer";
      List<File> ClusterLexiconFiles = Flags.getClusterLexiconFiles(ClusterLexiconPath);
      for (File ClusterLexiconFile : ClusterLexiconFiles) {
        String word2vecFilePath = ClusterLexiconFile.getCanonicalPath();
        artifactSerializers.put(serializerId, new WordCluster.WordClusterSerializer());
        loadResource(serializerId, artifactSerializers, word2vecFilePath, resources);
      }
    }
    if (Flags.isPOSTagModelFeatures(params)) {
      String morphoResourcesPath = Flags.getPOSTagModelFeatures(params);
      String posSerializerId = "seqmodelserializer";
      artifactSerializers.put(posSerializerId, new SequenceModelResource.SequenceModelResourceSerializer());
      loadResource(posSerializerId, artifactSerializers, morphoResourcesPath, resources);
    }
    if (Flags.isLemmaModelFeatures(params)) {
      String lemmaModelPath = Flags.getLemmaModelFeatures(params);
      String lemmaSerializerId = "seqmodelserializer";
      artifactSerializers.put(lemmaSerializerId, new SequenceModelResource.SequenceModelResourceSerializer());
      loadResource(lemmaSerializerId, artifactSerializers, lemmaModelPath, resources);
    }
    if (Flags.isLemmaDictionaryFeatures(params)) {
      String lemmaDictPath = Flags.getLemmaDictionaryFeatures(params);
      String[] lemmaDictResources = Flags.getLemmaDictionaryResources(lemmaDictPath);
      String posSerializerId = "seqmodelserializer";
      String lemmaDictSerializerId = "lemmadictserializer";
      artifactSerializers.put(posSerializerId, new SequenceModelResource.SequenceModelResourceSerializer());
      loadResource(posSerializerId, artifactSerializers, lemmaDictResources[0], resources);
      artifactSerializers.put(lemmaDictSerializerId, new DictionaryLemmatizer.DictionaryLemmatizerSerializer());
      loadResource(lemmaDictSerializerId, artifactSerializers, lemmaDictResources[1], resources);
    }
    return resources;
  }

  /**
   * Load a resource by resourceId.
   * @param serializerId the serializer id
   * @param artifactSerializers the serializers in which to put the resource
   * @param resourcePath the canonical path of the resource
   * @param resources the map in which to put the resource
   */
  public static void loadResource(String serializerId, @SuppressWarnings("rawtypes") Map<String, ArtifactSerializer> artifactSerializers, String resourcePath, Map<String, Object> resources) {

    File resourceFile = new File(resourcePath);
    if (resourceFile != null) {
      String resourceId = IOUtils.normalizeLexiconName(resourceFile.getName());
      ArtifactSerializer<?> serializer = artifactSerializers.get(serializerId);
      InputStream resourceIn = IOUtils.openFromFile(resourceFile);
      try {
        resources.put(resourceId, serializer.create(resourceIn));
      } catch (InvalidFormatException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        try {
          resourceIn.close();
        } catch (IOException e) {
        }
      }
    }
  }

}
