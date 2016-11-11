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

import java.util.List;

import eus.ixa.ixa.pipe.ml.utils.Span;
import opennlp.tools.util.SequenceValidator;

public interface SequenceLabelerCodec<T> {

  /**
   * Decodes a sequence T objects into Span objects.
   *
   * @param c
   *          the sequence of objects
   * @return the decoded Span array
   */
  Span[] decode(List<T> c);

  /**
   * Encodes Span objects into a sequence of T objects.
   *
   * @param sequences
   *          the sequences
   * @param length
   *          the length
   * @return the encoded spans
   */
  T[] encode(Span[] sequences, int length);

  /**
   * Creates a sequence validator which can validate a sequence of outcomes.
   * 
   * @return the sequence validator
   */
  SequenceValidator<T> createSequenceValidator();

  /**
   * Checks if the outcomes of the model are compatible with the codec.
   * 
   * @param outcomes
   *          all possible model outcomes
   * @return whether they are compatible or not
   */
  boolean areOutcomesCompatible(String[] outcomes);
}
