/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eus.ixa.ixa.pipe.ml.parse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;

import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.model.ArtifactSerializer;
import opennlp.tools.util.model.SerializableArtifact;

/**
 * Class for storing the English head rules associated with parsing. The
 * headrules are specified in $src/main/resources/en-head-rules
 *
 * This class is inspired by opennlp.tools.parser.lang.en.HeadRules.java.
 * @author ragerri
 * @version 2015-05-06
 */
public class PennTreebankHeadRules implements HeadRules, GapLabeler, SerializableArtifact {

  public static class PennTreebankHeadRulesSerializer implements ArtifactSerializer<PennTreebankHeadRules> {

    public PennTreebankHeadRules create(InputStream in) throws IOException,
        InvalidFormatException {
      return new PennTreebankHeadRules(new BufferedReader(new InputStreamReader(in, "UTF-8")));
    }

    public void serialize(PennTreebankHeadRules artifact, OutputStream out)
        throws IOException {
      artifact.serialize(new OutputStreamWriter(out, "UTF-8"));
    }
  }
  
  private Map<String, HeadRule> headRules;
  private final Set<String> punctSet;

  /**
   * Creates a new set of head rules based on the specified reader.
   * 
   * @param rulesReader
   *          the head rules reader.
   * 
   * @throws IOException
   *           if the head rules reader can not be read.
   */
  public PennTreebankHeadRules(final Reader rulesReader) throws IOException {
    final BufferedReader in = new BufferedReader(rulesReader);
    readHeadRules(in);

    this.punctSet = new HashSet<String>();
    this.punctSet.add(".");
    this.punctSet.add(",");
    this.punctSet.add("``");
    this.punctSet.add("''");
    // punctSet.add(":");
  }
  
  private void readHeadRules(final BufferedReader str) throws IOException {
    String line;
    this.headRules = new HashMap<String, HeadRule>(30);
    while ((line = str.readLine()) != null) {
      final StringTokenizer st = new StringTokenizer(line);
      final String num = st.nextToken();
      final String type = st.nextToken();
      final String dir = st.nextToken();
      final String[] tags = new String[Integer.parseInt(num) - 2];
      int ti = 0;
      while (st.hasMoreTokens()) {
        tags[ti] = st.nextToken();
        ti++;
      }
      this.headRules.put(type, new HeadRule(dir.equals("1"), tags));
    }
  }


  public Set<String> getPunctuationTags() {
    return this.punctSet;
  }

  public Parse getHead(final Parse[] constituents, final String type) {
    if (constituents[0].getType() == ShiftReduceParser.TOK_NODE) {
      return null;
    }
    HeadRule hr;
    //right
    if (type.equals("NP") || type.equals("NX")) {
      final String[] tags1 = { "NN", "NNP", "NNPS", "NNS", "NX", "JJR", "POS" };
      for (int ci = constituents.length - 1; ci >= 0; ci--) {
        for (int ti = tags1.length - 1; ti >= 0; ti--) {
          if (constituents[ci].getType().equals(tags1[ti])) {
            return constituents[ci];
          }
        }
      }
      //left
      for (final Parse constituent : constituents) {
        if (constituent.getType().equals("NP")) {
          return constituent;
        }
      }
      //right
      final String[] tags2 = { "$", "ADJP", "PRN" };
      for (int ci = constituents.length - 1; ci >= 0; ci--) {
        for (int ti = tags2.length - 1; ti >= 0; ti--) {
          if (constituents[ci].getType().equals(tags2[ti])) {
            return constituents[ci];
          }
        }
      }
      //right
      for (int ci = constituents.length - 1; ci >= 0; ci--) {
        if (constituents[ci].getType().equals("CD")) {
          return constituents[ci];
        }
      }
      //right
      final String[] tags3 = { "JJ", "JJS", "RB", "QP" };
      for (int ci = constituents.length - 1; ci >= 0; ci--) {
        for (int ti = tags3.length - 1; ti >= 0; ti--) {
          if (constituents[ci].getType().equals(tags3[ti])) {
            return constituents[ci];
          }
        }
      }
      //else return the last word
      return constituents[constituents.length - 1];
    } else if ((hr = this.headRules.get(type)) != null) {
      final String[] tags = hr.getTags();
      final int cl = constituents.length;
      final int tl = tags.length;
      if (hr.isLeftToRight()) {
        for (int ti = 0; ti < tl; ti++) {
          for (int ci = 0; ci < cl; ci++) {
            if (constituents[ci].getType().equals(tags[ti])) {
              return constituents[ci];
            }
          }
        }
        //else return the first constituent
        return constituents[0];
      } else {
        //right
        for (int ti = 0; ti < tl; ti++) {
          for (int ci = cl - 1; ci >= 0; ci--) {
            if (constituents[ci].getType().equals(tags[ti])) {
              return constituents[ci];
            }
          }
        }
        //else return the last constituent
        return constituents[cl - 1];
      }
    }
    return constituents[constituents.length - 1];
  }

  public void labelGaps(final Stack<Constituent> stack) {
    if (stack.size() > 4) {
      // Constituent con0 = (Constituent) stack.get(stack.size()-1);
      final Constituent con1 = stack.get(stack.size() - 2);
      final Constituent con2 = stack.get(stack.size() - 3);
      final Constituent con3 = stack.get(stack.size() - 4);
      final Constituent con4 = stack.get(stack.size() - 5);
      // System.err.println("con0="+con0.label+" con1="+con1.label+" con2="+con2.label+" con3="+con3.label+" con4="+con4.label);
      // subject extraction
      if (con1.getLabel().equals("NP") && con2.getLabel().equals("S")
          && con3.getLabel().equals("SBAR")) {
        con1.setLabel(con1.getLabel() + "-G");
        con2.setLabel(con2.getLabel() + "-G");
        con3.setLabel(con3.getLabel() + "-G");
      }
      // object extraction
      else if (con1.getLabel().equals("NP") && con2.getLabel().equals("VP")
          && con3.getLabel().equals("S") && con4.getLabel().equals("SBAR")) {
        con1.setLabel(con1.getLabel() + "-G");
        con2.setLabel(con2.getLabel() + "-G");
        con3.setLabel(con3.getLabel() + "-G");
        con4.setLabel(con4.getLabel() + "-G");
      }
    }
  }

  /**
   * Writes the head rules to the writer in a format suitable for loading the
   * head rules again with the constructor. The encoding must be taken into
   * account while working with the writer and reader.
   * <p>
   * After the entries have been written, the writer is flushed. The writer
   * remains open after this method returns.
   * 
   * @param writer
   *          the writer
   * @throws IOException
   *           if io exception
   */
  public void serialize(final Writer writer) throws IOException {

    for (final String type : this.headRules.keySet()) {
      final HeadRule headRule = this.headRules.get(type);
      // write num of tags
      writer.write(Integer.toString(headRule.getTags().length + 2));
      writer.write(' ');
      // write type
      writer.write(type);
      writer.write(' ');
      // write l2r true == 1
      if (headRule.isLeftToRight()) {
        writer.write("1");
      } else {
        writer.write("0");
      }
      // write tags
      for (final String tag : headRule.getTags()) {
        writer.write(' ');
        writer.write(tag);
      }
      writer.write('\n');
    }
    writer.flush();
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof PennTreebankHeadRules) {
      final PennTreebankHeadRules rules = (PennTreebankHeadRules) obj;

      return rules.headRules.equals(this.headRules)
          && rules.punctSet.equals(this.punctSet);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    assert false : "hashCode not designed";
    return 42; // any arbitrary constant will do
  }
  
  @Override
  public Class<?> getArtifactSerializerClass() {
    return PennTreebankHeadRulesSerializer.class;
  }
}
