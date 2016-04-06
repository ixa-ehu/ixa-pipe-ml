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

  private SequenceContextGenerator pcg;
  private final boolean useOutcomes;
  private ObjectStream<SequenceSample> psi;
  private SequenceCodec<String> seqCodec;

  public SequenceLabelerSequenceStream(ObjectStream<SequenceSample> psi) throws IOException {
    this(psi, new DefaultSequenceContextGenerator((AdaptiveFeatureGenerator) null), true);
  }

  public SequenceLabelerSequenceStream(ObjectStream<SequenceSample> psi, AdaptiveFeatureGenerator featureGen)
  throws IOException {
    this(psi, new DefaultSequenceContextGenerator(featureGen), true);
  }

  public SequenceLabelerSequenceStream(ObjectStream<SequenceSample> psi, AdaptiveFeatureGenerator featureGen, boolean useOutcomes)
  throws IOException {
    this(psi, new DefaultSequenceContextGenerator(featureGen), useOutcomes);
  }

  public SequenceLabelerSequenceStream(ObjectStream<SequenceSample> psi, SequenceContextGenerator pcg)
      throws IOException {
    this(psi, pcg, true);
  }

  public SequenceLabelerSequenceStream(ObjectStream<SequenceSample> psi, SequenceContextGenerator pcg, boolean useOutcomes)
      throws IOException {
    this(psi, pcg, useOutcomes, new BioCodec());
  }

  public SequenceLabelerSequenceStream(ObjectStream<SequenceSample> psi, SequenceContextGenerator pcg, boolean useOutcomes,
      SequenceCodec<String> seqCodec)
      throws IOException {
    this.psi = psi;
    this.useOutcomes = useOutcomes;
    this.pcg = pcg;
    this.seqCodec = seqCodec;
  }

  @SuppressWarnings("unchecked")
  public Event[] updateContext(Sequence sequence, AbstractModel model) {
    Sequence<SequenceSample> pss = sequence;
    SequenceLabeler tagger = new SequenceLabelerME(new SequenceLabelerModel("x-unspecified", model, Collections.<String, Object>emptyMap(), null));
    String[] sentence = pss.getSource().getTokens();
    String[] tags = seqCodec.encode(tagger.tag(sentence), sentence.length);
    Event[] events = new Event[sentence.length];

    SequenceLabelerEventStream.generateEvents(sentence,tags,pcg).toArray(events);

    return events;
  }

  @Override
  public Sequence read() throws IOException {
    SequenceSample sample = psi.read();
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
      Sequence<SequenceSample> sequence = new Sequence<SequenceSample>(events,sample);
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


