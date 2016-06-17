/*
 * Copyright 2015 Rodrigo Agerri

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
package eus.ixa.ixa.pipe.ml.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import opennlp.tools.util.TrainingParameters;

/**
 * Flags for training.
 * @author ragerri
 *
 */
public class Flags {

  public static final boolean DEBUG = false;
  public static final String DEFAULT_FEATURE_FLAG = "no";
  public static final String DEFAULT_WINDOW = "2:2";
  public static final String DEFAULT_TOKEN_RANGE = "lower";
  public static final String DEFAULT_TOKENCLASS_RANGE = "lower,wac";
  public static final String DEFAULT_SENTENCE_BEGIN = "true";
  public static final String DEFAULT_SENTENCE_END = "false";
  public static final String DEFAULT_PREFIX_BEGIN = "3";
  public static final String DEFAULT_PREFIX_END = "4";
  public static final String DEFAULT_SUFFIX_BEGIN = "0";
  public static final String DEFAULT_SUFFIX_END = "4";
  public static final String CHAR_NGRAM_RANGE = "2:5";
  public static final String DEFAULT_POSTAG_RANGE = "pos,posclass";
  public static final String DEFAULT_MFS_RANGE = "pos,posclass,lemma,mfs,no";
  public static final String DEFAULT_SUPERSENSE_RANGE = "mfs,monosemic";

  /**
   * Default beam size for decoding.
   */
  public static final int DEFAULT_BEAM_SIZE = 3;
  public static final int DEFAULT_FOLDS_VALUE = 10;
  public static final String DEFAULT_EVALUATE_MODEL = "off";
  public static final String DEFAULT_SEQUENCE_TYPES = "off";
  public static final String DEFAULT_LEXER = "off";
  public static final String DEFAULT_DICT_OPTION = "off";
  public static final String DEFAULT_DICT_PATH = "off";
  public static final String DEFAULT_OUTPUT_FORMAT = "naf";
  public static final String DEFAULT_SEQUENCE_CODEC = "BILOU";
  public static final String DEFAULT_EVAL_FORMAT = "conll02";
  public static final String DEFAULT_TASK = "ner";
  public static final String DEFAULT_HOSTNAME= "localhost";

  private Flags() {

  }
  
  
  public static String getComponent(final TrainingParameters params) {
    String component = null;
    if (params.getSettings().get("Component") == null) {
      componentException();
    } else {
      component = params.getSettings().get("Component");
    }
    return component;
  }

  public static String getLanguage(TrainingParameters params) {
    String lang = null;
    if (params.getSettings().get("Language") == null) {
      langException();
    } else {
      lang = params.getSettings().get("Language");
    }
    return lang;
  }

  public static String getDataSet(String dataset, TrainingParameters params) {
    String trainSet = null;
    if (params.getSettings().get(dataset) == null) {
      datasetException();
    } else {
      trainSet = params.getSettings().get(dataset);
    }
    return trainSet;
  }

  public static String getModel(TrainingParameters params) {
    String model = null;
    if (params.getSettings().get("OutputModel") == null) {
      modelException();
    } else if (params.getSettings().get("OutputModel") != null
        && params.getSettings().get("OutputModel").length() == 0) {
      modelException();
    } else {
      model = params.getSettings().get("OutputModel");
    }
    return model;
  }

  public static String getCorpusFormat(TrainingParameters params) {
    String corpusFormat = null;
    if (params.getSettings().get("CorpusFormat") == null) {
      corpusFormatException();
    } else {
      corpusFormat = params.getSettings().get("CorpusFormat");
    }
    return corpusFormat;
  }

  public static String getOutputFormat(TrainingParameters params) {
    String outFormatOption = null;
    if (params.getSettings().get("OutputFormat") != null) {
      outFormatOption = params.getSettings().get("OutputFormat");
    } else {
      outFormatOption = Flags.DEFAULT_OUTPUT_FORMAT;
    }
    return outFormatOption;
  }

  public static Integer getBeamsize(TrainingParameters params) {
    Integer beamsize = null;
    if (params.getSettings().get("BeamSize") == null) {
      beamsize = Flags.DEFAULT_BEAM_SIZE;
    } else {
      beamsize = Integer.parseInt(params.getSettings().get("BeamSize"));
    }
    return beamsize;
  }

  public static Integer getFolds(TrainingParameters params) {
    Integer beamsize = null;
    if (params.getSettings().get("Folds") == null) {
      beamsize = Flags.DEFAULT_FOLDS_VALUE;
    } else {
      beamsize = Integer.parseInt(params.getSettings().get("Folds"));
    }
    return beamsize;
  }

  public static String getSequenceCodec(TrainingParameters params) {
    String seqCodec = null;
    if (params.getSettings().get("SequenceCodec") == null) {
      seqCodec = Flags.DEFAULT_SEQUENCE_CODEC;
    } else {
      seqCodec = params.getSettings().get("SequenceCodec");
    }
    return seqCodec;
  }

  public static String getClearTrainingFeatures(TrainingParameters params) {
    String clearFeatures = null;
    if (params.getSettings().get("ClearTrainingFeatures") == null) {
      clearFeatures = Flags.DEFAULT_FEATURE_FLAG;
    } else {
      clearFeatures = params.getSettings().get("ClearTrainingFeatures");
    }
    return clearFeatures;
  }

  public static String getClearEvaluationFeatures(TrainingParameters params) {
    String clearFeatures = null;
    if (params.getSettings().get("ClearEvaluationFeatures") == null) {
      clearFeatures = Flags.DEFAULT_FEATURE_FLAG;
    } else {
      clearFeatures = params.getSettings().get("ClearEvaluationFeatures");
    }
    return clearFeatures;
  }

  public static String getWindow(TrainingParameters params) {
    String windowFlag = null;
    if (params.getSettings().get("Window") == null) {
      windowFlag = Flags.DEFAULT_WINDOW;
    } else {
      windowFlag = params.getSettings().get("Window");
    }
    return windowFlag;
  }

  public static String getTokenFeatures(TrainingParameters params) {
    String tokenFlag = null;
    if (params.getSettings().get("TokenFeatures") != null) {
      tokenFlag = params.getSettings().get("TokenFeatures");
    } else {
      tokenFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return tokenFlag;
  }
  
  public static String getTokenFeaturesRange(TrainingParameters params) {
    String tokenRangeFlag = null;
    if (params.getSettings().get("TokenFeaturesRange") != null) {
      tokenRangeFlag = params.getSettings().get("TokenFeaturesRange");
    } else {
      tokenRangeFlag = DEFAULT_TOKEN_RANGE;
    }
    return tokenRangeFlag;
  }

  public static String getTokenClassFeatures(TrainingParameters params) {
    String tokenClassFlag = null;
    if (params.getSettings().get("TokenClassFeatures") != null) {
      tokenClassFlag = params.getSettings().get("TokenClassFeatures");
    } else {
      tokenClassFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return tokenClassFlag;
  }
  
  public static String getTokenClassFeaturesRange(TrainingParameters params) {
    String tokenRangeFlag = null;
    if (params.getSettings().get("TokenClassFeaturesRange") != null) {
      tokenRangeFlag = params.getSettings().get("TokenClassFeaturesRange");
    } else {
      tokenRangeFlag = DEFAULT_TOKENCLASS_RANGE;
    }
    return tokenRangeFlag;
  }
  
  public static String[] processTokenClassFeaturesRange(String mfsFlag) {
    String[] mfsFlagArray = mfsFlag.split(",");
    if (mfsFlagArray.length != 2) {
      System.err.println("TokenClassFeaturesRange requires two fields but got " + mfsFlagArray.length);
      System.exit(1);
    }
    return mfsFlagArray;
  }

  public static String getWordShapeSuperSenseFeatures(TrainingParameters params) {
    String tokenClassFlag = null;
    if (params.getSettings().get("WordShapeSuperSenseFeatures") != null) {
      tokenClassFlag = params.getSettings().get("WordShapeSuperSenseFeatures");
    } else {
      tokenClassFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return tokenClassFlag;
  }

  public static String getOutcomePriorFeatures(TrainingParameters params) {
    String outcomePriorFlag = null;
    if (params.getSettings().get("OutcomePriorFeatures") != null) {
      outcomePriorFlag = params.getSettings().get("OutcomePriorFeatures");
    } else {
      outcomePriorFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return outcomePriorFlag;
  }

  public static String getPreviousMapFeatures(TrainingParameters params) {
    String previousMapFlag = null;
    if (params.getSettings().get("PreviousMapFeatures") != null) {
      previousMapFlag = params.getSettings().get("PreviousMapFeatures");
    } else {
      previousMapFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return previousMapFlag;
  }

  public static String getSentenceFeatures(TrainingParameters params) {
    String sentenceFlag = null;
    if (params.getSettings().get("SentenceFeatures") != null) {
      sentenceFlag = params.getSettings().get("SentenceFeatures");
    } else {
      sentenceFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return sentenceFlag;
  }
  
  public static String getSentenceFeaturesBegin(TrainingParameters params) {
    String beginFlag = null;
    if (params.getSettings().get("SentenceFeaturesBegin") != null) {
      beginFlag = params.getSettings().get("SentenceFeaturesBegin");
    } else {
      beginFlag = Flags.DEFAULT_SENTENCE_BEGIN;
    }
    return beginFlag;
  }
  
  public static String getSentenceFeaturesEnd(TrainingParameters params) {
    String endFlag = null;
    if (params.getSettings().get("SentenceFeaturesEnd") != null) {
      endFlag = params.getSettings().get("SentenceFeaturesEnd");
    } else {
      endFlag = Flags.DEFAULT_SENTENCE_END;
    }
    return endFlag;
  }


  public static String getPreffixFeatures(TrainingParameters params) {
    String prefixFlag = null;
    if (params.getSettings().get("PrefixFeatures") != null) {
      prefixFlag = params.getSettings().get("PrefixFeatures");
    } else {
      prefixFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return prefixFlag;
  }
  
  public static String getPrefixFeaturesBegin(TrainingParameters params) {
    String beginFlag = null;
    if (params.getSettings().get("PrefixFeaturesBegin") != null) {
      beginFlag = params.getSettings().get("PrefixFeaturesBegin");
    } else {
      beginFlag = Flags.DEFAULT_PREFIX_BEGIN;
    }
    return beginFlag;
  }
  
  public static String getPrefixFeaturesEnd(TrainingParameters params) {
    String endFlag = null;
    if (params.getSettings().get("PrefixFeaturesEnd") != null) {
      endFlag = params.getSettings().get("PrefixFeaturesEnd");
    } else {
      endFlag = Flags.DEFAULT_PREFIX_END;
    }
    return endFlag;
  }


  public static String getSuffixFeatures(TrainingParameters params) {
    String suffixFlag = null;
    if (params.getSettings().get("SuffixFeatures") != null) {
      suffixFlag = params.getSettings().get("SuffixFeatures");
    } else {
      suffixFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return suffixFlag;
  }
  
  public static String getSuffixFeaturesBegin(TrainingParameters params) {
    String beginFlag = null;
    if (params.getSettings().get("SuffixFeaturesBegin") != null) {
      beginFlag = params.getSettings().get("SuffixFeaturesBegin");
    } else {
      beginFlag = Flags.DEFAULT_SUFFIX_BEGIN;
    }
    return beginFlag;
  }
  
  public static String getSuffixFeaturesEnd(TrainingParameters params) {
    String endFlag = null;
    if (params.getSettings().get("SuffixFeaturesEnd") != null) {
      endFlag = params.getSettings().get("SuffixFeaturesEnd");
    } else {
      endFlag = Flags.DEFAULT_SUFFIX_END;
    }
    return endFlag;
  }

  public static String getBigramClassFeatures(TrainingParameters params) {
    String bigramClassFlag = null;
    if (params.getSettings().get("BigramClassFeatures") != null) {
      bigramClassFlag = params.getSettings().get("BigramClassFeatures");
    } else {
      bigramClassFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return bigramClassFlag;
  }

  public static String getTrigramClassFeatures(TrainingParameters params) {
    String trigramClassFlag = null;
    if (params.getSettings().get("TrigramClassFeatures") != null) {
      trigramClassFlag = params.getSettings().get("TrigramClassFeatures");
    } else {
      trigramClassFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return trigramClassFlag;
  }

  public static String getFourgramClassFeatures(TrainingParameters params) {
    String fourgramClassFlag = null;
    if (params.getSettings().get("FourgramClassFeatures") != null) {
      fourgramClassFlag = params.getSettings().get("FourgramClassFeatures");
    } else {
      fourgramClassFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return fourgramClassFlag;
  }

  public static String getFivegramClassFeatures(TrainingParameters params) {
    String fivegramClassFlag = null;
    if (params.getSettings().get("FivegramClassFeatures") != null) {
      fivegramClassFlag = params.getSettings().get("FivegramClassFeatures");
    } else {
      fivegramClassFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return fivegramClassFlag;
  }

  public static String getCharNgramFeatures(TrainingParameters params) {
    String charNgramFlag = null;
    if (params.getSettings().get("CharNgramFeatures") != null) {
      charNgramFlag = params.getSettings().get("CharNgramFeatures");
    } else {
      charNgramFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return charNgramFlag;
  }

  public static String getCharNgramFeaturesRange(TrainingParameters params) {
    String charNgramRangeFlag = null;
    if (params.getSettings().get("CharNgramFeaturesRange") != null) {
      charNgramRangeFlag = params.getSettings().get("CharNgramFeaturesRange");
    } else {
      charNgramRangeFlag = Flags.CHAR_NGRAM_RANGE;
    }
    return charNgramRangeFlag;
  }
  
  public static String[] processNgramRange(String charngramRangeFlag) {
    String[] charngramArray = charngramRangeFlag.split("[ :-]");
    if (charngramArray.length != 2) {
      System.err.println("CharNgramFeaturesRange requires two fieds but got " + charngramArray.length);
      System.exit(1);
    }
  return charngramArray;
}

  public static String getDictionaryFeatures(TrainingParameters params) {
    String dictionaryFlag = null;
    if (params.getSettings().get("DictionaryFeatures") != null) {
      dictionaryFlag = params.getSettings().get("DictionaryFeatures");
    } else {
      dictionaryFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return dictionaryFlag;
  }

  public static String getClarkFeatures(TrainingParameters params) {
    String distSimFlag = null;
    if (params.getSettings().get("ClarkClusterFeatures") != null) {
      distSimFlag = params.getSettings().get("ClarkClusterFeatures");
    } else {
      distSimFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return distSimFlag;
  }

  public static String getWord2VecClusterFeatures(TrainingParameters params) {
    String word2vecFlag = null;
    if (params.getSettings().get("Word2VecClusterFeatures") != null) {
      word2vecFlag = params.getSettings().get("Word2VecClusterFeatures");
    } else {
      word2vecFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return word2vecFlag;
  }

  public static String getBrownFeatures(TrainingParameters params) {
    String brownFlag = null;
    if (params.getSettings().get("BrownClusterFeatures") != null) {
      brownFlag = params.getSettings().get("BrownClusterFeatures");
    } else {
      brownFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return brownFlag;
  }
  
  public static String getPOSDictionaryFeatures(TrainingParameters params) {
    String dictionaryFlag = null;
    if (params.getSettings().get("POSDictionaryFeatures") != null) {
      dictionaryFlag = params.getSettings().get("POSDictionaryFeatures");
    } else {
      dictionaryFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return dictionaryFlag;
  }
  
  public static String getPOSTagModelFeatures(TrainingParameters params) {
    String morphoFlag = null;
    if (params.getSettings().get("POSTagModelFeatures") != null) {
      morphoFlag = params.getSettings().get("POSTagModelFeatures");
    } else {
      morphoFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return morphoFlag;
  }
  
  /**
   * Get the morphological features configuration.
   * 
   * @param params
   *          the training parameters
   * @return a list containing the options
   */
  public static String getPOSTagModelFeaturesRange(TrainingParameters params) {
    String lemmaRangeFlag = null;
    if (params.getSettings().get("POSTagModelFeaturesRange") != null) {
      lemmaRangeFlag = params.getSettings().get("POSTagModelFeaturesRange");
    } else {
      lemmaRangeFlag = Flags.DEFAULT_POSTAG_RANGE;
    }
    return lemmaRangeFlag;
  }
  
  public static String[] processPOSTagModelFeaturesRange(String mfsFlag) {
    String[] mfsFlagArray = mfsFlag.split(",");
    if (mfsFlagArray.length != 2) {
      System.err.println("POSTagFeaturesRange requires two fields but got " + mfsFlagArray.length);
      System.exit(1);
    }
    return mfsFlagArray;
  }
  
  
  public static String getLemmaModelFeatures(TrainingParameters params) {
    String morphoFlag = null;
    if (params.getSettings().get("LemmaModelFeatures") != null) {
      morphoFlag = params.getSettings().get("LemmaModelFeatures");
    } else {
      morphoFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return morphoFlag;
  }
  
  
  public static String getLemmaDictionaryFeatures(TrainingParameters params) {
    String morphoFlag = null;
    if (params.getSettings().get("LemmaDictionaryFeatures") != null) {
      morphoFlag = params.getSettings().get("LemmaDictionaryFeatures");
    } else {
      morphoFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return morphoFlag;
  }
  
  public static String[] getLemmaDictionaryResources(String mfsFlag) {
    String[] mfsFlagArray = mfsFlag.split(",");
    if (mfsFlagArray.length != 2) {
      System.err.println("LemmaDictionary resources requires two fields but got " + mfsFlagArray.length);
      System.exit(1);
    }
    return mfsFlagArray;
  }

  
  public static String getSuperSenseFeatures(TrainingParameters params) {
    String mfsFlag = null;
    if (params.getSettings().get("SuperSenseFeatures") != null) {
      mfsFlag = params.getSettings().get("SuperSenseFeatures");
    } else {
      mfsFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return mfsFlag;
  }
  
  public static String getPOSBaselineFeatures(TrainingParameters params) {
    String posFlag = null;
    if (params.getSettings().get("POSBaselineFeatures") != null) {
      posFlag = params.getSettings().get("POSBaselineFeatures");
    } else {
      posFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return posFlag;
  }
  
  public static String getMFSFeatures(TrainingParameters params) {
    String mfsFlag = null;
    if (params.getSettings().get("MFSFeatures") != null) {
      mfsFlag = params.getSettings().get("MFSFeatures");
    } else {
      mfsFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return mfsFlag;
  }
  
  
  public static String getLemmaBaselineFeatures(TrainingParameters params) {
    String lemmaFlag = null;
    if (params.getSettings().get("LemmaBaselineFeatures") != null) {
      lemmaFlag = params.getSettings().get("LemmaBaselineFeatures");
    } else {
      lemmaFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return lemmaFlag;
  }
  
  public static String getLemmaBaselineFeaturesRange(TrainingParameters params) {
    String mfsRangeFlag = null;
    if (params.getSettings().get("LemmaBaselineFeaturesRange") != null) {
      mfsRangeFlag = params.getSettings().get("LemmaBaselineFeaturesRange");
    } else {
      mfsRangeFlag = Flags.DEFAULT_POSTAG_RANGE;
    }
    return mfsRangeFlag;
  }
  
  public static String[] processLemmaBaselineFeaturesRange(String mfsFlag) {
    String[] mfsFlagArray = mfsFlag.split(",");
    if (mfsFlagArray.length != 2) {
      System.err.println("LemmaBaselineFeaturesRange requires two fields but got " + mfsFlagArray.length);
      System.exit(1);
    }
    return mfsFlagArray;
  }
  
  public static String getChunkBaselineFeatures(TrainingParameters params) {
    String lemmaFlag = null;
    if (params.getSettings().get("ChunkBaselineFeatures") != null) {
      lemmaFlag = params.getSettings().get("ChunkBaselineFeatures");
    } else {
      lemmaFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return lemmaFlag;
  }
  
  public static String[] getSuperSenseResources(String mfsFlag) {
    String[] mfsFlagArray = mfsFlag.split(",");
    if (mfsFlagArray.length != 3) {
      System.err.println("SuperSense resources requires three fields but got " + mfsFlagArray.length);
      System.exit(1);
    }
    return mfsFlagArray;
  }
  
  public static String[] getMFSResources(String mfsFlag) {
    String[] mfsFlagArray = mfsFlag.split(",");
    if (mfsFlagArray.length != 3) {
      System.err.println("MFS resources requires three fields but got " + mfsFlagArray.length);
      System.exit(1);
    }
    return mfsFlagArray;
  }
  
  public static String getSuperSenseFeaturesRange(TrainingParameters params) {
    String mfsRangeFlag = null;
    if (params.getSettings().get("SuperSenseFeaturesRange") != null) {
      mfsRangeFlag = params.getSettings().get("SuperSenseFeaturesRange");
    } else {
      mfsRangeFlag = Flags.DEFAULT_SUPERSENSE_RANGE;
    }
    return mfsRangeFlag;
  }
  
  public static String getMFSFeaturesRange(TrainingParameters params) {
    String mfsRangeFlag = null;
    if (params.getSettings().get("MFSFeaturesRange") != null) {
      mfsRangeFlag = params.getSettings().get("MFSFeaturesRange");
    } else {
      mfsRangeFlag = Flags.DEFAULT_SUPERSENSE_RANGE;
    }
    return mfsRangeFlag;
  }
  
  public static String[] processSuperSenseFeaturesRange(String mfsFlag) {
    String[] mfsFlagArray = mfsFlag.split(",");
    if (mfsFlagArray.length != 2) {
      System.err.println("SuperSenseFeaturesRange requires two fields but got " + mfsFlagArray.length);
      System.exit(1);
    }
    return mfsFlagArray;
  }
  
  public static String[] processMFSFeaturesRange(String mfsFlag) {
    String[] mfsFlagArray = mfsFlag.split(",");
    if (mfsFlagArray.length != 5) {
      System.err.println("MFSFeaturesRange requires five fields but got " + mfsFlagArray.length);
      System.exit(1);
    }
    return mfsFlagArray;
  }
  
  /**
   * Get a parameter in trainParams.prop file consisting of a list of clustering
   * lexicons separated by comma "," and return a list of files, one for each
   * lexicon.
   * 
   * @param clusterPath
   *          the clustering parameter in the prop file
   * @return a list of files one for each lexicon
   */
  public static List<File> getClusterLexiconFiles(String clusterPath) {
    List<File> clusterLexicons = new ArrayList<File>();
    String[] clusterPaths = clusterPath.split(",");
    for (String clusterName : clusterPaths) {
      clusterLexicons.add(new File(clusterName));
    }
    return clusterLexicons;

  }
  
  public static String getHeadRulesFile(TrainingParameters params) {
    String headRulesFlag = null;
    if (params.getSettings().get("HeadRules") != null) {
      headRulesFlag = params.getSettings().get("HeadRules");
    } else {
      System.err.println("Specify head rules file to train the parser!!");
    }
    return headRulesFlag; 
  }
  
  public static boolean isChunkBaselineFeatures(TrainingParameters params) {
    String posFeatures = getChunkBaselineFeatures(params);
    return !posFeatures.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }
  
  public static boolean isLemmaBaselineFeatures(TrainingParameters params) {
    String posFeatures = getLemmaBaselineFeatures(params);
    return !posFeatures.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }
  
  /**
   * Check if POS Baseline features are active.
   * @param params the parameters
   * @return whether the pos baseline features are activated or not
   */
  public static boolean isPOSBaselineFeatures(TrainingParameters params) {
    String posFeatures = getPOSBaselineFeatures(params);
    return !posFeatures.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }
  
  /**
   * Check if supersense tagger features are active.
   * @param params the parameters
   * @return whether the supersense features are activated or not
   */
  public static boolean isSuperSenseFeatures(TrainingParameters params) {
    String mfsFeatures = getSuperSenseFeatures(params);
    return !mfsFeatures.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }
  
  /**
   * Check if mfs features are active.
   * @param params the parameters
   * @return whether the mfs features are activated or not
   */
  public static boolean isMFSFeatures(TrainingParameters params) {
    String mfsFeatures = getMFSFeatures(params);
    return !mfsFeatures.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }
  
  public static boolean isLemmaDictionaryFeatures(TrainingParameters params) {
    String morphoFeatures = getLemmaDictionaryFeatures(params);
    return !morphoFeatures.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }
  
  public static boolean isLemmaModelFeatures(TrainingParameters params) {
    String morphoFeatures = getLemmaModelFeatures(params);
    return !morphoFeatures.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }
  
  public static boolean isPOSDictionaryFeatures(TrainingParameters params) {
    String dictFeatures = getDictionaryFeatures(params);
    return !dictFeatures.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }
  
  public static boolean isPOSTagModelFeatures(TrainingParameters params) {
    String morphoFeatures = getPOSTagModelFeatures(params);
    return !morphoFeatures.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isWord2VecClusterFeatures(TrainingParameters params) {
    String word2vecClusterFeatures = getWord2VecClusterFeatures(params);
    return !word2vecClusterFeatures
        .equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isClarkFeatures(TrainingParameters params) {
    String clarkFeatures = getClarkFeatures(params);
    return !clarkFeatures.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isBrownFeatures(TrainingParameters params) {
    String brownFeatures = getBrownFeatures(params);
    return !brownFeatures.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isDictionaryFeatures(TrainingParameters params) {
    String dictFeatures = getDictionaryFeatures(params);
    return !dictFeatures.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isCharNgramClassFeature(TrainingParameters params) {
    String charngramParam = getCharNgramFeatures(params);
    return !charngramParam.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isFivegramClassFeature(TrainingParameters params) {
    String fivegramParam = getFivegramClassFeatures(params);
    return !fivegramParam.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isFourgramClassFeature(TrainingParameters params) {
    String fourgramParam = getFourgramClassFeatures(params);
    return !fourgramParam.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isTrigramClassFeature(TrainingParameters params) {
    String trigramParam = getTrigramClassFeatures(params);
    return !trigramParam.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isBigramClassFeature(TrainingParameters params) {
    String bigramParam = getBigramClassFeatures(params);
    return !bigramParam.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isSuffixFeature(TrainingParameters params) {
    String suffixParam = getSuffixFeatures(params);
    return !suffixParam.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isPrefixFeature(TrainingParameters params) {
    String prefixParam = getPreffixFeatures(params);
    return !prefixParam.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isSentenceFeature(TrainingParameters params) {
    String sentenceParam = getSentenceFeatures(params);
    return !sentenceParam.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isPreviousMapFeature(TrainingParameters params) {
    String previousMapParam = getPreviousMapFeatures(params);
    return !previousMapParam.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isOutcomePriorFeature(TrainingParameters params) {
    String outcomePriorParam = getOutcomePriorFeatures(params);
    return !outcomePriorParam.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }
  
  public static boolean isWordShapeSuperSenseFeature(TrainingParameters params) {
    String tokenParam = getWordShapeSuperSenseFeatures(params);
    return !tokenParam.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isTokenClassFeature(TrainingParameters params) {
    String tokenParam = getTokenClassFeatures(params);
    return !tokenParam.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isTokenFeature(TrainingParameters params) {
    String tokenParam = getTokenFeatures(params);
    return !tokenParam.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }
  
  
  public static void componentException() {
    System.err
        .println("Please provide a component name in the Component field in the parameters file!");
    System.exit(1);
  }

  public static void devSetException() {
    System.err
        .println("UseDevSet options in the parameters file if CrossEval is activated!");
    System.exit(1);
  }

  public static void modelException() {
    System.err
        .println("Please provide a model in the OutputModel field in the parameters file!");
    System.exit(1);
  }

  public static void langException() {
    System.err
        .println("Please fill in the Language field in the parameters file!");
    System.exit(1);
  }

  public static void datasetException() {
    System.err
        .println("Please specify your training/testing sets in the TrainSet and TestSet fields in the parameters file!");
    System.exit(1);
  }

  public static void corpusFormatException() {
    System.err
        .println("Please fill in CorpusFormat field in the parameters file!");
    System.exit(1);
  }

  public static void dictionaryException() {
    System.err
        .println("You need to set the --dictPath option to the dictionaries directory to use the dictTag option!");
    System.exit(1);
  }

  public static void dictionaryFeaturesException() {
    System.err
        .println("You need to specify the DictionaryFeatures in the parameters file to use the DictionaryPath!");
    System.exit(1);
  }

}
