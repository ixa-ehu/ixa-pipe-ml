package eus.ixa.ixa.pipe.ml.sequence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import eus.ixa.ixa.pipe.ml.utils.Span;

import opennlp.tools.util.FilterObjectStream;
import opennlp.tools.util.ObjectStream;

/**
 * A stream which removes Name Samples which do not have a certain type.
 */
public class SequenceLabelSampleTypeFilter extends FilterObjectStream<SequenceLabelSample, SequenceLabelSample> {

  private final Set<String> types;

  public SequenceLabelSampleTypeFilter(String[] types, ObjectStream<SequenceLabelSample> samples) {
    super(samples);
    this.types = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(types)));
  }

  public SequenceLabelSampleTypeFilter(Set<String> types, ObjectStream<SequenceLabelSample> samples) {
    super(samples);
    this.types = Collections.unmodifiableSet(new HashSet<String>(types));
  }

  public SequenceLabelSample read() throws IOException {

    SequenceLabelSample sample = samples.read();

    if (sample != null) {

      List<Span> filteredNames = new ArrayList<Span>();

      for (Span name : sample.getSequences()) {
        if (types.contains(name.getType())) {
          filteredNames.add(name);
        }
      }

      return new SequenceLabelSample(sample.getId(), sample.getTokens(),
          filteredNames.toArray(new Span[filteredNames.size()]), null, sample.isClearAdaptiveDataSet());
    }
    else {
      return null;
    }
  }
}

