package eus.ixa.ixa.pipe.ml.sequence;

import java.io.IOException;

import opennlp.tools.ml.maxent.DataStream;
import opennlp.tools.util.FilterObjectStream;
import opennlp.tools.util.ObjectStream;

/**
 * The {@link SequenceLabelSampleDataStream} class converts tagged {@link String}s
 * provided by a {@link DataStream} to {@link SequenceLabelSample} objects.
 * It uses text that is is one-sentence per line and tokenized
 * with names identified by <code>&lt;START&gt;</code> and <code>&lt;END&gt;</code> tags.
 */
public class SequenceLabelSampleDataStream extends FilterObjectStream<String, SequenceLabelSample> {

  public static final String START_TAG_PREFIX = "<START:";
  public static final String START_TAG = "<START>";
  public static final String END_TAG = "<END>";

  public SequenceLabelSampleDataStream(ObjectStream<String> in) {
    super(in);
  }

  public SequenceLabelSample read() throws IOException {
      String token = samples.read();

      boolean isClearAdaptiveData = false;

      // An empty line indicates the begin of a new article
      // for which the adaptive data in the feature generators
      // must be cleared
      while (token != null && token.trim().length() == 0) {
          isClearAdaptiveData = true;
          token = samples.read();
      }

      if (token != null) {
        return SequenceLabelSample.parse(token, isClearAdaptiveData);
      }
      else {
        return null;
      }
  }
}

