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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicInteger;

import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.model.ArtifactSerializer;
import opennlp.tools.util.model.SerializableArtifact;

import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;

import eus.ixa.ixa.pipe.ml.utils.IOUtils;


/**
 * 
 * Class to load a serialized {@code TabulatedFormat} corpus and create an
 * automatic dictionary from it.
 * @author ragerri
 * @version 2016/07/13
 * 
 */
public class POSDictionary implements SerializableArtifact {
  
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
    
    try {
      Map<String, Map<String, AtomicInteger>> temp = IOUtils.readObjectFromInputStream(in);
      newEntries.putAll(temp);
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
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
    IOUtils.writeObjectToStream(newEntries, out);
  }

  public Class<?> getArtifactSerializerClass() {
    return POSDictionarySerializer.class;
  }
}


