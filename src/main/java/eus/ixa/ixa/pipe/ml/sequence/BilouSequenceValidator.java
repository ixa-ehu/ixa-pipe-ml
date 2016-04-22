package eus.ixa.ixa.pipe.ml.sequence;

import opennlp.tools.util.SequenceValidator;

public class BilouSequenceValidator implements
SequenceValidator<String> {

public boolean validSequence(int i, String[] inputSequence,
  String[] outcomesSequence, String outcome) {

if (outcome.endsWith(BilouCodec.CONTINUE) || outcome.endsWith(BilouCodec.LAST)) {

  int li = outcomesSequence.length - 1;

  if (li == -1) {
    return false;
  } else if (outcomesSequence[li].endsWith(BilouCodec.OTHER) ||
      outcomesSequence[li].endsWith(BilouCodec.UNIT)) {
    return false;
  } else if (outcomesSequence[li].endsWith(BilouCodec.CONTINUE) ||
      outcomesSequence[li].endsWith(BilouCodec.START)) {
    // if it is continue, we have to check if previous match was of the same type
    String previousNameType = SequenceLabelerME.extractNameType(outcomesSequence[li]);
    String nameType = SequenceLabelerME.extractNameType(outcome);
    if( previousNameType != null || nameType != null ) {
      if( nameType != null ) {
        if( nameType.equals(previousNameType) ){
          return true;
        }
      }
      return false; // outcomes types are not equal
    }
  }
}

if (outcomesSequence.length - 1 > 0) {
  if (outcome.endsWith(BilouCodec.OTHER)) {
    if (outcomesSequence[outcomesSequence.length - 1].endsWith(BilouCodec.START) || outcomesSequence[outcomesSequence.length - 1].endsWith(BilouCodec.CONTINUE)) {
      return false;
    }
  }
}

return true;
}
}