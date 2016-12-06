/*
 * Copyright 2016 Rodrigo Agerri

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
package eus.ixa.ixa.pipe.ml.features;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import eus.ixa.ixa.pipe.ml.utils.Flags;
import eus.ixa.ixa.pipe.ml.utils.IOUtils;
import eus.ixa.ixa.pipe.ml.utils.StringUtils;
import opennlp.tools.util.TrainingParameters;

/**
 * Class to automatically generate the feature descriptor from a
 * trainParams.properties file.
 * 
 * @author ragerri
 * @version 2016-04-08
 */
public final class XMLFeatureDescriptor {

  /**
   * The leftWindow length.
   */
  private static int leftWindow = -1;
  /**
   * The rightWindow length.
   */
  private static int rightWindow = -1;

  /**
   * This class is not to be instantiated.
   */
  private XMLFeatureDescriptor() {
  }

  /**
   * Get the left window feature length.
   * 
   * @return the leftWindow length
   */
  public static int getLeftWindow() {
    return leftWindow;
  }

  /**
   * Get the right window feature length.
   * 
   * @return the rightWindow length
   */
  public static int getRightWindow() {
    return rightWindow;
  }

  /**
   * Generate the XML feature descriptor from the TrainingParameters prop file.
   * 
   * @param params
   *          the properties file
   * @return the XML feature descriptor
   * @throws IOException
   *           if input output fails
   */
  public static String createXMLFeatureDescriptor(
      final TrainingParameters params) throws IOException {

    final Element aggGenerators = new Element("generators");
    final Document doc = new Document(aggGenerators);

    // <generators>
    // <cache>
    // <generators>
    final Element cached = new Element("cache");
    final Element generators = new Element("generators");
    // <window prevLength="2" nextLength="2">
    // <token />
    // </window>
    if (Flags.isTokenFeature(params)) {
      setWindow(params);
      final String tokenFeatureRange = Flags.getTokenFeaturesRange(params);
      final Element tokenFeature = new Element("custom");
      tokenFeature.setAttribute("class", TokenFeatureGenerator.class.getName());
      tokenFeature.setAttribute("range", tokenFeatureRange);
      final Element tokenWindow = new Element("window");
      tokenWindow.setAttribute("prevLength", Integer.toString(leftWindow));
      tokenWindow.setAttribute("nextLength", Integer.toString(rightWindow));
      tokenWindow.addContent(tokenFeature);
      generators.addContent(tokenWindow);
      System.err.println("-> Token features added!: Window range " + leftWindow
          + ":" + rightWindow);
    }
    if (Flags.isTokenClassFeature(params)) {
      setWindow(params);
      final String tokenClassFeatureRange = Flags
          .getTokenClassFeaturesRange(params);
      final Element tokenClassFeature = new Element("custom");
      tokenClassFeature.setAttribute("class",
          TokenClassFeatureGenerator.class.getName());
      tokenClassFeature.setAttribute("range", tokenClassFeatureRange);
      final Element tokenClassWindow = new Element("window");
      tokenClassWindow.setAttribute("prevLength", Integer.toString(leftWindow));
      tokenClassWindow.setAttribute("nextLength",
          Integer.toString(rightWindow));
      tokenClassWindow.addContent(tokenClassFeature);
      generators.addContent(tokenClassWindow);
      System.err.println("-> Token Class Features added!: Window range "
          + leftWindow + ":" + rightWindow);
    }
    if (Flags.isWordShapeSuperSenseFeature(params)) {
      setWindow(params);
      final Element wordShapeSuperSenseFeature = new Element("custom");
      wordShapeSuperSenseFeature.setAttribute("class",
          WordShapeSuperSenseFeatureGenerator.class.getName());
      final Element wordShapeWindow = new Element("window");
      wordShapeWindow.setAttribute("prevLength", Integer.toString(leftWindow));
      wordShapeWindow.setAttribute("nextLength", Integer.toString(rightWindow));
      wordShapeWindow.addContent(wordShapeSuperSenseFeature);
      generators.addContent(wordShapeWindow);
      System.err
          .println("-> Word Shape SuperSense Features added!: Window range "
              + leftWindow + ":" + rightWindow);
    }
    if (Flags.isOutcomePriorFeature(params)) {
      final Element outcomePriorFeature = new Element("custom");
      outcomePriorFeature.setAttribute("class",
          OutcomePriorFeatureGenerator.class.getName());
      generators.addContent(outcomePriorFeature);
      System.err.println("-> Outcome Prior Features added!");
    }
    if (Flags.isPreviousMapFeature(params)) {
      final Element previousMapFeature = new Element("custom");
      previousMapFeature.setAttribute("class",
          PreviousMapFeatureGenerator.class.getName());
      generators.addContent(previousMapFeature);
      System.err.println("-> Previous Map Features added!");
    }
    if (Flags.isSentenceFeature(params)) {
      final String beginSentence = Flags.getSentenceFeaturesBegin(params);
      final String endSentence = Flags.getSentenceFeaturesEnd(params);
      final Element sentenceFeature = new Element("custom");
      sentenceFeature.setAttribute("class",
          SentenceFeatureGenerator.class.getName());
      sentenceFeature.setAttribute("begin", beginSentence);
      sentenceFeature.setAttribute("end", endSentence);
      generators.addContent(sentenceFeature);
      System.err.println("-> Sentence Features added!");
    }
    if (Flags.isPrefixFeature(params)) {
      final String beginPrefix = Flags.getPrefixFeaturesBegin(params);
      final String endPrefix = Flags.getPrefixFeaturesEnd(params);
      final Element prefixFeature = new Element("custom");
      prefixFeature.setAttribute("class",
          PrefixFeatureGenerator.class.getName());
      prefixFeature.setAttribute("begin", beginPrefix);
      prefixFeature.setAttribute("end", endPrefix);
      generators.addContent(prefixFeature);
      System.err.println("-> Prefix Features added!");
    }
    if (Flags.isSuffixFeature(params)) {
      final String beginSuffix = Flags.getSuffixFeaturesBegin(params);
      final String endSuffix = Flags.getSuffixFeaturesEnd(params);
      final Element suffixFeature = new Element("custom");
      suffixFeature.setAttribute("class",
          SuffixFeatureGenerator.class.getName());
      suffixFeature.setAttribute("begin", beginSuffix);
      suffixFeature.setAttribute("end", endSuffix);
      generators.addContent(suffixFeature);
      System.err.println("-> Suffix Features added!");
    }
    if (Flags.isBigramClassFeature(params)) {
      final Element bigramFeature = new Element("custom");
      bigramFeature.setAttribute("class",
          BigramClassFeatureGenerator.class.getName());
      generators.addContent(bigramFeature);
      System.err.println("-> Bigram Class Features added!");
    }
    if (Flags.isTrigramClassFeature(params)) {
      final Element trigramFeature = new Element("custom");
      trigramFeature.setAttribute("class",
          TrigramClassFeatureGenerator.class.getName());
      generators.addContent(trigramFeature);
      System.err.println("-> Trigram Class Features added!");
    }
    if (Flags.isFourgramClassFeature(params)) {
      final Element fourgramFeature = new Element("custom");
      fourgramFeature.setAttribute("class",
          FourgramClassFeatureGenerator.class.getName());
      generators.addContent(fourgramFeature);
      System.err.println("-> Fourgram Class Features added!");
    }
    if (Flags.isFivegramClassFeature(params)) {
      final Element fivegramFeature = new Element("custom");
      fivegramFeature.setAttribute("class",
          FivegramClassFeatureGenerator.class.getName());
      generators.addContent(fivegramFeature);
      System.err.println("-> Fivegram Class Features added!");
    }
    if (Flags.isCharNgramClassFeature(params)) {
      final String charngramRange = Flags.getCharNgramFeaturesRange(params);
      final String[] rangeArray = Flags.processNgramRange(charngramRange);
      final Element charngramFeature = new Element("custom");
      charngramFeature.setAttribute("class",
          CharacterNgramFeatureGenerator.class.getName());
      charngramFeature.setAttribute("minLength", rangeArray[0]);
      charngramFeature.setAttribute("maxLength", rangeArray[1]);
      generators.addContent(charngramFeature);
      System.err.println("-> CharNgram Class Features added!");
    }
    // Dictionary Features
    if (Flags.isDictionaryFeatures(params)) {
      setWindow(params);
      final String dictPath = Flags.getDictionaryFeatures(params);
      final String seqCodec = Flags.getSequenceCodec(params);
      final List<File> fileList = StringUtils.getFilesInDir(new File(dictPath));
      for (final File dictFile : fileList) {
        final Element dictFeatures = new Element("custom");
        dictFeatures.setAttribute("class",
            DictionaryFeatureGenerator.class.getName());
        dictFeatures.setAttribute("dict",
            IOUtils.normalizeLexiconName(dictFile.getName()));
        dictFeatures.setAttribute("seqCodec", seqCodec);
        final Element dictWindow = new Element("window");
        dictWindow.setAttribute("prevLength", Integer.toString(leftWindow));
        dictWindow.setAttribute("nextLength", Integer.toString(rightWindow));
        dictWindow.addContent(dictFeatures);
        generators.addContent(dictWindow);
      }
      System.err.println("-> Dictionary Features added!");
    }
    // Brown clustering features
    if (Flags.isBrownFeatures(params)) {
      setWindow(params);
      // previous 2 maps features
      final Element prev2MapFeature = new Element("custom");
      prev2MapFeature.setAttribute("class",
          Prev2MapFeatureGenerator.class.getName());
      // generators.addContent(prev2MapFeature);
      // previous map and token feature (in window)
      final Element prevMapTokenFeature = new Element("custom");
      prevMapTokenFeature.setAttribute("class",
          PreviousMapTokenFeatureGenerator.class.getName());
      final Element prevMapTokenWindow = new Element("window");
      prevMapTokenWindow.setAttribute("prevLength",
          Integer.toString(leftWindow));
      prevMapTokenWindow.setAttribute("nextLength",
          Integer.toString(rightWindow));
      prevMapTokenWindow.addContent(prevMapTokenFeature);
      // generators.addContent(prevMapTokenWindow);
      // brown clustering features
      final String brownClusterPath = Flags.getBrownFeatures(params);
      final List<File> brownClusterFiles = Flags
          .getClusterLexiconFiles(brownClusterPath);
      for (final File brownClusterFile : brownClusterFiles) {
        // brown bigram class features
        final Element brownBigramFeatures = new Element("custom");
        brownBigramFeatures.setAttribute("class",
            BrownBigramFeatureGenerator.class.getName());
        brownBigramFeatures.setAttribute("dict",
            IOUtils.normalizeLexiconName(brownClusterFile.getName()));
        generators.addContent(brownBigramFeatures);
        // brown token feature
        final Element brownTokenFeature = new Element("custom");
        brownTokenFeature.setAttribute("class",
            BrownTokenFeatureGenerator.class.getName());
        brownTokenFeature.setAttribute("dict",
            IOUtils.normalizeLexiconName(brownClusterFile.getName()));
        final Element brownTokenWindow = new Element("window");
        brownTokenWindow.setAttribute("prevLength",
            Integer.toString(leftWindow));
        brownTokenWindow.setAttribute("nextLength",
            Integer.toString(rightWindow));
        brownTokenWindow.addContent(brownTokenFeature);
        generators.addContent(brownTokenWindow);
        // brown token class feature
        final Element brownTokenClassFeature = new Element("custom");
        brownTokenClassFeature.setAttribute("class",
            BrownTokenClassFeatureGenerator.class.getName());
        brownTokenClassFeature.setAttribute("dict",
            IOUtils.normalizeLexiconName(brownClusterFile.getName()));
        final Element brownTokenClassWindow = new Element("window");
        brownTokenClassWindow.setAttribute("prevLength",
            Integer.toString(leftWindow));
        brownTokenClassWindow.setAttribute("nextLength",
            Integer.toString(rightWindow));
        brownTokenClassWindow.addContent(brownTokenClassFeature);
        generators.addContent(brownTokenClassWindow);
      }
      System.err.println("-> Brown Cluster Features added!");
    }
    // Clark clustering features
    if (Flags.isClarkFeatures(params)) {
      setWindow(params);
      final String clarkClusterPath = Flags.getClarkFeatures(params);
      final List<File> clarkClusterFiles = Flags
          .getClusterLexiconFiles(clarkClusterPath);
      for (final File clarkCluster : clarkClusterFiles) {
        final Element clarkFeatures = new Element("custom");
        clarkFeatures.setAttribute("class",
            ClarkFeatureGenerator.class.getName());
        clarkFeatures.setAttribute("dict",
            IOUtils.normalizeLexiconName(clarkCluster.getName()));
        final Element clarkWindow = new Element("window");
        clarkWindow.setAttribute("prevLength", Integer.toString(leftWindow));
        clarkWindow.setAttribute("nextLength", Integer.toString(rightWindow));
        clarkWindow.addContent(clarkFeatures);
        generators.addContent(clarkWindow);
      }
      System.err.println("-> Clark Cluster Features added!");
    }
    // word2vec clustering features
    if (Flags.isWord2VecClusterFeatures(params)) {
      setWindow(params);
      final String word2vecClusterPath = Flags
          .getWord2VecClusterFeatures(params);
      final List<File> word2vecClusterFiles = Flags
          .getClusterLexiconFiles(word2vecClusterPath);
      for (final File word2vecFile : word2vecClusterFiles) {
        final Element word2vecClusterFeatures = new Element("custom");
        word2vecClusterFeatures.setAttribute("class",
            Word2VecClusterFeatureGenerator.class.getName());
        word2vecClusterFeatures.setAttribute("dict",
            IOUtils.normalizeLexiconName(word2vecFile.getName()));
        final Element word2vecClusterWindow = new Element("window");
        word2vecClusterWindow.setAttribute("prevLength",
            Integer.toString(leftWindow));
        word2vecClusterWindow.setAttribute("nextLength",
            Integer.toString(rightWindow));
        word2vecClusterWindow.addContent(word2vecClusterFeatures);
        generators.addContent(word2vecClusterWindow);
      }
      System.err.println("-> Word2Vec Clusters Features added!");
    }
    // Morphological features
    if (Flags.isPOSTagModelFeatures(params)) {
      setWindow(params);
      final String posModelPath = Flags.getPOSTagModelFeatures(params);
      final String posModelRange = Flags.getPOSTagModelFeaturesRange(params);
      final Element posTagClassFeatureElement = new Element("custom");
      posTagClassFeatureElement.setAttribute("class",
          POSTagModelFeatureGenerator.class.getName());
      posTagClassFeatureElement.setAttribute("model",
          IOUtils.normalizeLexiconName(new File(posModelPath).getName()));
      posTagClassFeatureElement.setAttribute("range", posModelRange);
      final Element posTagClassFeatureWindow = new Element("window");
      posTagClassFeatureWindow.setAttribute("prevLength",
          Integer.toString(leftWindow));
      posTagClassFeatureWindow.setAttribute("nextLength",
          Integer.toString(rightWindow));
      posTagClassFeatureWindow.addContent(posTagClassFeatureElement);
      generators.addContent(posTagClassFeatureWindow);
      System.err.println("-> POSTagModel Features added!");
    }
    if (Flags.isPOSDictionaryFeatures(params)) {
      setWindow(params);
      final String posDictPath = Flags.getPOSDictionaryFeatures(params);
      final Element posDictFeatures = new Element("custom");
      posDictFeatures.setAttribute("class",
          POSDictionaryFeatureGenerator.class.getName());
      posDictFeatures.setAttribute("dict",
          IOUtils.normalizeLexiconName(new File(posDictPath).getName()));
      /*final Element posDictWindow = new Element("window");
      posDictWindow.setAttribute("prevLength", Integer.toString(leftWindow));
      posDictWindow.setAttribute("nextLength", Integer.toString(rightWindow));
      posDictWindow.addContent(posDictFeatures);
      generators.addContent(posDictWindow);*/
      generators.addContent(posDictFeatures);
      System.err.println("-> POSDictionary Features added!");
    }
    if (Flags.isLemmaModelFeatures(params)) {
      setWindow(params);
      final String lemmaModelPath = Flags.getLemmaModelFeatures(params);
      final Element lemmaClassFeatureElement = new Element("custom");
      lemmaClassFeatureElement.setAttribute("class",
          LemmaModelFeatureGenerator.class.getName());
      lemmaClassFeatureElement.setAttribute("model",
          IOUtils.normalizeLexiconName(new File(lemmaModelPath).getName()));
      final Element lemmaClassFeatureWindow = new Element("window");
      lemmaClassFeatureWindow.setAttribute("prevLength",
          Integer.toString(leftWindow));
      lemmaClassFeatureWindow.setAttribute("nextLength",
          Integer.toString(rightWindow));
      lemmaClassFeatureWindow.addContent(lemmaClassFeatureElement);
      generators.addContent(lemmaClassFeatureWindow);
      System.err.println("-> LemmaModel Features added!");
    }
    if (Flags.isLemmaDictionaryFeatures(params)) {
      setWindow(params);
      final String lemmaDictPath = Flags.getLemmaDictionaryFeatures(params);
      final String[] lemmaDictResources = Flags
          .getLemmaDictionaryResources(lemmaDictPath);
      final Element lemmaClassFeatureElement = new Element("custom");
      lemmaClassFeatureElement.setAttribute("class",
          LemmaDictionaryFeatureGenerator.class.getName());
      lemmaClassFeatureElement.setAttribute("model", IOUtils
          .normalizeLexiconName(new File(lemmaDictResources[0]).getName()));
      lemmaClassFeatureElement.setAttribute("dict", IOUtils
          .normalizeLexiconName(new File(lemmaDictResources[1]).getName()));
      final Element lemmaClassFeatureWindow = new Element("window");
      lemmaClassFeatureWindow.setAttribute("prevLength",
          Integer.toString(leftWindow));
      lemmaClassFeatureWindow.setAttribute("nextLength",
          Integer.toString(rightWindow));
      lemmaClassFeatureWindow.addContent(lemmaClassFeatureElement);
      generators.addContent(lemmaClassFeatureWindow);
      System.err.println("-> LemmaDictionary Features added!");
    }
    if (Flags.isMFSFeatures(params)) {
      setWindow(params);
      final String mfsPath = Flags.getMFSFeatures(params);
      final String[] mfsResources = Flags.getMFSResources(mfsPath);
      final String mfsRange = Flags.getMFSFeaturesRange(params);
      final String seqCodec = Flags.getSequenceCodec(params);
      final Element mfsClassFeatureElement = new Element("custom");
      mfsClassFeatureElement.setAttribute("class",
          MFSFeatureGenerator.class.getName());
      mfsClassFeatureElement.setAttribute("model",
          IOUtils.normalizeLexiconName(new File(mfsResources[0]).getName()));
      mfsClassFeatureElement.setAttribute("dict",
          IOUtils.normalizeLexiconName(new File(mfsResources[1]).getName()));
      mfsClassFeatureElement.setAttribute("mfs",
          IOUtils.normalizeLexiconName(new File(mfsResources[2]).getName()));
      mfsClassFeatureElement.setAttribute("range", mfsRange);
      mfsClassFeatureElement.setAttribute("seqCodec", seqCodec);
      final Element mfsClassFeatureWindow = new Element("window");
      mfsClassFeatureWindow.setAttribute("prevLength",
          Integer.toString(leftWindow));
      mfsClassFeatureWindow.setAttribute("nextLength",
          Integer.toString(rightWindow));
      mfsClassFeatureWindow.addContent(mfsClassFeatureElement);
      generators.addContent(mfsClassFeatureWindow);
      System.err.println("-> MFS Features added");
    }
    if (Flags.isSuperSenseFeatures(params)) {
      final String mfsPath = Flags.getSuperSenseFeatures(params);
      final String[] mfsResources = Flags.getSuperSenseResources(mfsPath);
      final String mfsRange = Flags.getSuperSenseFeaturesRange(params);
      final String seqCodec = Flags.getSequenceCodec(params);
      final Element mfsClassFeatureElement = new Element("custom");
      mfsClassFeatureElement.setAttribute("class",
          SuperSenseFeatureGenerator.class.getName());
      mfsClassFeatureElement.setAttribute("model",
          IOUtils.normalizeLexiconName(new File(mfsResources[0]).getName()));
      mfsClassFeatureElement.setAttribute("dict",
          IOUtils.normalizeLexiconName(new File(mfsResources[1]).getName()));
      mfsClassFeatureElement.setAttribute("mfs",
          IOUtils.normalizeLexiconName(new File(mfsResources[2]).getName()));
      mfsClassFeatureElement.setAttribute("range", mfsRange);
      mfsClassFeatureElement.setAttribute("seqCodec", seqCodec);
      generators.addContent(mfsClassFeatureElement);
      System.err.println("-> SuperSense Features added!");
    }
    if (Flags.isPOSBaselineFeatures(params)) {
      final String beginPrefix = Flags.getPrefixBegin(params);
      final String endPrefix = Flags.getPrefixEnd(params);
      final String beginSuffix = Flags.getSuffixBegin(params);
      final String endSuffix = Flags.getSuffixEnd(params);
      final Element posFeatureElement = new Element("custom");
      posFeatureElement.setAttribute("class",
          POSBaselineContextGenerator.class.getName());
      posFeatureElement.setAttribute("prefBegin", beginPrefix);
      posFeatureElement.setAttribute("prefEnd", endPrefix);
      posFeatureElement.setAttribute("sufBegin", beginSuffix);
      posFeatureElement.setAttribute("sufEnd", endSuffix);
      generators.addContent(posFeatureElement);
      System.err.println("-> POS Baseline Context Generator added!");
    }
    if (Flags.isLemmaBaselineFeatures(params)) {
      final String beginPrefix = Flags.getPrefixBegin(params);
      final String endPrefix = Flags.getPrefixEnd(params);
      final String beginSuffix = Flags.getSuffixBegin(params);
      final String endSuffix = Flags.getSuffixEnd(params);
      final String posModel = Flags.getLemmaBaselineFeatures(params);
      final String lemmaRange = Flags.getLemmaBaselineFeaturesRange(params);
      final Element lemmaFeatureElement = new Element("custom");
      lemmaFeatureElement.setAttribute("class",
          LemmaBaselineContextGenerator.class.getName());
      lemmaFeatureElement.setAttribute("prefBegin", beginPrefix);
      lemmaFeatureElement.setAttribute("prefEnd", endPrefix);
      lemmaFeatureElement.setAttribute("sufBegin", beginSuffix);
      lemmaFeatureElement.setAttribute("sufEnd", endSuffix);
      lemmaFeatureElement.setAttribute("model",
          IOUtils.normalizeLexiconName(new File(posModel).getName()));
      lemmaFeatureElement.setAttribute("range", lemmaRange);
      generators.addContent(lemmaFeatureElement);
      System.err.println("-> Lemma Baseline Context Generator added!");
    }
    if (Flags.isChunkBaselineFeatures(params)) {
      final String posModel = Flags.getChunkBaselineFeatures(params);
      final Element chunkFeatureElement = new Element("custom");
      chunkFeatureElement.setAttribute("class",
          ChunkBaselineContextGenerator.class.getName());
      chunkFeatureElement.setAttribute("model",
          IOUtils.normalizeLexiconName(new File(posModel).getName()));
      generators.addContent(chunkFeatureElement);
      System.err.println("-> Chunk Baseline Context Generator added!");
    }

    if (Flags.isPredicateContextFeatures(params)) {
      final String predicateContextFile = Flags
          .getPredicateContextFeatures(params);
      final Element predicateContextFeatureElement = new Element("custom");
      predicateContextFeatureElement.setAttribute("class",
          PredicateContextFeatureGenerator.class.getName());
      predicateContextFeatureElement.setAttribute("dict", IOUtils
          .normalizeLexiconName(new File(predicateContextFile).getName()));
      generators.addContent(predicateContextFeatureElement);
      System.err.println("-> Predicate Context Generator added!");
    }

    aggGenerators.addContent(cached);
    cached.addContent(generators);

    final XMLOutputter xmlOutput = new XMLOutputter();
    final Format format = Format.getPrettyFormat();
    xmlOutput.setFormat(format);
    return xmlOutput.outputString(doc);

  }

  /**
   * Set the window length from the training parameters file.
   * 
   * @param params
   *          the properties file
   */
  public static void setWindow(final TrainingParameters params) {
    if (leftWindow == -1 || rightWindow == -1) {
      leftWindow = getWindowRange(params).get(0);
      rightWindow = getWindowRange(params).get(1);
    }
  }

  /**
   * Get the window range feature.
   * 
   * @param params
   *          the training parameters
   * @return the list containing the left and right window values
   */
  private static List<Integer> getWindowRange(final TrainingParameters params) {
    final List<Integer> windowRange = new ArrayList<Integer>();
    final String windowParam = Flags.getWindow(params);
    final String[] windowArray = windowParam.split("[ :-]");
    if (windowArray.length == 2) {
      windowRange.add(Integer.parseInt(windowArray[0]));
      windowRange.add(Integer.parseInt(windowArray[1]));
    }
    return windowRange;
  }

}
