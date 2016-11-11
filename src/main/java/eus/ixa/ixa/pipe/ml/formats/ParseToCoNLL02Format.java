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

import eus.ixa.ixa.pipe.ml.parse.Parse;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelSample;
import eus.ixa.ixa.pipe.ml.utils.Span;
import opennlp.tools.util.FilterObjectStream;
import opennlp.tools.util.ObjectStream;

/**
 * Obtains chunks from a Penn Treebank formatted parse tree and encodes them in
 * CoNLL 2000 format.
 * 
 * @author ragerri
 * @version 2016-05-10
 */
public class ParseToCoNLL02Format
    extends FilterObjectStream<Parse, SequenceLabelSample> {

  public ParseToCoNLL02Format(final ObjectStream<Parse> in) {
    super(in);
  }

  @Override
  public SequenceLabelSample read() throws IOException {

    final List<String> tokens = new ArrayList<>();
    final List<String> seqTypes = new ArrayList<>();
    final Parse parse = this.samples.read();
    boolean isClearAdaptiveData = false;

    if (parse != null) {
      isClearAdaptiveData = true;

      final List<Parse> chunks = getInitialChunks(parse);

      for (int ci = 0, cl = chunks.size(); ci < cl; ci++) {
        final Parse c = chunks.get(ci);
        if (c.isPosTag()) {
          tokens.add(c.getCoveredText());
          seqTypes.add("O");
        } else {
          boolean start = true;
          final String ctype = c.getType();
          final Parse[] children = c.getChildren();
          for (final Parse tok : children) {
            tokens.add(tok.getCoveredText());
            if (start) {
              seqTypes.add("B-" + ctype);
              start = false;
            } else {
              seqTypes.add("I-" + ctype);
            }
          }
        }
      }
    }
    if (tokens.size() > 0) {
      // convert sequence tags into spans
      final List<Span> sequences = new ArrayList<Span>();
      int beginIndex = -1;
      int endIndex = -1;
      for (int i = 0; i < seqTypes.size(); i++) {
        final String seqTag = seqTypes.get(i);
        if (seqTag.startsWith("B-")) {
          if (beginIndex != -1) {
            sequences.add(CoNLL02Format.extract(beginIndex, endIndex,
                seqTypes.get(beginIndex)));
            beginIndex = -1;
            endIndex = -1;
          }
          beginIndex = i;
          endIndex = i + 1;
        } else if (seqTag.startsWith("I-")) {
          endIndex++;
        } else if (seqTag.equals("O")) {
          if (beginIndex != -1) {
            sequences.add(CoNLL02Format.extract(beginIndex, endIndex,
                seqTypes.get(beginIndex)));
            beginIndex = -1;
            endIndex = -1;
          }
        } else {
          throw new IOException("Invalid tag: " + seqTag);
        }
      }
      // if one span remains, create it here
      if (beginIndex != -1) {
        sequences.add(CoNLL02Format.extract(beginIndex, endIndex,
            seqTypes.get(beginIndex)));
      }

      return new SequenceLabelSample(tokens.toArray(new String[tokens.size()]),
          sequences.toArray(new Span[sequences.size()]), isClearAdaptiveData);
    } else {
      // source stream is not returning anymore lines
      return null;
    }
  }

  private static List<Parse> getInitialChunks(final Parse p) {
    final List<Parse> chunks = new ArrayList<Parse>();
    getInitialChunks(p, chunks);
    return chunks;
  }

  private static void getInitialChunks(final Parse p,
      final List<Parse> ichunks) {
    if (p.isPosTag()) {
      ichunks.add(p);
    } else {
      final Parse[] kids = p.getChildren();
      boolean allKidsAreTags = true;
      for (int ci = 0, cl = kids.length; ci < cl; ci++) {
        if (!kids[ci].isPosTag()) {
          allKidsAreTags = false;
          break;
        }
      }
      if (allKidsAreTags) {
        ichunks.add(p);
      } else {
        for (final Parse kid : kids) {
          getInitialChunks(kid, ichunks);
        }
      }
    }
  }
}
