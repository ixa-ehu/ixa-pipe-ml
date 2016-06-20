package eus.ixa.ixa.pipe.ml.resources;

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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.model.ArtifactSerializer;
import opennlp.tools.util.model.SerializableArtifact;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;


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
  
  private Multimap<String, String> dictMultiMap = ArrayListMultimap.create();

  public POSDictionary(InputStream in) throws IOException {

    BufferedReader breader = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));
    String line;
	System.err.println("Creating POS Dictionary...");
    while ((line = breader.readLine()) != null) {
      String[] lineArray = tabPattern.split(line);
      if (lineArray.length == 2) {
        String normalizedToken = dotInsideI.matcher(lineArray[0]).replaceAll("i");
        dictMultiMap.put(normalizedToken, lineArray[1].intern());
      }
    }
    System.err.println(dictMultiMap.size());
System.err.println("POS Dictionary done!");
  }

  public Collection<String> getTags(String string) {
    return dictMultiMap.get(string);
  }
  
  public Multimap<String, String> getMap() {
    return dictMultiMap;
  }

  /**
   * Serialize the dictionary to original corpus format
   * @param out the output stream
   * @throws IOException if io problems
   */
  public void serialize(OutputStream out) throws IOException {
    Writer writer = new BufferedWriter(new OutputStreamWriter(out));

    for (Map.Entry<String, String> entry : dictMultiMap.entries()) {
      writer.write(entry.getKey() + "\t" + entry.getValue() + "\n");
    }

    writer.flush();
  }

  public Class<?> getArtifactSerializerClass() {
    return POSDictionarySerializer.class;
  }
}


