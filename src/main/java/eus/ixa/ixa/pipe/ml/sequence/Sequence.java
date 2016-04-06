/*
 *Copyright 2016 Rodrigo Agerri

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
 * A <code>Sequence</code> object contains a single String, a {@link Span}, a
 * startOffset and the length of the String. These attributes are set or
 * returned in response to requests.
 *
 * @author ragerri
 * @version 2016-03-14
 *
 */
public class Sequence {

  /**
   * The string of the sequence.
   */
  private String str;

  /**
   * The {@link Span} of the Sequence.
   */
  private Span sequenceSpan;
  /**
   * The Name Entity class.
   */
  private String type;

  /**
   * Start position of the <code>Sequence</code> in the original input string.
   */
  private int startOffset = -1;

  /**
   * Length of the Sequence in the original input string.
   */
  private int sequenceLength = -1;

  /**
   * Create a new <code>Sequence</code> with a null content (i.e., str).
   */
  public Sequence() {
  }

  /**
   * Create a new <code>Sequence</code> with the given string.
   *
   * @param aStr
   *          the new label's content
   * @param aType
   *          the class of the name
   */
  public Sequence(final String aStr, final String aType) {
    this.str = aStr;
    this.type = aType.toUpperCase();
  }

  /**
   * Create a new <code>Sequence</code> with the given string and Span.
   *
   * @param aStr
   *          the new label's content
   * @param aType
   *          the class of the Span
   * @param aSeqSpan
   *          the span of the sequence
   */
  public Sequence(final String aStr, final String aType, final Span aSeqSpan) {
    this.str = aStr;
    this.type = aType.toUpperCase();
    this.sequenceSpan = aSeqSpan;
  }

  /**
   * Creates a new <code>Sequence</code> with the given content.
   *
   * @param aStr
   *          The new label's content
   * @param aType
   *          the class of the Sequence
   * @param aStartOffset
   *          Start offset in original text
   * @param aSeqLength
   *          End offset in original text
   */
  public Sequence(final String aStr, final String aType,
      final int aStartOffset, final int aSeqLength) {
    this.str = aStr;
    this.type = aType.toUpperCase();
    setStartOffset(aStartOffset);
    setSequenceLength(aSeqLength);
  }

  /**
   * Creates a new <code>Sequence</code> with the given content.
   *
   * @param aStr
   *          The new label's content
   * @param aType
   *          the class of the sequence
   * @param aSeqSpan
   *          the sequence span
   * @param aStartOffset
   *          Start offset in original text
   * @param aSeqLength
   *          End offset in original text
   */
  public Sequence(final String aStr, final String aType, final Span aSeqSpan,
      final int aStartOffset, final int aSeqLength) {
    this.str = aStr;
    this.type = aType.toUpperCase();
    this.sequenceSpan = aSeqSpan;
    setStartOffset(aStartOffset);
    setSequenceLength(aSeqLength);
  }

  /**
   * Return the string of the sequence (or null if none).
   *
   * @return the string value for the label
   */
  public final String getString() {
    return str;
  }

  /**
   * Return the type of the Sequence.
   *
   * @return the type of the Sequence
   */
  public final String getType() {
    return type;
  }

  /**
   * Return the Span (or null if none).
   *
   * @return the Span
   */
  public final Span getSpan() {
    return sequenceSpan;
  }

  /**
   * Set the value for the Sequence.
   *
   * @param value
   *          The value for the Sequence
   */
  public final void setValue(final String value) {
    str = value;
  }

  /**
   * Set type of the Sequence.
   *
   * @param neType
   *          the class of the Sequence
   */
  public final void setType(final String neType) {
    type = neType.toUpperCase();
  }

  /**
   * Set the Span for the Name.
   *
   * @param span
   *          the Span of the name
   */
  public final void setSpan(final Span span) {
    sequenceSpan = span;
  }

  /**
   * Set the value from a String.
   *
   * @param aStr
   *          The str for the sequence
   */
  public final void setFromString(final String aStr) {
    this.str = aStr;
  }

  @Override
  public final String toString() {
    return str;
  }

  /**
   * @return the starting offset
   */
  public final int startOffset() {
    return startOffset;
  }

  /**
   * @return the length in characters of the sequence
   */
  public final int sequenceLength() {
    return sequenceLength;
  }

  /**
   * @param beginPosition
   *          the starting character
   */
  public final void setStartOffset(final int beginPosition) {
    this.startOffset = beginPosition;
  }

  /**
   * @param aSeqLength
   *          the length of the sequence
   */
  public final void setSequenceLength(final int aSeqLength) {
    this.sequenceLength = aSeqLength;
  }
}
