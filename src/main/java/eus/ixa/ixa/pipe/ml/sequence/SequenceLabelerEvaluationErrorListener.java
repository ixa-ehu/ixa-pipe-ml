package eus.ixa.ixa.pipe.ml.sequence;

import java.io.OutputStream;

import opennlp.tools.util.eval.EvaluationMonitor;

/**
 * A default implementation of {@link EvaluationMonitor} that prints
 * to an output stream.
 *
 */
public class SequenceLabelerEvaluationErrorListener extends
    EvaluationErrorPrinter<SequenceLabelSample> implements SequenceLabelerEvaluationMonitor {

  /**
   * Creates a listener that will print to System.err
   */
  public SequenceLabelerEvaluationErrorListener() {
    super(System.err);
  }

  /**
   * Creates a listener that will print to a given {@link OutputStream}
   *
 * @param outputStream the outputstream
 */
public SequenceLabelerEvaluationErrorListener(OutputStream outputStream) {
    super(outputStream);
  }

  @Override
  public void missclassified(SequenceLabelSample reference, SequenceLabelSample prediction) {
    printError(reference.getId(), reference.getSequences(), prediction.getSequences(), reference,
        prediction, reference.getTokens());
  }

}

