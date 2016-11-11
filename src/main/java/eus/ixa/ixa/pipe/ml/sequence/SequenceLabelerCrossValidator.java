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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import opennlp.tools.util.FilterObjectStream;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.TrainingParameters;
import opennlp.tools.util.eval.CrossValidationPartitioner;
import opennlp.tools.util.eval.FMeasure;

public class SequenceLabelerCrossValidator {

  private class DocumentSample {

    private final SequenceLabelSample samples[];

    DocumentSample(final SequenceLabelSample samples[]) {
      this.samples = samples;
    }

    private SequenceLabelSample[] getSamples() {
      return this.samples;
    }
  }

  /**
   * Reads Name Samples to group them as a document based on the clear adaptive
   * data flag.
   */
  private class NameToDocumentSampleStream
      extends FilterObjectStream<SequenceLabelSample, DocumentSample> {

    private SequenceLabelSample beginSample;

    protected NameToDocumentSampleStream(
        final ObjectStream<SequenceLabelSample> samples) {
      super(samples);
    }

    @Override
    public DocumentSample read() throws IOException {

      final List<SequenceLabelSample> document = new ArrayList<SequenceLabelSample>();

      if (this.beginSample == null) {
        // Assume that the clear flag is set
        this.beginSample = this.samples.read();
      }

      // Underlying stream is exhausted!
      if (this.beginSample == null) {
        return null;
      }

      document.add(this.beginSample);

      SequenceLabelSample sample;
      while ((sample = this.samples.read()) != null) {

        if (sample.isClearAdaptiveDataSet()) {
          this.beginSample = sample;
          break;
        }

        document.add(sample);
      }

      // Underlying stream is exhausted,
      // next call must return null
      if (sample == null) {
        this.beginSample = null;
      }

      return new DocumentSample(
          document.toArray(new SequenceLabelSample[document.size()]));
    }

    @Override
    public void reset() throws IOException, UnsupportedOperationException {
      super.reset();

      this.beginSample = null;
    }
  }

  /**
   * Splits DocumentSample into NameSamples.
   */
  private class DocumentToNameSampleStream
      extends FilterObjectStream<DocumentSample, SequenceLabelSample> {

    protected DocumentToNameSampleStream(
        final ObjectStream<DocumentSample> samples) {
      super(samples);
    }

    private Iterator<SequenceLabelSample> documentSamples = Collections
        .<SequenceLabelSample> emptyList().iterator();

    @Override
    public SequenceLabelSample read() throws IOException {

      // Note: Empty document samples should be skipped

      if (this.documentSamples.hasNext()) {
        return this.documentSamples.next();
      } else {
        final DocumentSample docSample = this.samples.read();

        if (docSample != null) {
          this.documentSamples = Arrays.asList(docSample.getSamples())
              .iterator();

          return read();
        } else {
          return null;
        }
      }
    }
  }

  private final String languageCode;
  private final TrainingParameters params;
  private final SequenceLabelerEvaluationMonitor[] listeners;

  private final FMeasure fmeasure = new FMeasure();
  private SequenceLabelerFactory factory;

  /**
   * SequenceLabeler Cross Validator.
   * 
   * @param languageCode
   *          the language
   * @param trainParams
   *          the parameters files
   * @param featureGeneratorBytes
   *          the feature descriptor
   * @param resources
   *          the external resources
   * @param codec
   *          the encoding
   * @param listeners
   *          the listeners
   */
  public SequenceLabelerCrossValidator(final String languageCode,
      final TrainingParameters trainParams, final byte[] featureGeneratorBytes,
      final Map<String, Object> resources,
      final SequenceLabelerCodec<String> codec,
      final SequenceLabelerEvaluationMonitor... listeners) {

    this.languageCode = languageCode;
    this.params = trainParams;

    this.listeners = listeners;
  }

  public SequenceLabelerCrossValidator(final String languageCode,
      final TrainingParameters trainParams, final byte[] featureGeneratorBytes,
      final Map<String, Object> resources,
      final SequenceLabelerEvaluationMonitor... listeners) {
    this(languageCode, trainParams, featureGeneratorBytes, resources,
        new BioCodec(), listeners);
  }

  public SequenceLabelerCrossValidator(final String languageCode,
      final TrainingParameters trainParams,
      final SequenceLabelerFactory factory,
      final SequenceLabelerEvaluationMonitor... listeners) {
    this.languageCode = languageCode;
    this.params = trainParams;
    this.factory = factory;
    this.listeners = listeners;
  }

  /**
   * Starts the evaluation.
   *
   * @param samples
   *          the data to train and test
   * @param nFolds
   *          number of folds
   * @throws IOException
   *           if io errors
   */
  public void evaluate(final ObjectStream<SequenceLabelSample> samples,
      final int nFolds) throws IOException {

    // Note: The name samples need to be grouped on a document basis.

    final CrossValidationPartitioner<DocumentSample> partitioner = new CrossValidationPartitioner<DocumentSample>(
        new NameToDocumentSampleStream(samples), nFolds);

    while (partitioner.hasNext()) {

      final CrossValidationPartitioner.TrainingSampleStream<DocumentSample> trainingSampleStream = partitioner
          .next();

      SequenceLabelerModel model = null;
      if (this.factory != null) {
        model = SequenceLabelerME.train(this.languageCode,
            new DocumentToNameSampleStream(trainingSampleStream), this.params,
            this.factory);
      } else {
        System.err.println("You need to implement a SequenceLabelerFactory!");
        System.exit(1);
      }

      // do testing
      final SequenceLabelerEvaluator evaluator = new SequenceLabelerEvaluator(
          new SequenceLabelerME(model), this.listeners);

      evaluator.evaluate(new DocumentToNameSampleStream(
          trainingSampleStream.getTestSampleStream()));

      this.fmeasure.mergeInto(evaluator.getFMeasure());
    }
  }

  public FMeasure getFMeasure() {
    return this.fmeasure;
  }
}
