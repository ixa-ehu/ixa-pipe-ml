package eus.ixa.ixa.pipe.ml.sequence;

import opennlp.tools.util.SequenceValidator;

public class BilouSequenceValidator implements
SequenceValidator<String> {

public boolean validSequence(int i, String[] inputSequence,
  String[] outcomesSequence, String outcome) {

if (outcome.endsWith(SequenceLabelerME.CONTINUE) || outcome.endsWith(BilouCodec.LAST)) {

  int li = outcomesSequence.length - 1;

  if (li == -1) {
    return false;
  } else if (outcomesSequence[li].endsWith(SequenceLabelerME.OTHER) ||
      outcomesSequence[li].endsWith(BilouCodec.UNIT)) {
    return false;
  } else if (outcomesSequence[li].endsWith(SequenceLabelerME.CONTINUE) ||
      outcomesSequence[li].endsWith(SequenceLabelerME.START)) {
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
  if (outcome.endsWith(SequenceLabelerME.OTHER)) {
    if (outcomesSequence[outcomesSequence.length - 1].endsWith(SequenceLabelerME.START) || outcomesSequence[outcomesSequence.length - 1].endsWith(SequenceLabelerME.CONTINUE)) {
      return false;
    }
  }
}

return true;
}
}