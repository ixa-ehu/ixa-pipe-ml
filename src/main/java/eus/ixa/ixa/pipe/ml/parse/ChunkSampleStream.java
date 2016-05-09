package eus.ixa.ixa.pipe.ml.parse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import opennlp.tools.util.FilterObjectStream;
import opennlp.tools.util.ObjectStream;
import eus.ixa.ixa.pipe.ml.formats.CoNLL02Format;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelSample;
import eus.ixa.ixa.pipe.ml.utils.Span;

public class ChunkSampleStream extends FilterObjectStream<Parse, SequenceLabelSample> {

  public ChunkSampleStream(ObjectStream<Parse> in) {
    super(in);
  }

  public SequenceLabelSample read() throws IOException {

    List<String> tokens = new ArrayList<>();
    List<String> seqTypes = new ArrayList<>();
    Parse parse = samples.read();
    boolean isClearAdaptiveData = false;
    
    if (parse != null) {
      isClearAdaptiveData = true;
      
      List<Parse> chunks = getInitialChunks(parse);
      
      for (int ci = 0, cl = chunks.size(); ci < cl; ci++) {
        Parse c = chunks.get(ci);
        if (c.isPosTag()) {
          tokens.add(c.getCoveredText());
          seqTypes.add("O");
        }
        else {
          boolean start = true;
          String ctype = c.getType();
          Parse[] children = c.getChildren();
          for (int ti = 0, tl = children.length; ti < tl; ti++) {
            Parse tok = children[ti];
            tokens.add(tok.getCoveredText());
            if (start) {
              seqTypes.add("B-" + ctype);
              start = false;
            }
            else {
              seqTypes.add("I-" + ctype);
            }
          }
        }
      }
    }
    if (tokens.size() > 0) {
      // convert sequence tags into spans
      List<Span> sequences = new ArrayList<Span>();
      int beginIndex = -1;
      int endIndex = -1;
      for (int i = 0; i < seqTypes.size(); i++) {
        String seqTag = seqTypes.get(i);
        if (seqTag.startsWith("B-")) {
          if (beginIndex != -1) {
            sequences.add(CoNLL02Format.extract(beginIndex, endIndex, seqTypes.get(beginIndex)));
            beginIndex = -1;
            endIndex = -1;
          }
          beginIndex = i;
          endIndex = i + 1;
        } else if (seqTag.startsWith("I-")) {
          endIndex++;
        } else if (seqTag.equals("O")) {
          if (beginIndex != -1) {
            sequences.add(CoNLL02Format.extract(beginIndex, endIndex, seqTypes.get(beginIndex)));
            beginIndex = -1;
            endIndex = -1;
          }
        } else {
          throw new IOException("Invalid tag: " + seqTag);
        }
      }
      // if one span remains, create it here
      if (beginIndex != -1)
        sequences.add(CoNLL02Format.extract(beginIndex, endIndex, seqTypes.get(beginIndex)));

      return new SequenceLabelSample(tokens.toArray(new String[tokens.size()]),
          sequences.toArray(new Span[sequences.size()]), isClearAdaptiveData);
    } else {
      // source stream is not returning anymore lines
      return null;
    }
  }
  

  private static List<Parse> getInitialChunks(Parse p) {
    List<Parse> chunks = new ArrayList<Parse>();
    getInitialChunks(p, chunks);
    return chunks;
  }

  private static void getInitialChunks(Parse p, List<Parse> ichunks) {
    if (p.isPosTag()) {
      ichunks.add(p);
    } else {
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
      } else {
        for (int ci = 0, cl = kids.length; ci < cl; ci++) {
          getInitialChunks(kids[ci], ichunks);
        }
      }
    }
  }
}

