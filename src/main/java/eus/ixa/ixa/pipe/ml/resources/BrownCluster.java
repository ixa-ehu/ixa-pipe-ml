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
package eus.ixa.ixa.pipe.ml.resources;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.model.ArtifactSerializer;
import opennlp.tools.util.model.SerializableArtifact;
import eus.ixa.ixa.pipe.ml.utils.IOUtils;


/**
 * 
 * Class to load a Brown cluster document: word\tword_class\tprob
 * http://metaoptimize.com/projects/wordreprs/
 * 
 * The file containing the clustering lexicon has to be passed as the 
 * argument of the BrownClusterFeatures parameter in the prop file.
 * 
 * @author ragerri
 * @version 2014/09/29
 * 
 */
public class BrownCluster implements SerializableArtifact {

  public static class BrownClusterSerializer implements ArtifactSerializer<BrownCluster> {

    public BrownCluster create(InputStream in) throws IOException,
        InvalidFormatException {
      return new BrownCluster(in);
    }

    public void serialize(BrownCluster artifact, OutputStream out)
        throws IOException {
      artifact.serialize(out);
    }
  }
  
  private Map<String, String> tokenToClusterMap = new HashMap<String, String>();

  /**
   * Generates the token to cluster map from Brown cluster input file.
   * NOTE: we only add those tokens with frequency larger than 5.
   * @param in the inputstream
   * @throws IOException the io exception
   */
  public BrownCluster(InputStream in) throws IOException {
    try {
      Map <String, String> tempMap = IOUtils.readObjectFromInputStream(in);
      tokenToClusterMap.putAll(tempMap);
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
  }

  /**
   * Check if a token is in the Brown paths, token map.
   * @param string the token to look-up
   * @return the brown class if such token is in the brown cluster map
   */
  public String lookupToken(String string) {
    return tokenToClusterMap.get(string);
  }

  public void serialize(OutputStream out) throws IOException {
    OutputStream outputStream = new BufferedOutputStream(out);
    ObjectOutputStream oos = new ObjectOutputStream(outputStream);
    oos.writeObject(tokenToClusterMap);
    oos.flush();
  }

  public Class<?> getArtifactSerializerClass() {
    return BrownClusterSerializer.class;
  }
}

