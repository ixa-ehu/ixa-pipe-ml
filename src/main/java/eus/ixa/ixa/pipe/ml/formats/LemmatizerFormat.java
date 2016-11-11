/*
 * Copyright 2016 Rodrigo Agerri

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

package eus.ixa.ixa.pipe.ml.formats;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelSample;
import eus.ixa.ixa.pipe.ml.utils.Span;
import eus.ixa.ixa.pipe.ml.utils.StringUtils;
import opennlp.tools.util.InputStreamFactory;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.StringUtil;

/**
 * 2 fields in tabulated format: word\tabclass\n. In this format every row is a
 * span, e.g., all the spans consist of one token. This is typically used when
 * no BIO chunk notation is used to denote multiple token spans.
 *
 * This format differs from TabulatedFormat in which the type of the Span
 * consists of an automatically induced lemma class.
 *
 * @author ragerri
 * @version 2016-03-15
 *
 */
public class LemmatizerFormat implements ObjectStream<SequenceLabelSample> {

  /**
   * The stream.
   */
  private final ObjectStream<String> lineStream;
  /**
   * Whether the adaptive features are to be reset or not.
   */
  private final String clearFeatures;

  /**
   * Construct a Sequence Stream from a language and a {@code ObjectStream}.
   *
   * @param clearFeatures
   *          reset the adaptive features
   * @param lineStream
   *          the stream
   */
  public LemmatizerFormat(final String clearFeatures,
      final ObjectStream<String> lineStream) {
    this.clearFeatures = clearFeatures;
    this.lineStream = lineStream;
  }

  /**
   * Construct a Sequence Stream from a language and an input stream.
   *
   * @param clearFeatures
   *          reset the adaptive features
   * @param in
   *          an input stream to read data
   * @throws IOException
   *           the input stream exception
   */
  public LemmatizerFormat(final String clearFeatures,
      final InputStreamFactory in) throws IOException {
    this.clearFeatures = clearFeatures;
    try {
      this.lineStream = new PlainTextByLineStream(in, "UTF-8");
      System.setOut(new PrintStream(System.out, true, "UTF-8"));
    } catch (final UnsupportedEncodingException e) {
      // UTF-8 is available on all JVMs, will never happen
      throw new IllegalStateException(e);
    }
  }

  @Override
  public SequenceLabelSample read() throws IOException {

    final List<String> tokens = new ArrayList<String>();
    final List<String> seqTypes = new ArrayList<String>();

    boolean isClearAdaptiveData = false;
    // Empty line indicates end of sentence
    String line;
    while ((line = this.lineStream.read()) != null
        && !StringUtil.isEmpty(line)) {
      // clear adaptive data if document mark appears following
      // CoNLL03 conventions
      if (this.clearFeatures.equalsIgnoreCase("docstart")
          && line.startsWith("-DOCSTART-")) {
        isClearAdaptiveData = true;
        final String emptyLine = this.lineStream.read();
        if (!StringUtil.isEmpty(emptyLine)) {
          throw new IOException(
              "Empty line after -DOCSTART- not empty: '" + emptyLine + "'!");
        }
        continue;
      }
      final String fields[] = line.split("\t");
      if (fields.length == 2) {
        tokens.add(fields[0]);
        final String ses = StringUtils.getShortestEditScript(fields[0],
            fields[1]);
        seqTypes.add(ses);
      } else {
        throw new IOException(
            "Expected two fields per line in training data, got "
                + fields.length + " for line '" + line + "'!");
      }
    }
    // check if we need to clear features every sentence
    if (this.clearFeatures.equalsIgnoreCase("yes")) {
      isClearAdaptiveData = true;
    }
    if (tokens.size() > 0) {
      // convert sequence tags into spans
      final List<Span> sequences = new ArrayList<Span>();
      int beginIndex = -1;
      int endIndex = -1;
      for (int i = 0; i < seqTypes.size(); i++) {
        if (beginIndex != -1) {
          sequences
              .add(new Span(beginIndex, endIndex, seqTypes.get(beginIndex)));
          beginIndex = -1;
          endIndex = -1;
        }
        beginIndex = i;
        endIndex = i + 1;
      }
      // if one span remains, create it here
      if (beginIndex != -1) {
        sequences.add(new Span(beginIndex, endIndex, seqTypes.get(beginIndex)));
      }

      return new SequenceLabelSample(tokens.toArray(new String[tokens.size()]),
          sequences.toArray(new Span[sequences.size()]), isClearAdaptiveData);
    } else if (line != null) {
      // Just filter out empty events, if two lines in a row are empty
      return read();
    } else {
      // source stream is not returning anymore lines
      return null;
    }
  }

  @Override
  public void reset() throws IOException, UnsupportedOperationException {
    this.lineStream.reset();
  }

  @Override
  public void close() throws IOException {
    this.lineStream.close();
  }
}
