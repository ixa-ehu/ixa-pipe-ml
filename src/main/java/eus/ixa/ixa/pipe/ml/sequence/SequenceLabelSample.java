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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eus.ixa.ixa.pipe.ml.utils.Span;
import opennlp.tools.tokenize.WhitespaceTokenizer;

/**
 * Class for holding sequences for a single unit of text.
 */
public class SequenceLabelSample {

  private final String id;
  private final List<String> tokens;
  private final List<Span> sequences;
  private final String[][] additionalContext;
  private final boolean isClearAdaptiveData;

  /** The a default type value when there is no type in training data. */
  public static final String DEFAULT_TYPE = "default";

  public SequenceLabelSample(final String id, final String[] tokens,
      Span[] sequences, final String[][] additionalContext,
      final boolean clearAdaptiveData) {

    this.id = id;

    if (tokens == null) {
      throw new IllegalArgumentException("sentence must not be null!");
    }

    if (sequences == null) {
      sequences = new Span[0];
    }

    this.tokens = Collections
        .unmodifiableList(new ArrayList<String>(Arrays.asList(tokens)));
    this.sequences = Collections
        .unmodifiableList(new ArrayList<Span>(Arrays.asList(sequences)));

    if (additionalContext != null) {
      this.additionalContext = new String[additionalContext.length][];

      for (int i = 0; i < additionalContext.length; i++) {
        this.additionalContext[i] = new String[additionalContext[i].length];
        System.arraycopy(additionalContext[i], 0, this.additionalContext[i], 0,
            additionalContext[i].length);
      }
    } else {
      this.additionalContext = null;
    }
    this.isClearAdaptiveData = clearAdaptiveData;

    // TODO: Check that name spans are not overlapping, otherwise throw
    // exception
  }

  /**
   * Initializes the current instance.
   *
   * @param tokens
   *          training sentence
   * @param sequences
   *          the sequences spans
   * @param additionalContext
   *          any additional context
   * @param clearAdaptiveData
   *          if true the adaptive data of the feature generators is cleared
   */
  public SequenceLabelSample(final String[] tokens, final Span[] sequences,
      final String[][] additionalContext, final boolean clearAdaptiveData) {
    this(null, tokens, sequences, additionalContext, clearAdaptiveData);
  }

  public SequenceLabelSample(final String[] tokens, final Span[] sequences,
      final boolean clearAdaptiveData) {
    this(tokens, sequences, null, clearAdaptiveData);
  }

  public String getId() {
    return this.id;
  }

  public String[] getTokens() {
    return this.tokens.toArray(new String[this.tokens.size()]);
  }

  public Span[] getSequences() {
    return this.sequences.toArray(new Span[this.sequences.size()]);
  }

  public String[][] getAdditionalContext() {
    return this.additionalContext;
  }

  public boolean isClearAdaptiveDataSet() {
    return this.isClearAdaptiveData;
  }

  @Override
  public boolean equals(final Object obj) {

    if (this == obj) {
      return true;
    } else if (obj instanceof SequenceLabelSample) {
      final SequenceLabelSample a = (SequenceLabelSample) obj;

      return Arrays.equals(getTokens(), a.getTokens())
          && Arrays.equals(getSequences(), a.getSequences())
          && Arrays.equals(getAdditionalContext(), a.getAdditionalContext())
          && isClearAdaptiveDataSet() == a.isClearAdaptiveDataSet();
    } else {
      return false;
    }

  }

  @Override
  public String toString() {
    final StringBuilder result = new StringBuilder();

    // If adaptive data must be cleared insert an empty line
    // before the sample sentence line
    if (isClearAdaptiveDataSet()) {
      result.append("\n");
    }

    for (int tokenIndex = 0; tokenIndex < this.tokens.size(); tokenIndex++) {
      // token

      for (final Span sequence : this.sequences) {
        if (sequence.getStart() == tokenIndex) {
          // check if nameTypes is null, or if the nameType for this specific
          // entity is empty. If it is, we leave the nameType blank.
          if (sequence.getType() == null) {
            result.append(SequenceLabelSampleDataStream.START_TAG).append(' ');
          } else {
            result.append(SequenceLabelSampleDataStream.START_TAG_PREFIX)
                .append(sequence.getType()).append("> ");
          }
        }

        if (sequence.getEnd() == tokenIndex) {
          result.append(SequenceLabelSampleDataStream.END_TAG).append(' ');
        }
      }

      result.append(this.tokens.get(tokenIndex)).append(' ');
    }

    if (this.tokens.size() > 1) {
      result.setLength(result.length() - 1);
    }

    for (final Span name : this.sequences) {
      if (name.getEnd() == this.tokens.size()) {
        result.append(' ').append(SequenceLabelSampleDataStream.END_TAG);
      }
    }

    return result.toString();
  }

  private static String errorTokenWithContext(final String sentence[],
      final int index) {

    final StringBuilder errorString = new StringBuilder();

    // two token before
    if (index > 1) {
      errorString.append(sentence[index - 2]).append(" ");
    }

    if (index > 0) {
      errorString.append(sentence[index - 1]).append(" ");
    }

    // token itself
    errorString.append("###");
    errorString.append(sentence[index]);
    errorString.append("###").append(" ");

    // two token after
    if (index + 1 < sentence.length) {
      errorString.append(sentence[index + 1]).append(" ");
    }

    if (index + 2 < sentence.length) {
      errorString.append(sentence[index + 2]);
    }

    return errorString.toString();
  }

  private static final Pattern START_TAG_PATTERN = Pattern
      .compile("<START(:([^:>\\s]*))?>");

  public static SequenceLabelSample parse(final String taggedTokens,
      final boolean isClearAdaptiveData) throws IOException {
    return parse(taggedTokens, DEFAULT_TYPE, isClearAdaptiveData);
  }

  public static SequenceLabelSample parse(final String taggedTokens,
      final String defaultType, final boolean isClearAdaptiveData)
      // TODO: Should throw another exception, and then convert it into an
      // IOException in the stream
      throws IOException {
    final String[] parts = WhitespaceTokenizer.INSTANCE.tokenize(taggedTokens);

    final List<String> tokenList = new ArrayList<String>(parts.length);
    final List<Span> seqList = new ArrayList<Span>();

    String sequenceType = defaultType;
    int startIndex = -1;
    int wordIndex = 0;

    // we check if at least one name has the a type. If no one has, we will
    // leave the SequenceType property of NameSample null.
    boolean catchingSequence = false;

    for (int pi = 0; pi < parts.length; pi++) {
      final Matcher startMatcher = START_TAG_PATTERN.matcher(parts[pi]);
      if (startMatcher.matches()) {
        if (catchingSequence) {
          throw new IOException("Found unexpected annotation"
              + " while handling a name sequence: "
              + errorTokenWithContext(parts, pi));
        }
        catchingSequence = true;
        startIndex = wordIndex;
        final String sequenceTypeFromSample = startMatcher.group(2);
        if (sequenceTypeFromSample != null) {
          if (sequenceTypeFromSample.length() == 0) {
            throw new IOException(
                "Missing a name type: " + errorTokenWithContext(parts, pi));
          }
          sequenceType = sequenceTypeFromSample;
        }

      } else if (parts[pi].equals(SequenceLabelSampleDataStream.END_TAG)) {
        if (catchingSequence == false) {
          throw new IOException("Found unexpected annotation: "
              + errorTokenWithContext(parts, pi));
        }
        catchingSequence = false;
        // create sequence
        seqList.add(new Span(startIndex, wordIndex, sequenceType));

      } else {
        tokenList.add(parts[pi]);
        wordIndex++;
      }
    }
    final String[] tokens = tokenList.toArray(new String[tokenList.size()]);
    final Span[] sequences = seqList.toArray(new Span[seqList.size()]);

    return new SequenceLabelSample(tokens, sequences, isClearAdaptiveData);
  }
}
