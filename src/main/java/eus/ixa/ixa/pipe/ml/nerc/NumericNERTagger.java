/*
 *  Copyright 2014 Rodrigo Agerri

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
package eus.ixa.ixa.pipe.ml.nerc;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;

import eus.ixa.ixa.pipe.ml.lexer.NumericNameLexer;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabel;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelFactory;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerME;
import eus.ixa.ixa.pipe.ml.utils.Span;
import eus.ixa.ixa.pipe.ml.utils.StringUtils;

public class NumericNERTagger {

  private final NumericNameLexer numericLexer;
  private final SequenceLabelFactory nameFactory;

  public NumericNERTagger(final BufferedReader breader) {
    this.nameFactory = new SequenceLabelFactory();
    this.numericLexer = new NumericNameLexer(breader, nameFactory);
  }

  public List<SequenceLabel> getNames(final String[] tokens) {
    final Span[] origSpans = nercToSpans(tokens);
    final Span[] neSpans = SequenceLabelerME.dropOverlappingSpans(origSpans);
    return getNamesFromSpans(neSpans, tokens);
  }

  public Span[] nercToSpans(final String[] tokens) {
    final List<Span> neSpans = new ArrayList<>();
    final List<SequenceLabel> flexNameList = this.numericLexer
        .getNumericNames();
    for (final SequenceLabel name : flexNameList) {
      // System.err.println("numeric name: " + name.value());
      final List<Integer> neIds = StringUtils
          .exactTokenFinderIgnoreCase(name.getString(), tokens);
      for (int i = 0; i < neIds.size(); i += 2) {
        final Span neSpan = new Span(neIds.get(i), neIds.get(i + 1),
            name.getType());
        neSpans.add(neSpan);
      }
    }
    return neSpans.toArray(new Span[neSpans.size()]);
  }

  public List<SequenceLabel> getNamesFromSpans(final Span[] neSpans,
      final String[] tokens) {
    final List<SequenceLabel> names = new ArrayList<>();
    for (final Span neSpan : neSpans) {
      final String nameString = neSpan.getCoveredText(tokens);
      final String neType = neSpan.getType();
      final SequenceLabel name = this.nameFactory.createSequence(nameString,
          neType, neSpan);
      names.add(name);
    }
    return names;
  }

  public void clearAdaptiveData() {
    // nothing to clear

  }

}
