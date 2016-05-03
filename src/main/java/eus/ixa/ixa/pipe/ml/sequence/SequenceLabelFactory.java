/*
 *Copyright 2014 Rodrigo Agerri

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package eus.ixa.ixa.pipe.ml.sequence;

import eus.ixa.ixa.pipe.ml.utils.Span;

/**
 * This class provides the functionality to create {@link SequenceLabel} objects.
 *
 * @author ragerri
 * @version 2016-03-14
 *
 */

public class SequenceLabelFactory {

  /**
   * Constructs a {@link SequenceLabel} as a String with a class type (e.g. Person,
   * location, organization, NNP, etc.)
   *
   * @param seqString
   *          string to be added to a Name object
   * @param seqType
   *          the type of the Name
   * @return a new Name object
   *
   */
  public final SequenceLabel createSequence(final String seqString, final String seqType) {
    SequenceLabel name = new SequenceLabel();
    name.setValue(seqString);
    name.setType(seqType);
    return name;
  }

  /**
   * Constructs a {@link SequenceLabel} as a String with a type and a {@link Span}
   * specified in terms of the number of tokens it contains.
   *
   * @param seqString
   *          string to be added to a Name object
   * @param seqType
   *          the type of the Name
   * @param seqSpan
   *          the span of the Name
   * @return a new Name object
   *
   */

  public final SequenceLabel createSequence(final String seqString, final String seqType,
      final Span seqSpan) {
    SequenceLabel sequence = new SequenceLabel();
    sequence.setValue(seqString);
    sequence.setType(seqType);
    sequence.setSpan(seqSpan);
    return sequence;
  }

  /**
   * Constructs a {@link SequenceLabel} as a String with corresponding offsets and length
   * from which to calculate start and end position of the Name.
   *
   * @param seqString
   *          string to be added to a Sequence object
   * @param seqType
   *          the type of the Sequence
   * @param offset
   *          the starting offset of the Sequence
   * @param length
   *          of the string
   * @return a new Sequence object
   *
   */
  public final SequenceLabel createSequence(final String seqString, final String seqType,
      final int offset, final int length) {
    SequenceLabel sequence = new SequenceLabel();
    sequence.setValue(seqString);
    sequence.setType(seqType);
    sequence.setStartOffset(offset);
    sequence.setSequenceLength(length);
    return sequence;
  }

  /**
   * Constructs a Sequence as a String with corresponding offsets and length from
   * which to calculate start and end position of the Sequence.
   *
   * @param seqString
   *          string to be added to a Name object
   * @param seqType
   *          the type of the Name
   * @param seqSpan the Span
   * @param offset
   *          the starting offset of the Name
   * @param length
   *          of the string
   * @return a new Name object
   *
   */
  public final SequenceLabel createSequence(final String seqString, final String seqType,
      final Span seqSpan, final int offset, final int length) {
    SequenceLabel sequence = new SequenceLabel();
    sequence.setValue(seqString);
    sequence.setType(seqType);
    sequence.setSpan(seqSpan);
    sequence.setStartOffset(offset);
    sequence.setSequenceLength(length);
    return sequence;
  }

}
