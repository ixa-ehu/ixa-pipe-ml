package eus.ixa.ixa.pipe.ml.resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import opennlp.tools.cmdline.CmdLineUtil;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.TrainingParameters;
import opennlp.tools.util.model.ArtifactSerializer;
import eus.ixa.ixa.pipe.ml.lemma.DictionaryLemmatizer;
import eus.ixa.ixa.pipe.ml.resources.SequenceModelResource.SequenceModelResourceSerializer;
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
      String brownClusterPath = Flags.getBrownFeatures(params);
      String serializerId = "brownserializer";
      List<File> brownClusterFiles = Flags.getClusterLexiconFiles(brownClusterPath);
      for (File brownClusterFile : brownClusterFiles) {
        String brownFilePath = brownClusterFile.getCanonicalPath();
        artifactSerializers.put(serializerId, new BrownCluster.BrownClusterSerializer());
        loadResource(serializerId, artifactSerializers, brownFilePath, resources);
      }
    }
    if (Flags.isClarkFeatures(params)) {
      String clarkClusterPath = Flags.getClarkFeatures(params);
      String serializerId = "clarkserializer";
      List<File> clarkClusterFiles = Flags.getClusterLexiconFiles(clarkClusterPath);
      for (File clarkClusterFile: clarkClusterFiles) {
        String clarkFilePath = clarkClusterFile.getCanonicalPath();
        artifactSerializers.put(serializerId, new ClarkCluster.ClarkClusterSerializer());
        loadResource(serializerId, artifactSerializers, clarkFilePath, resources);
      }
    }
    if (Flags.isWord2VecClusterFeatures(params)) {
      String word2vecClusterPath = Flags.getWord2VecClusterFeatures(params);
      String serializerId = "word2vecserializer";
      List<File> word2vecClusterFiles = Flags.getClusterLexiconFiles(word2vecClusterPath);
      for (File word2vecClusterFile : word2vecClusterFiles) {
        String word2vecFilePath = word2vecClusterFile.getCanonicalPath();
        artifactSerializers.put(serializerId, new Word2VecCluster.Word2VecClusterSerializer());
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
      String brownClusterPath = Flags.getBrownFeatures(params);
      String serializerId = "brownserializer";
      List<File> brownClusterFiles = Flags.getClusterLexiconFiles(brownClusterPath);
      for (File brownClusterFile : brownClusterFiles) {
        String brownFilePath = brownClusterFile.getCanonicalPath();
        artifactSerializers.put(serializerId, new BrownCluster.BrownClusterSerializer());
        loadResource(serializerId, artifactSerializers, brownFilePath, resources);
      }
    }
    if (Flags.isClarkFeatures(params)) {
      String clarkClusterPath = Flags.getClarkFeatures(params);
      String serializerId = "clarkserializer";
      List<File> clarkClusterFiles = Flags.getClusterLexiconFiles(clarkClusterPath);
      for (File clarkClusterFile: clarkClusterFiles) {
        String clarkFilePath = clarkClusterFile.getCanonicalPath();
        artifactSerializers.put(serializerId, new ClarkCluster.ClarkClusterSerializer());
        loadResource(serializerId, artifactSerializers, clarkFilePath, resources);
      }
    }
    if (Flags.isWord2VecClusterFeatures(params)) {
      String word2vecClusterPath = Flags.getWord2VecClusterFeatures(params);
      String serializerId = "word2vecserializer";
      List<File> word2vecClusterFiles = Flags.getClusterLexiconFiles(word2vecClusterPath);
      for (File word2vecClusterFile : word2vecClusterFiles) {
        String word2vecFilePath = word2vecClusterFile.getCanonicalPath();
        artifactSerializers.put(serializerId, new Word2VecCluster.Word2VecClusterSerializer());
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
      InputStream resourceIn = CmdLineUtil.openInFile(resourceFile);
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
