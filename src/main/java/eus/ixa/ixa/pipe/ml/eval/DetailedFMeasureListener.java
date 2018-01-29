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

package eus.ixa.ixa.pipe.ml.eval;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import eus.ixa.ixa.pipe.ml.utils.Span;
import opennlp.tools.util.eval.EvaluationMonitor;

/**
 * This listener will gather detailed information about the sample under
 * evaluation and will allow detailed FMeasure for each outcome.
 * <p>
 * <b>Note:</b> Do not use this class, internal use only!
 */
public abstract class DetailedFMeasureListener<T>
    implements EvaluationMonitor<T> {

  private int samples = 0;
  private final Stats generalStats = new Stats();
  private final Map<String, Stats> statsForOutcome = new HashMap<String, Stats>();

  protected abstract Span[] asSpanArray(T sample);

  @Override
  public void correctlyClassified(final T reference, final T prediction) {
    this.samples++;
    // add all true positives!
    final Span[] spans = asSpanArray(reference);
    for (final Span span : spans) {
      addTruePositive(span.getType());
    }
  }

  @Override
  public void missclassified(final T reference, final T prediction) {
    this.samples++;
    final Span[] references = asSpanArray(reference);
    final Span[] predictions = asSpanArray(prediction);

    final Set<Span> refSet = new HashSet<Span>(Arrays.asList(references));
    final Set<Span> predSet = new HashSet<Span>(Arrays.asList(predictions));

    for (final Span ref : refSet) {
      if (predSet.contains(ref)) {
        addTruePositive(ref.getType());
      } else {
        addFalseNegative(ref.getType());
      }
    }

    for (final Span pred : predSet) {
      if (!refSet.contains(pred)) {
        addFalsePositive(pred.getType());
      }
    }
  }

  private void addTruePositive(final String type) {
    final Stats s = initStatsForOutcomeAndGet(type);
    s.incrementTruePositive();
    s.incrementTarget();

    this.generalStats.incrementTruePositive();
    this.generalStats.incrementTarget();
  }

  private void addFalsePositive(final String type) {
    final Stats s = initStatsForOutcomeAndGet(type);
    s.incrementFalsePositive();
    this.generalStats.incrementFalsePositive();
  }

  private void addFalseNegative(final String type) {
    final Stats s = initStatsForOutcomeAndGet(type);
    s.incrementTarget();
    this.generalStats.incrementTarget();

  }

  private Stats initStatsForOutcomeAndGet(final String type) {
    if (!this.statsForOutcome.containsKey(type)) {
      this.statsForOutcome.put(type, new Stats());
    }
    return this.statsForOutcome.get(type);
  }

  private static final String PERCENT = "%\u00207.2f%%";
  private static final String FORMAT = "%12s: precision: " + PERCENT
      + ";  recall: " + PERCENT + "; F1: " + PERCENT + ".";
  private static final String FORMAT_EXTRA = FORMAT
      + " [target: %3d; tp: %3d; fp: %3d]";

  public String createReport() {
    return createReport(Locale.getDefault());
  }

  public String createReport(final Locale locale) {
    final StringBuilder ret = new StringBuilder();
    final int tp = this.generalStats.getTruePositives();
    final int found = this.generalStats.getFalsePositives() + tp;
    ret.append("Evaluated " + this.samples + " samples with "
        + this.generalStats.getTarget() + " entities; found: " + found
        + " entities; correct: " + tp + ".\n");

    ret.append(String.format(locale, FORMAT, "TOTAL",
        zeroOrPositive(this.generalStats.getPrecisionScore() * 100),
        zeroOrPositive(this.generalStats.getRecallScore() * 100),
        zeroOrPositive(this.generalStats.getFMeasure() * 100)));
    ret.append("\n");
    final SortedSet<String> set = new TreeSet<String>(new F1Comparator());
    set.addAll(this.statsForOutcome.keySet());
    for (final String type : set) {

      ret.append(String.format(locale, FORMAT_EXTRA, type,
          zeroOrPositive(
              this.statsForOutcome.get(type).getPrecisionScore() * 100),
          zeroOrPositive(this.statsForOutcome.get(type).getRecallScore() * 100),
          zeroOrPositive(this.statsForOutcome.get(type).getFMeasure() * 100),
          this.statsForOutcome.get(type).getTarget(),
          this.statsForOutcome.get(type).getTruePositives(),
          this.statsForOutcome.get(type).getFalsePositives()));
      ret.append("\n");
    }

    return ret.toString();
  }

  @Override
  public String toString() {
    return createReport();
  }

  private double zeroOrPositive(final double v) {
    if (v < 0) {
      return 0;
    }
    return v;
  }

  private class F1Comparator implements Comparator<String> {
    @Override
    public int compare(final String o1, final String o2) {
      if (o1.equals(o2)) {
        return 0;
      }
      double t1 = 0;
      double t2 = 0;

      if (DetailedFMeasureListener.this.statsForOutcome.containsKey(o1)) {
        t1 += DetailedFMeasureListener.this.statsForOutcome.get(o1)
            .getFMeasure();
      }
      if (DetailedFMeasureListener.this.statsForOutcome.containsKey(o2)) {
        t2 += DetailedFMeasureListener.this.statsForOutcome.get(o2)
            .getFMeasure();
      }

      t1 = zeroOrPositive(t1);
      t2 = zeroOrPositive(t2);

      if (t1 + t2 > 0d) {
        if (t1 > t2) {
          return -1;
        }
        return 1;
      }
      return o1.compareTo(o2);
    }

  }

  /**
   * Store the statistics.
   */
  private class Stats {

    // maybe we could use FMeasure class, but it wouldn't allow us to get
    // details like total number of false positives and true positives.

    private int falsePositiveCounter = 0;
    private int truePositiveCounter = 0;
    private int targetCounter = 0;

    public void incrementFalsePositive() {
      this.falsePositiveCounter++;
    }

    public void incrementTruePositive() {
      this.truePositiveCounter++;
    }

    public void incrementTarget() {
      this.targetCounter++;
    }

    public int getFalsePositives() {
      return this.falsePositiveCounter;
    }

    public int getTruePositives() {
      return this.truePositiveCounter;
    }

    public int getTarget() {
      return this.targetCounter;
    }

    /**
     * Retrieves the arithmetic mean of the precision scores calculated for each
     * evaluated sample.
     *
     * @return the arithmetic mean of all precision scores
     */
    public double getPrecisionScore() {
      final int tp = getTruePositives();
      final int selected = tp + getFalsePositives();
      return selected > 0 ? (double) tp / (double) selected : 0;
    }

    /**
     * Retrieves the arithmetic mean of the recall score calculated for each
     * evaluated sample.
     *
     * @return the arithmetic mean of all recall scores
     */
    public double getRecallScore() {
      final int target = getTarget();
      final int tp = getTruePositives();
      return target > 0 ? (double) tp / (double) target : 0;
    }

    /**
     * Retrieves the f-measure score.
     *
     * f-measure = 2 * precision * recall / (precision + recall)
     *
     * @return the f-measure or -1 if precision + recall <= 0
     */
    public double getFMeasure() {

      if (getPrecisionScore() + getRecallScore() > 0) {
        return 2 * (getPrecisionScore() * getRecallScore())
            / (getPrecisionScore() + getRecallScore());
      } else {
        // cannot divide by zero, return error code
        return -1;
      }
    }

  }

}
