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
package eus.ixa.ixa.pipe.ml.document;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import eus.ixa.ixa.pipe.ml.features.BrownBigramFeatureGenerator;
import eus.ixa.ixa.pipe.ml.features.BrownTokenClassFeatureGenerator;
import eus.ixa.ixa.pipe.ml.features.BrownTokenFeatureGenerator;
import eus.ixa.ixa.pipe.ml.features.ClarkFeatureGenerator;
import eus.ixa.ixa.pipe.ml.features.Prev2MapFeatureGenerator;
import eus.ixa.ixa.pipe.ml.features.PreviousMapTokenFeatureGenerator;
import eus.ixa.ixa.pipe.ml.features.Word2VecClusterFeatureGenerator;
import eus.ixa.ixa.pipe.ml.utils.Flags;
import eus.ixa.ixa.pipe.ml.utils.IOUtils;
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
    // Brown clustering features
    if (Flags.isBrownFeatures(params)) {
      // previous 2 maps features
      final Element prev2MapFeature = new Element("custom");
      prev2MapFeature.setAttribute("class",
          Prev2MapFeatureGenerator.class.getName());
      generators.addContent(prev2MapFeature);
      // previous map and token feature (in window)
      final Element prevMapTokenFeature = new Element("custom");
      prevMapTokenFeature.setAttribute("class",
          PreviousMapTokenFeatureGenerator.class.getName());
      generators.addContent(prevMapTokenFeature);
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
        generators.addContent(brownTokenFeature);
        // brown token class feature
        final Element brownTokenClassFeature = new Element("custom");
        brownTokenClassFeature.setAttribute("class",
            BrownTokenClassFeatureGenerator.class.getName());
        brownTokenClassFeature.setAttribute("dict",
            IOUtils.normalizeLexiconName(brownClusterFile.getName()));
        generators.addContent(brownTokenClassFeature);
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
            ClarkFeatureGenerator.class.getName());
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
            Word2VecClusterFeatureGenerator.class.getName());
        word2vecClusterFeatures.setAttribute("dict",
            IOUtils.normalizeLexiconName(word2vecFile.getName()));
        generators.addContent(word2vecClusterFeatures);
      }
      System.err.println("-> Word2Vec Clusters Features added!");
    }
    final XMLOutputter xmlOutput = new XMLOutputter();
    final Format format = Format.getPrettyFormat();
    xmlOutput.setFormat(format);
    return xmlOutput.outputString(doc);

  }

}
