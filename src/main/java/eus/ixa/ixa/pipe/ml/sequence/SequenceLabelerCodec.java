package eus.ixa.ixa.pipe.ml.sequence;

import java.util.List;

import opennlp.tools.util.SequenceValidator;

import eus.ixa.ixa.pipe.ml.utils.Span;

public interface SequenceLabelerCodec<T> {

  /**
   * Decodes a sequence T objects into Span objects.
   *
   * @param c the sequence of objects
   * @return the decoded Span array
   */
  Span[] decode(List<T> c);

  /**
   * Encodes Span objects into a sequence of T objects.
   *
   * @param sequences the sequences
   * @param length the length
   * @return the encoded spans
   */
  T[] encode(Span[] sequences, int length);

  /**
   * Creates a sequence validator which can validate a sequence of outcomes.
   * @return the sequence validator
   */
  SequenceValidator<T> createSequenceValidator();

  /**
   * Checks if the outcomes of the model are compatible with the codec.
   * @param outcomes all possible model outcomes
   * @return whether they are compatible or not
   */
  boolean areOutcomesCompatible(String[] outcomes);
}

