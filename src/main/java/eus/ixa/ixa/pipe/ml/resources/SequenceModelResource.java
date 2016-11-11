/*
 *  Copyright 2016 Rodrigo Agerri

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

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
 * This class loads a SequenceLabeler model required for the Feature Generation.
 * It also provides the serializer required to add it as a resource to the final
 * target model model.
 * 
 * @author ragerri
 * @version 2016-04-07
 *
 */
public class SequenceModelResource implements SerializableArtifact {

  public static class SequenceModelResourceSerializer
      implements ArtifactSerializer<SequenceModelResource> {

    @Override
    public SequenceModelResource create(final InputStream in)
        throws IOException, InvalidFormatException {
      return new SequenceModelResource(in);
    }

    @Override
    public void serialize(final SequenceModelResource artifact,
        final OutputStream out) throws IOException {
      artifact.serialize(out);
    }
  }

  /**
   * The Sequence Labeler model.
   */
  private final SequenceLabelerModel seqModel;
  /**
   * The SequenceLabeler.
   */
  private final SequenceLabelerME sequenceLabeler;

  /**
   * Construct the SequenceModelResource from the inputstream.
   * 
   * @param in
   *          the input stream
   * @throws IOException
   *           io exception
   */
  public SequenceModelResource(final InputStream in) throws IOException {
    this.seqModel = new SequenceLabelerModel(in);
    this.sequenceLabeler = new SequenceLabelerME(this.seqModel);
  }

  /**
   * Tag the current sentence.
   * 
   * @param tokens
   *          the current sentence
   * @return the array of span sequences
   */
  public Span[] seqToSpans(final String[] tokens) {
    final Span[] origSpans = this.sequenceLabeler.tag(tokens);
    final Span[] seqSpans = SequenceLabelerME.dropOverlappingSpans(origSpans);
    return seqSpans;
  }

  /**
   * Lemmatize the current sentence.
   * 
   * @param tokens
   *          the current sentence
   * @return the array of span sequences
   */
  public String[] lemmatize(final String[] tokens) {
    final Span[] origSpans = this.sequenceLabeler.tag(tokens);
    final Span[] seqSpans = SequenceLabelerME.dropOverlappingSpans(origSpans);
    // TODO work with Spans only
    final String[] decodedLemmas = StringUtils.decodeLemmas(tokens, seqSpans);
    return decodedLemmas;
  }

  /**
   * Serialize this model into the overall Sequence model.
   * 
   * @param out
   *          the output stream
   * @throws IOException
   *           io exception
   */
  public void serialize(final OutputStream out) throws IOException {
    final Writer writer = new BufferedWriter(new OutputStreamWriter(out));
    this.seqModel.serialize(out);
    writer.flush();
  }

  @Override
  public Class<?> getArtifactSerializerClass() {
    return SequenceModelResourceSerializer.class;
  }

}
