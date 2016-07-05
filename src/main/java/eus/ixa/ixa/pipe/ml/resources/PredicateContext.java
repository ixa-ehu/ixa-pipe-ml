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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.model.ArtifactSerializer;
import opennlp.tools.util.model.SerializableArtifact;


/**
 * 
 * Class to load a {@code PredicateContext} resource containing
 * predicate and window feature information about that predicate.
 * The format of the file contains: predicate, context predicate
 * and region mark in that order. For example:
 * set\tbeen set .\t1
 * @author ragerri
 * @version 2016/07/05
 * 
 */
public class PredicateContext implements SerializableArtifact {

  private static final Pattern tabPattern = Pattern.compile("\t");
  /**
   * Turkish capital letter I with dot.
   */
  public static final Pattern dotInsideI = Pattern.compile("\u0130", Pattern.UNICODE_CHARACTER_CLASS);
  
  public static class PredicateContextSerializer implements ArtifactSerializer<PredicateContext> {

    public PredicateContext create(InputStream in) throws IOException,
        InvalidFormatException {
      return new PredicateContext(in);
    }

    public void serialize(PredicateContext artifact, OutputStream out)
        throws IOException {
      artifact.serialize(out);
    }
  }

  List<ArrayList<String>> predicateContext = new ArrayList<ArrayList<String>>();

  public List<ArrayList<String>> getPredicateContext() {
    return predicateContext;
  }
  
  public PredicateContext(InputStream in) throws IOException {

    BufferedReader breader = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));
    String line;
    while ((line = breader.readLine()) != null) {
      String[] lineArray = tabPattern.split(line);
      populateLists(lineArray);
    }
  }
  
  private void populateLists(String[] lineArray) {
    if (lineArray.length == 3) {
      String normalizedToken = dotInsideI.matcher(lineArray[0]).replaceAll("i");
      String normalizedContext = dotInsideI.matcher(lineArray[1]).replaceAll("i");
      ArrayList<String> tokenValues = new ArrayList<>();
      tokenValues.add(normalizedToken);
      tokenValues.add(normalizedContext);
      tokenValues.add(lineArray[2]);
      predicateContext.add(tokenValues);
    }
  }

  /**
   * Serialize the dictionary to original corpus format
   * @param out the output stream
   * @throws IOException if io problems
   */
  public void serialize(OutputStream out) throws IOException {
    Writer writer = new BufferedWriter(new OutputStreamWriter(out));

    for (ArrayList<String> entry : predicateContext) {
      writer.write(entry.get(0) + "\t" + entry.get(1) + "\t" + entry.get(2) + "\n");
    }
    writer.flush();
  }

  public Class<?> getArtifactSerializerClass() {
    return PredicateContext.class;
  }
}


