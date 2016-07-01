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


/**
 * Calculates the arithmetic mean of values
 * added with the {@link #add(double)} method.
 */
public class Accuracy {

  /**
   * The sum of all added values.
   */
  private double sum;

  /**
   * The number of times a value was added.
   */
  private long count;
  
  private double sentSum;
  private double sentCount;

  /**
   * Adds a value to the arithmetic mean.
   *
   * @param value the value which should be added
   * to the arithmetic mean.
   */
  public void add(double value) {
    add(value, 1);
  }

  /**
   * Adds a value count times to the arithmetic mean.
   *
   * @param value the value which should be added
   * to the arithmetic mean.
   *
   * @param count number of times the value should be added to
   * arithmetic mean.
   */
  public void add(double value, long count) {
    sum += value * count;
    this.count += count;
  }

  /**
   * Retrieves the mean of all values added with
   * {@link #add(double)} or 0 if there are zero added
   * values.
   * @return the accuracy
   */
  public double mean() {
    return count > 0 ? sum / count : 0;
  }

  /**
   * Retrieves the number of times a value
   * was added to the mean.
   * @return the count
   */
  public long count() {
    return count;
  }
  
  /**
   * Adds a value to the arithmetic mean.
   *
   * @param value the value which should be added
   * to the arithmetic mean.
   */
  public void addSent(double value) {
    addSent(value, 1);
  }

  /**
   * Adds a value count times to the arithmetic mean.
   *
   * @param value the value which should be added
   * to the arithmetic mean.
   *
   * @param count number of times the value should be added to
   * arithmetic mean.
   */
  public void addSent(double value, long count) {
    sentSum += value * count;
    this.sentCount += count;
  }

  /**
   * Retrieves the mean of all values added with
   * {@link #add(double)} or 0 if there are zero added
   * values.
   * @return the accuracy
   */
  public double sentMean() {
    return sentCount > 0 ? sentSum / sentCount : 0;
  }
  
  public void updateScores(final Object[] references, final Object[] predictions) {
    int fails = 0;
    for (int i = 0; i < references.length; i++) {
      if (references[i].equals(predictions[i])) {
        add(1);
      }
      else {
        add(0);
        fails++;
      }
    }
    if (fails > 0) {
      addSent(0);
    } else {
      addSent(1);
    }
  }

  @Override
  public String toString() {
    return Double.toString(mean());
  }
}

