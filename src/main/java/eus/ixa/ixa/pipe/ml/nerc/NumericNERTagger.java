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
import eus.ixa.ixa.pipe.ml.sequence.Sequence;
import eus.ixa.ixa.pipe.ml.sequence.SequenceFactory;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerME;
import eus.ixa.ixa.pipe.ml.utils.Span;
import eus.ixa.ixa.pipe.ml.utils.StringUtils;

public class NumericNERTagger {
  
  private NumericNameLexer numericLexer;
  private SequenceFactory nameFactory;
  
  public NumericNERTagger(BufferedReader breader, SequenceFactory aNameFactory) {
    this.nameFactory = aNameFactory;
    numericLexer = new NumericNameLexer(breader, aNameFactory);
  }

  public List<Sequence> getNames(String[] tokens) {
    Span[] origSpans = nercToSpans(tokens);
    Span[] neSpans = SequenceLabelerME.dropOverlappingSpans(origSpans);
    List<Sequence> names = getNamesFromSpans(neSpans, tokens);
    return names;
  }

  public Span[] nercToSpans(final String[] tokens) {
    List<Span> neSpans = new ArrayList<Span>();
    List<Sequence> flexNameList = numericLexer.getNumericNames();
    for (Sequence name : flexNameList) {
      //System.err.println("numeric name: " + name.value());
      List<Integer> neIds = StringUtils.exactTokenFinderIgnoreCase(name.getString(), tokens);
      for (int i = 0; i < neIds.size(); i += 2) {
        Span neSpan = new Span(neIds.get(i), neIds.get(i+1), name.getType());
        neSpans.add(neSpan);
      }
    }
    return neSpans.toArray(new Span[neSpans.size()]);
  }

  public List<Sequence> getNamesFromSpans(Span[] neSpans, String[] tokens) {
    List<Sequence> names = new ArrayList<Sequence>();
    for (Span neSpan : neSpans) {
      String nameString = neSpan.getCoveredText(tokens);
      String neType = neSpan.getType();
      Sequence name = nameFactory.createSequence(nameString, neType, neSpan);
      names.add(name);
    }
    return names;
  }

  public void clearAdaptiveData() {
    // nothing to clear
    
  }

}
