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

package eus.ixa.ixa.pipe.ml.sequence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eus.ixa.ixa.pipe.ml.utils.Span;

import opennlp.tools.ml.BeamSearch;
import opennlp.tools.ml.EventModelSequenceTrainer;
import opennlp.tools.ml.EventTrainer;
import opennlp.tools.ml.SequenceTrainer;
import opennlp.tools.ml.TrainerFactory;
import opennlp.tools.ml.TrainerFactory.TrainerType;
import opennlp.tools.ml.model.Event;
import opennlp.tools.ml.model.MaxentModel;
import opennlp.tools.ml.model.SequenceClassificationModel;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.Sequence;
import opennlp.tools.util.SequenceValidator;
import opennlp.tools.util.TrainingParameters;
import opennlp.tools.util.featuregen.AdaptiveFeatureGenerator;
import opennlp.tools.util.featuregen.AdditionalContextFeatureGenerator;
import opennlp.tools.util.featuregen.BigramNameFeatureGenerator;
import opennlp.tools.util.featuregen.CachedFeatureGenerator;
import opennlp.tools.util.featuregen.OutcomePriorFeatureGenerator;
import opennlp.tools.util.featuregen.PreviousMapFeatureGenerator;
import opennlp.tools.util.featuregen.SentenceFeatureGenerator;
import opennlp.tools.util.featuregen.TokenClassFeatureGenerator;
import opennlp.tools.util.featuregen.TokenFeatureGenerator;
import opennlp.tools.util.featuregen.WindowFeatureGenerator;

public class SequenceLabelerME implements SequenceLabeler {

  private static String[][] EMPTY = new String[0][0];
  public static final int DEFAULT_BEAM_SIZE = 3;
  private static final Pattern typedOutcomePattern = Pattern.compile("(.+)-\\w+");
  
  public static Pattern startPattern = Pattern.compile("(\\S+)-start", Pattern.UNICODE_CHARACTER_CLASS);
  public static Pattern contPattern = Pattern.compile("(\\S+)-cont", Pattern.UNICODE_CHARACTER_CLASS);
  public static Pattern lastPattern = Pattern.compile("(\\S+)-last", Pattern.UNICODE_CHARACTER_CLASS);
  public static Pattern unitPattern = Pattern.compile("(\\S+)-unit", Pattern.UNICODE_CHARACTER_CLASS);
  public static Pattern otherPattern = Pattern.compile("other");

  private SequenceLabelerCodec<String> seqCodec = new BioCodec();

  protected SequenceClassificationModel<String> model;

  protected SequenceLabelerContextGenerator contextGenerator;
  private Sequence bestSequence;

  private AdditionalContextFeatureGenerator additionalContextFeatureGenerator
          = new AdditionalContextFeatureGenerator();
  private SequenceValidator<String> sequenceValidator;

  public SequenceLabelerME(SequenceLabelerModel model) {

    SequenceLabelerFactory factory = model.getFactory();

    seqCodec = factory.createSequenceCodec();
    sequenceValidator = seqCodec.createSequenceValidator();
    this.model = model.getSequenceLabelerModel();
    contextGenerator = factory.createContextGenerator();

    // TODO: We should deprecate this. And come up with a better solution!
    contextGenerator.addFeatureGenerator(
            new WindowFeatureGenerator(additionalContextFeatureGenerator, 8, 8));
  }

  @Deprecated
  /**
   * @deprecated the default feature generation is now always included in the models and loaded
   * if not by the factory. Subclasses using this methods should do the same.
   */
  static AdaptiveFeatureGenerator createFeatureGenerator() {
    return new CachedFeatureGenerator(
            new AdaptiveFeatureGenerator[]{
              new WindowFeatureGenerator(new TokenFeatureGenerator(), 2, 2),
              new WindowFeatureGenerator(new TokenClassFeatureGenerator(true), 2, 2),
              new OutcomePriorFeatureGenerator(),
              new PreviousMapFeatureGenerator(),
              new BigramNameFeatureGenerator(),
              new SentenceFeatureGenerator(true, false)
            });
  }

  public Span[] tag(String[] tokens) {
    return tag(tokens, EMPTY);
  }
  
  /**
   * Generates sequence tags for the given sequence, returning
   * spans for any identified sequences.
   *
   * @param tokens an array of the tokens or words, typically a
   * sentence.
   * @param additionalContext features which are based on context outside of the
   * sentence but which should also be used.
   *
   * @return an array of spans for each of the names identified.
   */
  public Span[] tag(String[] tokens, String[][] additionalContext) {
    additionalContextFeatureGenerator.setCurrentContext(additionalContext);
    bestSequence = model.bestSequence(tokens, additionalContext, contextGenerator, sequenceValidator);
    List<String> c = bestSequence.getOutcomes();
    contextGenerator.updateAdaptiveData(tokens, c.toArray(new String[c.size()]));
    Span[] spans = seqCodec.decode(c);
    spans = setProbs(spans);
    return spans;
  }
  
  public String[] tagToStrings(String[] tokens) {
    bestSequence = model.bestSequence(tokens, null, contextGenerator, sequenceValidator);
    List<String> c = bestSequence.getOutcomes();
    contextGenerator.updateAdaptiveData(tokens, c.toArray(new String[c.size()]));
    return c.toArray(new String[c.size()]);
  }
  
  /**
   * Returns at most the specified number of taggings for the specified sentence.
   *
   * @param numTaggings the number of labels to be returned.
   * @param tokens an array of tokens which make up a sentence.
   *
   * @return at most the specified number of labels for the specified sentence.
   */
  public Span[][] tag(int numTaggings, String[] tokens) {
    Sequence[] bestSequences = model.bestSequences(numTaggings, tokens, null,
        contextGenerator, sequenceValidator);
    Span[][] tags = new Span[bestSequences.length][];
    for (int i = 0; i < tags.length; i++) {
      List<String> c = bestSequences[i].getOutcomes();
      contextGenerator.updateAdaptiveData(tokens, c.toArray(new String[c.size()]));
      Span[] spans = seqCodec.decode(c);
      tags[i] = spans;
    }
    return tags;
  }
  
  public Sequence[] topKSequences(String[] tokens) {
    return model.bestSequences(DEFAULT_BEAM_SIZE, tokens, null, contextGenerator, sequenceValidator);
  }
  
  public Sequence[] topKSequences(String[] tokens, String[] tags, double minSequenceScore) {
    return model.bestSequences(DEFAULT_BEAM_SIZE, tokens, new Object[] { tags }, minSequenceScore,
        contextGenerator, sequenceValidator);
  }

  /**
   * Forgets all adaptive data which was collected during previous calls to one
   * of the find methods.
   *
   * This method is typical called at the end of a document.
   */
  public void clearAdaptiveData() {
    contextGenerator.clearAdaptiveData();
  }

  /**
   * Populates the specified array with the probabilities of the last decoded
   * sequence. The sequence was determined based on the previous call to
   * <code>chunk</code>. The specified array should be at least as large as the
   * number of tokens in the previous call to <code>chunk</code>.
   *
   * @param probs An array used to hold the probabilities of the last decoded
   * sequence.
   */
  public void probs(double[] probs) {
    bestSequence.getProbs(probs);
  }

  /**
   * Returns an array with the probabilities of the last decoded sequence. The
   * sequence was determined based on the previous call to <code>chunk</code>.
   *
   * @return An array with the same number of probabilities as tokens were sent
   * to <code>chunk</code> when it was last called.
   */
  public double[] probs() {
    return bestSequence.getProbs();
  }

  /**
   * sets the probs for the spans
   *
   * @param spans
   * @return
   */
  private Span[] setProbs(Span[] spans) {
     double[] probs = probs(spans);
     if (probs != null) {

      for (int i = 0; i < probs.length; i++) {
        double prob = probs[i];
        spans[i]= new Span(spans[i], prob);
      }
    }
    return spans;
  }

  /**
   * Returns an array of probabilities for each of the specified spans which is
   * the arithmetic mean of the probabilities for each of the outcomes which
   * make up the span.
   *
   * @param spans The spans of the names for which probabilities are desired.
   *
   * @return an array of probabilities for each of the specified spans.
   */
  public double[] probs(Span[] spans) {

    double[] sprobs = new double[spans.length];
    double[] probs = bestSequence.getProbs();

    for (int si = 0; si < spans.length; si++) {
      double p = 0;
      for (int oi = spans[si].getStart(); oi < spans[si].getEnd(); oi++) {
        p += probs[oi];
      }
      p /= spans[si].length();
      sprobs[si] = p;
    }

    return sprobs;
  }

  public static SequenceLabelerModel train(String languageCode, ObjectStream<SequenceLabelSample> samples, TrainingParameters trainParams,
          SequenceLabelerFactory factory) throws IOException {
    String beamSizeString = trainParams.getSettings().get(BeamSearch.BEAM_SIZE_PARAMETER);

    int beamSize = SequenceLabelerME.DEFAULT_BEAM_SIZE;
    if (beamSizeString != null) {
      beamSize = Integer.parseInt(beamSizeString);
    }

    Map<String, String> manifestInfoEntries = new HashMap<String, String>();

    MaxentModel nameFinderModel = null;

    SequenceClassificationModel<String> seqModel = null;

    TrainerType trainerType = TrainerFactory.getTrainerType(trainParams.getSettings());

    if (TrainerType.EVENT_MODEL_TRAINER.equals(trainerType)) {
      ObjectStream<Event> eventStream = new SequenceLabelerEventStream(samples,
              factory.createContextGenerator(), factory.createSequenceCodec());

      EventTrainer trainer = TrainerFactory.getEventTrainer(trainParams.getSettings(), manifestInfoEntries);
      nameFinderModel = trainer.train(eventStream);
    } // TODO: Maybe it is not a good idea, that these two don't use the context generator ?!
    // These also don't use the sequence codec ?!
    else if (TrainerType.EVENT_MODEL_SEQUENCE_TRAINER.equals(trainerType)) {
      SequenceLabelerSequenceStream ss = new SequenceLabelerSequenceStream(samples, factory.createContextGenerator());

      EventModelSequenceTrainer trainer = TrainerFactory.getEventModelSequenceTrainer(
              trainParams.getSettings(), manifestInfoEntries);
      nameFinderModel = trainer.train(ss);
    } else if (TrainerType.SEQUENCE_TRAINER.equals(trainerType)) {
      SequenceTrainer trainer = TrainerFactory.getSequenceModelTrainer(
              trainParams.getSettings(), manifestInfoEntries);

      SequenceLabelerSequenceStream ss = new SequenceLabelerSequenceStream(samples, factory.createContextGenerator(), false);
      seqModel = trainer.train(ss);
    } else {
      throw new IllegalStateException("Unexpected trainer type!");
    }

    if (seqModel != null) {
      return new SequenceLabelerModel(languageCode, seqModel, factory.getFeatureGenerator(),
              factory.getResources(), manifestInfoEntries, factory.getSequenceCodec(), factory);
    } else {
      return new SequenceLabelerModel(languageCode, nameFinderModel, beamSize, factory.getFeatureGenerator(),
              factory.getResources(), manifestInfoEntries, factory.getSequenceCodec(), factory);
    }
  }

  /**
   * Gets the name type from the outcome
   *
   * @param outcome the outcome
   * @return the name type, or null if not set
   */
  static final String extractNameType(String outcome) {
    Matcher matcher = typedOutcomePattern.matcher(outcome);
    if (matcher.matches()) {
      String nameType = matcher.group(1);
      return nameType;
    }

    return null;
  }

  /**
   * Removes spans with are intersecting or crossing in anyway.
   *
   * <p>
   * The following rules are used to remove the spans:<br>
   * Identical spans: The first span in the array after sorting it remains<br>
   * Intersecting spans: The first span after sorting remains<br>
   * Contained spans: All spans which are contained by another are removed<br>
   *
   * @param spans the spans
   *
   * @return non-overlapping spans
   */
  public static Span[] dropOverlappingSpans(Span[] spans) {

    List<Span> sortedSpans = new ArrayList<Span>(spans.length);
    Collections.addAll(sortedSpans, spans);
    Collections.sort(sortedSpans);

    Iterator<Span> it = sortedSpans.iterator();
    Span lastSpan = null;
    while (it.hasNext()) {
      Span span = it.next();

      if (lastSpan != null) {
        if (lastSpan.intersects(span)) {
          it.remove();
          span = lastSpan;
        }
      }
      lastSpan = span;
    }
    return sortedSpans.toArray(new Span[sortedSpans.size()]);
  }
  
  /**
   * Decode Sequences from an array of Strings.
   * @param preds the sequences in an string array.
   * @return the decoded sequences
   */
  public String[] decodeSequences(String[] preds) {
    List<String> decodedSequences = new ArrayList<>();
    for (String pred : preds) {
      pred = startPattern.matcher(pred).replaceAll("B-$1");
      pred = contPattern.matcher(pred).replaceAll("I-$1");
      pred = lastPattern.matcher(pred).replaceAll("I-$1");
      pred = unitPattern.matcher(pred).replaceAll("B-$1");
      pred = otherPattern.matcher(pred).replaceAll("O");
      decodedSequences.add(pred);
    }
    return decodedSequences.toArray(new String[decodedSequences.size()]);
  }
  
}

