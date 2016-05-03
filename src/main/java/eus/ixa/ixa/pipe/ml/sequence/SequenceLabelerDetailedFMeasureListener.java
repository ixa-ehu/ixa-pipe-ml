package eus.ixa.ixa.pipe.ml.sequence;

import eus.ixa.ixa.pipe.ml.utils.Span;

public class SequenceLabelerDetailedFMeasureListener extends
DetailedFMeasureListener<SequenceLabelSample> implements
SequenceLabelerEvaluationMonitor {

@Override
protected Span[] asSpanArray(SequenceLabelSample sample) {
return sample.getSequences();
}

}

