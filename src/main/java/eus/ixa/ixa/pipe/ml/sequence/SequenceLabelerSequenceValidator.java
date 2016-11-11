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

package eus.ixa.ixa.pipe.ml.sequence;

import opennlp.tools.util.SequenceValidator;

public class SequenceLabelerSequenceValidator
    implements SequenceValidator<String> {

  @Override
  public boolean validSequence(final int i, final String[] inputSequence,
      final String[] outcomesSequence, final String outcome) {

    // outcome is formatted like "cont" or "sometype-cont", so we
    // can check if it ends with "cont".
    if (outcome.endsWith(BilouCodec.CONTINUE)) {

      final int li = outcomesSequence.length - 1;

      if (li == -1) {
        return false;
      } else if (outcomesSequence[li].endsWith(BilouCodec.OTHER)) {
        return false;
      } else if (outcomesSequence[li].endsWith(BilouCodec.CONTINUE)) {
        // if it is continue, we have to check if previous match was of the same
        // type
        final String previousSeqType = SequenceLabelerME
            .extractNameType(outcomesSequence[li]);
        final String seqType = SequenceLabelerME.extractNameType(outcome);
        if (previousSeqType != null || seqType != null) {
          if (seqType != null) {
            if (seqType.equals(previousSeqType)) {
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
