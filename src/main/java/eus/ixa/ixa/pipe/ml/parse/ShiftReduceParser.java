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
import eus.ixa.ixa.pipe.ml.formats.ParseToCoNLL02Format;
import eus.ixa.ixa.pipe.ml.formats.ParseToTabulatedFormat;
import eus.ixa.ixa.pipe.ml.sequence.BioCodec;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerFactory;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerME;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerModel;
import eus.ixa.ixa.pipe.ml.utils.Span;

/**
 * Class for a shift reduce style parser based on Adwait Ratnaparkhi's 1998 thesis.
 * This class is based on opennlp.tools.parser.chunking.Parser.java.
 * @author ragerri
 * @version 2016-05-05
 */
public class ShiftReduceParser {
  
  private static Pattern untokenizedParenPattern1 = Pattern.compile("([^ ])([({)}])");
  private static Pattern untokenizedParenPattern2 = Pattern.compile("([({)}])([^ ])");
  /**
   * The maximum number of parses advanced from all preceding
   * parses at each derivation step.
   */
  private int M;
  /**
   * The maximum number of parses to advance from a single preceding parse.
   */
  private int K;
  /**
   * The minimum total probability mass of advanced outcomes.
   */
  private double Q;
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
  private Heap<Parse> completeParses;
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
  private HeadRules headRules;
  /**
   * The set strings which are considered punctuation for the parser.
   * Punctuation is not attached, but floats to the top of the parse as attachment
   * decisions are made about its non-punctuation sister nodes.
   */
  private Set<String> punctSet;
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
  private boolean createDerivationString = false;
  
  private SequenceLabelerME tagger;
  private SequenceLabelerME chunker;
  private MaxentModel buildModel;
  private MaxentModel checkModel;

  private BuildContextGenerator buildContextGenerator;
  private CheckContextGenerator checkContextGenerator;

  private double[] bprobs;
  private double[] cprobs;

  private static final String TOP_START = TOP_NODE + "-" + BioCodec.START;
  private int topStartIndex;
  private Map<String, String> startTypeMap;
  private Map<String, String> contTypeMap;

  private int completeIndex;
  private int incompleteIndex;
  /**
   * Turns debug print on or off.
   */
  protected boolean debugOn = false;

  public ShiftReduceParser(ParserModel model) {
    this(model, model.getBeamSize(), defaultAdvancePercentage);
  }
  
  public ShiftReduceParser(ParserModel model, int beamSize, double advancePercentage) {
    this(model.getBuildModel(), model.getCheckModel(),
        new SequenceLabelerME(model.getParserTaggerModel()),
        new SequenceLabelerME(model.getParserChunkerModel()),
            model.getHeadRules(), beamSize, advancePercentage);
  }

  /**
   * Creates a new parser using the specified models and head rules using the specified beam size and advance percentage.
   * @param buildModel The model to assign constituent labels.
   * @param checkModel The model to determine a constituent is complete.
   * @param tagger The model to assign pos-tags.
   * @param chunker The model to assign flat constituent labels.
   * @param headRules The head rules for head word perculation.
   * @param beamSize The number of different parses kept during parsing.
   * @param advancePercentage The minimal amount of probability mass which advanced outcomes must represent.
   * Only outcomes which contribute to the top "advancePercentage" will be explored.
   */
  public ShiftReduceParser(MaxentModel buildModel, MaxentModel checkModel, SequenceLabelerME tagger, SequenceLabelerME chunker, HeadRules headRules, int beamSize, double advancePercentage) {
    this.tagger = tagger;
    this.chunker = chunker;
    this.buildModel = buildModel;
    this.checkModel = checkModel;
    this.M = beamSize;
    this.K = beamSize;
    this.Q = advancePercentage;
    this.headRules = headRules;
    this.punctSet = headRules.getPunctuationTags();
    odh = new ListHeap<Parse>(K);
    ndh = new ListHeap<Parse>(K);
    completeParses = new ListHeap<Parse>(K);
    
    bprobs = new double[buildModel.getNumOutcomes()];
    cprobs = new double[checkModel.getNumOutcomes()];
    this.buildContextGenerator = new BuildContextGenerator();
    this.checkContextGenerator = new CheckContextGenerator();
    startTypeMap = new HashMap<String, String>();
    contTypeMap = new HashMap<String, String>();
    for (int boi = 0, bon = buildModel.getNumOutcomes(); boi < bon; boi++) {
      String outcome = buildModel.getOutcome(boi);
      if (outcome.endsWith(BioCodec.START)) {
        //System.err.println("startMap "+outcome+"->" + BioCodec.extractSequenceType(outcome));
        startTypeMap.put(outcome, BioCodec.extractSequenceType(outcome));
      }
      else if (outcome.endsWith(BioCodec.CONTINUE)) {
        //System.err.println("contMap "+outcome+"->"+ BioCodec.extractSequenceType(outcome));
        contTypeMap.put(outcome, BioCodec.extractSequenceType(outcome));
      }
    }
    topStartIndex = buildModel.getIndex(TOP_START);
    completeIndex = checkModel.getIndex(COMPLETE);
    incompleteIndex = checkModel.getIndex(INCOMPLETE);
  }
  
  private void advanceTop(Parse p) {
    buildModel.eval(buildContextGenerator.getContext(p.getChildren(), 0), bprobs);
    p.addProb(Math.log(bprobs[topStartIndex]));
    checkModel.eval(checkContextGenerator.getContext(p.getChildren(), TOP_NODE, 0, 0), cprobs);
    p.addProb(Math.log(cprobs[completeIndex]));
    p.setType(TOP_NODE);
  }

  private Parse[] advanceParses(final Parse p, double probMass) {
    double q = 1 - probMass;
    /** The closest previous node which has been labeled as a start node. */
    Parse lastStartNode = null;
    /** The index of the closest previous node which has been labeled as a start node. */
    int lastStartIndex = -1;
    /** The type of the closest previous node which has been labeled as a start node. */
    String lastStartType = null;
    /** The index of the node which will be labeled in this iteration of advancing the parse. */
    int advanceNodeIndex;
    /** The node which will be labeled in this iteration of advancing the parse. */
    Parse advanceNode=null;
    Parse[] originalChildren = p.getChildren();
    Parse[] children = collapsePunctuation(originalChildren,punctSet);
    int numNodes = children.length;
    if (numNodes == 0) {
      return null;
    }
    //determines which node needs to be labeled and prior labels.
    for (advanceNodeIndex = 0; advanceNodeIndex < numNodes; advanceNodeIndex++) {
      advanceNode = children[advanceNodeIndex];
      if (advanceNode.getLabel() == null) {
        break;
      }
      else if (startTypeMap.containsKey(advanceNode.getLabel())) {
        lastStartType = startTypeMap.get(advanceNode.getLabel());
        lastStartNode = advanceNode;
        lastStartIndex = advanceNodeIndex;
        //System.err.println("lastStart "+i+" "+lastStart.label+" "+lastStart.prob);
      }
    }
    int originalAdvanceIndex = mapParseIndex(advanceNodeIndex,children,originalChildren);
    List<Parse> newParsesList = new ArrayList<Parse>(buildModel.getNumOutcomes());
    //call build
    buildModel.eval(buildContextGenerator.getContext(children, advanceNodeIndex), bprobs);
    double bprobSum = 0;
    while (bprobSum < probMass) {
      // The largest unadvanced labeling.
      int max = 0;
      for (int pi = 1; pi < bprobs.length; pi++) { //for each build outcome
        if (bprobs[pi] > bprobs[max]) {
          max = pi;
        }
      }
      if (bprobs[max] == 0) {
        break;
      }
      double bprob = bprobs[max];
      bprobs[max] = 0; //zero out so new max can be found
      bprobSum += bprob;
      String tag = buildModel.getOutcome(max);
      //System.out.println("trying "+tag+" "+bprobSum+" lst="+lst);
      if (max == topStartIndex) { // can't have top until complete
        continue;
      }
      //System.err.println(i+" "+tag+" "+bprob);
      if (startTypeMap.containsKey(tag)) { //update last start
        lastStartIndex = advanceNodeIndex;
        lastStartNode = advanceNode;
        lastStartType = startTypeMap.get(tag);
      }
      else if (contTypeMap.containsKey(tag)) {
        if (lastStartNode == null || !lastStartType.equals(contTypeMap.get(tag))) {
          continue; //Cont must match previous start or continue
        }
      }
      Parse newParse1 = (Parse) p.clone(); //clone parse
      if (createDerivationString) newParse1.getDerivation().append(max).append("-");
      newParse1.setChild(originalAdvanceIndex,tag); //replace constituent being labeled to create new derivation
      newParse1.addProb(Math.log(bprob));
      //check
      //String[] context = checkContextGenerator.getContext(newParse1.getChildren(), lastStartType, lastStartIndex, advanceNodeIndex);
      checkModel.eval(checkContextGenerator.getContext(collapsePunctuation(newParse1.getChildren(),punctSet), lastStartType, lastStartIndex, advanceNodeIndex), cprobs);
      //System.out.println("check "+lastStartType+" "+cprobs[completeIndex]+" "+cprobs[incompleteIndex]+" "+tag+" "+java.util.Arrays.asList(context));
      Parse newParse2 = newParse1;
      if (cprobs[completeIndex] > q) { //make sure a reduce is likely
        newParse2 = (Parse) newParse1.clone();
        if (createDerivationString) newParse2.getDerivation().append(1).append(".");
        newParse2.addProb(Math.log(cprobs[completeIndex]));
        Parse[] cons = new Parse[advanceNodeIndex - lastStartIndex + 1];
        boolean flat = true;
        //first
        cons[0] = lastStartNode;
        flat &= cons[0].isPosTag();
        //last
        cons[advanceNodeIndex - lastStartIndex] = advanceNode;
        flat &= cons[advanceNodeIndex - lastStartIndex].isPosTag();
        //middle
        for (int ci = 1; ci < advanceNodeIndex - lastStartIndex; ci++) {
          cons[ci] = children[ci + lastStartIndex];
          flat &= cons[ci].isPosTag();
        }
        if (!flat) { //flat chunks are done by chunker
          if (lastStartIndex == 0 && advanceNodeIndex == numNodes-1) { //check for top node to include end and begining punctuation
            //System.err.println("ParserME.advanceParses: reducing entire span: "+new Span(lastStartNode.getSpan().getStart(), advanceNode.getSpan().getEnd())+" "+lastStartType+" "+java.util.Arrays.asList(children));
            newParse2.insert(new Parse(p.getText(), p.getSpan(), lastStartType, cprobs[1], headRules.getHead(cons, lastStartType)));
          }
          else {
            newParse2.insert(new Parse(p.getText(), new Span(lastStartNode.getSpan().getStart(), advanceNode.getSpan().getEnd()), lastStartType, cprobs[1], headRules.getHead(cons, lastStartType)));
          }
          newParsesList.add(newParse2);
        }
      }
      if (cprobs[incompleteIndex] > q) { //make sure a shift is likely
        if (createDerivationString) newParse1.getDerivation().append(0).append(".");
        if (advanceNodeIndex != numNodes - 1) { //can't shift last element
          newParse1.addProb(Math.log(cprobs[incompleteIndex]));
          newParsesList.add(newParse1);
        }
      }
    }
    Parse[] newParses = new Parse[newParsesList.size()];
    newParsesList.toArray(newParses);
    return newParses;
  }
  
  public static Parse[] parseLine(String line, ShiftReduceParser parser, int numParses) {
    line = untokenizedParenPattern1.matcher(line).replaceAll("$1 $2");
    line = untokenizedParenPattern2.matcher(line).replaceAll("$1 $2");
    StringTokenizer str = new StringTokenizer(line);
    StringBuilder sb = new StringBuilder();
    List<String> tokens = new ArrayList<String>();
    while (str.hasMoreTokens()) {
      String tok = str.nextToken();
      tokens.add(tok);
      sb.append(tok).append(" ");
    }
    String text = sb.substring(0, sb.length() - 1);
    Parse p = new Parse(text, new Span(0, text.length()), ShiftReduceParser.INC_NODE, 0, 0);
    int start = 0;
    int i = 0;
    for (Iterator<String> ti = tokens.iterator(); ti.hasNext(); i++) {
      String tok = ti.next();
      p.insert(new Parse(text, new Span(start, start + tok.length()), ShiftReduceParser.TOK_NODE, 0, i));
      start += tok.length() + 1;
    }
    Parse[] parses;
    if (numParses == 1) {
      parses = new Parse[]{parser.parse(p)};
    } else {
      parses = parser.parse(p, numParses);
    }
    return parses;
  }
  
  public Parse[] parse(Parse tokens, int numParses) {
    if (createDerivationString) tokens.setDerivation(new StringBuffer(100));
    odh.clear();
    ndh.clear();
    completeParses.clear();
    int derivationStage = 0; //derivation length
    int maxDerivationLength = 2 * tokens.getChildCount() + 3;
    odh.add(tokens);
    Parse guess = null;
    double minComplete = 2;
    double bestComplete = -100000; //approximating -infinity/0 in ln domain
    while (odh.size() > 0 && (completeParses.size() < M || (odh.first()).getProb() < minComplete) && derivationStage < maxDerivationLength) {
      ndh = new ListHeap<Parse>(K);

      int derivationRank = 0;
      for (Iterator<Parse> pi = odh.iterator(); pi.hasNext() && derivationRank < K; derivationRank++) { // forearch derivation
        Parse tp = pi.next();
        //TODO: Need to look at this for K-best parsing cases
        /*
         if (tp.getProb() < bestComplete) { //this parse and the ones which follow will never win, stop advancing.
         break;
         }
         */
        if (guess == null && derivationStage == 2) {
          guess = tp;
        }
        if (debugOn) {
          System.out.print(derivationStage + " " + derivationRank + " "+tp.getProb());
          tp.show();
          System.out.println();
        }
        Parse[] nd;
        if (0 == derivationStage) {
          nd = advanceTags(tp);
        }
        else if (1 == derivationStage) {
          if (ndh.size() < K) {
            //System.err.println("advancing ts "+j+" "+ndh.size()+" < "+K);
            nd = advanceChunks(tp,bestComplete);
          }
          else {
            //System.err.println("advancing ts "+j+" prob="+((Parse) ndh.last()).getProb());
            nd = advanceChunks(tp,(ndh.last()).getProb());
          }
        }
        else { // i > 1
          nd = advanceParses(tp, Q);
        }
        if (nd != null) {
          for (int k = 0, kl = nd.length; k < kl; k++) {
            if (nd[k].complete()) {
              advanceTop(nd[k]);
              if (nd[k].getProb() > bestComplete) {
                bestComplete = nd[k].getProb();
              }
              if (nd[k].getProb() < minComplete) {
                minComplete = nd[k].getProb();
              }
              completeParses.add(nd[k]);
            }
            else {
              ndh.add(nd[k]);
            }
          }
        }
        else {
          if (reportFailedParse) {
            System.err.println("Couldn't advance parse "+derivationStage+" stage "+derivationRank+"!\n");
          }
          advanceTop(tp);
          completeParses.add(tp);
        }
      }
      derivationStage++;
      odh = ndh;
    }
    if (completeParses.size() == 0) {
      if (reportFailedParse) System.err.println("Couldn't find parse for: " + tokens);
      //Parse r = (Parse) odh.first();
      //r.show();
      //System.out.println();
      return new Parse[] {guess};
    }
    else if (numParses == 1){
      return new Parse[] {completeParses.first()};
    }
    else {
      List<Parse> topParses = new ArrayList<Parse>(numParses);
      while(!completeParses.isEmpty() && topParses.size() < numParses) {
        Parse tp = completeParses.extract();
        topParses.add(tp);
        //parses.remove(tp);
      }
      return topParses.toArray(new Parse[topParses.size()]);
    }
  }
  
  public Parse parse(Parse tokens) {

    if (tokens.getChildCount() > 0) {
      Parse p = parse(tokens,1)[0];
      setParents(p);
      return p;
    }
    else {
      return tokens;
    }
  }

  /**
   * Assigns parent references for the specified parse so that they
   * are consistent with the children references.
   * @param p The parse whose parent references need to be assigned.
   */
  public static void setParents(Parse p) {
    Parse[] children = p.getChildren();
    for (int ci = 0; ci < children.length; ci++) {
      children[ci].setParent(p);
      setParents(children[ci]);
    }
  }
  
  /**
   * Advances the parse by assigning it POS tags and returns multiple tag sequences.
   * @param p The parse to be tagged.
   * @return Parses with different POS-tag sequence assignments.
   */
  protected Parse[] advanceTags(final Parse p) {
    Parse[] children = p.getChildren();
    String[] words = new String[children.length];
    double[] probs = new double[words.length];
    for (int i = 0,il = children.length; i < il; i++) {
      words[i] = children[i].getCoveredText();
    }
    Sequence[] ts = tagger.topKSequences(words);
    if (ts.length == 0) {
      System.err.println("no tag sequence");
    }
    Parse[] newParses = new Parse[ts.length];
    for (int i = 0; i < ts.length; i++) {
      String[] tags = ts[i].getOutcomes().toArray(new String[words.length]);
      ts[i].getProbs(probs);
      newParses[i] = (Parse) p.clone(); //copies top level
      if (createDerivationString) newParses[i].getDerivation().append(i).append(".");
      for (int j = 0; j < words.length; j++) {
        Parse word = children[j];
        //System.err.println("inserting tag "+tags[j]);
        double prob = probs[j];
        newParses[i].insert(new Parse(word.getText(), word.getSpan(), tags[j].replaceAll("-" + BioCodec.START, ""), prob,j));
        newParses[i].addProb(Math.log(prob));
        //newParses[i].show();
      }
    }
    return newParses;
  }

  /**
   * Returns the top chunk sequences for the specified parse.
   * @param p A pos-tag assigned parse.
   * @param minChunkScore A minimum score below which chunks should not be advanced.
   * @return The top chunk assignments to the specified parse.
   */
  protected Parse[] advanceChunks(final Parse p, double minChunkScore) {
    // chunk
    Parse[] children = p.getChildren();
    String words[] = new String[children.length];
    String ptags[] = new String[words.length];
    double probs[] = new double[words.length];
    Parse sp = null;
    for (int i = 0, il = children.length; i < il; i++) {
      sp = children[i];
      words[i] = sp.getHead().getCoveredText();
      ptags[i] = sp.getType();
    }
    //System.err.println("adjusted mcs = "+(minChunkScore-p.getProb()));
    Sequence[] cs = chunker.topKSequences(words, ptags,minChunkScore-p.getProb());
    Parse[] newParses = new Parse[cs.length];
    for (int si = 0, sl = cs.length; si < sl; si++) {
      newParses[si] = (Parse) p.clone(); //copies top level
      if (createDerivationString) newParses[si].getDerivation().append(si).append(".");
      String[] tags = cs[si].getOutcomes().toArray(new String[words.length]);
      cs[si].getProbs(probs);
      int start = -1;
      int end = 0;
      String type = null;
      //System.err.print("sequence "+si+" ");
      for (int j = 0; j <= tags.length; j++) {
        //if (j != tags.length) {System.err.println(words[j]+" "+ptags[j]+" "+tags[j]+" "+probs.get(j));}
        if (j != tags.length) {
          newParses[si].addProb(Math.log(probs[j]));
        }
        if (j != tags.length && tags[j].endsWith(BioCodec.CONTINUE)) { // if continue just update end chunking tag don't use contTypeMap
          end = j;
        }
        else { //make previous constituent if it exists
          if (type != null) {
            //System.err.println("inserting tag "+tags[j]);
            Parse p1 = p.getChildren()[start];
            Parse p2 = p.getChildren()[end];
            //System.err.println("Putting "+type+" at "+start+","+end+" for "+j+" "+newParses[si].getProb());
            Parse[] cons = new Parse[end - start + 1];
            cons[0] = p1;
            //cons[0].label="Start-"+type;
            if (end - start != 0) {
              cons[end - start] = p2;
              //cons[end-start].label="Cont-"+type;
              for (int ci = 1; ci < end - start; ci++) {
                cons[ci] = p.getChildren()[ci + start];
                //cons[ci].label="Cont-"+type;
              }
            }
            Parse chunk = new Parse(p1.getText(), new Span(p1.getSpan().getStart(), p2.getSpan().getEnd()), type, 1, headRules.getHead(cons, type));
            chunk.isChunk(true);
            newParses[si].insert(chunk);
          }
          if (j != tags.length) { //update for new constituent
            if (tags[j].endsWith(BioCodec.START)) { // don't use startTypeMap these are chunk tags
              type = tags[j].replaceAll("-" + BioCodec.START, "");
              start = j;
              end = j;
            }
            else { // other
              type = null;
            }
          }
        }
      }
      //newParses[si].show();System.out.println();
    }
    return newParses;
  }

  public static ParserModel train(String languageCode,
      ObjectStream<Parse> parseSamples, HeadRules rules,
      TrainingParameters trainParams, ParserFactory parserFactory,
      TrainingParameters taggerParams,
      SequenceLabelerFactory taggerFactory,
      TrainingParameters chunkerParams,
      SequenceLabelerFactory chunkerFactory) throws IOException {

    String beamSizeString = trainParams.getSettings().get(BeamSearch.BEAM_SIZE_PARAMETER);
    int beamSize = DEFAULT_BEAMSIZE;
    if (beamSizeString != null) {
      beamSize = Integer.parseInt(beamSizeString);
    }
    parseSamples.reset();

    Map<String, String> manifestInfoEntries = new HashMap<String, String>();

    // TODO tag
    System.err.println("Training POS tagger...");
    SequenceLabelerModel posModel = SequenceLabelerME.train(languageCode, null,
        new ParseToTabulatedFormat(parseSamples), taggerParams, taggerFactory);
    parseSamples.reset();

    // TODO chunk
    System.err.println("Training chunker...");
    SequenceLabelerModel chunkModel = SequenceLabelerME.train(languageCode,
        null, new ParseToCoNLL02Format(parseSamples), chunkerParams, chunkerFactory);
    parseSamples.reset();

    // TODO build
    System.err.println("Training builder...");
    ObjectStream<Event> bes = new ParserEventStream(parseSamples, rules,
        ParserEventTypeEnum.BUILD, parserFactory);
    Map<String, String> buildReportMap = new HashMap<String, String>();
    EventTrainer trainer = TrainerFactory.getEventTrainer(
        trainParams.getSettings(), buildReportMap);
    MaxentModel buildModel = trainer.train(bes);
    mergeReportIntoManifest(manifestInfoEntries, buildReportMap, "build");
    parseSamples.reset();

    // TODO check
    System.err.println("Training checker...");
    ObjectStream<Event> kes = new ParserEventStream(parseSamples, rules,
        ParserEventTypeEnum.CHECK);
    Map<String, String> checkReportMap = new HashMap<String, String>();
    EventTrainer checkTrainer = TrainerFactory.getEventTrainer(
        trainParams.getSettings(), checkReportMap);
    MaxentModel checkModel = checkTrainer.train(kes);
    mergeReportIntoManifest(manifestInfoEntries, checkReportMap, "check");

    return new ParserModel(languageCode, buildModel, checkModel, posModel,
        chunkModel, beamSize, rules, manifestInfoEntries);
  }
  
  public static ParserModel train(String languageCode,
      ObjectStream<Parse> parseSamples, HeadRules rules,
      TrainingParameters trainParams, ParserFactory parserFactory,
      SequenceLabelerModel posModel,
      TrainingParameters chunkerParams,
      SequenceLabelerFactory chunkerFactory) throws IOException {

    String beamSizeString = trainParams.getSettings().get(BeamSearch.BEAM_SIZE_PARAMETER);
    int beamSize = DEFAULT_BEAMSIZE;
    if (beamSizeString != null) {
      beamSize = Integer.parseInt(beamSizeString);
    }
    parseSamples.reset();

    Map<String, String> manifestInfoEntries = new HashMap<String, String>();

    // TODO chunk
    System.err.println("Training chunker...");
    SequenceLabelerModel chunkModel = SequenceLabelerME.train(languageCode,
        null, new ParseToCoNLL02Format(parseSamples), chunkerParams, chunkerFactory);
    parseSamples.reset();

    // TODO build
    System.err.println("Training builder...");
    ObjectStream<Event> bes = new ParserEventStream(parseSamples, rules,
        ParserEventTypeEnum.BUILD, parserFactory);
    Map<String, String> buildReportMap = new HashMap<String, String>();
    EventTrainer trainer = TrainerFactory.getEventTrainer(
        trainParams.getSettings(), buildReportMap);
    MaxentModel buildModel = trainer.train(bes);
    mergeReportIntoManifest(manifestInfoEntries, buildReportMap, "build");
    parseSamples.reset();

    // TODO check
    System.err.println("Training checker...");
    ObjectStream<Event> kes = new ParserEventStream(parseSamples, rules,
        ParserEventTypeEnum.CHECK);
    Map<String, String> checkReportMap = new HashMap<String, String>();
    EventTrainer checkTrainer = TrainerFactory.getEventTrainer(
        trainParams.getSettings(), checkReportMap);
    MaxentModel checkModel = checkTrainer.train(kes);
    mergeReportIntoManifest(manifestInfoEntries, checkReportMap, "check");

    return new ParserModel(languageCode, buildModel, checkModel, posModel,
        chunkModel, beamSize, rules, manifestInfoEntries);
  }
  
  public static void mergeReportIntoManifest(Map<String, String> manifest,
      Map<String, String> report, String namespace) {

    for (Map.Entry<String, String> entry : report.entrySet()) {
      manifest.put(namespace + "." + entry.getKey(), entry.getValue());
    }
  }
  
  /**
   * Removes the punctuation from the specified set of chunks, adds it to the parses
   * adjacent to the punctuation is specified, and returns a new array of parses with the punctuation
   * removed.
   * @param chunks A set of parses.
   * @param punctSet The set of punctuation which is to be removed.
   * @return An array of parses which is a subset of chunks with punctuation removed.
   */
  public static Parse[] collapsePunctuation(Parse[] chunks, Set<String> punctSet) {
    List<Parse> collapsedParses = new ArrayList<Parse>(chunks.length);
    int lastNonPunct = -1;
    int nextNonPunct = -1;
    for (int ci=0,cn=chunks.length;ci<cn;ci++) {
      if (punctSet.contains(chunks[ci].getType())) {
        if (lastNonPunct >= 0) {
          chunks[lastNonPunct].addNextPunctuation(chunks[ci]);
        }
        for (nextNonPunct=ci+1;nextNonPunct<cn;nextNonPunct++) {
          if (!punctSet.contains(chunks[nextNonPunct].getType())) {
            break;
          }
        }
        if (nextNonPunct < cn) {
          chunks[nextNonPunct].addPreviousPunctuation(chunks[ci]);
        }
      }
      else {
        collapsedParses.add(chunks[ci]);
        lastNonPunct = ci;
      }
    }
    if (collapsedParses.size() == chunks.length) {
      return chunks;
    }
    //System.err.println("collapsedPunctuation: collapsedParses"+collapsedParses);
    return collapsedParses.toArray(new Parse[collapsedParses.size()]);
  }
  
  /**
   * Determines the mapping between the specified index into the specified parses without punctuation to
   * the corresponding index into the specified parses.
   * @param index An index into the parses without punctuation.
   * @param nonPunctParses The parses without punctuation.
   * @param parses The parses wit punctuation.
   * @return An index into the specified parses which corresponds to the same node the specified index
   * into the parses with punctuation.
   */
  private int mapParseIndex(int index, Parse[] nonPunctParses, Parse[] parses) {
    int parseIndex = index;
    while (parses[parseIndex] != nonPunctParses[index]) {
      parseIndex++;
    }
    return parseIndex;
  }
  
  /**
   * Creates a n-gram dictionary from the specified data stream using the specified head rule and specified cut-off.
   *
   * @param data The data stream of parses.
   * @param rules The head rules for the parses.
   * @param cutoff The minimum number of entries required for the n-gram to be saved as part of the dictionary.
   * @throws IOException if io problems
   * @return A dictionary object.
   */
  public static Dictionary buildDictionary(ObjectStream<Parse> data, HeadRules rules, int cutoff)
      throws IOException {

    TrainingParameters params = new TrainingParameters();
    params.put("dict", TrainingParameters.CUTOFF_PARAM, Integer.toString(cutoff));

    return buildDictionary(data, rules, params);
  }
  
  /**
   * Creates a n-gram dictionary from the specified data stream using the specified head rule and specified cut-off.
   *
   * @param data The data stream of parses.
   * @param rules The head rules for the parses.
   * @param params can contain a cutoff, the minimum number of entries required for the n-gram to be saved as part of the dictionary.
   * @throws IOException if io problems
   * @return A dictionary object.
   */
  public static Dictionary buildDictionary(ObjectStream<Parse> data, HeadRules rules, TrainingParameters params) throws IOException {

    System.err.println("Building automatic ngram dictionary...");
    int cutoff = 5;

    String cutoffString = params.getSettings("dict").
        get(TrainingParameters.CUTOFF_PARAM);

    if (cutoffString != null) {
      // TODO: Maybe throw illegal argument exception if not parse able
      cutoff = Integer.parseInt(cutoffString);
    }

    NGramModel mdict = new NGramModel();
    Parse p;
    while((p = data.read()) != null) {
      p.updateHeads(rules);
      Parse[] pwords = p.getTagNodes();
      String[] words = new String[pwords.length];
      //add all uni-grams
      for (int wi=0;wi<words.length;wi++) {
        words[wi] = pwords[wi].getCoveredText();
      }

      mdict.add(new StringList(words), 1, 1);
      //add tri-grams and bi-grams for inital sequence
      Parse[] chunks = collapsePunctuation(ParserEventStream.getInitialChunks(p),rules.getPunctuationTags());
      String[] cwords = new String[chunks.length];
      for (int wi=0;wi<cwords.length;wi++) {
        cwords[wi] = chunks[wi].getHead().getCoveredText();
      }
      mdict.add(new StringList(cwords), 2, 3);

      //emulate reductions to produce additional n-grams
      int ci = 0;
      while (ci < chunks.length) {
        //System.err.println("chunks["+ci+"]="+chunks[ci].getHead().getCoveredText()+" chunks.length="+chunks.length + "  " + chunks[ci].getParent());

        if (chunks[ci].getParent() == null) {
          chunks[ci].show();
        }
        if (lastChild(chunks[ci], chunks[ci].getParent(),rules.getPunctuationTags())) {
          //perform reduce
          int reduceStart = ci;
          while (reduceStart >=0 && chunks[reduceStart].getParent() == chunks[ci].getParent()) {
            reduceStart--;
          }
          reduceStart++;
          chunks = ParserEventStream.reduceChunks(chunks,ci,chunks[ci].getParent());
          ci = reduceStart;
          if (chunks.length != 0) {
            String[] window = new String[5];
            int wi = 0;
            if (ci-2 >= 0) window[wi++] = chunks[ci-2].getHead().getCoveredText();
            if (ci-1 >= 0) window[wi++] = chunks[ci-1].getHead().getCoveredText();
            window[wi++] = chunks[ci].getHead().getCoveredText();
            if (ci+1 < chunks.length) window[wi++] = chunks[ci+1].getHead().getCoveredText();
            if (ci+2 < chunks.length) window[wi++] = chunks[ci+2].getHead().getCoveredText();
            if (wi < 5) {
              String[] subWindow = new String[wi];
              for (int swi=0;swi<wi;swi++) {
                subWindow[swi]=window[swi];
              }
              window = subWindow;
            }
            if (window.length >=3) {
              mdict.add(new StringList(window), 2, 3);
            }
            else if (window.length == 2) {
              mdict.add(new StringList(window), 2, 2);
            }
          }
          ci=reduceStart-1; //ci will be incremented at end of loop
        }
        ci++;
      }
    }
    //System.err.println("gas,and="+mdict.getCount((new TokenList(new String[] {"gas","and"}))));
    mdict.cutoff(cutoff, Integer.MAX_VALUE);
    System.err.println("Automatic dictionary created!");
    return mdict.toDictionary(true);
  }
  
  private static boolean lastChild(Parse child, Parse parent, Set<String> punctSet) {
    if (parent == null) {
      return false;
    }

    Parse[] kids = collapsePunctuation(parent.getChildren(), punctSet);
    return (kids[kids.length - 1] == child);
  }

  
  

}

