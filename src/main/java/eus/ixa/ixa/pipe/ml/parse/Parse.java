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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eus.ixa.ixa.pipe.ml.utils.Span;

/**
 * Data structure for holding parse constituents.
 */
public class Parse implements Cloneable, Comparable<Parse> {

  public static final String BRACKET_LRB = "(";
  public static final String BRACKET_RRB = ")";
  public static final String BRACKET_LCB = "{";
  public static final String BRACKET_RCB = "}";
  public static final String BRACKET_LSB = "[";
  public static final String BRACKET_RSB = "]";

  /**
   * The text string on which this parse is based. This object is shared among
   * all parses for the same sentence.
   */
  private final String text;

  /**
   * The character offsets into the text for this constituent.
   */
  private Span span;

  /**
   * The syntactic type of this parse.
   */
  private String type;

  /**
   * The sub-constituents of this parse.
   */
  private List<Parse> parts;

  /**
   * The head parse of this parse. A parse can be its own head.
   */
  private Parse head;

  /**
   * A string used during parse construction to specify which stage of parsing
   * has been performed on this node.
   */
  private String label;

  /**
   * Index in the sentence of the head of this constituent.
   */
  private int headIndex;

  /**
   * The parent parse of this parse.
   */
  private Parse parent;

  /**
   * The probability associated with the syntactic type assigned to this parse.
   */
  private double prob;

  /**
   * The string buffer used to track the derivation of this parse.
   */
  private StringBuffer derivation;

  /**
   * Specifies whether this constituent was built during the chunking phase.
   */
  private boolean isChunk;

  /**
   * The pattern used to find the base constituent label of a Penn Treebank
   * labeled constituent.
   */
  private static Pattern typePattern = Pattern.compile("^([^ =-]+)");

  /**
   * The pattern used to find the function tags.
   */
  private static Pattern funTypePattern = Pattern.compile("^[^ =-]+-([^ =-]+)");

  /**
   * The patter used to identify tokens in Penn Treebank labeled constituents.
   */
  private static Pattern tokenPattern = Pattern
      .compile("^[^ ()]+ ([^ ()]+)\\s*\\)");

  /**
   * The set of punctuation parses which are between this parse and the previous
   * parse.
   */
  private Collection<Parse> prevPunctSet;

  /**
   * The set of punctuation parses which are between this parse and the
   * subsequent parse.
   */
  private Collection<Parse> nextPunctSet;

  /**
   * Specifies whether constituent labels should include parts specified after
   * minus character.
   */
  private static boolean useFunctionTags;

  /**
   * Creates a new parse node for this specified text and span of the specified
   * type with the specified probability and the specified head index.
   *
   * @param text
   *          The text of the sentence for which this node is a part of.
   * @param span
   *          The character offsets for this node within the specified text.
   * @param type
   *          The constituent label of this node.
   * @param p
   *          The probability of this parse.
   * @param index
   *          The token index of the head of this parse.
   */
  public Parse(final String text, final Span span, final String type,
      final double p, final int index) {
    this.text = text;
    this.span = span;
    this.type = type;
    this.prob = p;
    this.head = this;
    this.headIndex = index;
    this.parts = new LinkedList<Parse>();
    this.label = null;
    this.parent = null;
  }

  /**
   * Creates a new parse node for this specified text and span of the specified
   * type with the specified probability and the specified head and head index.
   *
   * @param text
   *          The text of the sentence for which this node is a part of.
   * @param span
   *          The character offsets for this node within the specified text.
   * @param type
   *          The constituent label of this node.
   * @param p
   *          The probability of this parse.
   * @param h
   *          The head token of this parse.
   */
  public Parse(final String text, final Span span, final String type,
      final double p, final Parse h) {
    this(text, span, type, p, 0);
    if (h != null) {
      this.head = h;
      this.headIndex = h.headIndex;
    }
  }

  @Override
  public Object clone() {
    final Parse p = new Parse(this.text, this.span, this.type, this.prob,
        this.head);
    p.parts = new LinkedList<Parse>();
    p.parts.addAll(this.parts);

    if (this.derivation != null) {
      p.derivation = new StringBuffer(100);
      p.derivation.append(this.derivation.toString());
    }
    p.label = this.label;
    return p;
  }

  /**
   * Clones the right frontier of parse up to the specified node.
   *
   * @param node
   *          The last node in the right frontier of the parse tree which should
   *          be cloned.
   * @return A clone of this parse and its right frontier up to and including
   *         the specified node.
   */
  public Parse clone(final Parse node) {
    if (this == node) {
      return (Parse) this.clone();
    } else {
      final Parse c = (Parse) this.clone();
      final Parse lc = c.parts.get(this.parts.size() - 1);
      c.parts.set(this.parts.size() - 1, lc.clone(node));
      return c;
    }
  }

  /**
   * Clones the right frontier of this root parse up to and including the
   * specified node.
   *
   * @param node
   *          The last node in the right frontier of the parse tree which should
   *          be cloned.
   * @param parseIndex
   *          The child index of the parse for this root node.
   * @return A clone of this root parse and its right frontier up to and
   *         including the specified node.
   */
  public Parse cloneRoot(final Parse node, final int parseIndex) {
    final Parse c = (Parse) this.clone();
    final Parse fc = c.parts.get(parseIndex);
    c.parts.set(parseIndex, fc.clone(node));
    return c;
  }

  /**
   * Specifies whether function tags should be included as part of the
   * constituent type.
   *
   * @param uft
   *          true is they should be included; false otherwise.
   */
  public static void useFunctionTags(final boolean uft) {
    useFunctionTags = uft;
  }

  /**
   * Set the type of this constituent to the specified type.
   *
   * @param type
   *          The type of this constituent.
   */
  public void setType(final String type) {
    this.type = type;
  }

  /**
   * Returns the constituent label for this node of the parse.
   *
   * @return The constituent label for this node of the parse.
   */
  public String getType() {
    return this.type;
  }

  /**
   * Returns the set of punctuation parses that occur immediately before this
   * parse.
   *
   * @return the set of punctuation parses that occur immediately before this
   *         parse.
   */
  public Collection<Parse> getPreviousPunctuationSet() {
    return this.prevPunctSet;
  }

  /**
   * Designates that the specified punctuation should is prior to this parse.
   *
   * @param punct
   *          The punctuation.
   */
  public void addPreviousPunctuation(final Parse punct) {
    if (this.prevPunctSet == null) {
      this.prevPunctSet = new TreeSet<Parse>();
    }
    this.prevPunctSet.add(punct);
  }

  /**
   * Returns the set of punctuation parses that occur immediately after this
   * parse.
   *
   * @return the set of punctuation parses that occur immediately after this
   *         parse.
   */
  public Collection<Parse> getNextPunctuationSet() {
    return this.nextPunctSet;
  }

  /**
   * Designates that the specified punctuation follows this parse.
   *
   * @param punct
   *          The punctuation set.
   */
  public void addNextPunctuation(final Parse punct) {
    if (this.nextPunctSet == null) {
      this.nextPunctSet = new TreeSet<Parse>();
    }
    this.nextPunctSet.add(punct);
  }

  /**
   * Sets the set of punctuation tags which follow this parse.
   *
   * @param punctSet
   *          The set of punctuation tags which follow this parse.
   */
  public void setNextPunctuation(final Collection<Parse> punctSet) {
    this.nextPunctSet = punctSet;
  }

  /**
   * Sets the set of punctuation tags which preceed this parse.
   *
   * @param punctSet
   *          The set of punctuation tags which preceed this parse.
   */
  public void setPrevPunctuation(final Collection<Parse> punctSet) {
    this.prevPunctSet = punctSet;
  }

  /**
   * Inserts the specified constituent into this parse based on its text
   * span.This method assumes that the specified constituent can be inserted
   * into this parse.
   *
   * @param constituent
   *          The constituent to be inserted.
   */
  public void insert(final Parse constituent) {
    final Span ic = constituent.span;
    if (this.span.contains(ic)) {
      // double oprob=c.prob;
      int pi = 0;
      int pn = this.parts.size();
      for (; pi < pn; pi++) {
        final Parse subPart = this.parts.get(pi);
        // System.err.println("Parse.insert:con="+constituent+" sp["+pi+"]
        // "+subPart+" "+subPart.getType());
        final Span sp = subPart.span;
        if (sp.getStart() >= ic.getEnd()) {
          break;
        }
        // constituent contains subPart
        else if (ic.contains(sp)) {
          // System.err.println("Parse.insert:con contains subPart");
          this.parts.remove(pi);
          pi--;
          constituent.parts.add(subPart);
          subPart.setParent(constituent);
          // System.err.println("Parse.insert: "+subPart.hashCode()+" ->
          // "+subPart.getParent().hashCode());
          pn = this.parts.size();
        } else if (sp.contains(ic)) {
          // System.err.println("Parse.insert:subPart contains con");
          subPart.insert(constituent);
          return;
        }
      }
      // System.err.println("Parse.insert:adding con="+constituent+" to "+this);
      this.parts.add(pi, constituent);
      constituent.setParent(this);
      // System.err.println("Parse.insert: "+constituent.hashCode()+" ->
      // "+constituent.getParent().hashCode());
    } else {
      throw new IllegalArgumentException(
          "Inserting constituent not contained in the sentence!");
    }
  }

  /**
   * Appends the specified string buffer with a string representation of this
   * parse.
   *
   * @param sb
   *          A string buffer into which the parse string can be appended.
   */
  public void show(final StringBuffer sb) {
    int start;
    start = this.span.getStart();
    if (!this.type.equals(ShiftReduceParser.TOK_NODE)) {
      sb.append("(");
      sb.append(this.type).append(" ");
      // System.out.print(label+" ");
      // System.out.print(head+" ");
      // System.out.print(df.format(prob)+" ");
    }
    for (Parse c : this.parts) {
      final Span s = c.span;
      if (start < s.getStart()) {
        // System.out.println("pre "+start+" "+s.getStart());
        sb.append(encodeToken(this.text.substring(start, s.getStart())));
      }
      c.show(sb);
      start = s.getEnd();
    }
    if (start < this.span.getEnd()) {
      sb.append(encodeToken(this.text.substring(start, this.span.getEnd())));
    }
    if (!this.type.equals(ShiftReduceParser.TOK_NODE)) {
      sb.append(")");
    }
  }

  /**
   * Displays this parse using Penn Treebank-style formatting.
   */
  public void show() {
    final StringBuffer sb = new StringBuffer(this.text.length() * 4);
    show(sb);
    System.out.println(sb);
  }

  /**
   * Returns the probability associated with the pos-tag sequence assigned to
   * this parse.
   *
   * @return The probability associated with the pos-tag sequence assigned to
   *         this parse.
   */
  public double getTagSequenceProb() {
    // System.err.println("Parse.getTagSequenceProb: "+type+" "+this);
    if (this.parts.size() == 1
        && this.parts.get(0).type.equals(ShiftReduceParser.TOK_NODE)) {
      // System.err.println(this+" "+prob);
      return Math.log(this.prob);
    } else if (this.parts.size() == 0) {
      System.err.println("Parse.getTagSequenceProb: Wrong base case!");
      return 0.0;
    } else {
      double sum = 0.0;
      for (final Parse parse : this.parts) {
        sum += parse.getTagSequenceProb();
      }
      return sum;
    }
  }

  /**
   * Returns whether this parse is complete.
   *
   * @return Returns true if the parse contains a single top-most node.
   */
  public boolean complete() {
    return this.parts.size() == 1;
  }

  public String getCoveredText() {
    return this.text.substring(this.span.getStart(), this.span.getEnd());
  }

  /**
   * Represents this parse in a human readable way.
   */
  @Override
  public String toString() {
    // TODO: Use the commented code in next bigger release,
    // change probably breaks backward compatibility in some
    // applications
    // StringBuffer buffer = new StringBuffer();
    // show(buffer);
    // return buffer.toString();
    return getCoveredText();
  }

  /**
   * Returns the text of the sentence over which this parse was formed.
   *
   * @return The text of the sentence over which this parse was formed.
   */
  public String getText() {
    return this.text;
  }

  /**
   * Returns the character offsets for this constituent.
   *
   * @return The character offsets for this constituent.
   */
  public Span getSpan() {
    return this.span;
  }

  /**
   * Returns the log of the product of the probability associated with all the
   * decisions which formed this constituent.
   *
   * @return The log of the product of the probability associated with all the
   *         decisions which formed this constituent.
   */
  public double getProb() {
    return this.prob;
  }

  /**
   * Adds the specified probability log to this current log for this parse.
   *
   * @param logProb
   *          The probability of an action performed on this parse.
   */
  public void addProb(final double logProb) {
    this.prob += logProb;
  }

  /**
   * Returns the child constituents of this constituent .
   * 
   * @return The child constituents of this constituent.
   */
  public Parse[] getChildren() {
    return this.parts.toArray(new Parse[this.parts.size()]);
  }

  /**
   * Replaces the child at the specified index with a new child with the
   * specified label.
   *
   * @param index
   *          The index of the child to be replaced.
   * @param label
   *          The label to be assigned to the new child.
   */
  public void setChild(final int index, final String label) {
    final Parse newChild = (Parse) this.parts.get(index).clone();
    newChild.setLabel(label);
    this.parts.set(index, newChild);
  }

  public void add(final Parse daughter, final HeadRules rules) {
    if (daughter.prevPunctSet != null) {
      this.parts.addAll(daughter.prevPunctSet);
    }
    this.parts.add(daughter);
    this.span = new Span(this.span.getStart(), daughter.getSpan().getEnd());
    this.head = rules.getHead(getChildren(), this.type);
    if (this.head == null) {
      System.err.println(this.parts);
    }
    this.headIndex = this.head.headIndex;
  }

  public void remove(final int index) {
    this.parts.remove(index);
    if (!this.parts.isEmpty()) {
      if (index == 0 || index == this.parts.size()) { // size is orig last
                                                      // element
        this.span = new Span(this.parts.get(0).span.getStart(),
            this.parts.get(this.parts.size() - 1).span.getEnd());
      }
    }
  }

  public Parse adjoinRoot(final Parse node, final HeadRules rules,
      final int parseIndex) {
    final Parse lastChild = this.parts.get(parseIndex);
    final Parse adjNode = new Parse(this.text,
        new Span(lastChild.getSpan().getStart(), node.getSpan().getEnd()),
        lastChild.getType(), 1,
        rules.getHead(new Parse[] { lastChild, node }, lastChild.getType()));
    adjNode.parts.add(lastChild);
    if (node.prevPunctSet != null) {
      adjNode.parts.addAll(node.prevPunctSet);
    }
    adjNode.parts.add(node);
    this.parts.set(parseIndex, adjNode);
    return adjNode;
  }

  /**
   * Sister adjoins this node's last child and the specified sister node and
   * returns their new parent node. The new parent node replace this nodes last
   * child.
   *
   * @param sister
   *          The node to be adjoined.
   * @param rules
   *          The head rules for the parser.
   * @return The new parent node of this node and the specified sister node.
   */
  public Parse adjoin(final Parse sister, final HeadRules rules) {
    final Parse lastChild = this.parts.get(this.parts.size() - 1);
    final Parse adjNode = new Parse(this.text,
        new Span(lastChild.getSpan().getStart(), sister.getSpan().getEnd()),
        lastChild.getType(), 1,
        rules.getHead(new Parse[] { lastChild, sister }, lastChild.getType()));
    adjNode.parts.add(lastChild);
    if (sister.prevPunctSet != null) {
      adjNode.parts.addAll(sister.prevPunctSet);
    }
    adjNode.parts.add(sister);
    this.parts.set(this.parts.size() - 1, adjNode);
    this.span = new Span(this.span.getStart(), sister.getSpan().getEnd());
    this.head = rules.getHead(getChildren(), this.type);
    this.headIndex = this.head.headIndex;
    return adjNode;
  }

  public void expandTopNode(final Parse root) {
    boolean beforeRoot = true;
    // System.err.println("expandTopNode: parts="+parts);
    for (int pi = 0, ai = 0; pi < this.parts.size(); pi++, ai++) {
      final Parse node = this.parts.get(pi);
      if (node == root) {
        beforeRoot = false;
      } else if (beforeRoot) {
        root.parts.add(ai, node);
        this.parts.remove(pi);
        pi--;
      } else {
        root.parts.add(node);
        this.parts.remove(pi);
        pi--;
      }
    }
    root.updateSpan();
  }

  /**
   * Returns the number of children for this parse node.
   *
   * @return the number of children for this parse node.
   */
  public int getChildCount() {
    return this.parts.size();
  }

  /**
   * Returns the index of this specified child.
   *
   * @param child
   *          A child of this parse.
   *
   * @return the index of this specified child or -1 if the specified child is
   *         not a child of this parse.
   */
  public int indexOf(final Parse child) {
    return this.parts.indexOf(child);
  }

  /**
   * Returns the head constituent associated with this constituent.
   *
   * @return The head constituent associated with this constituent.
   */
  public Parse getHead() {
    return this.head;
  }

  /**
   * Returns the index within a sentence of the head token for this parse.
   *
   * @return The index within a sentence of the head token for this parse.
   */
  public int getHeadIndex() {
    return this.headIndex;
  }

  /**
   * Returns the label assigned to this parse node during parsing which
   * specifies how this node will be formed into a constituent.
   *
   * @return The outcome label assigned to this node during parsing.
   */
  public String getLabel() {
    return this.label;
  }

  /**
   * Assigns this parse the specified label. This is used by parsing schemes to
   * tag parsing nodes while building.
   *
   * @param label
   *          A label indicating something about the stage of building for this
   *          parse node.
   */
  public void setLabel(final String label) {
    this.label = label;
  }

  private static String getType(final String rest) {
    if (rest.startsWith("-LCB-")) {
      return "-LCB-";
    } else if (rest.startsWith("-RCB-")) {
      return "-RCB-";
    } else if (rest.startsWith("-LRB-")) {
      return "-LRB-";
    } else if (rest.startsWith("-RRB-")) {
      return "-RRB-";
    } else if (rest.startsWith("-RSB-")) {
      return "-RSB-";
    } else if (rest.startsWith("-LSB-")) {
      return "-LSB-";
    }

    else if (rest.startsWith("-NONE-")) {
      return "-NONE-";
    } else {
      final Matcher typeMatcher = typePattern.matcher(rest);
      if (typeMatcher.find()) {
        String type = typeMatcher.group(1);
        if (useFunctionTags) {
          final Matcher funMatcher = funTypePattern.matcher(rest);
          if (funMatcher.find()) {
            final String ftag = funMatcher.group(1);
            type = type + "-" + ftag;
          }
        }
        return type;
      }
    }
    return null;
  }

  private static String encodeToken(final String token) {
    if (BRACKET_LRB.equals(token)) {
      return "-LRB-";
    } else if (BRACKET_RRB.equals(token)) {
      return "-RRB-";
    } else if (BRACKET_LCB.equals(token)) {
      return "-LCB-";
    } else if (BRACKET_RCB.equals(token)) {
      return "-RCB-";
    } else if (BRACKET_LSB.equals(token)) {
      return "-LSB-";
    } else if (BRACKET_RSB.equals(token)) {
      return "-RSB-";
    }

    return token;
  }

  private static String decodeToken(final String token) {
    if ("-LRB-".equals(token)) {
      return BRACKET_LRB;
    } else if ("-RRB-".equals(token)) {
      return BRACKET_RRB;
    } else if ("-LCB-".equals(token)) {
      return BRACKET_LCB;
    } else if ("-RCB-".equals(token)) {
      return BRACKET_RCB;
    } else if ("-LSB-".equals(token)) {
      return BRACKET_LSB;
    } else if ("-RSB-".equals(token)) {
      return BRACKET_RSB;
    }

    return token;
  }

  /**
   * Returns the string containing the token for the specified portion of the
   * parse string or null if the portion of the parse string does not represent
   * a token.
   *
   * @param rest
   *          The portion of the parse string remaining to be processed.
   *
   * @return The string containing the token for the specified portion of the
   *         parse string or null if the portion of the parse string does not
   *         represent a token.
   */
  private static String getToken(final String rest) {
    final Matcher tokenMatcher = tokenPattern.matcher(rest);
    if (tokenMatcher.find()) {
      return decodeToken(tokenMatcher.group(1));
    }
    return null;
  }

  /**
   * Computes the head parses for this parse and its sub-parses and stores this
   * information in the parse data structure.
   *
   * @param rules
   *          The head rules which determine how the head of the parse is
   *          computed.
   */
  public void updateHeads(final HeadRules rules) {
    if (this.parts != null && this.parts.size() != 0) {
      for (int pi = 0, pn = this.parts.size(); pi < pn; pi++) {
        final Parse c = this.parts.get(pi);
        c.updateHeads(rules);
      }
      this.head = rules
          .getHead(this.parts.toArray(new Parse[this.parts.size()]), this.type);
      if (this.head == null) {
        this.head = this;
      } else {
        this.headIndex = this.head.headIndex;
      }
    } else {
      this.head = this;
    }
  }

  public void updateSpan() {
    this.span = new Span(this.parts.get(0).span.getStart(),
        this.parts.get(this.parts.size() - 1).span.getEnd());
  }

  /**
   * Prune the specified sentence parse of vacuous productions.
   *
   * @param parse
   *          the parse tree
   */
  public static void pruneParse(final Parse parse) {
    final List<Parse> nodes = new LinkedList<Parse>();
    nodes.add(parse);
    while (nodes.size() != 0) {
      final Parse node = nodes.remove(0);
      final Parse[] children = node.getChildren();
      if (children.length == 1
          && node.getType().equals(children[0].getType())) {
        final int index = node.getParent().parts.indexOf(node);
        children[0].setParent(node.getParent());
        node.getParent().parts.set(index, children[0]);
        node.parent = null;
        node.parts = null;
      }
      nodes.addAll(Arrays.asList(children));
    }
  }

  public static void fixPossesives(final Parse parse) {
    final Parse[] tags = parse.getTagNodes();
    for (int ti = 0; ti < tags.length; ti++) {
      if (tags[ti].getType().equals("POS")) {
        if (ti + 1 < tags.length
            && tags[ti + 1].getParent() == tags[ti].getParent().getParent()) {
          final int start = tags[ti + 1].getSpan().getStart();
          int end = tags[ti + 1].getSpan().getEnd();
          for (int npi = ti + 2; npi < tags.length; npi++) {
            if (tags[npi].getParent() == tags[npi - 1].getParent()) {
              end = tags[npi].getSpan().getEnd();
            } else {
              break;
            }
          }
          final Parse npPos = new Parse(parse.getText(), new Span(start, end),
              "NP", 1, tags[ti + 1]);
          parse.insert(npPos);
        }
      }
    }
  }

  /**
   * Parses the specified tree-bank style parse string and return a Parse
   * structure for that string.
   *
   * @param parse
   *          A tree-bank style parse string.
   *
   * @return a Parse structure for the specified tree-bank style parse string.
   */
  public static Parse parseParse(final String parse) {
    return parseParse(parse, null);
  }

  /**
   * Parses the specified tree-bank style parse string and return a Parse
   * structure for that string.
   *
   * @param parse
   *          A tree-bank style parse string.
   * @param gl
   *          The gap labeler.
   *
   * @return a Parse structure for the specified tree-bank style parse string.
   */
  public static Parse parseParse(final String parse, final GapLabeler gl) {
    final StringBuilder text = new StringBuilder();
    int offset = 0;
    final Stack<Constituent> stack = new Stack<Constituent>();
    final List<Constituent> cons = new LinkedList<Constituent>();
    for (int ci = 0, cl = parse.length(); ci < cl; ci++) {
      final char c = parse.charAt(ci);
      if (c == '(') {
        final String rest = parse.substring(ci + 1);
        final String type = getType(rest);
        if (type == null) {
          System.err.println("null type for: " + rest);
        }
        final String token = getToken(rest);
        stack.push(new Constituent(type, new Span(offset, offset)));
        if (token != null) {
          if (type.equals("-NONE-") && gl != null) {
            // System.err.println("stack.size="+stack.size());
            gl.labelGaps(stack);
          } else {
            cons.add(new Constituent(ShiftReduceParser.TOK_NODE,
                new Span(offset, offset + token.length())));
            text.append(token).append(" ");
            offset += token.length() + 1;
          }
        }
      } else if (c == ')') {
        final Constituent con = stack.pop();
        final int start = con.getSpan().getStart();
        if (start < offset) {
          cons.add(
              new Constituent(con.getLabel(), new Span(start, offset - 1)));
        }
      }
    }
    final String txt = text.toString();
    int tokenIndex = -1;
    final Parse p = new Parse(txt, new Span(0, txt.length()),
        ShiftReduceParser.TOP_NODE, 1, 0);
    for (int ci = 0; ci < cons.size(); ci++) {
      final Constituent con = cons.get(ci);
      final String type = con.getLabel();
      if (!type.equals(ShiftReduceParser.TOP_NODE)) {
        if (type == ShiftReduceParser.TOK_NODE) {
          tokenIndex++;
        }
        final Parse c = new Parse(txt, con.getSpan(), type, 1, tokenIndex);
        // System.err.println("insert["+ci+"] "+type+" "+c.toString()+"
        // "+c.hashCode());
        p.insert(c);
        // codeTree(p);
      }
    }
    return p;
  }

  /**
   * Returns the parent parse node of this constituent.
   *
   * @return The parent parse node of this constituent.
   */
  public Parse getParent() {
    return this.parent;
  }

  /**
   * Specifies the parent parse node for this constituent.
   *
   * @param parent
   *          The parent parse node for this constituent.
   */
  public void setParent(final Parse parent) {
    this.parent = parent;
  }

  /**
   * Indicates whether this parse node is a pos-tag.
   *
   * @return true if this node is a pos-tag, false otherwise.
   */
  public boolean isPosTag() {
    return this.parts.size() == 1
        && this.parts.get(0).getType().equals(ShiftReduceParser.TOK_NODE);
  }

  /**
   * Returns true if this constituent contains no sub-constituents.
   *
   * @return true if this constituent contains no sub-constituents; false
   *         otherwise.
   */
  public boolean isFlat() {
    boolean flat = true;
    for (int ci = 0; ci < this.parts.size(); ci++) {
      flat &= this.parts.get(ci).isPosTag();
    }
    return flat;
  }

  public void isChunk(final boolean ic) {
    this.isChunk = ic;
  }

  public boolean isChunk() {
    return this.isChunk;
  }

  /**
   * Returns the parse nodes which are children of this node and which are pos
   * tags.
   *
   * @return the parse nodes which are children of this node and which are pos
   *         tags.
   */
  public Parse[] getTagNodes() {
    final List<Parse> tags = new LinkedList<Parse>();
    final List<Parse> nodes = new LinkedList<Parse>();
    nodes.addAll(this.parts);
    while (nodes.size() != 0) {
      final Parse p = nodes.remove(0);
      if (p.isPosTag()) {
        tags.add(p);
      } else {
        nodes.addAll(0, p.parts);
      }
    }
    return tags.toArray(new Parse[tags.size()]);
  }

  /**
   * Returns the deepest shared parent of this node and the specified node. If
   * the nodes are identical then their parent is returned. If one node is the
   * parent of the other then the parent node is returned.
   *
   * @param node
   *          The node from which parents are compared to this node's parents.
   *
   * @return the deepest shared parent of this node and the specified node.
   */
  public Parse getCommonParent(Parse node) {
    if (this == node) {
      return this.parent;
    }
    final Set<Parse> parents = new HashSet<Parse>();
    Parse cparent = this;
    while (cparent != null) {
      parents.add(cparent);
      cparent = cparent.getParent();
    }
    while (node != null) {
      if (parents.contains(node)) {
        return node;
      }
      node = node.getParent();
    }
    return null;
  }

  @Override
  public boolean equals(final Object o) {
    if (o instanceof Parse) {
      final Parse p = (Parse) o;
      if (this.label == null) {
        if (p.label != null) {
          return false;
        }
      } else if (!this.label.equals(p.label)) {
        return false;
      }
      if (!this.span.equals(p.span)) {
        return false;
      }
      if (!this.text.equals(p.text)) {
        return false;
      }
      if (this.parts.size() != p.parts.size()) {
        return false;
      }
      for (int ci = 0; ci < this.parts.size(); ci++) {
        if (!this.parts.get(ci).equals(p.parts.get(ci))) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 37 * result + this.span.hashCode();

    // TODO: This might lead to a performance regression
    // result = 37*result + label.hashCode();
    result = 37 * result + this.text.hashCode();
    return result;
  }

  @Override
  public int compareTo(final Parse p) {
    if (this.getProb() > p.getProb()) {
      return -1;
    } else if (this.getProb() < p.getProb()) {
      return 1;
    }
    return 0;
  }

  /**
   * Returns the derivation string for this parse if one has been created.
   *
   * @return the derivation string for this parse or null if no derivation
   *         string has been created.
   */
  public StringBuffer getDerivation() {
    return this.derivation;
  }

  /**
   * Specifies the derivation string to be associated with this parse.
   *
   * @param derivation
   *          The derivation string to be associated with this parse.
   */
  public void setDerivation(final StringBuffer derivation) {
    this.derivation = derivation;
  }

  private void codeTree(final Parse p, final int[] levels) {
    final Parse[] kids = p.getChildren();
    final StringBuilder levelsBuff = new StringBuilder();
    levelsBuff.append("[");
    final int[] nlevels = new int[levels.length + 1];
    for (int li = 0; li < levels.length; li++) {
      nlevels[li] = levels[li];
      levelsBuff.append(levels[li]).append(".");
    }
    for (int ki = 0; ki < kids.length; ki++) {
      nlevels[levels.length] = ki;
      System.out.println(levelsBuff.toString() + ki + "] " + kids[ki].getType()
          + " " + kids[ki].hashCode() + " -> " + kids[ki].getParent().hashCode()
          + " " + kids[ki].getParent().getType() + " "
          + kids[ki].getCoveredText());
      codeTree(kids[ki], nlevels);
    }
  }

  /**
   * Prints to standard out a representation of the specified parse which
   * contains hash codes so that parent/child relationships can be explicitly
   * seen.
   */
  public void showCodeTree() {
    codeTree(this, new int[0]);
  }

  /**
   * Utility method to inserts named entities.
   *
   * @param tag
   *          the tags
   * @param names
   *          the spans
   * @param tokens
   *          the tokens
   */
  public static void addNames(final String tag, final Span[] names,
      final Parse[] tokens) {
    for (final Span nameTokenSpan : names) {
      final Parse startToken = tokens[nameTokenSpan.getStart()];
      final Parse endToken = tokens[nameTokenSpan.getEnd() - 1];
      final Parse commonParent = startToken.getCommonParent(endToken);
      // System.err.println("addNames: "+startToken+" .. "+endToken+"
      // commonParent = "+commonParent);
      if (commonParent != null) {
        final Span nameSpan = new Span(startToken.getSpan().getStart(),
            endToken.getSpan().getEnd());
        if (nameSpan.equals(commonParent.getSpan())) {
          commonParent.insert(new Parse(commonParent.getText(), nameSpan, tag,
              1.0, endToken.getHeadIndex()));
        } else {
          final Parse[] kids = commonParent.getChildren();
          boolean crossingKids = false;
          for (final Parse kid : kids) {
            if (nameSpan.crosses(kid.getSpan())) {
              crossingKids = true;
            }
          }
          if (!crossingKids) {
            commonParent.insert(new Parse(commonParent.getText(), nameSpan, tag,
                1.0, endToken.getHeadIndex()));
          } else {
            if (commonParent.getType().equals("NP")) {
              final Parse[] grandKids = kids[0].getChildren();
              if (grandKids.length > 1 && nameSpan
                  .contains(grandKids[grandKids.length - 1].getSpan())) {
                commonParent.insert(
                    new Parse(commonParent.getText(), commonParent.getSpan(),
                        tag, 1.0, commonParent.getHeadIndex()));
              }
            }
          }
        }
      }
    }
  }

}
