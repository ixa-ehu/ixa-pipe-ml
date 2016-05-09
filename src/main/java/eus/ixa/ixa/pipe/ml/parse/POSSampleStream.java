package eus.ixa.ixa.pipe.ml.parse;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import opennlp.tools.util.FilterObjectStream;
import opennlp.tools.util.ObjectStream;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelSample;
import eus.ixa.ixa.pipe.ml.utils.Span;

public class POSSampleStream extends
    FilterObjectStream<Parse, SequenceLabelSample> {
  
  public POSSampleStream(ObjectStream<Parse> in) {
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
