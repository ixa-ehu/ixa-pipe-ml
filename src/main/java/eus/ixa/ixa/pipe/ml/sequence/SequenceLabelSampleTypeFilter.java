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
public class SequenceLabelSampleTypeFilter
    extends FilterObjectStream<SequenceLabelSample, SequenceLabelSample> {

  private final Set<String> types;

  public SequenceLabelSampleTypeFilter(final String[] types,
      final ObjectStream<SequenceLabelSample> samples) {
    super(samples);
    this.types = Collections
        .unmodifiableSet(new HashSet<String>(Arrays.asList(types)));
  }

  public SequenceLabelSampleTypeFilter(final Set<String> types,
      final ObjectStream<SequenceLabelSample> samples) {
    super(samples);
    this.types = Collections.unmodifiableSet(new HashSet<String>(types));
  }

  @Override
  public SequenceLabelSample read() throws IOException {

    final SequenceLabelSample sample = this.samples.read();

    if (sample != null) {

      final List<Span> filteredNames = new ArrayList<Span>();

      for (final Span name : sample.getSequences()) {
        if (this.types.contains(name.getType())) {
          filteredNames.add(name);
        }
      }

      return new SequenceLabelSample(sample.getId(), sample.getTokens(),
          filteredNames.toArray(new Span[filteredNames.size()]), null,
          sample.isClearAdaptiveDataSet());
    } else {
      return null;
    }
  }
}
