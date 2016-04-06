package eus.ixa.ixa.pipe.ml.sequence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import eus.ixa.ixa.pipe.ml.utils.Span;

import opennlp.tools.util.SequenceValidator;

public class BilouCodec implements SequenceCodec<String> {

  public static final String START = "start";
  public static final String CONTINUE = "cont";
  public static final String LAST = "last";
  public static final String UNIT = "unit";
  public static final String OTHER = "other";

  @Override
  public Span[] decode(List<String> c) {
    int start = -1;
    int end = -1;
    List<Span> spans = new ArrayList<Span>(c.size());
    for (int li = 0; li < c.size(); li++) {
      String chunkTag = c.get(li);
      if (chunkTag.endsWith(BioCodec.START)) {
        start = li;
        end = li + 1;
      }
      else if (chunkTag.endsWith(BioCodec.CONTINUE)) {
        end = li + 1;
      }
      else if (chunkTag.endsWith(LAST)) {
        if (start != -1) {
          spans.add(new Span(start, end + 1, BioCodec.extractSequenceType(c.get(li - 1))));
          start = -1;
          end = -1;
        }
      }
      else if (chunkTag.endsWith(UNIT)) {
        spans.add(new Span(li, li + 1, BioCodec.extractSequenceType(c.get(li))));
      }
      else if (chunkTag.endsWith(BioCodec.OTHER)) {
        // in this case do nothing
      }
    }
    return spans.toArray(new Span[spans.size()]);
  }

  @Override
  public String[] encode(Span[] sequences, int length) {
    String[] outcomes = new String[length];
    Arrays.fill(outcomes, BioCodec.OTHER);

    for (Span name : sequences) {

      if (name.length() > 1) {
        if (name.getType() == null) {
          outcomes[name.getStart()] = "default" + "-" + BioCodec.START;
        }
        else {
          outcomes[name.getStart()] = name.getType() + "-" + BioCodec.START;
        }
        // now iterate from begin + 1 till end
        for (int i = name.getStart() + 1; i < name.getEnd() - 1; i++) {
          if (name.getType() == null) {
            outcomes[i] = "default" + "-" + BioCodec.CONTINUE;
          }
          else {
            outcomes[i] = name.getType() + "-" + BioCodec.CONTINUE;
          }
        }
        if (name.getType() == null) {
          outcomes[name.getEnd() - 1] = "default" + "-" + BilouCodec.LAST;
        }
        else {
          outcomes[name.getEnd() - 1] = name.getType() + "-" + BilouCodec.LAST;
        }
      }
      else {
        if (name.getType() == null) {
          outcomes[name.getEnd() - 1] = "default" + "-" + BilouCodec.UNIT;
        }
        else {
          outcomes[name.getEnd() - 1] = name.getType() + "-" + BilouCodec.UNIT;
        }
      }
    }
    return outcomes;
  }

  @Override
  public SequenceValidator<String> createSequenceValidator() {
    return new BilouSequenceValidator();
  }

  @Override
  public boolean areOutcomesCompatible(String[] outcomes) {
    return true;
  }
}

