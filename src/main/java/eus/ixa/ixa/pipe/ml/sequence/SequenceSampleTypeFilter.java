package eus.ixa.ixa.pipe.ml.sequence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import opennlp.tools.util.FilterObjectStream;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.Span;

/**
 * A stream which removes Name Samples which do not have a certain type.
 */
public class SequenceSampleTypeFilter extends FilterObjectStream<SequenceSample, SequenceSample> {

  private final Set<String> types;

  public SequenceSampleTypeFilter(String[] types, ObjectStream<SequenceSample> samples) {
    super(samples);
    this.types = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(types)));
  }

  public SequenceSampleTypeFilter(Set<String> types, ObjectStream<SequenceSample> samples) {
    super(samples);
    this.types = Collections.unmodifiableSet(new HashSet<String>(types));
  }

  public SequenceSample read() throws IOException {

    SequenceSample sample = samples.read();

    if (sample != null) {

      List<Span> filteredNames = new ArrayList<Span>();

      for (Span name : sample.getSequences()) {
        if (types.contains(name.getType())) {
          filteredNames.add(name);
        }
      }

      return new SequenceSample(sample.getId(), sample.getTokens(),
          filteredNames.toArray(new Span[filteredNames.size()]), null, sample.isClearAdaptiveDataSet());
    }
    else {
      return null;
    }
  }
}

