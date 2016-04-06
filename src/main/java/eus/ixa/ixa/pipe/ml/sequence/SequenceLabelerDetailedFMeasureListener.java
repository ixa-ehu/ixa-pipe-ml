package eus.ixa.ixa.pipe.ml.sequence;

import eus.ixa.ixa.pipe.ml.utils.Span;

public class SequenceLabelerDetailedFMeasureListener extends
DetailedFMeasureListener<SequenceSample> implements
SequenceLabelerEvaluationMonitor {

@Override
protected Span[] asSpanArray(SequenceSample sample) {
return sample.getSequences();
}

}

