package eus.ixa.ixa.pipe.ml.features;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.featuregen.ArtifactToSerializerMapper;
import opennlp.tools.util.featuregen.CustomFeatureGenerator;
import opennlp.tools.util.featuregen.FeatureGeneratorResourceProvider;
import opennlp.tools.util.model.ArtifactSerializer;
import eus.ixa.ixa.pipe.ml.resources.SequenceModelResource;
import eus.ixa.ixa.pipe.ml.utils.Span;

/**
 * Implements a Baseline Feature Generator for Statistical Lemmatization.
 * @author ragerri
 * @version 2016-05-12
 */
public class LemmaBaselineContextGenerator extends CustomFeatureGenerator implements ArtifactToSerializerMapper {

  private SequenceModelResource posModelResource;
  private Span[] currentTags;
  private String[] currentSentence;
  private static final int PREFIX_LENGTH = 5;
  private static final int SUFFIX_LENGTH = 7;

  private static Pattern hasCap = Pattern.compile("[A-Z]");
  private static Pattern hasNum = Pattern.compile("[0-9]");
  
  public LemmaBaselineContextGenerator() {
  }
  
  private static String[] getPrefixes(String lex) {
    String[] prefs = new String[PREFIX_LENGTH];
    for (int li = 1, ll = PREFIX_LENGTH; li < ll; li++) {
      prefs[li] = lex.substring(0, Math.min(li + 1, lex.length()));
    }
    return prefs;
  }

  private static String[] getSuffixes(String lex) {
    String[] suffs = new String[SUFFIX_LENGTH];
    for (int li = 1, ll = SUFFIX_LENGTH; li < ll; li++) {
      suffs[li] = lex.substring(Math.max(lex.length() - li - 1, 0));
    }
    return suffs;
  }
  
  @Override
  public void createFeatures(List<String> features, String[] tokens, int index,
      String[] previousOutcomes) {
    
    // cache annotation results for each sentence
    if (currentSentence != tokens) {
      currentSentence = tokens;
      currentTags = posModelResource.seqToSpans(tokens);
    }
    // Word
    String w0;
    // Tag
    String t0;
    // Previous prediction
    String p_1;

    String lex = tokens[index].toString();
    if (index < 1) {
      p_1 = "p_1=bos";
    }
    else {
      p_1 = "p_1=" + previousOutcomes[index - 1];
    }
    w0 = "w0=" + tokens[index];
    t0 = "t0=" + currentTags[index].getType();
    
    features.add(w0);
    features.add(t0);
    features.add(p_1);
    features.add(p_1 + t0);
    features.add(p_1 + w0);
    
    // do some basic suffix analysis
    String[] suffs = getSuffixes(lex);
    for (int i = 0; i < suffs.length; i++) {
      features.add("suf=" + suffs[i]);
    }

    String[] prefs = getPrefixes(lex);
    for (int i = 0; i < prefs.length; i++) {
      features.add("pre=" + prefs[i]);
    }
    // see if the word has any special characters
    if (lex.indexOf('-') != -1) {
      features.add("h");
    }

    if (hasCap.matcher(lex).find()) {
      features.add("c");
    }

    if (hasNum.matcher(lex).find()) {
      features.add("d");
    }
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
  }

  @Override
  public Map<String, ArtifactSerializer<?>> getArtifactSerializerMapping() {
    Map<String, ArtifactSerializer<?>> mapping = new HashMap<>();
    mapping.put("posmodelserializer",
        new SequenceModelResource.SequenceModelResourceSerializer());
    return  Collections.unmodifiableMap(mapping);
  }

}
