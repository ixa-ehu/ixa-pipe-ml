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

import opennlp.tools.util.Span;

/**
 * The interface for sequence labelers which provide tags for a sequence of tokens.
 * @author ragerri
 * @version 2016-03-14
 */
public interface SequenceLabeler {

  /** Generates tags for the given sequence, typically a sentence, returning token spans for any identified sequences.
   * @param tokens an array of the tokens or words of the sequence, typically a sentence.
   * @return an array of spans for each of the sequences identified.
   */
  public Span[] find(String tokens[]);

  /**
   * Forgets all adaptive data which was collected during previous
   * calls to one of the find methods.
   *
   * This method is typical called at the end of a document.
   */
  public void clearAdaptiveData();

}

