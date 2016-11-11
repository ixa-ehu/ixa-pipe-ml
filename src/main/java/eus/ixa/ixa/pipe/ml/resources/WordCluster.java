/*
 *  Copyright 2016 Rodrigo Agerri

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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.model.ArtifactSerializer;
import opennlp.tools.util.model.SerializableArtifact;

/**
 * Class to hold word cluster information in a Map of String.
 * <ol>
 * <li>Brown cluster documents: http://metaoptimize.com/projects/wordreprs/</li>
 * <li>Clark cluster documents: https://github.com/ninjin/clark_pos_induction
 * </li>
 * <li>Word2Vec cluster documents: http://code.google.com/p/word2vec/</li>
 * </ol>
 * The cluster lexicons are normalized by the ixa-pipe-convert
 * SerializeResources class.
 *
 * @author ragerri
 * @version 2016-07-18
 *
 */
public class WordCluster implements SerializableArtifact {

  private final static char spaceDelimiter = ' ';

  public static class WordClusterSerializer
      implements ArtifactSerializer<WordCluster> {

    @Override
    public WordCluster create(final InputStream in)
        throws IOException, InvalidFormatException {
      return new WordCluster(in);
    }

    @Override
    public void serialize(final WordCluster artifact, final OutputStream out)
        throws IOException {
      artifact.serialize(out);
    }
  }

  private final Map<String, String> tokenToClusterMap = new HashMap<String, String>();

  public WordCluster(final InputStream in) throws IOException {

    final BufferedReader breader = new BufferedReader(new InputStreamReader(
        new BufferedInputStream(in), Charset.forName("UTF-8")));
    String line;
    while ((line = breader.readLine()) != null) {
      final int index = line.indexOf(spaceDelimiter);
      final String token = line.substring(0, index);
      final String tokenClass = line.substring(index + 1).intern();
      this.tokenToClusterMap.put(token, tokenClass);
    }
  }

  public String lookupToken(final String string) {
    return this.tokenToClusterMap.get(string);
  }

  public Map<String, String> getMap() {
    return this.tokenToClusterMap;
  }

  public void serialize(final OutputStream out) throws IOException {

    final Writer writer = new BufferedWriter(
        new OutputStreamWriter(out, "UTF-8"));
    for (final Map.Entry<String, String> entry : this.tokenToClusterMap
        .entrySet()) {
      writer.write(entry.getKey() + spaceDelimiter + entry.getValue() + "\n");
    }
    writer.flush();
  }

  @Override
  public Class<?> getArtifactSerializerClass() {
    return WordClusterSerializer.class;
  }
}
