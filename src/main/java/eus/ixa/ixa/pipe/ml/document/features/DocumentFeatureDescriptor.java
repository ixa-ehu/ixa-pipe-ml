/*
 * Copyright 2017 Rodrigo Agerri

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
 * @version 2017-05-28
 */
public final class DocumentFeatureDescriptor {

  /**
   * This class is not to be instantiated.
   */
  private DocumentFeatureDescriptor() {
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
  public static String createDocumentFeatureDescriptor(
      final TrainingParameters params) throws IOException {

    // <generators>
    final Element generators = new Element("generators");
    final Document doc = new Document(generators);
    
    // <custom bagofwords /.
    if (Flags.isBagOfWordsFeature(params)) {
      final String tokenFeatureRange = Flags.getBagOfWordsFeaturesRange(params);
      final Element tokenFeature = new Element("custom");
      tokenFeature.setAttribute("class", BagOfWordsFeatureGenerator.class.getName());
      tokenFeature.setAttribute("range", tokenFeatureRange);
      generators.addContent(tokenFeature);
      System.err.println("-> BOW features added!");
    }
    if (Flags.isTokenClassFeature(params)) {
      final String tokenClassFeatureRange = Flags
          .getTokenClassFeaturesRange(params);
      final Element tokenClassFeature = new Element("custom");
      tokenClassFeature.setAttribute("class",
          DocTokenClassFeatureGenerator.class.getName());
      tokenClassFeature.setAttribute("range", tokenClassFeatureRange);
      generators.addContent(tokenClassFeature);
      System.err.println("-> Token Class Features added!");
    }
    if (Flags.isOutcomePriorFeature(params)) {
      final Element outcomePriorFeature = new Element("custom");
      outcomePriorFeature.setAttribute("class",
          DocOutcomePriorFeatureGenerator.class.getName());
      generators.addContent(outcomePriorFeature);
      System.err.println("-> Outcome Prior Features added!");
    }
    if (Flags.isSentenceFeature(params)) {
      final String beginSentence = Flags.getSentenceFeaturesBegin(params);
      final String endSentence = Flags.getSentenceFeaturesEnd(params);
      final Element sentenceFeature = new Element("custom");
      sentenceFeature.setAttribute("class",
          DocSentenceFeatureGenerator.class.getName());
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
          DocPrefixFeatureGenerator.class.getName());
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
          DocSuffixFeatureGenerator.class.getName());
      suffixFeature.setAttribute("begin", beginSuffix);
      suffixFeature.setAttribute("end", endSuffix);
      generators.addContent(suffixFeature);
      System.err.println("-> Suffix Features added!");
    }
    if (Flags.isNgramFeature(params)) {
      final String charngramRange = Flags.getNgramFeaturesRange(params);
      final String[] rangeArray = Flags.processNgramRange(charngramRange);
      final Element charngramFeature = new Element("custom");
      charngramFeature.setAttribute("class",
          NGramFeatureGenerator.class.getName());
      charngramFeature.setAttribute("minLength", rangeArray[0]);
      charngramFeature.setAttribute("maxLength", rangeArray[1]);
      generators.addContent(charngramFeature);
      System.err.println("-> Ngram Features added!");
    }
    if (Flags.isCharNgramClassFeature(params)) {
      final String charngramRange = Flags.getCharNgramFeaturesRange(params);
      final String[] rangeArray = Flags.processNgramRange(charngramRange);
      final Element charngramFeature = new Element("custom");
      charngramFeature.setAttribute("class",
          DocCharacterNgramFeatureGenerator.class.getName());
      charngramFeature.setAttribute("minLength", rangeArray[0]);
      charngramFeature.setAttribute("maxLength", rangeArray[1]);
      generators.addContent(charngramFeature);
      System.err.println("-> CharNgram Class Features added!");
    }
    // Polarity Dictionary Features
    if (Flags.isDictionaryFeatures(params)) {
      final String dictPath = Flags.getDictionaryFeatures(params);
      final List<File> fileList = StringUtils.getFilesInDir(new File(dictPath));
      for (final File dictFile : fileList) {
        final Element dictFeatures = new Element("custom");
        dictFeatures.setAttribute("class",
            DocPolarityDictionaryFeatureGenerator.class.getName());
        dictFeatures.setAttribute("dict",
            IOUtils.normalizeLexiconName(dictFile.getName()));
        generators.addContent(dictFeatures);
      }
      System.err.println("-> Dictionary Polarity Features added!");
    }
    // Frequent Word Features
    if (Flags.isFrequentWordFeatures(params)) {
      final String dictPath = Flags.getFrequentWordFeatures(params);
      final List<File> fileList = StringUtils.getFilesInDir(new File(dictPath));
      for (final File dictFile : fileList) {
        final Element dictFeatures = new Element("custom");
        dictFeatures.setAttribute("class",
            FrequentWordFeatureGenerator.class.getName());
        dictFeatures.setAttribute("dict",
            IOUtils.normalizeLexiconName(dictFile.getName()));
        generators.addContent(dictFeatures);
      }
      System.err.println("-> Frequent Word Features added!");
    }
    // Brown clustering features
    if (Flags.isBrownFeatures(params)) {
      final String brownClusterPath = Flags.getBrownFeatures(params);
      final List<File> brownClusterFiles = Flags
          .getClusterLexiconFiles(brownClusterPath);
      for (final File brownClusterFile : brownClusterFiles) {
        // brown bigram class features
        final Element brownBigramFeatures = new Element("custom");
        brownBigramFeatures.setAttribute("class",
            DocBrownBigramFeatureGenerator.class.getName());
        brownBigramFeatures.setAttribute("dict",
            IOUtils.normalizeLexiconName(brownClusterFile.getName()));
        //generators.addContent(brownBigramFeatures);
        // brown token feature
        final Element brownTokenFeature = new Element("custom");
        brownTokenFeature.setAttribute("class",
            DocBrownTokenFeatureGenerator.class.getName());
        brownTokenFeature.setAttribute("dict",
            IOUtils.normalizeLexiconName(brownClusterFile.getName()));
        generators.addContent(brownTokenFeature);
        // brown token class feature
        final Element brownTokenClassFeature = new Element("custom");
        brownTokenClassFeature.setAttribute("class",
            DocBrownTokenClassFeatureGenerator.class.getName());
        brownTokenClassFeature.setAttribute("dict",
            IOUtils.normalizeLexiconName(brownClusterFile.getName()));
        //generators.addContent(brownTokenClassFeature);
      }
      System.err.println("-> Brown Cluster Features added!");
    }
    // Clark clustering features
    if (Flags.isClarkFeatures(params)) {
      final String clarkClusterPath = Flags.getClarkFeatures(params);
      final List<File> clarkClusterFiles = Flags
          .getClusterLexiconFiles(clarkClusterPath);
      for (final File clarkCluster : clarkClusterFiles) {
        final Element clarkFeatures = new Element("custom");
        clarkFeatures.setAttribute("class",
            DocClarkFeatureGenerator.class.getName());
        clarkFeatures.setAttribute("dict",
            IOUtils.normalizeLexiconName(clarkCluster.getName()));
        generators.addContent(clarkFeatures);
      }
      System.err.println("-> Clark Cluster Features added!");
    }
    // word2vec clustering features
    if (Flags.isWord2VecClusterFeatures(params)) {
      final String word2vecClusterPath = Flags
          .getWord2VecClusterFeatures(params);
      final List<File> word2vecClusterFiles = Flags
          .getClusterLexiconFiles(word2vecClusterPath);
      for (final File word2vecFile : word2vecClusterFiles) {
        final Element word2vecClusterFeatures = new Element("custom");
        word2vecClusterFeatures.setAttribute("class",
            DocWord2VecClusterFeatureGenerator.class.getName());
        word2vecClusterFeatures.setAttribute("dict",
            IOUtils.normalizeLexiconName(word2vecFile.getName()));
        generators.addContent(word2vecClusterFeatures);
      }
      System.err.println("-> Word2Vec Clusters Features added!");
    }
    // Morphological features
    if (Flags.isPOSTagModelFeatures(params)) {
      final String posModelPath = Flags.getPOSTagModelFeatures(params);
      final String posModelRange = Flags.getPOSTagModelFeaturesRange(params);
      final Element posTagClassFeatureElement = new Element("custom");
      posTagClassFeatureElement.setAttribute("class",
          DocPOSTagModelFeatureGenerator.class.getName());
      posTagClassFeatureElement.setAttribute("model",
          IOUtils.normalizeLexiconName(new File(posModelPath).getName()));
      posTagClassFeatureElement.setAttribute("range", posModelRange);
      generators.addContent(posTagClassFeatureElement);
      System.err.println("-> POSTagModel Features added!");
    }
    if (Flags.isLemmaModelFeatures(params)) {
      final String lemmaModelPath = Flags.getLemmaModelFeatures(params);
      final Element lemmaClassFeatureElement = new Element("custom");
      lemmaClassFeatureElement.setAttribute("class",
          DocLemmaModelFeatureGenerator.class.getName());
      lemmaClassFeatureElement.setAttribute("model",
          IOUtils.normalizeLexiconName(new File(lemmaModelPath).getName()));
      generators.addContent(lemmaClassFeatureElement);
      System.err.println("-> LemmaModel Features added!");
    }
    if (Flags.isLemmaDictionaryFeatures(params)) {
      final String lemmaDictPath = Flags.getLemmaDictionaryFeatures(params);
      final String[] lemmaDictResources = Flags
          .getLemmaDictionaryResources(lemmaDictPath);
      final Element lemmaClassFeatureElement = new Element("custom");
      lemmaClassFeatureElement.setAttribute("class",
          DocLemmaDictionaryFeatureGenerator.class.getName());
      lemmaClassFeatureElement.setAttribute("model", IOUtils
          .normalizeLexiconName(new File(lemmaDictResources[0]).getName()));
      lemmaClassFeatureElement.setAttribute("dict", IOUtils
          .normalizeLexiconName(new File(lemmaDictResources[1]).getName()));
      generators.addContent(lemmaClassFeatureElement);
      System.err.println("-> LemmaDictionary Features added!");
    }
    final XMLOutputter xmlOutput = new XMLOutputter();
    final Format format = Format.getPrettyFormat();
    xmlOutput.setFormat(format);
    return xmlOutput.outputString(doc);

  }

}
