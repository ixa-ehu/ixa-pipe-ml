package eus.ixa.ixa.pipe.ml.formats;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import opennlp.tools.util.InputStreamFactory;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.StringUtil;
import eus.ixa.ixa.pipe.ml.sequence.SequenceSample;
import eus.ixa.ixa.pipe.ml.utils.Span;

/**
 * 2 fields in tabulated format: word\tabclass\n. In this format every row is a
 * span, e.g., all the spans consist of one token. This is typically used when
 * no BIO chunk notation is used to denote multiple token spans.
 * 
 * This is usually the case of word level annotations such as POS tagging.
 * 
 * @author ragerri
 * @version 2016-03-15
 * 
 */
public class TabulatedFormat implements ObjectStream<SequenceSample> {

  /**
   * The stream.
   */
  private final ObjectStream<String> lineStream;
  /**
   * Whether the adaptive features are to be reset or not.
   */
  private String clearFeatures;

  /**
   * Construct a Sequence Stream from a language and a {@code ObjectStream}.
   * 
   * @param clearFeatures
   *          reset the adaptive features
   * @param lineStream
   *          the stream
   */
  public TabulatedFormat(String clearFeatures, ObjectStream<String> lineStream) {
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
  public TabulatedFormat(String clearFeatures, InputStreamFactory in)
      throws IOException {
    this.clearFeatures = clearFeatures;
    try {
      this.lineStream = new PlainTextByLineStream(in, "UTF-8");
      System.setOut(new PrintStream(System.out, true, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      // UTF-8 is available on all JVMs, will never happen
      throw new IllegalStateException(e);
    }
  }

  public SequenceSample read() throws IOException {

    List<String> tokens = new ArrayList<String>();
    List<String> seqTypes = new ArrayList<String>();

    boolean isClearAdaptiveData = false;
    // Empty line indicates end of sentence
    String line;
    while ((line = lineStream.read()) != null && !StringUtil.isEmpty(line)) {
      // clear adaptive data if document mark appears following
      // CoNLL03 conventions
      if (clearFeatures.equalsIgnoreCase("docstart")
          && line.startsWith("-DOCSTART-")) {
        isClearAdaptiveData = true;
        String emptyLine = lineStream.read();
        if (!StringUtil.isEmpty(emptyLine))
          throw new IOException("Empty line after -DOCSTART- not empty: '"
              + emptyLine + "'!");
        continue;
      }
      String fields[] = line.split("\t");
      if (fields.length != 2) {
        System.err.println("Skipping corrupt line: " + line);
      }
      else {
        tokens.add(fields[0]);
        seqTypes.add(fields[1]);
      }
    }
    // check if we need to clear features every sentence
    if (clearFeatures.equalsIgnoreCase("yes")) {
      isClearAdaptiveData = true;
    }
    if (tokens.size() > 0) {
      // convert sequence tags into spans
      List<Span> sequences = new ArrayList<Span>();
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
      if (beginIndex != -1)
        sequences.add(new Span(beginIndex, endIndex, seqTypes.get(beginIndex)));

      return new SequenceSample(tokens.toArray(new String[tokens.size()]),
          sequences.toArray(new Span[sequences.size()]), isClearAdaptiveData);
    } else if (line != null) {
      // Just filter out empty events, if two lines in a row are empty
      return read();
    } else {
      // source stream is not returning anymore lines
      return null;
    }
  }

  public void reset() throws IOException, UnsupportedOperationException {
    lineStream.reset();
  }

  public void close() throws IOException {
    lineStream.close();
  }
}
