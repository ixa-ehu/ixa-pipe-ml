package eus.ixa.ixa.pipe.ml.sequence;

import opennlp.tools.cmdline.DetailedFMeasureListener;
import opennlp.tools.util.Span;

public class SequenceLabelerDetailedFMeasureListener extends
DetailedFMeasureListener<SequenceSample> implements
SequenceLabelerEvaluationMonitor {

@Override
protected Span[] asSpanArray(SequenceSample sample) {
return sample.getSequences();
}

}

