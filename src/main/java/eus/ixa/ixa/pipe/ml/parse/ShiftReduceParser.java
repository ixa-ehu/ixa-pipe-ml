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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import eus.ixa.ixa.pipe.ml.formats.ParseToCoNLL02Format;
import eus.ixa.ixa.pipe.ml.formats.ParseToTabulatedFormat;
import eus.ixa.ixa.pipe.ml.sequence.BioCodec;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerFactory;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerME;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerModel;
import eus.ixa.ixa.pipe.ml.utils.Span;
import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.ml.BeamSearch;
import opennlp.tools.ml.EventTrainer;
import opennlp.tools.ml.TrainerFactory;
import opennlp.tools.ml.model.Event;
import opennlp.tools.ml.model.MaxentModel;
import opennlp.tools.ngram.NGramModel;
import opennlp.tools.parser.ParserEventTypeEnum;
import opennlp.tools.util.Heap;
import opennlp.tools.util.ListHeap;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.Sequence;
import opennlp.tools.util.StringList;
import opennlp.tools.util.TrainingParameters;

/**
 * Class for a shift reduce style parser based on Adwait Ratnaparkhi's 1998
 * thesis. This class is based on opennlp.tools.parser.chunking.Parser in Apache
 * OpenNLP.
 * 
 * @author ragerri
 * @version 2016-05-05
 */
public class ShiftReduceParser {

  private static Pattern untokenizedParenPattern1 = Pattern
      .compile("([^ ])([({)}])");
  private static Pattern untokenizedParenPattern2 = Pattern
      .compile("([({)}])([^ ])");
  /**
   * The maximum number of parses advanced from all preceding parses at each
   * derivation step.
   */
  private final int M;
  /**
   * The maximum number of parses to advance from a single preceding parse.
   */
  private final int K;
  /**
   * The minimum total probability mass of advanced outcomes.
   */
  private final double Q;
  /**
   * The default beam size used if no beam size is given.
   */
  public static final int DEFAULT_BEAMSIZE = 20;
  /**
   * The default amount of probability mass required of advanced outcomes.
   */
  public static final double defaultAdvancePercentage = 0.95;
  /**
   * Completed parses.
   */
  private final Heap<Parse> completeParses;
  /**
   * Incomplete parses which will be advanced.
   */
  private Heap<Parse> odh;
  /**
   * Incomplete parses which have been advanced.
   */
  private Heap<Parse> ndh;
  /**
   * The head rules for the parser.
   */
  private final HeadRules headRules;
  /**
   * The set strings which are considered punctuation for the parser.
   * Punctuation is not attached, but floats to the top of the parse as
   * attachment decisions are made about its non-punctuation sister nodes.
   */
  private final Set<String> punctSet;
  /**
   * The label for the top node.
   */
  public static final String TOP_NODE = "TOP";
  /**
   * The label for the top if an incomplete node.
   */
  public static final String INC_NODE = "INC";
  /**
   * The label for a token node.
   */
  public static final String TOK_NODE = "TK";
  /**
   * The integer 0.
   */
  public static final Integer ZERO = 0;
  /**
   * Outcome used when a constituent is complete.
   */
  public static final String COMPLETE = "c";
  /**
   * Outcome used when a constituent is incomplete.
   */
  public static final String INCOMPLETE = "i";
  /**
   * Specifies whether failed parses should be reported to standard error.
   */
  private boolean reportFailedParse;
  /**
   * Specifies whether a derivation string should be created during parsing.
   * This is useful for debugging.
   */
  private final boolean createDerivationString = false;

  private final SequenceLabelerME tagger;
  private final SequenceLabelerME chunker;
  private final MaxentModel buildModel;
  private final MaxentModel checkModel;

  private final BuildContextGenerator buildContextGenerator;
  private final CheckContextGenerator checkContextGenerator;

  private final double[] bprobs;
  private final double[] cprobs;

  private static final String TOP_START = TOP_NODE + "-" + BioCodec.START;
  private final int topStartIndex;
  private final Map<String, String> startTypeMap;
  private final Map<String, String> contTypeMap;

  private final int completeIndex;
  private final int incompleteIndex;
  /**
   * Turns debug print on or off.
   */
  protected boolean debugOn = false;

  public ShiftReduceParser(final ParserModel model) {
    this(model, model.getBeamSize(), defaultAdvancePercentage);
  }

  public ShiftReduceParser(final ParserModel model, final int beamSize,
      final double advancePercentage) {
    this(model.getBuildModel(), model.getCheckModel(),
        new SequenceLabelerME(model.getParserTaggerModel()),
        new SequenceLabelerME(model.getParserChunkerModel()),
        model.getHeadRules(), beamSize, advancePercentage);
  }

  /**
   * Creates a new parser using the specified models and head rules using the
   * specified beam size and advance percentage.
   * 
   * @param buildModel
   *          The model to assign constituent labels.
   * @param checkModel
   *          The model to determine a constituent is complete.
   * @param tagger
   *          The model to assign pos-tags.
   * @param chunker
   *          The model to assign flat constituent labels.
   * @param headRules
   *          The head rules for head word perculation.
   * @param beamSize
   *          The number of different parses kept during parsing.
   * @param advancePercentage
   *          The minimal amount of probability mass which advanced outcomes
   *          must represent. Only outcomes which contribute to the top
   *          "advancePercentage" will be explored.
   */
  public ShiftReduceParser(final MaxentModel buildModel,
      final MaxentModel checkModel, final SequenceLabelerME tagger,
      final SequenceLabelerME chunker, final HeadRules headRules,
      final int beamSize, final double advancePercentage) {
    this.tagger = tagger;
    this.chunker = chunker;
    this.buildModel = buildModel;
    this.checkModel = checkModel;
    this.M = beamSize;
    this.K = beamSize;
    this.Q = advancePercentage;
    this.headRules = headRules;
    this.punctSet = headRules.getPunctuationTags();
    this.odh = new ListHeap<Parse>(this.K);
    this.ndh = new ListHeap<Parse>(this.K);
    this.completeParses = new ListHeap<Parse>(this.K);

    this.bprobs = new double[buildModel.getNumOutcomes()];
    this.cprobs = new double[checkModel.getNumOutcomes()];
    this.buildContextGenerator = new BuildContextGenerator();
    this.checkContextGenerator = new CheckContextGenerator();
    this.startTypeMap = new HashMap<String, String>();
    this.contTypeMap = new HashMap<String, String>();
    for (int boi = 0, bon = buildModel.getNumOutcomes(); boi < bon; boi++) {
      final String outcome = buildModel.getOutcome(boi);
      if (outcome.endsWith(BioCodec.START)) {
        // System.err.println("startMap "+outcome+"->" +
        // BioCodec.extractSequenceType(outcome));
        this.startTypeMap.put(outcome, BioCodec.extractSequenceType(outcome));
      } else if (outcome.endsWith(BioCodec.CONTINUE)) {
        // System.err.println("contMap "+outcome+"->"+
        // BioCodec.extractSequenceType(outcome));
        this.contTypeMap.put(outcome, BioCodec.extractSequenceType(outcome));
      }
    }
    this.topStartIndex = buildModel.getIndex(TOP_START);
    this.completeIndex = checkModel.getIndex(COMPLETE);
    this.incompleteIndex = checkModel.getIndex(INCOMPLETE);
  }

  private void advanceTop(final Parse p) {
    this.buildModel.eval(
        this.buildContextGenerator.getContext(p.getChildren(), 0), this.bprobs);
    p.addProb(Math.log(this.bprobs[this.topStartIndex]));
    this.checkModel.eval(
        this.checkContextGenerator.getContext(p.getChildren(), TOP_NODE, 0, 0),
        this.cprobs);
    p.addProb(Math.log(this.cprobs[this.completeIndex]));
    p.setType(TOP_NODE);
  }

  private Parse[] advanceParses(final Parse p, final double probMass) {
    final double q = 1 - probMass;
    /** The closest previous node which has been labeled as a start node. */
    Parse lastStartNode = null;
    /**
     * The index of the closest previous node which has been labeled as a start
     * node.
     */
    int lastStartIndex = -1;
    /**
     * The type of the closest previous node which has been labeled as a start
     * node.
     */
    String lastStartType = null;
    /**
     * The index of the node which will be labeled in this iteration of
     * advancing the parse.
     */
    int advanceNodeIndex;
    /**
     * The node which will be labeled in this iteration of advancing the parse.
     */
    Parse advanceNode = null;
    final Parse[] originalChildren = p.getChildren();
    final Parse[] children = collapsePunctuation(originalChildren,
        this.punctSet);
    final int numNodes = children.length;
    if (numNodes == 0) {
      return null;
    }
    // determines which node needs to be labeled and prior labels.
    for (advanceNodeIndex = 0; advanceNodeIndex < numNodes; advanceNodeIndex++) {
      advanceNode = children[advanceNodeIndex];
      if (advanceNode.getLabel() == null) {
        break;
      } else if (this.startTypeMap.containsKey(advanceNode.getLabel())) {
        lastStartType = this.startTypeMap.get(advanceNode.getLabel());
        lastStartNode = advanceNode;
        lastStartIndex = advanceNodeIndex;
        // System.err.println("lastStart "+i+" "+lastStart.label+"
        // "+lastStart.prob);
      }
    }
    final int originalAdvanceIndex = mapParseIndex(advanceNodeIndex, children,
        originalChildren);
    final List<Parse> newParsesList = new ArrayList<Parse>(
        this.buildModel.getNumOutcomes());
    // call build
    this.buildModel.eval(
        this.buildContextGenerator.getContext(children, advanceNodeIndex),
        this.bprobs);
    double bprobSum = 0;
    while (bprobSum < probMass) {
      // The largest unadvanced labeling.
      int max = 0;
      for (int pi = 1; pi < this.bprobs.length; pi++) { // for each build
                                                        // outcome
        if (this.bprobs[pi] > this.bprobs[max]) {
          max = pi;
        }
      }
      if (this.bprobs[max] == 0) {
        break;
      }
      final double bprob = this.bprobs[max];
      this.bprobs[max] = 0; // zero out so new max can be found
      bprobSum += bprob;
      final String tag = this.buildModel.getOutcome(max);
      // System.out.println("trying "+tag+" "+bprobSum+" lst="+lst);
      if (max == this.topStartIndex) { // can't have top until complete
        continue;
      }
      // System.err.println(i+" "+tag+" "+bprob);
      if (this.startTypeMap.containsKey(tag)) { // update last start
        lastStartIndex = advanceNodeIndex;
        lastStartNode = advanceNode;
        lastStartType = this.startTypeMap.get(tag);
      } else if (this.contTypeMap.containsKey(tag)) {
        if (lastStartNode == null
            || !lastStartType.equals(this.contTypeMap.get(tag))) {
          continue; // Cont must match previous start or continue
        }
      }
      final Parse newParse1 = (Parse) p.clone(); // clone parse
      if (this.createDerivationString) {
        newParse1.getDerivation().append(max).append("-");
      }
      newParse1.setChild(originalAdvanceIndex, tag); // replace constituent
                                                     // being labeled to create
                                                     // new derivation
      newParse1.addProb(Math.log(bprob));
      // check
      // String[] context =
      // checkContextGenerator.getContext(newParse1.getChildren(),
      // lastStartType, lastStartIndex, advanceNodeIndex);
      this.checkModel.eval(
          this.checkContextGenerator.getContext(
              collapsePunctuation(newParse1.getChildren(), this.punctSet),
              lastStartType, lastStartIndex, advanceNodeIndex),
          this.cprobs);
      // System.out.println("check "+lastStartType+" "+cprobs[completeIndex]+"
      // "+cprobs[incompleteIndex]+" "+tag+"
      // "+java.util.Arrays.asList(context));
      Parse newParse2 = newParse1;
      if (this.cprobs[this.completeIndex] > q) { // make sure a reduce is likely
        newParse2 = (Parse) newParse1.clone();
        if (this.createDerivationString) {
          newParse2.getDerivation().append(1).append(".");
        }
        newParse2.addProb(Math.log(this.cprobs[this.completeIndex]));
        final Parse[] cons = new Parse[advanceNodeIndex - lastStartIndex + 1];
        boolean flat = true;
        // first
        cons[0] = lastStartNode;
        flat &= cons[0].isPosTag();
        // last
        cons[advanceNodeIndex - lastStartIndex] = advanceNode;
        flat &= cons[advanceNodeIndex - lastStartIndex].isPosTag();
        // middle
        for (int ci = 1; ci < advanceNodeIndex - lastStartIndex; ci++) {
          cons[ci] = children[ci + lastStartIndex];
          flat &= cons[ci].isPosTag();
        }
        if (!flat) { // flat chunks are done by chunker
          if (lastStartIndex == 0 && advanceNodeIndex == numNodes - 1) { // check
                                                                         // for
                                                                         // top
                                                                         // node
                                                                         // to
                                                                         // include
                                                                         // end
                                                                         // and
                                                                         // begining
                                                                         // punctuation
            // System.err.println("ParserME.advanceParses: reducing entire span:
            // "+new Span(lastStartNode.getSpan().getStart(),
            // advanceNode.getSpan().getEnd())+" "+lastStartType+"
            // "+java.util.Arrays.asList(children));
            newParse2.insert(new Parse(p.getText(), p.getSpan(), lastStartType,
                this.cprobs[1], this.headRules.getHead(cons, lastStartType)));
          } else {
            newParse2.insert(new Parse(p.getText(),
                new Span(lastStartNode.getSpan().getStart(),
                    advanceNode.getSpan().getEnd()),
                lastStartType, this.cprobs[1],
                this.headRules.getHead(cons, lastStartType)));
          }
          newParsesList.add(newParse2);
        }
      }
      if (this.cprobs[this.incompleteIndex] > q) { // make sure a shift is
                                                   // likely
        if (this.createDerivationString) {
          newParse1.getDerivation().append(0).append(".");
        }
        if (advanceNodeIndex != numNodes - 1) { // can't shift last element
          newParse1.addProb(Math.log(this.cprobs[this.incompleteIndex]));
          newParsesList.add(newParse1);
        }
      }
    }
    final Parse[] newParses = new Parse[newParsesList.size()];
    newParsesList.toArray(newParses);
    return newParses;
  }

  public static Parse[] parseLine(String line, final ShiftReduceParser parser,
      final int numParses) {
    line = untokenizedParenPattern1.matcher(line).replaceAll("$1 $2");
    line = untokenizedParenPattern2.matcher(line).replaceAll("$1 $2");
    final StringTokenizer str = new StringTokenizer(line);
    final StringBuilder sb = new StringBuilder();
    final List<String> tokens = new ArrayList<String>();
    while (str.hasMoreTokens()) {
      final String tok = str.nextToken();
      tokens.add(tok);
      sb.append(tok).append(" ");
    }
    final String text = sb.substring(0, sb.length() - 1);
    final Parse p = new Parse(text, new Span(0, text.length()),
        ShiftReduceParser.INC_NODE, 0, 0);
    int start = 0;
    int i = 0;
    for (final Iterator<String> ti = tokens.iterator(); ti.hasNext(); i++) {
      final String tok = ti.next();
      p.insert(new Parse(text, new Span(start, start + tok.length()),
          ShiftReduceParser.TOK_NODE, 0, i));
      start += tok.length() + 1;
    }
    Parse[] parses;
    if (numParses == 1) {
      parses = new Parse[] { parser.parse(p) };
    } else {
      parses = parser.parse(p, numParses);
    }
    return parses;
  }

  public Parse[] parse(final Parse tokens, final int numParses) {
    if (this.createDerivationString) {
      tokens.setDerivation(new StringBuffer(100));
    }
    this.odh.clear();
    this.ndh.clear();
    this.completeParses.clear();
    int derivationStage = 0; // derivation length
    final int maxDerivationLength = 2 * tokens.getChildCount() + 3;
    this.odh.add(tokens);
    Parse guess = null;
    double minComplete = 2;
    double bestComplete = -100000; // approximating -infinity/0 in ln domain
    while (this.odh.size() > 0
        && (this.completeParses.size() < this.M
            || this.odh.first().getProb() < minComplete)
        && derivationStage < maxDerivationLength) {
      this.ndh = new ListHeap<Parse>(this.K);

      int derivationRank = 0;
      for (final Iterator<Parse> pi = this.odh.iterator(); pi.hasNext()
          && derivationRank < this.K; derivationRank++) { // forearch derivation
        final Parse tp = pi.next();
        // TODO: Need to look at this for K-best parsing cases
        /*
         * if (tp.getProb() < bestComplete) { //this parse and the ones which
         * follow will never win, stop advancing. break; }
         */
        if (guess == null && derivationStage == 2) {
          guess = tp;
        }
        if (this.debugOn) {
          System.out.print(
              derivationStage + " " + derivationRank + " " + tp.getProb());
          tp.show();
          System.out.println();
        }
        Parse[] nd;
        if (0 == derivationStage) {
          nd = advanceTags(tp);
        } else if (1 == derivationStage) {
          if (this.ndh.size() < this.K) {
            // System.err.println("advancing ts "+j+" "+ndh.size()+" < "+K);
            nd = advanceChunks(tp, bestComplete);
          } else {
            // System.err.println("advancing ts "+j+" prob="+((Parse)
            // ndh.last()).getProb());
            nd = advanceChunks(tp, this.ndh.last().getProb());
          }
        } else { // i > 1
          nd = advanceParses(tp, this.Q);
        }
        if (nd != null) {
          for (final Parse element : nd) {
            if (element.complete()) {
              advanceTop(element);
              if (element.getProb() > bestComplete) {
                bestComplete = element.getProb();
              }
              if (element.getProb() < minComplete) {
                minComplete = element.getProb();
              }
              this.completeParses.add(element);
            } else {
              this.ndh.add(element);
            }
          }
        } else {
          if (this.reportFailedParse) {
            System.err.println("Couldn't advance parse " + derivationStage
                + " stage " + derivationRank + "!\n");
          }
          advanceTop(tp);
          this.completeParses.add(tp);
        }
      }
      derivationStage++;
      this.odh = this.ndh;
    }
    if (this.completeParses.size() == 0) {
      if (this.reportFailedParse) {
        System.err.println("Couldn't find parse for: " + tokens);
      }
      // Parse r = (Parse) odh.first();
      // r.show();
      // System.out.println();
      return new Parse[] { guess };
    } else if (numParses == 1) {
      return new Parse[] { this.completeParses.first() };
    } else {
      final List<Parse> topParses = new ArrayList<Parse>(numParses);
      while (!this.completeParses.isEmpty() && topParses.size() < numParses) {
        final Parse tp = this.completeParses.extract();
        topParses.add(tp);
        // parses.remove(tp);
      }
      return topParses.toArray(new Parse[topParses.size()]);
    }
  }

  public Parse parse(final Parse tokens) {

    if (tokens.getChildCount() > 0) {
      final Parse p = parse(tokens, 1)[0];
      setParents(p);
      return p;
    } else {
      return tokens;
    }
  }

  /**
   * Assigns parent references for the specified parse so that they are
   * consistent with the children references.
   * 
   * @param p
   *          The parse whose parent references need to be assigned.
   */
  public static void setParents(final Parse p) {
    final Parse[] children = p.getChildren();
    for (final Parse element : children) {
      element.setParent(p);
      setParents(element);
    }
  }

  /**
   * Advances the parse by assigning it POS tags and returns multiple tag
   * sequences.
   * 
   * @param p
   *          The parse to be tagged.
   * @return Parses with different POS-tag sequence assignments.
   */
  protected Parse[] advanceTags(final Parse p) {
    final Parse[] children = p.getChildren();
    final String[] words = new String[children.length];
    final double[] probs = new double[words.length];
    for (int i = 0, il = children.length; i < il; i++) {
      words[i] = children[i].getCoveredText();
    }
    final Sequence[] ts = this.tagger.topKSequences(words);
    if (ts.length == 0) {
      System.err.println("no tag sequence");
    }
    final Parse[] newParses = new Parse[ts.length];
    for (int i = 0; i < ts.length; i++) {
      final String[] tags = ts[i].getOutcomes()
          .toArray(new String[words.length]);
      ts[i].getProbs(probs);
      newParses[i] = (Parse) p.clone(); // copies top level
      if (this.createDerivationString) {
        newParses[i].getDerivation().append(i).append(".");
      }
      for (int j = 0; j < words.length; j++) {
        final Parse word = children[j];
        // System.err.println("inserting tag "+tags[j]);
        final double prob = probs[j];
        newParses[i].insert(new Parse(word.getText(), word.getSpan(),
            tags[j].replaceAll("-" + BioCodec.START, ""), prob, j));
        newParses[i].addProb(Math.log(prob));
        // newParses[i].show();
      }
    }
    return newParses;
  }

  /**
   * Returns the top chunk sequences for the specified parse.
   * 
   * @param p
   *          A pos-tag assigned parse.
   * @param minChunkScore
   *          A minimum score below which chunks should not be advanced.
   * @return The top chunk assignments to the specified parse.
   */
  protected Parse[] advanceChunks(final Parse p, final double minChunkScore) {
    // chunk
    final Parse[] children = p.getChildren();
    final String words[] = new String[children.length];
    final String ptags[] = new String[words.length];
    final double probs[] = new double[words.length];
    Parse sp = null;
    for (int i = 0, il = children.length; i < il; i++) {
      sp = children[i];
      words[i] = sp.getHead().getCoveredText();
      ptags[i] = sp.getType();
    }
    // System.err.println("adjusted mcs = "+(minChunkScore-p.getProb()));
    final Sequence[] cs = this.chunker.topKSequences(words, ptags,
        minChunkScore - p.getProb());
    final Parse[] newParses = new Parse[cs.length];
    for (int si = 0, sl = cs.length; si < sl; si++) {
      newParses[si] = (Parse) p.clone(); // copies top level
      if (this.createDerivationString) {
        newParses[si].getDerivation().append(si).append(".");
      }
      final String[] tags = cs[si].getOutcomes()
          .toArray(new String[words.length]);
      cs[si].getProbs(probs);
      int start = -1;
      int end = 0;
      String type = null;
      // System.err.print("sequence "+si+" ");
      for (int j = 0; j <= tags.length; j++) {
        // if (j != tags.length) {System.err.println(words[j]+" "+ptags[j]+"
        // "+tags[j]+" "+probs.get(j));}
        if (j != tags.length) {
          newParses[si].addProb(Math.log(probs[j]));
        }
        if (j != tags.length && tags[j].endsWith(BioCodec.CONTINUE)) { // if
                                                                       // continue
                                                                       // just
                                                                       // update
                                                                       // end
                                                                       // chunking
                                                                       // tag
                                                                       // don't
                                                                       // use
                                                                       // contTypeMap
          end = j;
        } else { // make previous constituent if it exists
          if (type != null) {
            // System.err.println("inserting tag "+tags[j]);
            final Parse p1 = p.getChildren()[start];
            final Parse p2 = p.getChildren()[end];
            // System.err.println("Putting "+type+" at "+start+","+end+" for
            // "+j+" "+newParses[si].getProb());
            final Parse[] cons = new Parse[end - start + 1];
            cons[0] = p1;
            // cons[0].label="Start-"+type;
            if (end - start != 0) {
              cons[end - start] = p2;
              // cons[end-start].label="Cont-"+type;
              for (int ci = 1; ci < end - start; ci++) {
                cons[ci] = p.getChildren()[ci + start];
                // cons[ci].label="Cont-"+type;
              }
            }
            final Parse chunk = new Parse(p1.getText(),
                new Span(p1.getSpan().getStart(), p2.getSpan().getEnd()), type,
                1, this.headRules.getHead(cons, type));
            chunk.isChunk(true);
            newParses[si].insert(chunk);
          }
          if (j != tags.length) { // update for new constituent
            if (tags[j].endsWith(BioCodec.START)) { // don't use startTypeMap
                                                    // these are chunk tags
              type = tags[j].replaceAll("-" + BioCodec.START, "");
              start = j;
              end = j;
            } else { // other
              type = null;
            }
          }
        }
      }
      // newParses[si].show();System.out.println();
    }
    return newParses;
  }

  public static ParserModel train(final String languageCode,
      final ObjectStream<Parse> parseSamples, final HeadRules rules,
      final TrainingParameters trainParams, final ParserFactory parserFactory,
      final TrainingParameters taggerParams,
      final SequenceLabelerFactory taggerFactory,
      final TrainingParameters chunkerParams,
      final SequenceLabelerFactory chunkerFactory) throws IOException {

    final String beamSizeString = trainParams.getSettings()
        .get(BeamSearch.BEAM_SIZE_PARAMETER);
    int beamSize = DEFAULT_BEAMSIZE;
    if (beamSizeString != null) {
      beamSize = Integer.parseInt(beamSizeString);
    }
    parseSamples.reset();

    final Map<String, String> manifestInfoEntries = new HashMap<String, String>();

    System.err.println("Training POS tagger...");
    final SequenceLabelerModel posModel = SequenceLabelerME.train(languageCode,
        new ParseToTabulatedFormat(parseSamples), taggerParams, taggerFactory);
    parseSamples.reset();

    System.err.println("Training chunker...");
    final SequenceLabelerModel chunkModel = SequenceLabelerME.train(
        languageCode, new ParseToCoNLL02Format(parseSamples), chunkerParams,
        chunkerFactory);
    parseSamples.reset();

    // TODO build clusters
    System.err.println("Training builder...");
    final ObjectStream<Event> bes = new ParserEventStream(parseSamples, rules,
        ParserEventTypeEnum.BUILD, parserFactory);
    final Map<String, String> buildReportMap = new HashMap<String, String>();
    final EventTrainer trainer = TrainerFactory
        .getEventTrainer(trainParams.getSettings("build"), buildReportMap);
    final MaxentModel buildModel = trainer.train(bes);
    mergeReportIntoManifest(manifestInfoEntries, buildReportMap, "build");
    parseSamples.reset();

    // TODO check clusters
    System.err.println("Training checker...");
    final ObjectStream<Event> kes = new ParserEventStream(parseSamples, rules,
        ParserEventTypeEnum.CHECK);
    final Map<String, String> checkReportMap = new HashMap<String, String>();
    final EventTrainer checkTrainer = TrainerFactory
        .getEventTrainer(trainParams.getSettings("check"), checkReportMap);
    final MaxentModel checkModel = checkTrainer.train(kes);
    mergeReportIntoManifest(manifestInfoEntries, checkReportMap, "check");

    return new ParserModel(languageCode, buildModel, checkModel, posModel,
        chunkModel, beamSize, rules, manifestInfoEntries);
  }

  // TODO this is not done, right?
  public static ParserModel train(final String languageCode,
      final ObjectStream<Parse> parseSamples, final HeadRules rules,
      final TrainingParameters trainParams, final ParserFactory parserFactory,
      final SequenceLabelerModel posModel,
      final TrainingParameters chunkerParams,
      final SequenceLabelerFactory chunkerFactory) throws IOException {

    final String beamSizeString = trainParams.getSettings()
        .get(BeamSearch.BEAM_SIZE_PARAMETER);
    int beamSize = DEFAULT_BEAMSIZE;
    if (beamSizeString != null) {
      beamSize = Integer.parseInt(beamSizeString);
    }
    parseSamples.reset();

    final Map<String, String> manifestInfoEntries = new HashMap<String, String>();

    // TODO chunk
    System.err.println("Training chunker...");
    final SequenceLabelerModel chunkModel = SequenceLabelerME.train(
        languageCode, new ParseToCoNLL02Format(parseSamples), chunkerParams,
        chunkerFactory);
    parseSamples.reset();

   // TODO build clusters
    System.err.println("Training builder...");
    final ObjectStream<Event> bes = new ParserEventStream(parseSamples, rules,
        ParserEventTypeEnum.BUILD, parserFactory);
    final Map<String, String> buildReportMap = new HashMap<String, String>();
    final EventTrainer trainer = TrainerFactory
        .getEventTrainer(trainParams.getSettings("build"), buildReportMap);
    final MaxentModel buildModel = trainer.train(bes);
    mergeReportIntoManifest(manifestInfoEntries, buildReportMap, "build");
    parseSamples.reset();

    // TODO check clusters
    System.err.println("Training checker...");
    final ObjectStream<Event> kes = new ParserEventStream(parseSamples, rules,
        ParserEventTypeEnum.CHECK);
    final Map<String, String> checkReportMap = new HashMap<String, String>();
    final EventTrainer checkTrainer = TrainerFactory
        .getEventTrainer(trainParams.getSettings("check"), checkReportMap);
    final MaxentModel checkModel = checkTrainer.train(kes);
    mergeReportIntoManifest(manifestInfoEntries, checkReportMap, "check");

    return new ParserModel(languageCode, buildModel, checkModel, posModel,
        chunkModel, beamSize, rules, manifestInfoEntries);
  }

  public static void mergeReportIntoManifest(final Map<String, String> manifest,
      final Map<String, String> report, final String namespace) {

    for (final Map.Entry<String, String> entry : report.entrySet()) {
      manifest.put(namespace + "." + entry.getKey(), entry.getValue());
    }
  }

  /**
   * Removes the punctuation from the specified set of chunks, adds it to the
   * parses adjacent to the punctuation is specified, and returns a new array of
   * parses with the punctuation removed.
   * 
   * @param chunks
   *          A set of parses.
   * @param punctSet
   *          The set of punctuation which is to be removed.
   * @return An array of parses which is a subset of chunks with punctuation
   *         removed.
   */
  public static Parse[] collapsePunctuation(final Parse[] chunks,
      final Set<String> punctSet) {
    final List<Parse> collapsedParses = new ArrayList<Parse>(chunks.length);
    int lastNonPunct = -1;
    int nextNonPunct = -1;
    for (int ci = 0, cn = chunks.length; ci < cn; ci++) {
      if (punctSet.contains(chunks[ci].getType())) {
        if (lastNonPunct >= 0) {
          chunks[lastNonPunct].addNextPunctuation(chunks[ci]);
        }
        for (nextNonPunct = ci + 1; nextNonPunct < cn; nextNonPunct++) {
          if (!punctSet.contains(chunks[nextNonPunct].getType())) {
            break;
          }
        }
        if (nextNonPunct < cn) {
          chunks[nextNonPunct].addPreviousPunctuation(chunks[ci]);
        }
      } else {
        collapsedParses.add(chunks[ci]);
        lastNonPunct = ci;
      }
    }
    if (collapsedParses.size() == chunks.length) {
      return chunks;
    }
    // System.err.println("collapsedPunctuation:
    // collapsedParses"+collapsedParses);
    return collapsedParses.toArray(new Parse[collapsedParses.size()]);
  }

  /**
   * Determines the mapping between the specified index into the specified
   * parses without punctuation to the corresponding index into the specified
   * parses.
   * 
   * @param index
   *          An index into the parses without punctuation.
   * @param nonPunctParses
   *          The parses without punctuation.
   * @param parses
   *          The parses wit punctuation.
   * @return An index into the specified parses which corresponds to the same
   *         node the specified index into the parses with punctuation.
   */
  private int mapParseIndex(final int index, final Parse[] nonPunctParses,
      final Parse[] parses) {
    int parseIndex = index;
    while (parses[parseIndex] != nonPunctParses[index]) {
      parseIndex++;
    }
    return parseIndex;
  }

  /**
   * Creates a n-gram dictionary from the specified data stream using the
   * specified head rule and specified cut-off.
   *
   * @param data
   *          The data stream of parses.
   * @param rules
   *          The head rules for the parses.
   * @param cutoff
   *          The minimum number of entries required for the n-gram to be saved
   *          as part of the dictionary.
   * @throws IOException
   *           if io problems
   * @return A dictionary object.
   */
  public static Dictionary buildDictionary(final ObjectStream<Parse> data,
      final HeadRules rules, final int cutoff) throws IOException {

    final TrainingParameters params = new TrainingParameters();
    params.put("dict", TrainingParameters.CUTOFF_PARAM,
        Integer.toString(cutoff));

    return buildDictionary(data, rules, params);
  }

  /**
   * Creates a n-gram dictionary from the specified data stream using the
   * specified head rule and specified cut-off.
   *
   * @param data
   *          The data stream of parses.
   * @param rules
   *          The head rules for the parses.
   * @param params
   *          can contain a cutoff, the minimum number of entries required for
   *          the n-gram to be saved as part of the dictionary.
   * @throws IOException
   *           if io problems
   * @return A dictionary object.
   */
  public static Dictionary buildDictionary(final ObjectStream<Parse> data,
      final HeadRules rules, final TrainingParameters params)
      throws IOException {

    System.err.println("Building automatic ngram dictionary...");
    int cutoff = 5;

    final String cutoffString = params.getSettings("dict")
        .get(TrainingParameters.CUTOFF_PARAM);

    if (cutoffString != null) {
      // TODO: Maybe throw illegal argument exception if not parsable
      cutoff = Integer.parseInt(cutoffString);
    }

    final NGramModel mdict = new NGramModel();
    Parse p;
    while ((p = data.read()) != null) {
      p.updateHeads(rules);
      final Parse[] pwords = p.getTagNodes();
      final String[] words = new String[pwords.length];
      // add all uni-grams
      for (int wi = 0; wi < words.length; wi++) {
        words[wi] = pwords[wi].getCoveredText();
      }

      mdict.add(new StringList(words), 1, 1);
      // add tri-grams and bi-grams for inital sequence
      Parse[] chunks = collapsePunctuation(
          ParserEventStream.getInitialChunks(p), rules.getPunctuationTags());
      final String[] cwords = new String[chunks.length];
      for (int wi = 0; wi < cwords.length; wi++) {
        cwords[wi] = chunks[wi].getHead().getCoveredText();
      }
      mdict.add(new StringList(cwords), 2, 3);

      // emulate reductions to produce additional n-grams
      int ci = 0;
      while (ci < chunks.length) {
        // System.err.println("chunks["+ci+"]="+chunks[ci].getHead().getCoveredText()+"
        // chunks.length="+chunks.length + " " + chunks[ci].getParent());

        if (chunks[ci].getParent() == null) {
          chunks[ci].show();
        }
        if (lastChild(chunks[ci], chunks[ci].getParent(),
            rules.getPunctuationTags())) {
          // perform reduce
          int reduceStart = ci;
          while (reduceStart >= 0
              && chunks[reduceStart].getParent() == chunks[ci].getParent()) {
            reduceStart--;
          }
          reduceStart++;
          chunks = ParserEventStream.reduceChunks(chunks, ci,
              chunks[ci].getParent());
          ci = reduceStart;
          if (chunks.length != 0) {
            String[] window = new String[5];
            int wi = 0;
            if (ci - 2 >= 0) {
              window[wi++] = chunks[ci - 2].getHead().getCoveredText();
            }
            if (ci - 1 >= 0) {
              window[wi++] = chunks[ci - 1].getHead().getCoveredText();
            }
            window[wi++] = chunks[ci].getHead().getCoveredText();
            if (ci + 1 < chunks.length) {
              window[wi++] = chunks[ci + 1].getHead().getCoveredText();
            }
            if (ci + 2 < chunks.length) {
              window[wi++] = chunks[ci + 2].getHead().getCoveredText();
            }
            if (wi < 5) {
              final String[] subWindow = new String[wi];
              for (int swi = 0; swi < wi; swi++) {
                subWindow[swi] = window[swi];
              }
              window = subWindow;
            }
            if (window.length >= 3) {
              mdict.add(new StringList(window), 2, 3);
            } else if (window.length == 2) {
              mdict.add(new StringList(window), 2, 2);
            }
          }
          ci = reduceStart - 1; // ci will be incremented at end of loop
        }
        ci++;
      }
    }
    // System.err.println("gas,and="+mdict.getCount((new TokenList(new String[]
    // {"gas","and"}))));
    mdict.cutoff(cutoff, Integer.MAX_VALUE);
    System.err.println("Automatic dictionary created!");
    return mdict.toDictionary(true);
  }

  private static boolean lastChild(final Parse child, final Parse parent,
      final Set<String> punctSet) {
    if (parent == null) {
      return false;
    }

    final Parse[] kids = collapsePunctuation(parent.getChildren(), punctSet);
    return kids[kids.length - 1] == child;
  }

}
