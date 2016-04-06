package eus.ixa.ixa.pipe.ml.sequence;

import java.util.List;

import opennlp.tools.util.SequenceValidator;

import eus.ixa.ixa.pipe.ml.utils.Span;

public interface SequenceCodec<T> {

  /**
   * Decodes a sequence T objects into Span objects.
   *
   * @param c
   *
   * @return
   */
  Span[] decode(List<T> c);

  /**
   * Encodes Span objects into a sequence of T objects.
   *
   * @param names
   * @param length
   *
   * @return
   */
  T[] encode(Span[] sequences, int length);

  /**
   * Creates a sequence validator which can validate a sequence of outcomes.
   *
   * @return
   */
  SequenceValidator<T> createSequenceValidator();

  /**
   * Checks if the outcomes of the model are compatible with the codec.
   *
   * @param outcomes all possible model outcomes
   *
   * @return
   */
  boolean areOutcomesCompatible(String[] outcomes);
}

