package eus.ixa.ixa.pipe.ml.sequence;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eus.ixa.ixa.pipe.ml.utils.Span;

public class BioCodec implements SequenceCodec<String> {

  public static final String START = "start";
  public static final String CONTINUE = "cont";
  public static final String OTHER = "other";

  private static final Pattern typedOutcomePattern = Pattern.compile("(.+)-\\w+");

  static final String extractSequenceType(String outcome) {
    Matcher matcher = typedOutcomePattern.matcher(outcome);
    if(matcher.matches()) {
      String seqType = matcher.group(1);
      return seqType;
    }

    return null;
  }

  public Span[] decode(List<String> c) {
    int start = -1;
    int end = -1;
    List<Span> spans = new ArrayList<Span>(c.size());
    for (int li = 0; li < c.size(); li++) {
      String chunkTag = c.get(li);
      if (chunkTag.endsWith(BioCodec.START)) {
        if (start != -1) {
          spans.add(new Span(start, end, extractSequenceType(c.get(li - 1))));
        }

        start = li;
        end = li + 1;

      }
      else if (chunkTag.endsWith(BioCodec.CONTINUE)) {
        end = li + 1;
      }
      else if (chunkTag.endsWith(BioCodec.OTHER)) {
        if (start != -1) {
          spans.add(new Span(start, end, extractSequenceType(c.get(li - 1))));
          start = -1;
          end = -1;
        }
      }
    }

    if (start != -1) {
      spans.add(new Span(start, end, extractSequenceType(c.get(c.size() - 1))));
    }

    return spans.toArray(new Span[spans.size()]);
  }

  public String[] encode(Span[] sequences, int length) {
    String[] outcomes = new String[length];
    for (int i = 0; i < outcomes.length; i++) {
      outcomes[i] = BioCodec.OTHER;
    }
    for (Span sequence : sequences) {
      if (sequence.getType() == null) {
        outcomes[sequence.getStart()] = "default" + "-" + BioCodec.START;
      }
      else {
        outcomes[sequence.getStart()] = sequence.getType() + "-" + BioCodec.START;
      }
      // now iterate from begin + 1 till end
      for (int i = sequence.getStart() + 1; i < sequence.getEnd(); i++) {
        if (sequence.getType() == null) {
          outcomes[i] = "default" + "-" + BioCodec.CONTINUE;
        }
        else {
          outcomes[i] = sequence.getType() + "-" + BioCodec.CONTINUE;
        }
      }
    }

    return outcomes;
  }

  public SequenceLabelerSequenceValidator createSequenceValidator() {
    return new SequenceLabelerSequenceValidator();
  }

  @Override
  public boolean areOutcomesCompatible(String[] outcomes) {
    // We should have *optionally* one outcome named "other", some named xyz-start and sometimes
    // they have a pair xyz-cont. We should not have any other outcome
    // To validate the model we check if we have one outcome named "other", at least
    // one outcome with suffix start. After that we check if all outcomes that ends with
    // "cont" have a pair that ends with "start".
    List<String> start = new ArrayList<String>();
    List<String> cont = new ArrayList<String>();

    for (int i = 0; i < outcomes.length; i++) {
      String outcome = outcomes[i];
      if (outcome.endsWith(START)) {
        start.add(outcome.substring(0, outcome.length()
            - START.length()));
      } else if (outcome.endsWith(CONTINUE)) {
        cont.add(outcome.substring(0, outcome.length()
            - CONTINUE.length()));
      } else if (outcome.equals(OTHER)) {
        // don't fail anymore if couldn't find outcome named OTHER
      } else {
        // got unexpected outcome
        return false;
      }
    }

    if (start.size() == 0) {
      return false;
    } else {
      for (String contPreffix : cont) {
        if (!start.contains(contPreffix)) {
          return false;
        }
      }
    }

    return true;
  }
}

