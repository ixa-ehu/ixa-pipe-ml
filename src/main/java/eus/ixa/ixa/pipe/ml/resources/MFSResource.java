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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.model.ArtifactSerializer;
import opennlp.tools.util.model.SerializableArtifact;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;

import eus.ixa.ixa.pipe.ml.sequence.BilouCodec;
import eus.ixa.ixa.pipe.ml.sequence.BioCodec;
import eus.ixa.ixa.pipe.ml.utils.IOUtils;
import eus.ixa.ixa.pipe.ml.utils.Span;

/**
 * Reads wordnet lexicons formated as house#n\t1092#noun.artifact
 * in a serialized ListMultimap object to search for most frequent senses.
 * @author ragerri
 * @version 2016-07-13
 * 
 */
public class MFSResource implements SerializableArtifact {
  
  public static class MFSResourceSerializer implements ArtifactSerializer<MFSResource> {

    public MFSResource create(InputStream in) throws IOException,
        InvalidFormatException {
      return new MFSResource(in);
    }

    public void serialize(MFSResource artifact, OutputStream out)
        throws IOException {
      artifact.serialize(out);
    }
  }
  
  /**
   * The dictionary for finding the MFS.
   */
  private ListMultimap<String, String> multiMap = ArrayListMultimap.create();
  
  /**
   * Build the MFS Dictionary.
   * @param in the input stream
   * @throws ClassNotFoundException 
   * @throws IOException the io exception
   */
  public MFSResource(InputStream in) throws IOException {
    try {
      ListMultimap<String, String> temp = IOUtils.readObjectFromInputStream(in);
      multiMap.putAll(temp);
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
  }
  
  /**
   * Extract most frequent sense baseline from WordNet data, using Ciaramita and
   * Altun's (2006) approach for a bio encoding.
   * 
   * @param lemmas
   *          in the sentence
   * @param posTags the postags of the sentence
   * @return the most frequent senses for the sentence
   */
  public List<String> getFirstSenseBio(List<String> lemmas, Span[] posTags) {

    List<String> mostFrequentSenseList = new ArrayList<String>();

    String prefix = "-" + BioCodec.START;
    String mostFrequentSense = null;
    String searchSpan = null;
    // iterative over lemmas from the beginning
    for (int i = 0; i < lemmas.size(); i++) {
      mostFrequentSense = null;
      String pos = posTags[i].getType();
      int j;
      // iterate over lemmas from the end
      for (j = lemmas.size() - 1; j >= i; j--) {
        // create span for search in multimap; the first search takes as span
        // the whole sentence
        String endPos = posTags[j].getType();
        searchSpan = createSpan(lemmas, i, j);
        String firstSpan = (searchSpan + "#" + pos.substring(0, 1))
            .toLowerCase();
        TreeMultimap<Integer, String> mfsMap = getOrderedMap(firstSpan);
        if (!mfsMap.isEmpty()) {
          mostFrequentSense = getMFS(mfsMap);
          break;
        }
        String lastSpan = (searchSpan + "#" + endPos.substring(0, 1))
            .toLowerCase();
        TreeMultimap<Integer, String> mfsMapEnd = getOrderedMap(lastSpan);
        if (!mfsMapEnd.isEmpty()) {
          mostFrequentSense = getMFS(mfsMapEnd);
          break;
        }
      }
      prefix = "-" + BioCodec.START;
      // multi-token case
      if (mostFrequentSense != null) {
        while (i < j) {
          mostFrequentSenseList.add((mostFrequentSense + prefix).intern());
          prefix = "-" + BioCodec.CONTINUE;
          i++;
        }
      }
      // one word case or last member of multispan
      if (mostFrequentSense != null) {
        mostFrequentSenseList.add((mostFrequentSense + prefix).intern());
      } else {
        mostFrequentSenseList.add(BioCodec.OTHER);
      }
    }
    return mostFrequentSenseList;
  }
  
  /**
   * Extract most frequent sense baseline from WordNet data, using Ciaramita and
   * Altun's (2006) approach for bilou encoding.
   * 
   * @param lemmas
   *          in the sentence
   * @param posTags posTags in the sentence
   * @return the most frequent senses for the sentence
   */
  public List<String> getFirstSenseBilou(List<String> lemmas, Span[] posTags) {

    List<String> mostFrequentSenseList = new ArrayList<String>();

    String prefix = "-" + BioCodec.START;
    String mostFrequentSense = null;
    String searchSpan = null;
    // iterative over lemmas from the beginning
    for (int i = 0; i < lemmas.size(); i++) {
      mostFrequentSense = null;
      String pos = posTags[i].getType();
      int j;
      // iterate over lemmas from the end
      for (j = lemmas.size() - 1; j >= i; j--) {
        // create span for search in multimap; the first search takes as span
        // the whole sentence
        String endPos = posTags[j].getType();
        searchSpan = createSpan(lemmas, i, j);
        String firstSpan = (searchSpan + "#" + pos.substring(0, 1))
            .toLowerCase();
        TreeMultimap<Integer, String> mfsMap = getOrderedMap(firstSpan);
        if (!mfsMap.isEmpty()) {
          mostFrequentSense = getMFS(mfsMap);
          break;
        }
        String lastSpan = (searchSpan + "#" + endPos.substring(0, 1))
            .toLowerCase();
        TreeMultimap<Integer, String> mfsMapEnd = getOrderedMap(lastSpan);
        if (!mfsMapEnd.isEmpty()) {
          mostFrequentSense = getMFS(mfsMapEnd);
          break;
        }
      }
      prefix = "-" + BilouCodec.START;
      // multi-token case
      if (mostFrequentSense != null) {
        while (i < j) {
          mostFrequentSenseList.add((mostFrequentSense + prefix).intern());
          prefix = "-" + BilouCodec.CONTINUE;
          i++;
        }
      }
      // one word case or last member of multi-span
      if (mostFrequentSense != null) {
        if (prefix.equals("-" + BilouCodec.CONTINUE)) {
          mostFrequentSenseList.add((mostFrequentSense + "-" + BilouCodec.LAST).intern());
        } else if (prefix.equals("-" + BilouCodec.START)) {
          mostFrequentSenseList.add((mostFrequentSense + "-" + BilouCodec.UNIT).intern());
        }
      } else {
        mostFrequentSenseList.add(BilouCodec.OTHER);
      }
    }
    return mostFrequentSenseList;
  }
  
  /**
   * Create lemma span for search of multiwords in MFS dictionary.
   * 
   * @param lemmas
   *          the lemmas of the sentence
   * @param from
   *          the starting index
   * @param to
   *          the end index
   * @return the string representing a perhaps multi word entry
   */
  private String createSpan(List<String> lemmas, int from, int to) {
    String lemmaSpan = "";
    for (int i = from; i < to; i++) {
      lemmaSpan += lemmas.get(i) + "_";
    }
    lemmaSpan += lemmas.get(to);
    return lemmaSpan;
  }
  
  /**
   * Get the ordered Map of most frequent senses for a lemma#pos entry.
   * @param lemmaPOSClass the lemma#pos entry
   * @return the ordered multimap of senses
   */
  public TreeMultimap<Integer, String> getOrderedMap(String lemmaPOSClass) {
    List<String> mfsList = multiMap.get(lemmaPOSClass);
    TreeMultimap<Integer, String> mfsMap = TreeMultimap.create(Ordering.natural().reverse(), Ordering.natural());
    if (!mfsList.isEmpty()) {
      getOrderedSenses(mfsList, mfsMap);
    }
    return mfsMap;
  }
  
  /**
   * Look-up lemma#pos string as key in dictionary.
   * @param mfsList the list containing the freq#sense values
   * @param mfsResultsMap the map in which the results are stored
   */
  public void getOrderedSenses(List<String> mfsList, TreeMultimap<Integer, String> mfsResultsMap) {
    if (!mfsList.isEmpty()) {
      for (String mfsResult : mfsList) {
        String[] mfsEntry = mfsResult.split("#");
        mfsResultsMap.put(Integer.valueOf(mfsEntry[0]), mfsEntry[1]);
      }
    }
  }
  
  /**
   * Get the MFS from a lemma#posClass entry, e.g., house#n.
   * @param mfsMap map to get the MFS from
   * @return the most frequent sense
   */
  public String getMFS(TreeMultimap<Integer, String> mfsMap) {
    SortedSet<String> mfs = mfsMap.get(mfsMap.keySet().first());
    return mfs.first();
   }
  
  /**
   * Get a rank of senses ordered by MFS. 
   * @param lemmaPOSClass the lemma#pos entry
   * @param rankSize the size of the rank
   * @return the ordered multimap containing the rank
   */
  public TreeMultimap<Integer, String> getMFSRanking(String lemmaPOSClass, Integer rankSize) {
    
    TreeMultimap<Integer, String> mfsResultsMap = getOrderedMap(lemmaPOSClass);
    TreeMultimap<Integer, String> mfsRankMap = TreeMultimap.create(Ordering.natural().reverse(), Ordering.natural());
    for (Map.Entry<Integer, String> freqSenseEntry : Iterables.limit(mfsResultsMap.entries(), rankSize)) {
      mfsRankMap.put(freqSenseEntry.getKey(), freqSenseEntry.getValue());
    }
    return mfsRankMap;
  }
 
  
  /**
   * Serialize the lexicon in the original format.
   * @param out the output stream
   * @throws IOException if io errors
   */
  public void serialize(OutputStream out) {
    IOUtils.writeObjectToStream(multiMap, out);
  }

  public Class<?> getArtifactSerializerClass() {
    return MFSResourceSerializer.class;
  }

}



