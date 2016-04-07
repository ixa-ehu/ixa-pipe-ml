package eus.ixa.ixa.pipe.ml.lemma;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import eus.ixa.ixa.pipe.ml.utils.Span;

import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.model.ArtifactSerializer;
import opennlp.tools.util.model.SerializableArtifact;

/**
 * Lemmatize by simple dictionary lookup into a hashmap built from a file
 * containing, for each line, word\tablemma\tabpostag.
 * 
 * @author ragerri
 * @version 2014-07-08
 */
public class DictionaryLemmatizer implements SerializableArtifact {

private static final Pattern spacePattern = Pattern.compile("\t");
  
  public static class DictionaryLemmatizerSerializer implements ArtifactSerializer<DictionaryLemmatizer> {

    public DictionaryLemmatizer create(InputStream in) throws IOException,
        InvalidFormatException {
      return new DictionaryLemmatizer(in);
    }

    public void serialize(DictionaryLemmatizer artifact, OutputStream out)
        throws IOException {
      artifact.serialize(out);
    }
  }
  
  /**
   * The hashmap containing the dictionary.
   */
  private final HashMap<List<String>, String> dictMap;

  /**
   * Construct a hashmap from the input tab separated dictionary.
   * 
   * The input file should have, for each line, word\tablemma\tabpostag
   * 
   * @param dictionary
   *          the input dictionary via inputstream
   */
  public DictionaryLemmatizer(final InputStream dictionary) {
    this.dictMap = new HashMap<List<String>, String>();
    final BufferedReader breader = new BufferedReader(new InputStreamReader(
        dictionary));
    String line;
    try {
      while ((line = breader.readLine()) != null) {
        final String[] elems = spacePattern.split(line);
        this.dictMap.put(Arrays.asList(elems[0], elems[2]), elems[1]);
      }
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Get the Map containing the dictionary.
   * 
   * @return dictMap the Map
   */
  public HashMap<List<String>, String> getDictMap() {
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
  private List<String> getDictKeys(final String word, final String postag) {
    final List<String> keys = new ArrayList<String>();
    keys.addAll(Arrays.asList(word.toLowerCase(), postag));
    return keys;
  }
  
  public List<String> lemmatize(final String[] tokens, final Span[] postags) {
    List<String> lemmas = new ArrayList<String>();
    for (int i = 0; i < tokens.length; i++) {
      lemmas.add(this.apply(tokens[i], postags[i].getType())); 
    }
    return lemmas;
  }

  /**
   * Lookup lemma in a dictionary. Outputs "O" if not found.
   * @param word the token
   * @param postag the postag
   * @return the lemma
   */
  public String apply(final String word, final String postag) {
    String lemma = null;
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
  
  public void serialize(OutputStream out) throws IOException {
    Writer writer = new BufferedWriter(new OutputStreamWriter(out));

    for (Map.Entry<List<String>, String> entry : dictMap.entrySet()) {
      writer.write(entry.getKey().get(0) + "\t" + entry.getValue() + "\t" + entry.getKey().get(1) +"\n");
    }
    writer.flush();
  }

  public Class<?> getArtifactSerializerClass() {
    return DictionaryLemmatizerSerializer.class;
  }
  
}

