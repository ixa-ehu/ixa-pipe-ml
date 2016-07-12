/*
 *  Copyright 2014 Rodrigo Agerri

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

package eus.ixa.ixa.pipe.ml.resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.model.ArtifactSerializer;
import opennlp.tools.util.model.SerializableArtifact;
import eus.ixa.ixa.pipe.ml.utils.IOUtils;



/**
 * 
 * Class to load a Word2Vec cluster document: word\\s+word_class
 * http://code.google.com/p/word2vec/
 * 
 * The file containing the clustering lexicon has to be passed as the 
 * argument of the Word2VecCluster property.
 * 
 * @author ragerri
 * @version 2014/07/29
 * 
 */
public class Word2VecCluster implements SerializableArtifact {
  
  public static class Word2VecClusterSerializer implements ArtifactSerializer<Word2VecCluster> {

    public Word2VecCluster create(InputStream in) throws IOException,
        InvalidFormatException {
      return new Word2VecCluster(in);
    }

    public void serialize(Word2VecCluster artifact, OutputStream out)
        throws IOException {
      artifact.serialize(out);
    }
  }
  
  private Map<String, String> tokenToClusterMap = new HashMap<String, String>();
  
  public Word2VecCluster(InputStream in) throws IOException {
    try {
      Map <String, String> tempMap = IOUtils.readObjectFromInputStream(in);
      tokenToClusterMap.putAll(tempMap);
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
  }
  
  public String lookupToken(String string) {
    return tokenToClusterMap.get(string);
  }
  
  public Map<String, String> getMap() {
    return tokenToClusterMap;
  }

  public void serialize(OutputStream out) throws IOException {
    IOUtils.writeObjectToStream(tokenToClusterMap, out);
  }

  public Class<?> getArtifactSerializerClass() {
    return Word2VecClusterSerializer.class;
  }

}

