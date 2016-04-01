package eus.ixa.ixa.pipe.ml.sequence;

import java.io.OutputStream;

import opennlp.tools.cmdline.EvaluationErrorPrinter;
import opennlp.tools.util.eval.EvaluationMonitor;

/**
 * A default implementation of {@link EvaluationMonitor} that prints
 * to an output stream.
 *
 */
public class SequenceEvaluationErrorListener extends
    EvaluationErrorPrinter<SequenceSample> implements SequenceLabelerEvaluationMonitor {

  /**
   * Creates a listener that will print to System.err
   */
  public SequenceEvaluationErrorListener() {
    super(System.err);
  }

  /**
   * Creates a listener that will print to a given {@link OutputStream}
   *
 * @param outputStream the outputstream
 */
public SequenceEvaluationErrorListener(OutputStream outputStream) {
    super(outputStream);
  }

  @Override
  public void missclassified(SequenceSample reference, SequenceSample prediction) {
    printError(reference.getId(), reference.getSequences(), prediction.getSequences(), reference,
        prediction, reference.getTokens());
  }

}

