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
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;

import eus.ixa.ixa.pipe.ml.utils.IOUtils;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.StringUtil;
import opennlp.tools.util.featuregen.StringPattern;
import opennlp.tools.util.model.ArtifactSerializer;
import opennlp.tools.util.model.SerializableArtifact;

/**
 *
 * Class to load a serialized {@code TabulatedFormat} corpus and create an
 * automatic dictionary from it.
 * 
 * @author ragerri
 * @version 2016/07/19
 *
 */
public class POSDictionary implements SerializableArtifact {

  private final static Pattern tabDelimiter = Pattern.compile("\t");

  public static class POSDictionarySerializer
      implements ArtifactSerializer<POSDictionary> {

    @Override
    public POSDictionary create(final InputStream in)
        throws IOException, InvalidFormatException {
      return new POSDictionary(in);
    }

    @Override
    public void serialize(final POSDictionary artifact, final OutputStream out)
        throws IOException {
      artifact.serialize(out);
    }
  }

  private final Map<String, Map<String, AtomicInteger>> newEntries = new HashMap<String, Map<String, AtomicInteger>>();

  public POSDictionary(final InputStream in) throws IOException {
    final BufferedReader breader = new BufferedReader(
        new InputStreamReader(in, Charset.forName("UTF-8")));
    String line;
    while ((line = breader.readLine()) != null) {
      String[] lineArray = tabDelimiter.split(line);
      populatePOSMap(lineArray);
    }
  }


  private void populatePOSMap(final String[] lineArray) {
    if (lineArray.length == 2) {
      // only store words
      if (!StringPattern.recognize(lineArray[0]).containsDigit()) {

        final String word = StringUtil.toLowerCase(lineArray[0]);
        if (!newEntries.containsKey(word)) {
          newEntries.put(word, new HashMap<String, AtomicInteger>());
        }
        if (!newEntries.get(word).containsKey(lineArray[1])) {
          newEntries.get(word).put(lineArray[1], new AtomicInteger(1));
        } else {
          newEntries.get(word).get(lineArray[1]).incrementAndGet();
        }
      }
    }
  }

  public String getMostFrequentTag(final String word) {
    final TreeMultimap<Integer, String> mfTagMap = getOrderedMap(word);
    String mfTag = null;
    if (!mfTagMap.isEmpty()) {
      final SortedSet<String> mfTagSet = mfTagMap
          .get(mfTagMap.keySet().first());
      mfTag = mfTagSet.first();
    } else {
      mfTag = "O";
    }
    return mfTag;
  }
  
  public String getAllTags(final String word) {
    final TreeMultimap<Integer, String> mfTagMap = getOrderedMap(word);
    String mfTag = null;
    if (!mfTagMap.isEmpty()) {
      final SortedSet<String> mfTagSet = mfTagMap
          .get(mfTagMap.keySet().first());
      mfTag = mfTagSet.first();
    } else {
      mfTag = "O";
    }
    return mfTag;
  }

  public TreeMultimap<Integer, String> getOrderedMap(final String word) {
    final Map<String, AtomicInteger> tagFreqsMap = this.newEntries.get(word);
    final TreeMultimap<Integer, String> mfTagMap = TreeMultimap
        .create(Ordering.natural().reverse(), Ordering.natural());
    if (tagFreqsMap != null) {
      getOrderedTags(tagFreqsMap, mfTagMap);
    }
    return mfTagMap;
  }

  private void getOrderedTags(final Map<String, AtomicInteger> tagFreqsMap,
      final TreeMultimap<Integer, String> mfTagMap) {

    if (!tagFreqsMap.isEmpty() || tagFreqsMap != null) {
      for (final Map.Entry<String, AtomicInteger> entry : tagFreqsMap
          .entrySet()) {
        mfTagMap.put(entry.getValue().intValue(), entry.getKey());
      }
    }
  }

  /**
   * Serialize the dictionary to original corpus format
   * 
   * @param out
   *          the output stream
   * @throws IOException
   *           if io problems
   */
  public void serialize(final OutputStream out) throws IOException {

    final Writer writer = new BufferedWriter(new OutputStreamWriter(out));
    for (final Map.Entry<String, Map<String, AtomicInteger>> entry : this.newEntries
        .entrySet()) {
      System.err.println(entry.getKey() + IOUtils.TAB_DELIMITER
          + entry.getValue());
      writer.write(entry.getKey() + IOUtils.TAB_DELIMITER
          + entry.getValue() + "\n");
    }
    writer.flush();
  }

  @Override
  public Class<?> getArtifactSerializerClass() {
    return POSDictionarySerializer.class;
  }
}
