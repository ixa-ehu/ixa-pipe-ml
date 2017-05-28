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

import java.io.IOException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import eus.ixa.ixa.pipe.ml.utils.Flags;
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
    final XMLOutputter xmlOutput = new XMLOutputter();
    final Format format = Format.getPrettyFormat();
    xmlOutput.setFormat(format);
    return xmlOutput.outputString(doc);

  }

}
