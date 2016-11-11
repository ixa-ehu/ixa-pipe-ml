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

package eus.ixa.ixa.pipe.ml.eval;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import eus.ixa.ixa.pipe.ml.SequenceLabelerTrainer;
import eus.ixa.ixa.pipe.ml.features.XMLFeatureDescriptor;
import eus.ixa.ixa.pipe.ml.resources.LoadModelResources;
import eus.ixa.ixa.pipe.ml.sequence.BilouCodec;
import eus.ixa.ixa.pipe.ml.sequence.BioCodec;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelSample;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelSampleTypeFilter;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerCodec;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerCrossValidator;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerDetailedFMeasureListener;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerEvaluationErrorListener;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerEvaluationMonitor;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerFactory;
import eus.ixa.ixa.pipe.ml.utils.Flags;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.TrainingParameters;
import opennlp.tools.util.eval.EvaluationMonitor;

/**
 * Abstract class for common training functionalities. Every other trainer class
 * needs to extend this class.
 * 
 * @author ragerri
 * @version 2014-04-17
 */
public class CrossValidator {

  /**
   * The language.
   */
  private final String lang;
  /**
   * String holding the training data.
   */
  private final String trainData;
  /**
   * ObjectStream of the training data.
   */
  private ObjectStream<SequenceLabelSample> trainSamples;
  /**
   * beamsize value needs to be established in any class extending this one.
   */
  private final int beamSize;
  /**
   * The folds value for cross validation.
   */
  private final int folds;
  /**
   * The sequence encoding of the named entity spans, e.g., BIO or BILOU.
   */
  private final SequenceLabelerCodec<String> sequenceCodec;
  /**
   * The corpus format: conll02, conll03.
   */
  private final String corpusFormat;
  /**
   * features needs to be implemented by any class extending this one.
   */
  private SequenceLabelerFactory nameClassifierFactory;
  /**
   * The evaluation listeners.
   */
  private final List<EvaluationMonitor<SequenceLabelSample>> listeners = new LinkedList<EvaluationMonitor<SequenceLabelSample>>();
  SequenceLabelerDetailedFMeasureListener detailedFListener;

  public CrossValidator(final TrainingParameters params) throws IOException {

    this.lang = Flags.getLanguage(params);
    final String clearFeatures = Flags.getClearTrainingFeatures(params);
    this.corpusFormat = Flags.getCorpusFormat(params);
    this.trainData = params.getSettings().get("TrainSet");
    this.trainSamples = SequenceLabelerTrainer.getSequenceStream(this.trainData,
        clearFeatures, this.corpusFormat);
    this.beamSize = Flags.getBeamsize(params);
    this.folds = Flags.getFolds(params);
    this.sequenceCodec = SequenceLabelerFactory.instantiateSequenceCodec(
        getSequenceCodec(Flags.getSequenceCodec(params)));
    if (params.getSettings().get("Types") != null) {
      final String netypes = params.getSettings().get("Types");
      final String[] neTypes = netypes.split(",");
      this.trainSamples = new SequenceLabelSampleTypeFilter(neTypes,
          this.trainSamples);
    }
    createNameFactory(params);
    getEvalListeners(params);
  }

  private void createNameFactory(final TrainingParameters params)
      throws IOException {
    final String featureDescription = XMLFeatureDescriptor
        .createXMLFeatureDescriptor(params);
    System.err.println(featureDescription);
    final byte[] featureGeneratorBytes = featureDescription
        .getBytes(Charset.forName("UTF-8"));
    final Map<String, Object> resources = LoadModelResources
        .loadSequenceResources(params);
    this.nameClassifierFactory = SequenceLabelerFactory.create(
        SequenceLabelerFactory.class.getName(), featureGeneratorBytes,
        resources, this.sequenceCodec);
  }

  private void getEvalListeners(final TrainingParameters params) {
    if (params.getSettings().get("EvaluationType").equalsIgnoreCase("error")) {
      this.listeners.add(new SequenceLabelerEvaluationErrorListener());
    }
    if (params.getSettings().get("EvaluationType")
        .equalsIgnoreCase("detailed")) {
      this.detailedFListener = new SequenceLabelerDetailedFMeasureListener();
      this.listeners.add(this.detailedFListener);
    }
  }

  public final void crossValidate(final TrainingParameters params) {
    if (this.nameClassifierFactory == null) {
      throw new IllegalStateException(
          "Classes derived from AbstractNameFinderTrainer must create and fill the AdaptiveFeatureGenerator features!");
    }
    SequenceLabelerCrossValidator validator = null;
    try {
      validator = new SequenceLabelerCrossValidator(this.lang, params,
          this.nameClassifierFactory, this.listeners.toArray(
              new SequenceLabelerEvaluationMonitor[this.listeners.size()]));
      validator.evaluate(this.trainSamples, this.folds);
    } catch (final IOException e) {
      System.err.println("IO error while loading training set!");
      e.printStackTrace();
      System.exit(1);
    } finally {
      try {
        this.trainSamples.close();
      } catch (final IOException e) {
        System.err.println("IO error with the train samples!");
      }
    }
    if (this.detailedFListener == null) {
      System.out.println(validator.getFMeasure());
    } else {
      System.out.println(this.detailedFListener.toString());
    }
  }

  /**
   * Get the Sequence codec.
   * 
   * @param seqCodecOption
   *          the codec chosen
   * @return the sequence codec
   */
  public final String getSequenceCodec(final String seqCodecOption) {
    String seqCodec = null;
    if ("BIO".equals(seqCodecOption)) {
      seqCodec = BioCodec.class.getName();
    } else if ("BILOU".equals(seqCodecOption)) {
      seqCodec = BilouCodec.class.getName();
    }
    return seqCodec;
  }

  public final int getBeamSize() {
    return this.beamSize;
  }

}
