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
  
  private NumericNameLexer numericLexer;
  private SequenceLabelFactory nameFactory;
  
  public NumericNERTagger(BufferedReader breader, SequenceLabelFactory aNameFactory) {
    this.nameFactory = aNameFactory;
    numericLexer = new NumericNameLexer(breader, aNameFactory);
  }

  public List<SequenceLabel> getNames(String[] tokens) {
    Span[] origSpans = nercToSpans(tokens);
    Span[] neSpans = SequenceLabelerME.dropOverlappingSpans(origSpans);
    List<SequenceLabel> names = getNamesFromSpans(neSpans, tokens);
    return names;
  }

  public Span[] nercToSpans(final String[] tokens) {
    List<Span> neSpans = new ArrayList<Span>();
    List<SequenceLabel> flexNameList = numericLexer.getNumericNames();
    for (SequenceLabel name : flexNameList) {
      //System.err.println("numeric name: " + name.value());
      List<Integer> neIds = StringUtils.exactTokenFinderIgnoreCase(name.getString(), tokens);
      for (int i = 0; i < neIds.size(); i += 2) {
        Span neSpan = new Span(neIds.get(i), neIds.get(i+1), name.getType());
        neSpans.add(neSpan);
      }
    }
    return neSpans.toArray(new Span[neSpans.size()]);
  }

  public List<SequenceLabel> getNamesFromSpans(Span[] neSpans, String[] tokens) {
    List<SequenceLabel> names = new ArrayList<SequenceLabel>();
    for (Span neSpan : neSpans) {
      String nameString = neSpan.getCoveredText(tokens);
      String neType = neSpan.getType();
      SequenceLabel name = nameFactory.createSequence(nameString, neType, neSpan);
      names.add(name);
    }
    return names;
  }

  public void clearAdaptiveData() {
    // nothing to clear
    
  }

}
