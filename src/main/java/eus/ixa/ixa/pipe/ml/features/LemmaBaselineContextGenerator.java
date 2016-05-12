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
import eus.ixa.ixa.pipe.ml.utils.Flags;
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
  private boolean isPos;
  private boolean isPosClass;
  
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
    String t0 = null;
    // Previous prediction
    String p_1;
    String lex = tokens[index].toString();
    String posTag = currentTags[index].getType();
    if (index < 1) {
      p_1 = "p_1=bos";
    } else {
      p_1 = "p_1=" + previousOutcomes[index - 1];
    }
    w0 = "w0=" + tokens[index];
    if (isPos) {
      t0 = "t0=" + posTag;
    }
    if (isPosClass) {
      String posTagClass = posTag.substring(0, 1);
      features.add("posTagClass=" + posTagClass);
    }
    
    //adding features
    features.add(w0);
    features.add(t0);
    features.add(p_1);
    features.add(p_1 + t0);
    features.add(p_1 + w0);
    addTokenShapeFeatures(features, lex);
  }
  
  private void addTokenShapeFeatures(List<String> features, String lex) {
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
    Object posResource = resourceProvider.getResource(properties.get("model"));
    if (!(posResource instanceof SequenceModelResource)) {
      throw new InvalidFormatException("Not a SequenceModelResource for key: " + properties.get("model"));
    }
    this.posModelResource = (SequenceModelResource) posResource;
    processRangeOptions(properties);
  }
  
  /**
   * Process the options of which kind of features are to be generated.
   * @param properties the properties map
   */
  private void processRangeOptions(Map<String, String> properties) {
    String featuresRange = properties.get("range");
    String[] rangeArray = Flags.processLemmaBaselineFeaturesRange(featuresRange);
    if (rangeArray[0].equalsIgnoreCase("pos")) {
      isPos = true;
    }
    if (rangeArray[1].equalsIgnoreCase("posclass")) {
      isPosClass = true;
    }
  }
  
  
  @Override
  public Map<String, ArtifactSerializer<?>> getArtifactSerializerMapping() {
    Map<String, ArtifactSerializer<?>> mapping = new HashMap<>();
    mapping.put("seqmodelserializer", new SequenceModelResource.SequenceModelResourceSerializer());
    return Collections.unmodifiableMap(mapping);
  }

}
