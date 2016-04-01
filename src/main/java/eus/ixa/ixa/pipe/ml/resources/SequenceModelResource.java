package eus.ixa.ixa.pipe.ml.resources;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerME;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerModel;

import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.Span;
import opennlp.tools.util.model.ArtifactSerializer;
import opennlp.tools.util.model.SerializableArtifact;



/**
 * This class loads the pos tagger model required for
 * the POS FeatureGenerators. It also provides the serializer
 * required to add it as a resource to the ixa-pipe-nerc
 * model.
 * @author ragerri
 * @version 2015-10-03
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
   * The POS model.
   */
  private SequenceLabelerModel posModel;
  /**
   * The POS tagger.
   */
  private SequenceLabelerME posTagger;
  
  /**
   * Construct the POSModelResource from the inputstream.
   * @param in the input stream
   * @throws IOException io exception
   */
  public SequenceModelResource(InputStream in) throws IOException {
    posModel = new SequenceLabelerModel(in);
    posTagger = new SequenceLabelerME(posModel);
  }
  
  /**
   * POS tag the current sentence.
   * @param tokens the current sentence
   * @return the array containing the pos tags
   */
  public Span[] posTag(String[] tokens) {
    Span[] posTags = posTagger.find(tokens);
    return posTags;
  }
  
  /**
   * Serialize the POS model into the NERC model.
   * @param out the output stream
   * @throws IOException io exception
   */
  public void serialize(OutputStream out) throws IOException {
    Writer writer = new BufferedWriter(new OutputStreamWriter(out));
    posModel.serialize(out);

    writer.flush();
  }

  public Class<?> getArtifactSerializerClass() {
    return SequenceModelResourceSerializer.class;
  }

}



