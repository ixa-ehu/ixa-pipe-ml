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

import opennlp.tools.util.FilterObjectStream;
import opennlp.tools.util.ObjectStream;
import eus.ixa.ixa.pipe.ml.sequence.SequenceSample;
import eus.ixa.ixa.pipe.ml.utils.Span;


public class POSSampleStream extends FilterObjectStream<Parse, SequenceSample> {

  public POSSampleStream(ObjectStream<Parse> in) {
    super(in);
  }

  public SequenceSample read() throws IOException {

    Parse parse = samples.read();
    boolean isClearAdaptiveData = false;
    
    if (parse != null) {
      isClearAdaptiveData = true;

      Parse[] nodes = parse.getTagNodes();

      String toks[] = new String[nodes.length];
      Span[] preds = new Span[nodes.length];

      for (int ti=0; ti < nodes.length; ti++) {
        Parse tok = nodes[ti];
        toks[ti] = tok.getCoveredText();
        preds[ti] = tok.getSpan();
      }

      return new SequenceSample(toks, preds, isClearAdaptiveData);
    }
    else {
      return null;
    }
  }
}

