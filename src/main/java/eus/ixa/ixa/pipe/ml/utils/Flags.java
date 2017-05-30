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
 * 
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
  public static final String DEFAULT_BOW_RANGE = "no,no";

  /**
   * Default beam size for decoding.
   */
  public static final int DEFAULT_BEAM_SIZE = 3;
  public static final int DEFAULT_FOLDS_VALUE = 10;
  public static final String DEFAULT_EVALUATE_MODEL = "off";
  public static final String DEFAULT_SEQUENCE_TYPES = "off";
  public static final String DEFAULT_LEXER = "off";
  public static final int DEFAULT_DICT_CUTOFF = -1;
  public static final String DEFAULT_DICT_OPTION = "off";
  public static final String DEFAULT_DICT_PATH = "off";
  public static final String DEFAULT_OUTPUT_FORMAT = "naf";
  public static final String DEFAULT_SEQUENCE_CODEC = "BILOU";
  public static final String DEFAULT_EVAL_FORMAT = "conll02";
  public static final String DEFAULT_TASK = "ner";
  public static final String DEFAULT_HOSTNAME = "localhost";

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

  public static String getLanguage(final TrainingParameters params) {
    String lang = null;
    if (params.getSettings().get("Language") == null) {
      langException();
    } else {
      lang = params.getSettings().get("Language");
    }
    return lang;
  }

  public static String getDataSet(final String dataset,
      final TrainingParameters params) {
    String trainSet = null;
    if (params.getSettings().get(dataset) == null) {
      datasetException();
    } else {
      trainSet = params.getSettings().get(dataset);
    }
    return trainSet;
  }

  public static String getModel(final TrainingParameters params) {
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

  public static String getCorpusFormat(final TrainingParameters params) {
    String corpusFormat = null;
    if (params.getSettings().get("CorpusFormat") == null) {
      corpusFormatException();
    } else {
      corpusFormat = params.getSettings().get("CorpusFormat");
    }
    return corpusFormat;
  }

  public static String getOutputFormat(final TrainingParameters params) {
    String outFormatOption = null;
    if (params.getSettings().get("OutputFormat") != null) {
      outFormatOption = params.getSettings().get("OutputFormat");
    } else {
      outFormatOption = Flags.DEFAULT_OUTPUT_FORMAT;
    }
    return outFormatOption;
  }

  public static Integer getBeamsize(final TrainingParameters params) {
    Integer beamsize = null;
    if (params.getSettings().get("BeamSize") == null) {
      beamsize = Flags.DEFAULT_BEAM_SIZE;
    } else {
      beamsize = Integer.parseInt(params.getSettings().get("BeamSize"));
    }
    return beamsize;
  }

  public static Integer getFolds(final TrainingParameters params) {
    Integer beamsize = null;
    if (params.getSettings().get("Folds") == null) {
      beamsize = Flags.DEFAULT_FOLDS_VALUE;
    } else {
      beamsize = Integer.parseInt(params.getSettings().get("Folds"));
    }
    return beamsize;
  }

  public static String getSequenceCodec(final TrainingParameters params) {
    String seqCodec = null;
    if (params.getSettings().get("SequenceCodec") == null) {
      seqCodec = Flags.DEFAULT_SEQUENCE_CODEC;
    } else {
      seqCodec = params.getSettings().get("SequenceCodec");
    }
    return seqCodec;
  }

  public static String getClearTrainingFeatures(
      final TrainingParameters params) {
    String clearFeatures = null;
    if (params.getSettings().get("ClearTrainingFeatures") == null) {
      clearFeatures = Flags.DEFAULT_FEATURE_FLAG;
    } else {
      clearFeatures = params.getSettings().get("ClearTrainingFeatures");
    }
    return clearFeatures;
  }

  public static String getClearEvaluationFeatures(
      final TrainingParameters params) {
    String clearFeatures = null;
    if (params.getSettings().get("ClearEvaluationFeatures") == null) {
      clearFeatures = Flags.DEFAULT_FEATURE_FLAG;
    } else {
      clearFeatures = params.getSettings().get("ClearEvaluationFeatures");
    }
    return clearFeatures;
  }

  public static String getWindow(final TrainingParameters params) {
    String windowFlag = null;
    if (params.getSettings().get("Window") == null) {
      windowFlag = Flags.DEFAULT_WINDOW;
    } else {
      windowFlag = params.getSettings().get("Window");
    }
    return windowFlag;
  }

  public static String getTokenFeatures(final TrainingParameters params) {
    String tokenFlag = null;
    if (params.getSettings().get("TokenFeatures") != null) {
      tokenFlag = params.getSettings().get("TokenFeatures");
    } else {
      tokenFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return tokenFlag;
  }

  public static String getTokenFeaturesRange(final TrainingParameters params) {
    String tokenRangeFlag = null;
    if (params.getSettings().get("TokenFeaturesRange") != null) {
      tokenRangeFlag = params.getSettings().get("TokenFeaturesRange");
    } else {
      tokenRangeFlag = DEFAULT_TOKEN_RANGE;
    }
    return tokenRangeFlag;
  }

  public static String getTokenClassFeatures(final TrainingParameters params) {
    String tokenClassFlag = null;
    if (params.getSettings().get("TokenClassFeatures") != null) {
      tokenClassFlag = params.getSettings().get("TokenClassFeatures");
    } else {
      tokenClassFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return tokenClassFlag;
  }

  public static String getTokenClassFeaturesRange(
      final TrainingParameters params) {
    String tokenRangeFlag = null;
    if (params.getSettings().get("TokenClassFeaturesRange") != null) {
      tokenRangeFlag = params.getSettings().get("TokenClassFeaturesRange");
    } else {
      tokenRangeFlag = DEFAULT_TOKENCLASS_RANGE;
    }
    return tokenRangeFlag;
  }

  public static String[] processTokenClassFeaturesRange(final String mfsFlag) {
    final String[] mfsFlagArray = mfsFlag.split(",");
    if (mfsFlagArray.length != 2) {
      System.err.println("TokenClassFeaturesRange requires two fields but got "
          + mfsFlagArray.length);
      System.exit(1);
    }
    return mfsFlagArray;
  }

  public static String getWordShapeSuperSenseFeatures(
      final TrainingParameters params) {
    String tokenClassFlag = null;
    if (params.getSettings().get("WordShapeSuperSenseFeatures") != null) {
      tokenClassFlag = params.getSettings().get("WordShapeSuperSenseFeatures");
    } else {
      tokenClassFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return tokenClassFlag;
  }

  public static String getOutcomePriorFeatures(
      final TrainingParameters params) {
    String outcomePriorFlag = null;
    if (params.getSettings().get("OutcomePriorFeatures") != null) {
      outcomePriorFlag = params.getSettings().get("OutcomePriorFeatures");
    } else {
      outcomePriorFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return outcomePriorFlag;
  }

  public static String getPreviousMapFeatures(final TrainingParameters params) {
    String previousMapFlag = null;
    if (params.getSettings().get("PreviousMapFeatures") != null) {
      previousMapFlag = params.getSettings().get("PreviousMapFeatures");
    } else {
      previousMapFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return previousMapFlag;
  }

  public static String getSentenceFeatures(final TrainingParameters params) {
    String sentenceFlag = null;
    if (params.getSettings().get("SentenceFeatures") != null) {
      sentenceFlag = params.getSettings().get("SentenceFeatures");
    } else {
      sentenceFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return sentenceFlag;
  }

  public static String getSentenceFeaturesBegin(
      final TrainingParameters params) {
    String beginFlag = null;
    if (params.getSettings().get("SentenceFeaturesBegin") != null) {
      beginFlag = params.getSettings().get("SentenceFeaturesBegin");
    } else {
      beginFlag = Flags.DEFAULT_SENTENCE_BEGIN;
    }
    return beginFlag;
  }

  public static String getSentenceFeaturesEnd(final TrainingParameters params) {
    String endFlag = null;
    if (params.getSettings().get("SentenceFeaturesEnd") != null) {
      endFlag = params.getSettings().get("SentenceFeaturesEnd");
    } else {
      endFlag = Flags.DEFAULT_SENTENCE_END;
    }
    return endFlag;
  }

  public static String getPreffixFeatures(final TrainingParameters params) {
    String prefixFlag = null;
    if (params.getSettings().get("PrefixFeatures") != null) {
      prefixFlag = params.getSettings().get("PrefixFeatures");
    } else {
      prefixFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return prefixFlag;
  }

  public static String getPrefixFeaturesBegin(final TrainingParameters params) {
    String beginFlag = null;
    if (params.getSettings().get("PrefixFeaturesBegin") != null) {
      beginFlag = params.getSettings().get("PrefixFeaturesBegin");
    } else {
      beginFlag = Flags.DEFAULT_PREFIX_BEGIN;
    }
    return beginFlag;
  }

  public static String getPrefixFeaturesEnd(final TrainingParameters params) {
    String endFlag = null;
    if (params.getSettings().get("PrefixFeaturesEnd") != null) {
      endFlag = params.getSettings().get("PrefixFeaturesEnd");
    } else {
      endFlag = Flags.DEFAULT_PREFIX_END;
    }
    return endFlag;
  }

  public static String getSuffixFeatures(final TrainingParameters params) {
    String suffixFlag = null;
    if (params.getSettings().get("SuffixFeatures") != null) {
      suffixFlag = params.getSettings().get("SuffixFeatures");
    } else {
      suffixFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return suffixFlag;
  }

  public static String getSuffixFeaturesBegin(final TrainingParameters params) {
    String beginFlag = null;
    if (params.getSettings().get("SuffixFeaturesBegin") != null) {
      beginFlag = params.getSettings().get("SuffixFeaturesBegin");
    } else {
      beginFlag = Flags.DEFAULT_SUFFIX_BEGIN;
    }
    return beginFlag;
  }

  public static String getSuffixFeaturesEnd(final TrainingParameters params) {
    String endFlag = null;
    if (params.getSettings().get("SuffixFeaturesEnd") != null) {
      endFlag = params.getSettings().get("SuffixFeaturesEnd");
    } else {
      endFlag = Flags.DEFAULT_SUFFIX_END;
    }
    return endFlag;
  }

  public static String getBigramClassFeatures(final TrainingParameters params) {
    String bigramClassFlag = null;
    if (params.getSettings().get("BigramClassFeatures") != null) {
      bigramClassFlag = params.getSettings().get("BigramClassFeatures");
    } else {
      bigramClassFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return bigramClassFlag;
  }

  public static String getTrigramClassFeatures(
      final TrainingParameters params) {
    String trigramClassFlag = null;
    if (params.getSettings().get("TrigramClassFeatures") != null) {
      trigramClassFlag = params.getSettings().get("TrigramClassFeatures");
    } else {
      trigramClassFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return trigramClassFlag;
  }

  public static String getFourgramClassFeatures(
      final TrainingParameters params) {
    String fourgramClassFlag = null;
    if (params.getSettings().get("FourgramClassFeatures") != null) {
      fourgramClassFlag = params.getSettings().get("FourgramClassFeatures");
    } else {
      fourgramClassFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return fourgramClassFlag;
  }

  public static String getFivegramClassFeatures(
      final TrainingParameters params) {
    String fivegramClassFlag = null;
    if (params.getSettings().get("FivegramClassFeatures") != null) {
      fivegramClassFlag = params.getSettings().get("FivegramClassFeatures");
    } else {
      fivegramClassFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return fivegramClassFlag;
  }

  public static String getCharNgramFeatures(final TrainingParameters params) {
    String charNgramFlag = null;
    if (params.getSettings().get("CharNgramFeatures") != null) {
      charNgramFlag = params.getSettings().get("CharNgramFeatures");
    } else {
      charNgramFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return charNgramFlag;
  }

  public static String getCharNgramFeaturesRange(
      final TrainingParameters params) {
    String charNgramRangeFlag = null;
    if (params.getSettings().get("CharNgramFeaturesRange") != null) {
      charNgramRangeFlag = params.getSettings().get("CharNgramFeaturesRange");
    } else {
      charNgramRangeFlag = Flags.CHAR_NGRAM_RANGE;
    }
    return charNgramRangeFlag;
  }

  public static String[] processNgramRange(final String charngramRangeFlag) {
    final String[] charngramArray = charngramRangeFlag.split("[ :-]");
    if (charngramArray.length != 2) {
      System.err.println("CharNgramFeaturesRange requires two fieds but got "
          + charngramArray.length);
      System.exit(1);
    }
    return charngramArray;
  }

  public static String getDictionaryFeatures(final TrainingParameters params) {
    String dictionaryFlag = null;
    if (params.getSettings().get("DictionaryFeatures") != null) {
      dictionaryFlag = params.getSettings().get("DictionaryFeatures");
    } else {
      dictionaryFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return dictionaryFlag;
  }

  public static String getClarkFeatures(final TrainingParameters params) {
    String distSimFlag = null;
    if (params.getSettings().get("ClarkClusterFeatures") != null) {
      distSimFlag = params.getSettings().get("ClarkClusterFeatures");
    } else {
      distSimFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return distSimFlag;
  }

  public static String getWord2VecClusterFeatures(
      final TrainingParameters params) {
    String word2vecFlag = null;
    if (params.getSettings().get("Word2VecClusterFeatures") != null) {
      word2vecFlag = params.getSettings().get("Word2VecClusterFeatures");
    } else {
      word2vecFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return word2vecFlag;
  }

  public static String getBrownFeatures(final TrainingParameters params) {
    String brownFlag = null;
    if (params.getSettings().get("BrownClusterFeatures") != null) {
      brownFlag = params.getSettings().get("BrownClusterFeatures");
    } else {
      brownFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return brownFlag;
  }

  public static String getPOSDictionaryFeatures(
      final TrainingParameters params) {
    String dictionaryFlag = null;
    if (params.getSettings().get("POSDictionaryFeatures") != null) {
      dictionaryFlag = params.getSettings().get("POSDictionaryFeatures");
    } else {
      dictionaryFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return dictionaryFlag;
  }

  public static String getPOSTagModelFeatures(final TrainingParameters params) {
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
  public static String getPOSTagModelFeaturesRange(
      final TrainingParameters params) {
    String lemmaRangeFlag = null;
    if (params.getSettings().get("POSTagModelFeaturesRange") != null) {
      lemmaRangeFlag = params.getSettings().get("POSTagModelFeaturesRange");
    } else {
      lemmaRangeFlag = Flags.DEFAULT_POSTAG_RANGE;
    }
    return lemmaRangeFlag;
  }

  public static String[] processPOSTagModelFeaturesRange(final String mfsFlag) {
    final String[] mfsFlagArray = mfsFlag.split(",");
    if (mfsFlagArray.length != 2) {
      System.err.println("POSTagFeaturesRange requires two fields but got "
          + mfsFlagArray.length);
      System.exit(1);
    }
    return mfsFlagArray;
  }

  public static String getLemmaModelFeatures(final TrainingParameters params) {
    String morphoFlag = null;
    if (params.getSettings().get("LemmaModelFeatures") != null) {
      morphoFlag = params.getSettings().get("LemmaModelFeatures");
    } else {
      morphoFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return morphoFlag;
  }

  public static String getLemmaDictionaryFeatures(
      final TrainingParameters params) {
    String morphoFlag = null;
    if (params.getSettings().get("LemmaDictionaryFeatures") != null) {
      morphoFlag = params.getSettings().get("LemmaDictionaryFeatures");
    } else {
      morphoFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return morphoFlag;
  }

  public static String[] getLemmaDictionaryResources(final String mfsFlag) {
    final String[] mfsFlagArray = mfsFlag.split(",");
    if (mfsFlagArray.length != 2) {
      System.err
          .println("LemmaDictionary resources requires two fields but got "
              + mfsFlagArray.length);
      System.exit(1);
    }
    return mfsFlagArray;
  }

  public static String getSuperSenseFeatures(final TrainingParameters params) {
    String mfsFlag = null;
    if (params.getSettings().get("SuperSenseFeatures") != null) {
      mfsFlag = params.getSettings().get("SuperSenseFeatures");
    } else {
      mfsFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return mfsFlag;
  }

  public static String getPOSBaselineFeatures(final TrainingParameters params) {
    String posFlag = null;
    if (params.getSettings().get("POSBaselineFeatures") != null) {
      posFlag = params.getSettings().get("POSBaselineFeatures");
    } else {
      posFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return posFlag;
  }

  public static String getPrefixBegin(final TrainingParameters params) {
    String beginFlag = null;
    if (params.getSettings().get("PrefixBegin") != null) {
      beginFlag = params.getSettings().get("PrefixBegin");
    } else {
      beginFlag = Flags.DEFAULT_PREFIX_BEGIN;
    }
    return beginFlag;
  }

  public static String getPrefixEnd(final TrainingParameters params) {
    String endFlag = null;
    if (params.getSettings().get("PrefixEnd") != null) {
      endFlag = params.getSettings().get("PrefixEnd");
    } else {
      endFlag = Flags.DEFAULT_PREFIX_END;
    }
    return endFlag;
  }

  public static String getSuffixBegin(final TrainingParameters params) {
    String beginFlag = null;
    if (params.getSettings().get("SuffixBegin") != null) {
      beginFlag = params.getSettings().get("SuffixBegin");
    } else {
      beginFlag = Flags.DEFAULT_SUFFIX_BEGIN;
    }
    return beginFlag;
  }

  public static String getSuffixEnd(final TrainingParameters params) {
    String endFlag = null;
    if (params.getSettings().get("SuffixEnd") != null) {
      endFlag = params.getSettings().get("SuffixEnd");
    } else {
      endFlag = Flags.DEFAULT_SUFFIX_END;
    }
    return endFlag;
  }

  public static String getMFSFeatures(final TrainingParameters params) {
    String mfsFlag = null;
    if (params.getSettings().get("MFSFeatures") != null) {
      mfsFlag = params.getSettings().get("MFSFeatures");
    } else {
      mfsFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return mfsFlag;
  }

  public static String getLemmaBaselineFeatures(
      final TrainingParameters params) {
    String lemmaFlag = null;
    if (params.getSettings().get("LemmaBaselineFeatures") != null) {
      lemmaFlag = params.getSettings().get("LemmaBaselineFeatures");
    } else {
      lemmaFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return lemmaFlag;
  }

  public static String getLemmaBaselineFeaturesRange(
      final TrainingParameters params) {
    String mfsRangeFlag = null;
    if (params.getSettings().get("LemmaBaselineFeaturesRange") != null) {
      mfsRangeFlag = params.getSettings().get("LemmaBaselineFeaturesRange");
    } else {
      mfsRangeFlag = Flags.DEFAULT_POSTAG_RANGE;
    }
    return mfsRangeFlag;
  }

  public static String[] processLemmaBaselineFeaturesRange(
      final String mfsFlag) {
    final String[] mfsFlagArray = mfsFlag.split(",");
    if (mfsFlagArray.length != 2) {
      System.err
          .println("LemmaBaselineFeaturesRange requires two fields but got "
              + mfsFlagArray.length);
      System.exit(1);
    }
    return mfsFlagArray;
  }

  public static String getPredicateContextFeatures(
      final TrainingParameters params) {
    String lemmaFlag = null;
    if (params.getSettings().get("PredicateContextFeatures") != null) {
      lemmaFlag = params.getSettings().get("PredicateContextFeatures");
    } else {
      lemmaFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return lemmaFlag;
  }

  public static String getChunkBaselineFeatures(
      final TrainingParameters params) {
    String lemmaFlag = null;
    if (params.getSettings().get("ChunkBaselineFeatures") != null) {
      lemmaFlag = params.getSettings().get("ChunkBaselineFeatures");
    } else {
      lemmaFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return lemmaFlag;
  }

  public static String[] getSuperSenseResources(final String mfsFlag) {
    final String[] mfsFlagArray = mfsFlag.split(",");
    if (mfsFlagArray.length != 3) {
      System.err.println("SuperSense resources requires three fields but got "
          + mfsFlagArray.length);
      System.exit(1);
    }
    return mfsFlagArray;
  }

  public static String[] getMFSResources(final String mfsFlag) {
    final String[] mfsFlagArray = mfsFlag.split(",");
    if (mfsFlagArray.length != 3) {
      System.err.println(
          "MFS resources requires three fields but got " + mfsFlagArray.length);
      System.exit(1);
    }
    return mfsFlagArray;
  }

  public static String getSuperSenseFeaturesRange(
      final TrainingParameters params) {
    String mfsRangeFlag = null;
    if (params.getSettings().get("SuperSenseFeaturesRange") != null) {
      mfsRangeFlag = params.getSettings().get("SuperSenseFeaturesRange");
    } else {
      mfsRangeFlag = Flags.DEFAULT_SUPERSENSE_RANGE;
    }
    return mfsRangeFlag;
  }

  public static String getMFSFeaturesRange(final TrainingParameters params) {
    String mfsRangeFlag = null;
    if (params.getSettings().get("MFSFeaturesRange") != null) {
      mfsRangeFlag = params.getSettings().get("MFSFeaturesRange");
    } else {
      mfsRangeFlag = Flags.DEFAULT_SUPERSENSE_RANGE;
    }
    return mfsRangeFlag;
  }

  public static String[] processSuperSenseFeaturesRange(final String mfsFlag) {
    final String[] mfsFlagArray = mfsFlag.split(",");
    if (mfsFlagArray.length != 2) {
      System.err.println("SuperSenseFeaturesRange requires two fields but got "
          + mfsFlagArray.length);
      System.exit(1);
    }
    return mfsFlagArray;
  }

  public static String[] processMFSFeaturesRange(final String mfsFlag) {
    final String[] mfsFlagArray = mfsFlag.split(",");
    if (mfsFlagArray.length != 5) {
      System.err.println("MFSFeaturesRange requires five fields but got "
          + mfsFlagArray.length);
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
  public static List<File> getClusterLexiconFiles(final String clusterPath) {
    final List<File> clusterLexicons = new ArrayList<File>();
    final String[] clusterPaths = clusterPath.split(",");
    for (final String clusterName : clusterPaths) {
      clusterLexicons.add(new File(clusterName));
    }
    return clusterLexicons;

  }

  public static String getHeadRulesFile(final TrainingParameters params) {
    String headRulesFlag = null;
    if (params.getSettings().get("HeadRules") != null) {
      headRulesFlag = params.getSettings().get("HeadRules");
    } else {
      System.err.println("Specify head rules file to train the parser!!");
    }
    return headRulesFlag;
  }
  
  public static String getBagOfWordsFeatures(final TrainingParameters params) {
    String tokenFlag = null;
    if (params.getSettings().get("BagOfWordsFeatures") != null) {
      tokenFlag = params.getSettings().get("BagOfWordsFeatures");
    } else {
      tokenFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return tokenFlag;
  }
  

  public static String getBagOfWordsFeaturesRange(final TrainingParameters params) {
    String tokenRangeFlag = null;
    if (params.getSettings().get("BagOfWordsFeaturesRange") != null) {
      tokenRangeFlag = params.getSettings().get("BagOfWordsFeaturesRange");
    } else {
      tokenRangeFlag = DEFAULT_BOW_RANGE;
    }
    return tokenRangeFlag;
  }
  
  public static String getNgramFeatures(final TrainingParameters params) {
    String charNgramFlag = null;
    if (params.getSettings().get("NGramFeatures") != null) {
      charNgramFlag = params.getSettings().get("NGramFeatures");
    } else {
      charNgramFlag = Flags.DEFAULT_FEATURE_FLAG;
    }
    return charNgramFlag;
  }

  public static String getNgramFeaturesRange(
      final TrainingParameters params) {
    String charNgramRangeFlag = null;
    if (params.getSettings().get("NGramFeaturesRange") != null) {
      charNgramRangeFlag = params.getSettings().get("NGramFeaturesRange");
    } else {
      charNgramRangeFlag = Flags.CHAR_NGRAM_RANGE;
    }
    return charNgramRangeFlag;
  }

  public static boolean isPredicateContextFeatures(
      final TrainingParameters params) {
    final String posFeatures = getPredicateContextFeatures(params);
    return !posFeatures.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isChunkBaselineFeatures(
      final TrainingParameters params) {
    final String posFeatures = getChunkBaselineFeatures(params);
    return !posFeatures.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isLemmaBaselineFeatures(
      final TrainingParameters params) {
    final String posFeatures = getLemmaBaselineFeatures(params);
    return !posFeatures.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  /**
   * Check if POS Baseline features are active.
   * 
   * @param params
   *          the parameters
   * @return whether the pos baseline features are activated or not
   */
  public static boolean isPOSBaselineFeatures(final TrainingParameters params) {
    final String posFeatures = getPOSBaselineFeatures(params);
    return !posFeatures.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  /**
   * Check if supersense tagger features are active.
   * 
   * @param params
   *          the parameters
   * @return whether the supersense features are activated or not
   */
  public static boolean isSuperSenseFeatures(final TrainingParameters params) {
    final String mfsFeatures = getSuperSenseFeatures(params);
    return !mfsFeatures.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  /**
   * Check if mfs features are active.
   * 
   * @param params
   *          the parameters
   * @return whether the mfs features are activated or not
   */
  public static boolean isMFSFeatures(final TrainingParameters params) {
    final String mfsFeatures = getMFSFeatures(params);
    return !mfsFeatures.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isLemmaDictionaryFeatures(
      final TrainingParameters params) {
    final String morphoFeatures = getLemmaDictionaryFeatures(params);
    return !morphoFeatures.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isLemmaModelFeatures(final TrainingParameters params) {
    final String morphoFeatures = getLemmaModelFeatures(params);
    return !morphoFeatures.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isPOSDictionaryFeatures(
      final TrainingParameters params) {
    final String dictFeatures = getPOSDictionaryFeatures(params);
    return !dictFeatures.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isPOSTagModelFeatures(final TrainingParameters params) {
    final String morphoFeatures = getPOSTagModelFeatures(params);
    return !morphoFeatures.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isWord2VecClusterFeatures(
      final TrainingParameters params) {
    final String word2vecClusterFeatures = getWord2VecClusterFeatures(params);
    return !word2vecClusterFeatures
        .equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isClarkFeatures(final TrainingParameters params) {
    final String clarkFeatures = getClarkFeatures(params);
    return !clarkFeatures.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isBrownFeatures(final TrainingParameters params) {
    final String brownFeatures = getBrownFeatures(params);
    return !brownFeatures.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isDictionaryFeatures(final TrainingParameters params) {
    final String dictFeatures = getDictionaryFeatures(params);
    return !dictFeatures.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isCharNgramClassFeature(
      final TrainingParameters params) {
    final String charngramParam = getCharNgramFeatures(params);
    return !charngramParam.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isFivegramClassFeature(
      final TrainingParameters params) {
    final String fivegramParam = getFivegramClassFeatures(params);
    return !fivegramParam.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isFourgramClassFeature(
      final TrainingParameters params) {
    final String fourgramParam = getFourgramClassFeatures(params);
    return !fourgramParam.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isTrigramClassFeature(final TrainingParameters params) {
    final String trigramParam = getTrigramClassFeatures(params);
    return !trigramParam.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isBigramClassFeature(final TrainingParameters params) {
    final String bigramParam = getBigramClassFeatures(params);
    return !bigramParam.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isSuffixFeature(final TrainingParameters params) {
    final String suffixParam = getSuffixFeatures(params);
    return !suffixParam.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isPrefixFeature(final TrainingParameters params) {
    final String prefixParam = getPreffixFeatures(params);
    return !prefixParam.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isSentenceFeature(final TrainingParameters params) {
    final String sentenceParam = getSentenceFeatures(params);
    return !sentenceParam.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isPreviousMapFeature(final TrainingParameters params) {
    final String previousMapParam = getPreviousMapFeatures(params);
    return !previousMapParam.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isOutcomePriorFeature(final TrainingParameters params) {
    final String outcomePriorParam = getOutcomePriorFeatures(params);
    return !outcomePriorParam.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isWordShapeSuperSenseFeature(
      final TrainingParameters params) {
    final String tokenParam = getWordShapeSuperSenseFeatures(params);
    return !tokenParam.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isTokenClassFeature(final TrainingParameters params) {
    final String tokenParam = getTokenClassFeatures(params);
    return !tokenParam.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static boolean isTokenFeature(final TrainingParameters params) {
    final String tokenParam = getTokenFeatures(params);
    return !tokenParam.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }
  
  public static boolean isNgramFeature(
      final TrainingParameters params) {
    final String charngramParam = getNgramFeatures(params);
    return !charngramParam.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }
  
  public static boolean isBagOfWordsFeature(final TrainingParameters params) {
    final String tokenParam = getBagOfWordsFeatures(params);
    return !tokenParam.equalsIgnoreCase(Flags.DEFAULT_FEATURE_FLAG);
  }

  public static void componentException() {
    System.err.println(
        "Please provide a component name in the Component field in the parameters file!");
    System.exit(1);
  }

  public static void devSetException() {
    System.err.println(
        "UseDevSet options in the parameters file if CrossEval is activated!");
    System.exit(1);
  }

  public static void modelException() {
    System.err.println(
        "Please provide a model in the OutputModel field in the parameters file!");
    System.exit(1);
  }

  public static void langException() {
    System.err
        .println("Please fill in the Language field in the parameters file!");
    System.exit(1);
  }

  public static void datasetException() {
    System.err.println(
        "Please specify your training/testing sets in the TrainSet and TestSet fields in the parameters file!");
    System.exit(1);
  }

  public static void corpusFormatException() {
    System.err
        .println("Please fill in CorpusFormat field in the parameters file!");
    System.exit(1);
  }

  public static void dictionaryException() {
    System.err.println(
        "You need to set the --dictPath option to the dictionaries directory to use the dictTag option!");
    System.exit(1);
  }

  public static void dictionaryFeaturesException() {
    System.err.println(
        "You need to specify the DictionaryFeatures in the parameters file to use the DictionaryPath!");
    System.exit(1);
  }

}
