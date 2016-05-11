package eus.ixa.ixa.pipe.ml.features;

/*
 * Copyright 2014 Rodrigo Agerri

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.util.Cache;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.StringList;
import opennlp.tools.util.featuregen.ArtifactToSerializerMapper;
import opennlp.tools.util.featuregen.CustomFeatureGenerator;
import opennlp.tools.util.featuregen.FeatureGeneratorResourceProvider;
import opennlp.tools.util.model.ArtifactSerializer;

/**
 * A baseline context generator for the POS Tagger. This baseline generator
 * provides more contextual features such as bigrams to the
 * {@code @DefaultPOSContextGenerator}. These extra features require at least
 * 2GB memory to train, more if training data is large.
 *
 * @author ragerri
 * @version 2014-07-08
 */
public class POSBaselineContextGenerator extends CustomFeatureGenerator implements
ArtifactToSerializerMapper {

  /**
   * The ending string.
   */
  private final String SE = "*SE*";
  /**
   * The starting string.
   */
  private final String SB = "*SB*";
  /**
   * Default prefix length.
   */
  private static final int PREFIX_LENGTH = 4;
  /**
   * Default suffix length.
   */
  private static final int SUFFIX_LENGTH = 4;
  /**
   * Has capital regexp.
   */
  private static Pattern hasCap = Pattern.compile("[A-Z]");
  /**
   * Has number regexp.
   */
  private static Pattern hasNum = Pattern.compile("[0-9]");
  /**
   * The context Cache.
   */
  private Cache contextsCache;
  /**
   * The words key.
   */
  private Object wordsKey;
  /**
   * The tag dictionary.
   */
  private Dictionary dict;
  /**
   * The dictionary ngrams.
   */
  private String[] dictGram;

  public POSBaselineContextGenerator() {
    dictGram = new String[1];
  }
  
  /**
   * Initializes the current instance.
   * @param aDict
   *          the dictionary
   */
  public POSBaselineContextGenerator(final Dictionary aDict) {
    this(0, aDict);
  }

  /**
   * Initializes the current instance.
   * @param cacheSize
   *          the cache size
   * @param aDict
   *          the dictionary
   */
  public POSBaselineContextGenerator(final int cacheSize, final Dictionary aDict) {
    this.dict = aDict;
    dictGram = new String[1];
    if (cacheSize > 0) {
      contextsCache = new Cache(cacheSize);
    }
  }

  /**
   * Obtain prefixes for each token.
   * @param lex
   *          the current word
   * @return the prefixes
   */
  protected static String[] getPrefixes(final String lex) {
    String[] prefs = new String[PREFIX_LENGTH];
    for (int li = 2, ll = PREFIX_LENGTH; li < ll; li++) {
      prefs[li] = lex.substring(0, Math.min(li + 1, lex.length()));
    }
    return prefs;
  }

  /**
   * Obtain suffixes for each token.
   * @param lex
   *          the word
   * @return the suffixes
   */
  protected static String[] getSuffixes(final String lex) {
    String[] suffs = new String[SUFFIX_LENGTH];
    for (int li = 0, ll = SUFFIX_LENGTH; li < ll; li++) {
      suffs[li] = lex.substring(Math.max(lex.length() - li - 1, 0));
    }
    return suffs;
  }

  public void createFeatures(List<String> features, String[] tokens, int index,
      String[] previousOutcomes) {
    
    String next, nextnext, lex, prev, prevprev;
    String tagprev, tagprevprev;
    tagprev = tagprevprev = null;
    next = nextnext = lex = prev = prevprev = null;

    lex = tokens[index].toString();
    if (tokens.length > index + 1) {
      next = tokens[index + 1].toString();
      if (tokens.length > index + 2) {
        nextnext = tokens[index + 2].toString();
      } else {
        nextnext = SE; // Sentence End
      }

    } else {
      next = SE; // Sentence End
    }

    if (index - 1 >= 0) {
      prev = tokens[index - 1].toString();
      tagprev = previousOutcomes[index - 1];

      if (index - 2 >= 0) {
        prevprev = tokens[index - 2].toString();
        tagprevprev = previousOutcomes[index - 2];
      } else {
        prevprev = SB; // Sentence Beginning
      }
    } else {
      prev = SB; // Sentence Beginning
    }
    String cacheKey = index + tagprev + tagprevprev;
    /*if (contextsCache != null) {
      if (wordsKey == tokens) {
        String[] cachedContexts = (String[]) contextsCache.get(cacheKey);
        if (cachedContexts != null) {
          return cachedContexts;
        }
      } else {
        contextsCache.clear();
        wordsKey = tokens;
      }
    }*/
    features.add("default");
    // add the word itself
    features.add("w=" + lex);
    dictGram[0] = lex;
    if (dict == null || !dict.contains(new StringList(dictGram))) {
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
    // add the words and pos's of the surrounding context
    if (prev != null) {
      features.add("pw=" + prev);
      // bigram w-1,w
      features.add("pw,w=" + prev + "," + lex);
      if (tagprev != null) {
        features.add("pt=" + tagprev);
        // bigram tag-1, w
        features.add("pt,w=" + tagprev + "," + lex);
      }
      if (prevprev != null) {
        features.add("ppw=" + prevprev);
        if (tagprevprev != null) {
          // bigram tag-2,tag-1
          features.add("pt2,pt1=" + tagprevprev + "," + tagprev);
        }
      }
    }

    if (next != null) {
      features.add("nw=" + next);
      if (nextnext != null) {
        features.add("nnw=" + nextnext);

      }
    }
    /*String[] contexts = featureList.toArray(new String[featureList.size()]);
    if (contextsCache != null) {
      contextsCache.put(cacheKey, contexts);
    }
    return contexts;*/
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
   return null;
  }


}

