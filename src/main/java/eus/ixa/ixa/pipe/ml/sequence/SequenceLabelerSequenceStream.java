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
import java.util.Collections;

import opennlp.tools.ml.model.AbstractModel;
import opennlp.tools.ml.model.Event;
import opennlp.tools.ml.model.Sequence;
import opennlp.tools.ml.model.SequenceStream;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.featuregen.AdaptiveFeatureGenerator;

public class SequenceLabelerSequenceStream implements SequenceStream {

  private SequenceLabelerContextGenerator pcg;
  private final boolean useOutcomes;
  private ObjectStream<SequenceLabelSample> psi;
  private SequenceLabelerCodec<String> seqCodec;

  public SequenceLabelerSequenceStream(ObjectStream<SequenceLabelSample> psi) throws IOException {
    this(psi, new DefaultSequenceLabelerContextGenerator((AdaptiveFeatureGenerator) null), true);
  }

  public SequenceLabelerSequenceStream(ObjectStream<SequenceLabelSample> psi, AdaptiveFeatureGenerator featureGen)
  throws IOException {
    this(psi, new DefaultSequenceLabelerContextGenerator(featureGen), true);
  }

  public SequenceLabelerSequenceStream(ObjectStream<SequenceLabelSample> psi, AdaptiveFeatureGenerator featureGen, boolean useOutcomes)
  throws IOException {
    this(psi, new DefaultSequenceLabelerContextGenerator(featureGen), useOutcomes);
  }

  public SequenceLabelerSequenceStream(ObjectStream<SequenceLabelSample> psi, SequenceLabelerContextGenerator pcg)
      throws IOException {
    this(psi, pcg, true);
  }

  public SequenceLabelerSequenceStream(ObjectStream<SequenceLabelSample> psi, SequenceLabelerContextGenerator pcg, boolean useOutcomes)
      throws IOException {
    this(psi, pcg, useOutcomes, new BioCodec());
  }

  public SequenceLabelerSequenceStream(ObjectStream<SequenceLabelSample> psi, SequenceLabelerContextGenerator pcg, boolean useOutcomes,
      SequenceLabelerCodec<String> seqCodec)
      throws IOException {
    this.psi = psi;
    this.useOutcomes = useOutcomes;
    this.pcg = pcg;
    this.seqCodec = seqCodec;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public Event[] updateContext(Sequence sequence, AbstractModel model) {
    Sequence<SequenceLabelSample> pss = sequence;
    SequenceLabeler tagger = new SequenceLabelerME(new SequenceLabelerModel("x-unspecified", model, Collections.<String, Object>emptyMap(), null));
    String[] sentence = pss.getSource().getTokens();
    String[] tags = seqCodec.encode(tagger.tag(sentence), sentence.length);
    Event[] events = new Event[sentence.length];

    SequenceLabelerEventStream.generateEvents(sentence,tags,pcg).toArray(events);

    return events;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Sequence read() throws IOException {
    SequenceLabelSample sample = psi.read();
    if (sample != null) {
      String sentence[] = sample.getTokens();
      String tags[] = seqCodec.encode(sample.getSequences(), sentence.length);
      Event[] events = new Event[sentence.length];

      for (int i=0; i < sentence.length; i++) {

        // it is safe to pass the tags as previous tags because
        // the context generator does not look for non predicted tags
        String[] context;
        if (useOutcomes) {
          context = pcg.getContext(i, sentence, tags, null);
        }
        else {
          context = pcg.getContext(i, sentence, null, null);
        }

        events[i] = new Event(tags[i], context);
      }
      Sequence<SequenceLabelSample> sequence = new Sequence<SequenceLabelSample>(events,sample);
      return sequence;
      }
      else {
        return null;
      }
  }

  @Override
  public void reset() throws IOException, UnsupportedOperationException {
    psi.reset();
  }

  @Override
  public void close() throws IOException {
    psi.close();
  }
}


