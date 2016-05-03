package eus.ixa.ixa.pipe.ml.parse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;

import opennlp.tools.util.model.SerializableArtifact;

public abstract class AbstractCollinsHeadRules implements HeadRules, GapLabeler, SerializableArtifact {

  private Map<String, HeadRule> headRules;
  private final Set<String> punctSet;

  
  public AbstractCollinsHeadRules(final Reader rulesReader) {
    final BufferedReader in = new BufferedReader(rulesReader);
    try {
      readHeadRules(in);
    } catch (IOException e) {
      e.printStackTrace();
    }
    this.punctSet = new HashSet<String>();
    this.punctSet.add(".");
    this.punctSet.add(",");
    this.punctSet.add("``");
    this.punctSet.add("''");
  }
  
  private void readHeadRules(final BufferedReader breader) throws IOException {
    String line;
    this.headRules = new HashMap<String, HeadRule>(60);
    while ((line = breader.readLine()) != null) {
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
  
  @Override
  public void labelGaps(Stack<Constituent> stack) {
  }

  @Override
  public Parse getHead(Parse[] constituents, String type) {
    return null;
  }

  @Override
  public Set<String> getPunctuationTags() {
    return this.punctSet;
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
      writer.write(Integer.toString(headRule.tags.length + 2));
      writer.write(' ');
      // write type
      writer.write(type);
      writer.write(' ');
      // write l2r true == 1
      if (headRule.leftToRight) {
        writer.write("1");
      } else {
        writer.write("0");
      }
      // write tags
      for (final String tag : headRule.tags) {
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
    } else if (obj instanceof AbstractCollinsHeadRules) {
      final AbstractCollinsHeadRules rules = (AbstractCollinsHeadRules) obj;

      return rules.headRules.equals(this.headRules)
          && rules.punctSet.equals(this.punctSet);
    } else {
      return false;
    }
  }

  private static class HeadRule {
    public boolean leftToRight;
    public String[] tags;

    public HeadRule(final boolean l2r, final String[] tags) {
      this.leftToRight = l2r;

      for (final String tag : tags) {
        if (tag == null) {
          throw new IllegalArgumentException(
              "tags must not contain null values!");
        }
      }

      this.tags = tags;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      } else if (obj instanceof HeadRule) {
        final HeadRule rule = (HeadRule) obj;

        return rule.leftToRight == this.leftToRight
            && Arrays.equals(rule.tags, this.tags);
      } else {
        return false;
      }
    }
  }
  
  @Override
  public int hashCode() {
    assert false : "hashCode not designed";
    return 42; // any arbitrary constant will do
  }

}
