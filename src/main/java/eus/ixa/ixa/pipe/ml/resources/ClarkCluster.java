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
import java.util.regex.Pattern;

import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.model.ArtifactSerializer;
import opennlp.tools.util.model.SerializableArtifact;
import eus.ixa.ixa.pipe.ml.utils.IOUtils;


/**
 * 
 * Class to load a Clark cluster document: word\\s+word_class\\s+prob
 * https://github.com/ninjin/clark_pos_induction
 * 
 * The file containing the clustering lexicon has to be passed as the 
 * argument of the DistSim property.
 * 
 * @author ragerri
 * @version 2014/07/29
 * 
 */
public class ClarkCluster implements SerializableArtifact {

  /**
   * Turkish capital letter I with dot.
   */
  public static final Pattern dotInsideI = Pattern.compile("\u0130", Pattern.UNICODE_CHARACTER_CLASS);
  
  public static class ClarkClusterSerializer implements ArtifactSerializer<ClarkCluster> {

    public ClarkCluster create(InputStream in) throws IOException,
        InvalidFormatException {
      return new ClarkCluster(in);
    }

    public void serialize(ClarkCluster artifact, OutputStream out)
        throws IOException {
      artifact.serialize(out);
    }
  }
  
  private Map<String, String> tokenToClusterMap = new HashMap<String, String>();

  public ClarkCluster(InputStream in) throws IOException {

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
    return ClarkClusterSerializer.class;
  }
}

