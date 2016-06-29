package eus.ixa.ixa.pipe.ml.features;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.featuregen.ArtifactToSerializerMapper;
import opennlp.tools.util.featuregen.CustomFeatureGenerator;
import opennlp.tools.util.featuregen.FeatureGeneratorResourceProvider;
import opennlp.tools.util.model.ArtifactSerializer;
import eus.ixa.ixa.pipe.ml.resources.SequenceModelResource;
import eus.ixa.ixa.pipe.ml.utils.Span;

/**
 * Baseline features to train a Chunker.
 * @author ragerri
 * @version 2016-05-13
 */
public class ChunkBaselineContextGenerator extends CustomFeatureGenerator implements ArtifactToSerializerMapper {

  private SequenceModelResource posModelResource;
  private String[] currentSentence;
  private Span[] currentTags;
  
  @Override
  public void createFeatures(List<String> features, String[] tokens, int index,
      String[] previousOutcomes) {
    
    //cache annotations for each sentence
    if (currentSentence != tokens) {
      currentSentence = tokens;
      currentTags = posModelResource.seqToSpans(tokens);
    }
    // Words in a 5-word window
    String w_2, w_1, w0, w1, w2;
    // Tags in a 5-word window
    String t_2, t_1, t0, t1, t2;
    // Previous predictions
    String p_2, p_1;

    if (index < 2) {
      w_2 = "bos";
      t_2 = "bos";
      p_2 = "bos";
    } else {
      w_2 = tokens[index - 2];
      t_2 = currentTags[index - 2].getType();
      p_2 = previousOutcomes[index - 2];
    }
    if (index < 1) {
      w_1 = "bos";
      t_1 = "bos";
      p_1 = "bos";
    } else {
      w_1 = tokens[index - 1];
      t_1 = currentTags[index - 1].getType();
      p_1 = previousOutcomes[index - 1];
    }

    w0 = tokens[index];
    t0 = currentTags[index].getType();

    if (index + 1 >= tokens.length) {
      w1 = "eos";
      t1 = "eos";
    } else {
      w1 = tokens[index + 1];
      t1 = currentTags[index + 1].getType();
    }
    if (index + 2 >= tokens.length) {
      w2 = "eos";
      t2 = "eos";
    } else {
      w2 = tokens[index + 2];
      t2 = currentTags[index + 2].getType();
    }
    //add word features
    features.add("w_2=" + w_2);
    features.add("w_1=" + w_1);
    features.add("w0=" + w0);
    features.add("w1=" + w1);
    features.add("w2=" + w2);
    features.add("w_1,w0=" + w_1 + "," + w0);
    features.add("w0,w1=" + w0 + "," + w1);
    //add tag features
    features.add("t_2=" + t_2);
    features.add("t_1=" + t_1);
    features.add("t0=" + t0);
    features.add("t1=" + t1);
    features.add("t2=" + t2);
    features.add("t_2,t_1=" + t_2 + "," + t_1);
    features.add("t_1,t0=" + t_1 + "," + t0);
    features.add("t0,t1=" + t0 + "," + t1);
    features.add("t1,t2=" + t1 + "," + t2);
    features.add("t_2,t_1,t0=" + t_2 + "," + t_1 + "," + t0);
    features.add("t_1,t0,t1=" + t_1 + "," + t0 + "," + t1);
    features.add("t0,t1,t2=" + t0 + "," + t1 + "," + t2);
    //add pred tags
    features.add("p_2=" + p_2);
    features.add("p_1=" + p_1);
    features.add("p_2,p_1=" + p_2 + "," + p_1);
    //add pred and tag
    features.add("p_1,t_2=" + p_1 + "," + t_2);
    features.add("p_1,t_1=" + p_1 + "," + t_1);
    features.add("p_1,t0=" + p_1 + "," + t0);
    features.add("p_1,t1=" + p_1 + "," + t1);
    //features.add("p_1,t2=" + p_1 + "," + t2);
    //features.add("p_1,t_2,t_1=" + p_1 + "," + t_2 + "," + t_1);
    features.add("p_1,t_1,t0=" + p_1 + "," + t_1 + "," + t0);
    features.add("p_1,t0,t1=" + p_1 + "," + t0 + "," + t1);
    features.add("p_1,t1,t2=" + p_1 + "," + t1 + "," + t2);
    features.add("p_1,t_2,t_1,t0=" + p_1 + "," + t_2 + "," + t_1 + "," + t0);
    features.add("p_1,t_1,t0,t1=" + p_1 + "," + t_1 + "," + t0 + "," + t1);
    //features.add("p_1,t0,t1,t2=" + p_1 + "," + t0 + "," + t1 + "," + t2);
    //add pred and word
    features.add("p_1,w_2=" + p_1 + "," + w_2);
    features.add("p_1,w_1=" + p_1 + "," + w_1);
    //features.add("p_1,w0=" + p_1 + "," + w0);
    features.add("p_1,w1=" + p_1 + "," + w1);
    features.add("p_1,w2=" + p_1 + "," + w2);
    //features.add("p_1,w_1,w0=" + p_1 + "," + w_1 + "," + w0);
    features.add("p_1,w0,w1=" + p_1 + "," + w0 + "," + w1);
  }
  
  @Override
  public void updateAdaptiveData(String[] tokens, String[] outcomes) {
  }

  @Override
  public void clearAdaptiveData() {
  }
  @Override
  public void init(Map<String, String> properties,
      FeatureGeneratorResourceProvider resourceProvider)
      throws InvalidFormatException {
    Object posResource = resourceProvider.getResource(properties.get("model"));
    if (!(posResource instanceof SequenceModelResource)) {
      throw new InvalidFormatException("Not a SequenceModelResource for key: " + properties.get("model"));
    }
    this.posModelResource = (SequenceModelResource) posResource;
  }
  
  @Override
  public Map<String, ArtifactSerializer<?>> getArtifactSerializerMapping() {
    Map<String, ArtifactSerializer<?>> mapping = new HashMap<>();
    mapping.put("seqmodelserializer", new SequenceModelResource.SequenceModelResourceSerializer());
    return Collections.unmodifiableMap(mapping);
  }

  

}
