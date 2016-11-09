/*
 * Copyright 2016 Rodrigo Agerri

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

package eus.ixa.ixa.pipe.ml.formats;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import opennlp.tools.util.FilterObjectStream;
import opennlp.tools.util.ObjectStream;
import eus.ixa.ixa.pipe.ml.parse.Parse;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelSample;
import eus.ixa.ixa.pipe.ml.utils.Span;

/**
 * Obtains the POS tags from a Penn Treebank formatted parse tree and
 * and encodes them in {@code TabulatedFormat} for training a
 * POS tagger.
 * @author ragerri
 * @version 2016-05-10
 */
public class ParseToTabulatedFormat extends
    FilterObjectStream<Parse, SequenceLabelSample> {
  
  public ParseToTabulatedFormat(ObjectStream<Parse> in) {
    super(in);
  }

  public SequenceLabelSample read() throws IOException {

    List<String> tokens = new ArrayList<>();
    List<String> seqTypes = new ArrayList<>();
    boolean isClearAdaptiveData = false;
    Parse parse = samples.read();

    if (parse != null) {
      Parse[] nodes = parse.getTagNodes();
      for (int i = 0; i < nodes.length; i++) {
        Parse tok = nodes[i];
        tokens.add(tok.getCoveredText());
        seqTypes.add(tok.getType());
      }
    }
    // check if we need to clear features every sentence
    //isClearAdaptiveData = true;
    if (tokens.size() > 0) {
      // convert sequence tags into spans
      List<Span> sequences = new ArrayList<Span>();
      int beginIndex = -1;
      int endIndex = -1;
      for (int i = 0; i < seqTypes.size(); i++) {
        if (beginIndex != -1) {
          sequences
              .add(new Span(beginIndex, endIndex, seqTypes.get(beginIndex)));
          beginIndex = -1;
          endIndex = -1;
        }
        beginIndex = i;
        endIndex = i + 1;
      }
      // if one span remains, create it here
      if (beginIndex != -1)
        sequences.add(new Span(beginIndex, endIndex, seqTypes.get(beginIndex)));

      return new SequenceLabelSample(tokens.toArray(new String[tokens.size()]),
          sequences.toArray(new Span[sequences.size()]), isClearAdaptiveData);
    } else {
      // source stream is not returning anymore lines
      return null;
    }
  }
}
