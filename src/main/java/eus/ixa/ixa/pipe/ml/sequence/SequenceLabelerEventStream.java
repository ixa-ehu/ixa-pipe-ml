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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import opennlp.tools.ml.model.Event;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.featuregen.AdditionalContextFeatureGenerator;
import opennlp.tools.util.featuregen.WindowFeatureGenerator;

/**
 * Class for creating an event stream out of data files for training an name
 * finder.
 */
public class SequenceLabelerEventStream extends opennlp.tools.util.AbstractEventStream<SequenceLabelSample> {

  private SequenceLabelerContextGenerator contextGenerator;
  private AdditionalContextFeatureGenerator additionalContextFeatureGenerator = new AdditionalContextFeatureGenerator();
  private SequenceLabelerCodec<String> codec;

  /**
   * Creates a new name finder event stream using the specified data stream and context generator.
   * @param dataStream The data stream of events.
   * @param contextGenerator The context generator used to generate features for the event stream.
   * @param codec the encoding
   */
  public SequenceLabelerEventStream(ObjectStream<SequenceLabelSample> dataStream, SequenceLabelerContextGenerator contextGenerator, SequenceLabelerCodec<String> codec) {
    super(dataStream);

    this.codec = codec;
    if (codec == null) {
      this.codec = new BioCodec();
    }
    this.contextGenerator = contextGenerator;
    this.contextGenerator.addFeatureGenerator(new WindowFeatureGenerator(additionalContextFeatureGenerator, 8, 8));
  }

  public SequenceLabelerEventStream(ObjectStream<SequenceLabelSample> dataStream) {
    this(dataStream, new DefaultSequenceLabelerContextGenerator(), null);
  }

  public static List<Event> generateEvents(String[] sentence, String[] outcomes, SequenceLabelerContextGenerator cg) {
    List<Event> events = new ArrayList<Event>(outcomes.length);
    for (int i = 0; i < outcomes.length; i++) {
      events.add(new Event(outcomes[i], cg.getContext(i, sentence, outcomes,null)));
    }
    cg.updateAdaptiveData(sentence, outcomes);
    return events;
  }

  @Override
  protected Iterator<Event> createEvents(SequenceLabelSample sample) {

    if (sample.isClearAdaptiveDataSet()) {
      contextGenerator.clearAdaptiveData();
    }
    String outcomes[] = codec.encode(sample.getSequences(), sample.getTokens().length);
    additionalContextFeatureGenerator.setCurrentContext(sample.getAdditionalContext());
    String[] tokens = new String[sample.getTokens().length];

    for (int i = 0; i < sample.getTokens().length; i++) {
      tokens[i] = sample.getTokens()[i];
    }

    return generateEvents(tokens, outcomes, contextGenerator).iterator();
  }


  /**
   * Generated previous decision features for each token based on contents of the specified map.
   * @param tokens The token for which the context is generated.
   * @param prevMap A mapping of tokens to their previous decisions.
   * @return An additional context array with features for each token.
   */
  public static String[][] additionalContext(String[] tokens, Map<String, String> prevMap) {
    String[][] ac = new String[tokens.length][1];
    for (int ti=0;ti<tokens.length;ti++) {
      String pt = prevMap.get(tokens[ti]);
      ac[ti][0]="pd="+pt;
    }
    return ac;

  }
}

