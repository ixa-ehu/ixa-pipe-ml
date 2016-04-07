package eus.ixa.ixa.pipe.ml.resources;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerME;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerModel;
import eus.ixa.ixa.pipe.ml.utils.Span;
import eus.ixa.ixa.pipe.ml.utils.StringUtils;

import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.model.ArtifactSerializer;
import opennlp.tools.util.model.SerializableArtifact;



/**
 * This class loads a SequenceLabeler model required for
 * the Feature Generation. It also provides the serializer
 * required to add it as a resource to the final target model
 * model.
 * @author ragerri
 * @version 2016-04-07
 * 
 */
public class SequenceModelResource implements SerializableArtifact {
  
  public static class SequenceModelResourceSerializer implements ArtifactSerializer<SequenceModelResource> {

    public SequenceModelResource create(InputStream in) throws IOException,
        InvalidFormatException {
      return new SequenceModelResource(in);
    }

    public void serialize(SequenceModelResource artifact, OutputStream out)
        throws IOException {
      artifact.serialize(out);
    }
  }
  
  /**
   * The Sequence Labeler model.
   */
  private SequenceLabelerModel seqModel;
  /**
   * The SequenceLabeler.
   */
  private SequenceLabelerME sequenceLabeler;
  
  /**
   * Construct the SequenceModelResource from the inputstream.
   * @param in the input stream
   * @throws IOException io exception
   */
  public SequenceModelResource(InputStream in) throws IOException {
    seqModel = new SequenceLabelerModel(in);
    sequenceLabeler = new SequenceLabelerME(seqModel);
  }
  
  /**
   * Tag the current sentence.
   * @param tokens the current sentence
   * @return the array of span sequences
   */
  public Span[] seqToSpans(String[] tokens) {
    Span[] origSpans = sequenceLabeler.tag(tokens);
    Span[] seqSpans = SequenceLabelerME.dropOverlappingSpans(origSpans);
    return seqSpans;
  }
  
  /**
   * Lemmatize the current sentence.
   * @param tokens the current sentence
   * @return the array of span sequences
   */
  public String[] lemmatize(String[] tokens) {
    Span[] origSpans = sequenceLabeler.tag(tokens);
    Span[] seqSpans = SequenceLabelerME.dropOverlappingSpans(origSpans);
    String[] decodedLemmas = StringUtils.decodeLemmas(tokens, seqSpans);
    return decodedLemmas;
  }
  
  /**
   * Serialize this model into the overall Sequence model.
   * @param out the output stream
   * @throws IOException io exception
   */
  public void serialize(OutputStream out) throws IOException {
    Writer writer = new BufferedWriter(new OutputStreamWriter(out));
    seqModel.serialize(out);
    writer.flush();
  }

  public Class<?> getArtifactSerializerClass() {
    return SequenceModelResourceSerializer.class;
  }

}



