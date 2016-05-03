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

import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelSample;
import eus.ixa.ixa.pipe.ml.utils.Span;

import opennlp.tools.util.FilterObjectStream;
import opennlp.tools.util.ObjectStream;

public class ChunkSampleStream extends FilterObjectStream<Parse, SequenceLabelSample> {

  public ChunkSampleStream(ObjectStream<Parse> in) {
    super(in);
  }

  private static void getInitialChunks(Parse p, List<Parse> ichunks) {
    if (p.isPosTag()) {
      ichunks.add(p);
    }
    else {
      Parse[] kids = p.getChildren();
      boolean allKidsAreTags = true;
      for (int ci = 0, cl = kids.length; ci < cl; ci++) {
        if (!kids[ci].isPosTag()) {
          allKidsAreTags = false;
          break;
        }
      }
      if (allKidsAreTags) {
        ichunks.add(p);
      }
      else {
        for (int ci = 0, cl = kids.length; ci < cl; ci++) {
          getInitialChunks(kids[ci], ichunks);
        }
      }
    }
  }

  public static Parse[] getInitialChunks(Parse p) {
    List<Parse> chunks = new ArrayList<>();
    getInitialChunks(p, chunks);
    return chunks.toArray(new Parse[chunks.size()]);
  }

  public SequenceLabelSample read() throws IOException {

    Parse parse = samples.read();
    boolean isClearAdaptiveData = false;
    
    if (parse != null) { 
      isClearAdaptiveData = true;
      
      Parse[] chunks = getInitialChunks(parse);
      List<String> toks = new ArrayList<>();
      List<Span> preds = new ArrayList<>();
      for (int ci = 0, cl = chunks.length; ci < cl; ci++) {
        Parse c = chunks[ci];
        if (c.isPosTag()) {
          toks.add(c.getCoveredText());
          Span cSpan = c.getSpan();
          c.setType(c.getType());
          preds.add(cSpan);
        }
        else {
          boolean start = true;
          String ctype = c.getType();
          Parse[] kids = c.getChildren();
          for (int ti = 0, tl = kids.length; ti < tl; ti++) {
            Parse tok = kids[ti];
            toks.add(tok.getCoveredText());
            Span cSpan = c.getSpan();
            cSpan.setType(ShiftReduceParser.START + ctype);
            if (start) {
              preds.add(cSpan);
              start = false;
            }
            else {
              cSpan.setType(ShiftReduceParser.CONT + c.getType());
              preds.add(cSpan);
            }
          }
        }
      }

      return new SequenceLabelSample(toks.toArray(new String[toks.size()]),
          preds.toArray(new Span[preds.size()]), isClearAdaptiveData);
    }
    else {
      return null;
    }
  }
}

