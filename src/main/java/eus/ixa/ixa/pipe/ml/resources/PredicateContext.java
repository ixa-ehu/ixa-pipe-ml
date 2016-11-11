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
 * Class to load a {@code PredicateContext} resource containing predicate and
 * window feature information about that predicate. The format of the file
 * contains: predicate, context predicate and region mark in that order. For
 * example: set\tbeen set .\t1
 * 
 * @author ragerri
 * @version 2016/07/05
 *
 */
public class PredicateContext implements SerializableArtifact {

  private static final Pattern tabPattern = Pattern.compile("\t");
  /**
   * Turkish capital letter I with dot.
   */
  public static final Pattern dotInsideI = Pattern.compile("\u0130",
      Pattern.UNICODE_CHARACTER_CLASS);

  public static class PredicateContextSerializer
      implements ArtifactSerializer<PredicateContext> {

    @Override
    public PredicateContext create(final InputStream in)
        throws IOException, InvalidFormatException {
      return new PredicateContext(in);
    }

    @Override
    public void serialize(final PredicateContext artifact,
        final OutputStream out) throws IOException {
      artifact.serialize(out);
    }
  }

  List<List<String>> predContexts = new ArrayList<List<String>>();

  public List<List<String>> getPredicateContext() {
    return this.predContexts;
  }

  public PredicateContext(final InputStream in) throws IOException {

    final BufferedReader breader = new BufferedReader(
        new InputStreamReader(in, Charset.forName("UTF-8")));
    String line;
    while ((line = breader.readLine()) != null) {
      final String[] lineArray = tabPattern.split(line);
      populateLists(lineArray);
    }
  }

  private void populateLists(final String[] lineArray) {
    if (lineArray.length == 3) {
      final String normalizedToken = dotInsideI.matcher(lineArray[0])
          .replaceAll("i");
      final String normalizedContext = dotInsideI.matcher(lineArray[1])
          .replaceAll("i");
      final List<String> tokenValues = new ArrayList<>();
      tokenValues.add(normalizedToken);
      tokenValues.add(normalizedContext);
      tokenValues.add(lineArray[2]);
      this.predContexts.add(tokenValues);
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

    for (final List<String> entry : this.predContexts) {
      writer.write(
          entry.get(0) + "\t" + entry.get(1) + "\t" + entry.get(2) + "\n");
    }
    writer.flush();
  }

  @Override
  public Class<?> getArtifactSerializerClass() {
    return PredicateContextSerializer.class;
  }
}
