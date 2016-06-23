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

import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.StringUtil;
import opennlp.tools.util.featuregen.StringPattern;
import opennlp.tools.util.model.ArtifactSerializer;
import opennlp.tools.util.model.SerializableArtifact;

import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;


/**
 * 
 * Class to load a {@code TabulatedFormat} corpus and create an
 * automatic dictionary from it.
 * @author ragerri
 * @version 2016/06/17
 * 
 */
public class POSDictionary implements SerializableArtifact {

  private static final Pattern tabPattern = Pattern.compile("\t");
  /**
   * Turkish capital letter I with dot.
   */
  public static final Pattern dotInsideI = Pattern.compile("\u0130", Pattern.UNICODE_CHARACTER_CLASS);
  
  public static class POSDictionarySerializer implements ArtifactSerializer<POSDictionary> {

    public POSDictionary create(InputStream in) throws IOException,
        InvalidFormatException {
      return new POSDictionary(in);
    }

    public void serialize(POSDictionary artifact, OutputStream out)
        throws IOException {
      artifact.serialize(out);
    }
  }

  private Map<String, Map<String, AtomicInteger>> newEntries = new HashMap<String, Map<String, AtomicInteger>>();

  public POSDictionary(InputStream in) throws IOException {

    BufferedReader breader = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));
    String line;
    while ((line = breader.readLine()) != null) {
      String[] lineArray = tabPattern.split(line);
      populateMap(lineArray);
    }
  }
  
  private void populateMap(String[] lineArray) {
    if (lineArray.length == 2) {
      String normalizedToken = dotInsideI.matcher(lineArray[0]).replaceAll("i");
      // only store words
      if (!StringPattern.recognize(normalizedToken).containsDigit()) {
        
        String word = StringUtil.toLowerCase(normalizedToken);
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
  
  public String getMostFrequentTag(String word) {
    TreeMultimap<Integer, String> mfTagMap = getOrderedMap(word);
    String mfTag = null;
    if (!mfTagMap.isEmpty()) {
    SortedSet<String> mfTagSet = mfTagMap.get(mfTagMap.keySet().first());
    mfTag = mfTagSet.first();
    } else {
      mfTag = "O";
    }
    return mfTag;
  }
  
  public TreeMultimap<Integer, String> getOrderedMap(String word) {
    Map<String, AtomicInteger> tagFreqsMap = newEntries.get(word);
    TreeMultimap<Integer, String> mfTagMap = TreeMultimap.create(Ordering.natural().reverse(), Ordering.natural());
    if (tagFreqsMap != null) {
      getOrderedTags(tagFreqsMap, mfTagMap);
    }
    return mfTagMap;
  }
  
  private void getOrderedTags(Map<String, AtomicInteger> tagFreqsMap, TreeMultimap<Integer, String> mfTagMap) {
   
    if (!tagFreqsMap.isEmpty() || tagFreqsMap != null) {
      for (Map.Entry<String, AtomicInteger> entry: tagFreqsMap.entrySet()) {
        mfTagMap.put(entry.getValue().intValue(), entry.getKey());
      }
    }
  }

  /**
   * Serialize the dictionary to original corpus format
   * @param out the output stream
   * @throws IOException if io problems
   */
  public void serialize(OutputStream out) throws IOException {
    Writer writer = new BufferedWriter(new OutputStreamWriter(out));

    for (Map.Entry<String, Map<String, AtomicInteger>> entry : newEntries.entrySet()) {
      writer.write(entry.getKey() + "\t" + entry.getValue().get(entry.getKey()) + "\n");
    }
    writer.flush();
  }

  public Class<?> getArtifactSerializerClass() {
    return POSDictionarySerializer.class;
  }
}


