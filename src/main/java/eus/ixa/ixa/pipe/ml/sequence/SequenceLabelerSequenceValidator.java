package eus.ixa.ixa.pipe.ml.sequence;


import opennlp.tools.util.SequenceValidator;

public class SequenceLabelerSequenceValidator implements
    SequenceValidator<String> {

  public boolean validSequence(int i, String[] inputSequence,
      String[] outcomesSequence, String outcome) {

    // outcome is formatted like "cont" or "sometype-cont", so we
    // can check if it ends with "cont".
    if (outcome.endsWith(SequenceLabelerME.CONTINUE)) {

      int li = outcomesSequence.length - 1;

      if (li == -1) {
        return false;
      } else if (outcomesSequence[li].endsWith(SequenceLabelerME.OTHER)) {
        return false;
      } else if (outcomesSequence[li].endsWith(SequenceLabelerME.CONTINUE)) {
        // if it is continue, we have to check if previous match was of the same type
        String previousSeqType = SequenceLabelerME.extractNameType(outcomesSequence[li]);
        String seqType = SequenceLabelerME.extractNameType(outcome);
        if( previousSeqType != null || seqType != null ) {
          if( seqType != null ) {
            if( seqType.equals(previousSeqType) ){
              return true;
            }
          }
          return false; // outcomes types are not equal
        }
      }
    }
    return true;
  }
}
