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

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Abstract class containing many of the methods used to generate contexts for
 * parsing.
 */
public abstract class AbstractContextGenerator {

  protected static final String EOS = "eos";

  protected boolean zeroBackOff;
  /** Set of punctuation to be used in generating features. */
  protected Set<String> punctSet;
  protected boolean useLabel;

  /**
   * Creates punctuation feature for the specified punctuation at the specified
   * index based on the punctuation mark.
   * 
   * @param punct
   *          The punctuation which is in context.
   * @param i
   *          The index of the punctuation with relative to the parse.
   * @return Punctuation feature for the specified parse and the specified
   *         punctuation at the specfied index.
   */
  protected String punct(final Parse punct, final int i) {
    final StringBuilder feat = new StringBuilder(5);
    feat.append(i).append("=");
    feat.append(punct.getCoveredText());
    return feat.toString();
  }

  /**
   * Creates punctuation feature for the specified punctuation at the specfied
   * index based on the punctuation's tag.
   * 
   * @param punct
   *          The punctuation which is in context.
   * @param i
   *          The index of the punctuation relative to the parse.
   * @return Punctuation feature for the specified parse and the specified
   *         punctuation at the specfied index.
   */
  protected String punctbo(final Parse punct, final int i) {
    final StringBuilder feat = new StringBuilder(5);
    feat.append(i).append("=");
    feat.append(punct.getType());
    return feat.toString();
  }

  protected String cons(final Parse p, final int i) {
    final StringBuilder feat = new StringBuilder(20);
    feat.append(i).append("=");
    if (p != null) {
      if (this.useLabel && i < 0) {
        feat.append(p.getLabel()).append("|");
      }
      feat.append(p.getType()).append("|").append(p.getHead().getCoveredText());
    } else {
      feat.append(EOS);
    }
    return feat.toString();
  }

  protected String consbo(final Parse p, final int i) { // cons back-off
    final StringBuilder feat = new StringBuilder(20);
    feat.append(i).append("*=");
    if (p != null) {
      if (this.useLabel && i < 0) {
        feat.append(p.getLabel()).append("|");
      }
      feat.append(p.getType());
    } else {
      feat.append(EOS);
    }
    return feat.toString();
  }

  /**
   * Generates a string representing the grammar rule production that the
   * specified parse is starting. The rule is of the form p.type -&gt;
   * c.children[0..n].type.
   * 
   * @param p
   *          The parse which stats teh production.
   * @param includePunctuation
   *          Whether punctuation should be included in the production.
   * @return a string representing the grammar rule production that the
   *         specified parse is starting.
   */
  protected String production(final Parse p, final boolean includePunctuation) {
    final StringBuilder production = new StringBuilder(20);
    production.append(p.getType()).append("->");
    final Parse[] children = ShiftReduceParser
        .collapsePunctuation(p.getChildren(), this.punctSet);
    for (int ci = 0; ci < children.length; ci++) {
      production.append(children[ci].getType());
      if (ci + 1 != children.length) {
        production.append(",");
        final Collection<Parse> nextPunct = children[ci]
            .getNextPunctuationSet();
        if (includePunctuation && nextPunct != null) {
          // TODO: make sure multiple punctuation comes out the same
          for (final Parse punct : nextPunct) {
            production.append(punct.getType()).append(",");
          }
        }
      }
    }
    return production.toString();
  }

  protected void cons2(final List<String> features, final Cons c0,
      final Cons c1, final Collection<Parse> punct1s, final boolean bigram) {
    if (punct1s != null) {
      for (final Parse p : punct1s) {
        // String punct = punct(p,c1.index);
        final String punctbo = punctbo(p,
            c1.index <= 0 ? c1.index - 1 : c1.index);

        // punctbo(1);
        features.add(punctbo);
        if (c0.index == 0) { // TODO look at removing case
          // cons(0)punctbo(1)
          if (c0.unigram) {
            features.add(c0.cons + "," + punctbo);
          }
          features.add(c0.consbo + "," + punctbo);
        }
        if (c1.index == 0) { // TODO look at removing case
          // punctbo(1)cons(1)
          if (c1.unigram) {
            features.add(punctbo + "," + c1.cons);
          }
          features.add(punctbo + "," + c1.consbo);
        }

        // cons(0)punctbo(1)cons(1)
        if (bigram) {
          features.add(c0.cons + "," + punctbo + "," + c1.cons);
        }
        if (c1.unigram) {
          features.add(c0.consbo + "," + punctbo + "," + c1.cons);
        }
        if (c0.unigram) {
          features.add(c0.cons + "," + punctbo + "," + c1.consbo);
        }
        features.add(c0.consbo + "," + punctbo + "," + c1.consbo);
      }
    } else {
      // cons(0),cons(1)
      if (bigram) {
        features.add(c0.cons + "," + c1.cons);
      }
      if (c1.unigram) {
        features.add(c0.consbo + "," + c1.cons);
      }
      if (c0.unigram) {
        features.add(c0.cons + "," + c1.consbo);
      }
      features.add(c0.consbo + "," + c1.consbo);
    }
  }

  /**
   * Creates cons features involving the 3 specified nodes and adds them to the
   * specified feature list.
   * 
   * @param features
   *          The list of features.
   * @param c0
   *          The first node.
   * @param c1
   *          The second node.
   * @param c2
   *          The third node.
   * @param punct1s
   *          The punctuation between the first and second node.
   * @param punct2s
   *          The punctuation between the second and third node.
   * @param trigram
   *          Specifies whether lexical tri-gram features between these nodes
   *          should be generated.
   * @param bigram1
   *          Specifies whether lexical bi-gram features between the first and
   *          second node should be generated.
   * @param bigram2
   *          Specifies whether lexical bi-gram features between the second and
   *          third node should be generated.
   */
  protected void cons3(final List<String> features, final Cons c0,
      final Cons c1, final Cons c2, final Collection<Parse> punct1s,
      final Collection<Parse> punct2s, final boolean trigram,
      final boolean bigram1, final boolean bigram2) {
    // features.add("stage=cons(0),cons(1),cons(2)");
    if (punct1s != null) {
      if (c0.index == -2) {
        for (final Parse p : punct1s) {
          // String punct = punct(p,c1.index);
          final String punctbo = punctbo(p,
              c1.index <= 0 ? c1.index - 1 : c1.index);
          // punct(-2)
          // TODO consider changing
          // features.add(punct);

          // punctbo(-2)
          features.add(punctbo);
        }
      }
    }
    if (punct2s != null) {
      if (c2.index == 2) {
        for (final Parse p : punct2s) {
          // String punct = punct(p,c2.index);
          final String punctbo = punctbo(p,
              c2.index <= 0 ? c2.index - 1 : c2.index);
          // punct(2)
          // TODO consider changing
          // features.add(punct);

          // punctbo(2)
          features.add(punctbo);
        }
      }
      if (punct1s != null) {
        // cons(0),punctbo(1),cons(1),punctbo(2),cons(2)
        for (final Parse parse : punct2s) {
          final String punctbo2 = punctbo(parse,
              c2.index <= 0 ? c2.index - 1 : c2.index);
          for (final Parse parse2 : punct1s) {
            final String punctbo1 = punctbo(parse2,
                c1.index <= 0 ? c1.index - 1 : c1.index);
            if (trigram) {
              features.add(c0.cons + "," + punctbo1 + "," + c1.cons + ","
                  + punctbo2 + "," + c2.cons);
            }

            if (bigram2) {
              features.add(c0.consbo + "," + punctbo1 + "," + c1.cons + ","
                  + punctbo2 + "," + c2.cons);
            }
            if (c0.unigram && c2.unigram) {
              features.add(c0.cons + "," + punctbo1 + "," + c1.consbo + ","
                  + punctbo2 + "," + c2.cons);
            }
            if (bigram1) {
              features.add(c0.cons + "," + punctbo1 + "," + c1.cons + ","
                  + punctbo2 + "," + c2.consbo);
            }

            if (c2.unigram) {
              features.add(c0.consbo + "," + punctbo1 + "," + c1.consbo + ","
                  + punctbo2 + "," + c2.cons);
            }
            if (c1.unigram) {
              features.add(c0.consbo + "," + punctbo1 + "," + c1.cons + ","
                  + punctbo2 + "," + c2.consbo);
            }
            if (c0.unigram) {
              features.add(c0.cons + "," + punctbo1 + "," + c1.consbo + ","
                  + punctbo2 + "," + c2.consbo);
            }

            features.add(c0.consbo + "," + punctbo1 + "," + c1.consbo + ","
                + punctbo2 + "," + c2.consbo);
            if (this.zeroBackOff) {
              if (bigram1) {
                features.add(
                    c0.cons + "," + punctbo1 + "," + c1.cons + "," + punctbo2);
              }
              if (c1.unigram) {
                features.add(c0.consbo + "," + punctbo1 + "," + c1.cons + ","
                    + punctbo2);
              }
              if (c0.unigram) {
                features.add(c0.cons + "," + punctbo1 + "," + c1.consbo + ","
                    + punctbo2);
              }
              features.add(c0.consbo + "," + punctbo1 + "," + c1.consbo + ","
                  + punctbo2);
            }
          }
        }
      } else { // punct1s == null
        // cons(0),cons(1),punctbo(2),cons(2)
        for (final Parse parse : punct2s) {
          final String punctbo2 = punctbo(parse,
              c2.index <= 0 ? c2.index - 1 : c2.index);
          if (trigram) {
            features
                .add(c0.cons + "," + c1.cons + "," + punctbo2 + "," + c2.cons);
          }

          if (bigram2) {
            features.add(
                c0.consbo + "," + c1.cons + "," + punctbo2 + "," + c2.cons);
          }
          if (c0.unigram && c2.unigram) {
            features.add(
                c0.cons + "," + c1.consbo + "," + punctbo2 + "," + c2.cons);
          }
          if (bigram1) {
            features.add(
                c0.cons + "," + c1.cons + "," + punctbo2 + "," + c2.consbo);
          }

          if (c2.unigram) {
            features.add(
                c0.consbo + "," + c1.consbo + "," + punctbo2 + "," + c2.cons);
          }
          if (c1.unigram) {
            features.add(
                c0.consbo + "," + c1.cons + "," + punctbo2 + "," + c2.consbo);
          }
          if (c0.unigram) {
            features.add(
                c0.cons + "," + c1.consbo + "," + punctbo2 + "," + c2.consbo);
          }

          features.add(
              c0.consbo + "," + c1.consbo + "," + punctbo2 + "," + c2.consbo);

          if (this.zeroBackOff) {
            if (bigram1) {
              features.add(c0.cons + "," + c1.cons + "," + punctbo2);
            }
            if (c1.unigram) {
              features.add(c0.consbo + "," + c1.cons + "," + punctbo2);
            }
            if (c0.unigram) {
              features.add(c0.cons + "," + c1.consbo + "," + punctbo2);
            }
            features.add(c0.consbo + "," + c1.consbo + "," + punctbo2);
          }
        }
      }
    } else {
      if (punct1s != null) {
        // cons(0),punctbo(1),cons(1),cons(2)
        for (final Parse parse : punct1s) {
          final String punctbo1 = punctbo(parse,
              c1.index <= 0 ? c1.index - 1 : c1.index);
          if (trigram) {
            features
                .add(c0.cons + "," + punctbo1 + "," + c1.cons + "," + c2.cons);
          }

          if (bigram2) {
            features.add(
                c0.consbo + "," + punctbo1 + "," + c1.cons + "," + c2.cons);
          }
          if (c0.unigram && c2.unigram) {
            features.add(
                c0.cons + "," + punctbo1 + "," + c1.consbo + "," + c2.cons);
          }
          if (bigram1) {
            features.add(
                c0.cons + "," + punctbo1 + "," + c1.cons + "," + c2.consbo);
          }

          if (c2.unigram) {
            features.add(
                c0.consbo + "," + punctbo1 + "," + c1.consbo + "," + c2.cons);
          }
          if (c1.unigram) {
            features.add(
                c0.consbo + "," + punctbo1 + "," + c1.cons + "," + c2.consbo);
          }
          if (c0.unigram) {
            features.add(
                c0.cons + "," + punctbo1 + "," + c1.consbo + "," + c2.consbo);
          }

          features.add(
              c0.consbo + "," + punctbo1 + "," + c1.consbo + "," + c2.consbo);

          // zero backoff case covered by cons(0)cons(1)
        }
      } else {
        // cons(0),cons(1),cons(2)
        if (trigram) {
          features.add(c0.cons + "," + c1.cons + "," + c2.cons);
        }

        if (bigram2) {
          features.add(c0.consbo + "," + c1.cons + "," + c2.cons);
        }
        if (c0.unigram && c2.unigram) {
          features.add(c0.cons + "," + c1.consbo + "," + c2.cons);
        }
        if (bigram1) {
          features.add(c0.cons + "," + c1.cons + "," + c2.consbo);
        }

        if (c2.unigram) {
          features.add(c0.consbo + "," + c1.consbo + "," + c2.cons);
        }
        if (c1.unigram) {
          features.add(c0.consbo + "," + c1.cons + "," + c2.consbo);
        }
        if (c0.unigram) {
          features.add(c0.cons + "," + c1.consbo + "," + c2.consbo);
        }

        features.add(c0.consbo + "," + c1.consbo + "," + c2.consbo);
      }
    }
  }

  /**
   * Generates features for nodes surrounding a completed node of the specified
   * type.
   * 
   * @param node
   *          A surrounding node.
   * @param i
   *          The index of the surrounding node with respect to the completed
   *          node.
   * @param type
   *          The type of the completed node.
   * @param punctuation
   *          The punctuation adjacent and between the specified surrounding
   *          node.
   * @param features
   *          A list to which features are added.
   */
  protected void surround(final Parse node, final int i, final String type,
      final Collection<Parse> punctuation, final List<String> features) {
    final StringBuilder feat = new StringBuilder(20);
    feat.append("s").append(i).append("=");
    if (punctuation != null) {
      for (final Parse punct : punctuation) {
        if (node != null) {
          feat.append(node.getHead().getCoveredText()).append("|").append(type)
              .append("|").append(node.getType()).append("|")
              .append(punct.getType());
        } else {
          feat.append(type).append("|").append(EOS).append("|")
              .append(punct.getType());
        }
        features.add(feat.toString());

        feat.setLength(0);
        feat.append("s").append(i).append("*=");
        if (node != null) {
          feat.append(type).append("|").append(node.getType()).append("|")
              .append(punct.getType());
        } else {
          feat.append(type).append("|").append(EOS).append("|")
              .append(punct.getType());
        }
        features.add(feat.toString());

        feat.setLength(0);
        feat.append("s").append(i).append("*=");
        feat.append(type).append("|").append(punct.getType());
        features.add(feat.toString());
      }
    } else {
      if (node != null) {
        feat.append(node.getHead().getCoveredText()).append("|").append(type)
            .append("|").append(node.getType());
      } else {
        feat.append(type).append("|").append(EOS);
      }
      features.add(feat.toString());
      feat.setLength(0);
      feat.append("s").append(i).append("*=");
      if (node != null) {
        feat.append(type).append("|").append(node.getType());
      } else {
        feat.append(type).append("|").append(EOS);
      }
      features.add(feat.toString());
    }
  }

  /**
   * Produces features to determine whether the specified child node is part of
   * a complete constituent of the specified type and adds those features to the
   * specfied list.
   * 
   * @param child
   *          The parse node to consider.
   * @param i
   *          A string indicating the position of the child node.
   * @param type
   *          The type of constituent being built.
   * @param features
   *          List to add features to.
   */
  protected void checkcons(final Parse child, final String i, final String type,
      final List<String> features) {
    final StringBuilder feat = new StringBuilder(20);
    feat.append("c").append(i).append("=").append(child.getType()).append("|")
        .append(child.getHead().getCoveredText()).append("|").append(type);
    features.add(feat.toString());
    feat.setLength(0);
    feat.append("c").append(i).append("*=").append(child.getType()).append("|")
        .append(type);
    features.add(feat.toString());
  }

  protected void checkcons(final Parse p1, final Parse p2, final String type,
      final List<String> features) {
    final StringBuilder feat = new StringBuilder(20);
    feat.append("cil=").append(type).append(",").append(p1.getType())
        .append("|").append(p1.getHead().getCoveredText()).append(",")
        .append(p2.getType()).append("|").append(p2.getHead().getCoveredText());
    features.add(feat.toString());
    feat.setLength(0);
    feat.append("ci*l=").append(type).append(",").append(p1.getType())
        .append(",").append(p2.getType()).append("|")
        .append(p2.getHead().getCoveredText());
    features.add(feat.toString());
    feat.setLength(0);
    feat.append("cil*=").append(type).append(",").append(p1.getType())
        .append("|").append(p1.getHead().getCoveredText()).append(",")
        .append(p2.getType());
    features.add(feat.toString());
    feat.setLength(0);
    feat.append("ci*l*=").append(type).append(",").append(p1.getType())
        .append(",").append(p2.getType());
    features.add(feat.toString());
  }

  /**
   * Populates specified nodes array with left-most right frontier node with a
   * unique head. If the right frontier doesn't contain enough nodes, then nulls
   * are placed in the array elements.
   * 
   * @param rf
   *          The current right frontier.
   * @param nodes
   *          The array to be populated.
   */
  protected void getFrontierNodes(final List<Parse> rf, final Parse[] nodes) {
    int leftIndex = 0;
    int prevHeadIndex = -1;

    for (int fi = 0; fi < rf.size(); fi++) {
      final Parse fn = rf.get(fi);
      final int headIndex = fn.getHeadIndex();
      if (headIndex != prevHeadIndex) {
        nodes[leftIndex] = fn;
        leftIndex++;
        prevHeadIndex = headIndex;
        if (leftIndex == nodes.length) {
          break;
        }
      }
    }
    for (int ni = leftIndex; ni < nodes.length; ni++) {
      nodes[ni] = null;
    }
  }

}
