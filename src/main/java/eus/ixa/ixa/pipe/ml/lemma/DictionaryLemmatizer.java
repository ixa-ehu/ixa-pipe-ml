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
package eus.ixa.ixa.pipe.ml.lemma;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eus.ixa.ixa.pipe.ml.utils.IOUtils;
import eus.ixa.ixa.pipe.ml.utils.Span;
import eus.ixa.ixa.pipe.ml.utils.StringUtils;
import opennlp.tools.util.model.ArtifactSerializer;
import opennlp.tools.util.model.SerializableArtifact;

/**
 * Lemmatize by simple dictionary lookup into a serialized hashmap built from a
 * file containing, for each line, word\tablemma\tabpostag.
 *
 * @author ragerri
 * @version 2016-07-19
 */
public class DictionaryLemmatizer implements SerializableArtifact {

  private static final char TAB_DELIMITER = '\t';

  public static class DictionaryLemmatizerSerializer
      implements ArtifactSerializer<DictionaryLemmatizer> {

    @Override
    public DictionaryLemmatizer create(final InputStream in)
        throws IOException {
      return new DictionaryLemmatizer(in);
    }

    @Override
    public void serialize(final DictionaryLemmatizer artifact,
        final OutputStream out) throws IOException {
      artifact.serialize(out);
    }
  }

  /**
   * The hashmap containing the dictionary.
   */
  private final Map<List<String>, String> dictMap = new HashMap<>();
  private final String[] splitted = new String[64];

  /**
   * Construct a hashmap from the input tab separated dictionary.
   *
   * The input file should have, for each line, word\tablemma\tabpostag
   *
   * @param in
   *          the input dictionary via inputstream
   * @throws IOException
   *           if io problems
   */
  public DictionaryLemmatizer(final InputStream in) throws IOException {

    final BufferedReader breader = new BufferedReader(new InputStreamReader(
        new BufferedInputStream(in), Charset.forName("UTF-8")));
    String line;
    while ((line = breader.readLine()) != null) {
      StringUtils.splitLine(line, TAB_DELIMITER, this.splitted);
      this.dictMap.put(Arrays.asList(this.splitted[0], this.splitted[2]),
          this.splitted[1]);
    }
  }

  /**
   * Get the Map containing the dictionary.
   *
   * @return dictMap the Map
   */
  public Map<List<String>, String> getDictMap() {
    return this.dictMap;
  }

  /**
   * Get the dictionary keys (word and postag).
   *
   * @param word
   *          the surface form word
   * @param postag
   *          the assigned postag
   * @return returns the dictionary keys
   */
  public List<String> getDictKeys(final String word, final String postag) {
    final List<String> keys = new ArrayList<>();
    keys.addAll(Arrays.asList(word.toLowerCase(), postag));
    return keys;
  }

  public List<String> lemmatize(final String[] tokens, final Span[] postags) {
    final List<String> lemmas = new ArrayList<>();
    for (int i = 0; i < tokens.length; i++) {
      lemmas.add(this.apply(tokens[i], postags[i].getType()));
    }
    return lemmas;
  }

  /**
   * Lookup lemma in a dictionary. Outputs "O" if not found.
   * 
   * @param word
   *          the token
   * @param postag
   *          the postag
   * @return the lemma
   */
  private String apply(final String word, final String postag) {
    String lemma;
    final List<String> keys = this.getDictKeys(word, postag);
    // lookup lemma as value of the map
    final String keyValue = this.dictMap.get(keys);
    if (keyValue != null) {
      lemma = keyValue;
    } else {
      lemma = "O";
    }
    return lemma;
  }

  public void serialize(final OutputStream out) throws IOException {

    final Writer writer = new BufferedWriter(new OutputStreamWriter(out));
    for (final Map.Entry<List<String>, String> entry : this.dictMap
        .entrySet()) {
      writer.write(
          entry.getKey().get(0) + IOUtils.TAB_DELIMITER + entry.getValue()
              + IOUtils.TAB_DELIMITER + entry.getKey().get(1) + "\n");
    }
    writer.flush();
  }

  @Override
  public Class<?> getArtifactSerializerClass() {
    return DictionaryLemmatizerSerializer.class;
  }
}
