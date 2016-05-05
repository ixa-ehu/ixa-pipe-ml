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

package eus.ixa.ixa.pipe.ml.train;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.TrainingParameters;
import eus.ixa.ixa.pipe.ml.features.XMLFeatureDescriptor;
import eus.ixa.ixa.pipe.ml.formats.CoNLL02Format;
import eus.ixa.ixa.pipe.ml.formats.CoNLL03Format;
import eus.ixa.ixa.pipe.ml.formats.LemmatizerFormat;
import eus.ixa.ixa.pipe.ml.formats.TabulatedFormat;
import eus.ixa.ixa.pipe.ml.resources.LoadModelResources;
import eus.ixa.ixa.pipe.ml.sequence.BilouCodec;
import eus.ixa.ixa.pipe.ml.sequence.BioCodec;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerCodec;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerEvaluator;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerFactory;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerME;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerModel;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelSample;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelSampleTypeFilter;
import eus.ixa.ixa.pipe.ml.utils.Flags;
import eus.ixa.ixa.pipe.ml.utils.IOUtils;

/**
 * Trainer based on Apache OpenNLP Machine Learning API. This class creates
 * a feature set based on the features activated in the trainParams.properties
 * file:
 * <ol>
 * <li>Window: specify left and right window lengths.
 * <li>TokenFeatures: tokens as features in a window length.
 * <li>TokenClassFeatures: token shape features in a window length.
 * <li>WordShapeSuperSenseFeatures: token shape features from Ciaramita and Altun (2006).
 * <li>OutcomePriorFeatures: take into account previous outcomes.
 * <li>PreviousMapFeatures: add features based on tokens and previous decisions.
 * <li>SentenceFeatures: add beginning and end of sentence words.
 * <li>PrefixFeatures: first 4 characters in current token.
 * <li>SuffixFeatures: last 4 characters in current token.
 * <li>BigramClassFeatures: bigrams of tokens and token class.
 * <li>TrigramClassFeatures: trigrams of token and token class.
 * <li>FourgramClassFeatures: fourgrams of token and token class.
 * <li>FivegramClassFeatures: fivegrams of token and token class.
 * <li>CharNgramFeatures: character ngram features of current token.
 * <li>DictionaryFeatures: check if current token appears in some gazetteer.
 * <li>ClarkClusterFeatures: use the clustering class of a token as a feature.
 * <li>BrownClusterFeatures: use brown clusters as features for each feature
 * containing a token.
 * <li>Word2VecClusterFeatures: use the word2vec clustering class of a token as
 * a feature.
 * <li>POSTagModelFeatures: use pos tags, pos tag class as features.
 * <li>LemmaModelFeatures: use lemma as features.
 * <li>LemmaDictionaryFeatures: use lemma from a dictionary as features.
 * <li>MFSFeatures: Most Frequent sense feature.
 * <li>SuperSenseFeatures: Ciaramita and Altun (2006) features for super sense tagging.
 * </ol>
 * @author ragerri
 * @version 2016-05-06
 */
public class SequenceLabelerTrainer {
  
  /**
   * The language.
   */
  private String lang;
  /**
   * String holding the training data.
   */
  private String trainData;
  /**
   * String holding the testData.
   */
  private String testData;
  /**
   * ObjectStream of the training data.
   */
  private ObjectStream<SequenceLabelSample> trainSamples;
  /**
   * ObjectStream of the test data.
   */
  private ObjectStream<SequenceLabelSample> testSamples;
  /**
   * The corpus format: conll02, conll03, lemmatizer, tabulated.
   */
  private String corpusFormat;
  /**
   * beamsize value needs to be established in any class extending this one.
   */
  private int beamSize;
  /**
   * The sequence encoding of the named entity spans, e.g., BIO or BILOU.
   */
  private String sequenceCodec;
  /**
   * Reset the adaptive features every newline in the training data.
   */
  private String clearTrainingFeatures;
  /**
   * Reset the adaptive features every newline in the testing data.
   */
  private String clearEvaluationFeatures;
  /**
   * features needs to be implemented by any class extending this one.
   */
  private SequenceLabelerFactory nameClassifierFactory;

  /**
   * Construct a trainer with training and test data, and with options for
   * language, beamsize for decoding, sequence codec and corpus format (conll or opennlp).
   * @param params the training parameters
   * @throws IOException
   *           io exception
   */
  public SequenceLabelerTrainer(final TrainingParameters params) throws IOException {
    
    this.lang = Flags.getLanguage(params);
    this.clearTrainingFeatures = Flags.getClearTrainingFeatures(params);
    this.clearEvaluationFeatures = Flags.getClearEvaluationFeatures(params);
    this.corpusFormat = Flags.getCorpusFormat(params);
    this.trainData = params.getSettings().get("TrainSet");
    this.testData = params.getSettings().get("TestSet");
    trainSamples = getSequenceStream(trainData, clearTrainingFeatures, corpusFormat);
    testSamples = getSequenceStream(testData, clearEvaluationFeatures, corpusFormat);
    this.beamSize = Flags.getBeamsize(params);
    this.sequenceCodec = Flags.getSequenceCodec(params);
    if (params.getSettings().get("Types") != null) {
      String netypes = params.getSettings().get("Types");
      String[] neTypes = netypes.split(",");
      trainSamples = new SequenceLabelSampleTypeFilter(neTypes, trainSamples);
      testSamples = new SequenceLabelSampleTypeFilter(neTypes, testSamples);
    }
    createSequenceLabelerFactory(params);
  }
  
  /**
   * Create {@code SequenceLabelerFactory} with custom features.
   * 
   * @param params
   *          the parameter training file
   * @throws IOException if io error
   */
  public void createSequenceLabelerFactory(TrainingParameters params) throws IOException {
    String seqCodec = getSequenceCodec();
    SequenceLabelerCodec<String> sequenceCodec = SequenceLabelerFactory
        .instantiateSequenceCodec(seqCodec);
    String featureDescription = XMLFeatureDescriptor
        .createXMLFeatureDescriptor(params);
    System.err.println(featureDescription);
    byte[] featureGeneratorBytes = featureDescription.getBytes(Charset
        .forName("UTF-8"));
    Map<String, Object> resources = LoadModelResources.loadResources(params, featureGeneratorBytes);
    setSequenceLabelerFactory(SequenceLabelerFactory.create(
        SequenceLabelerFactory.class.getName(), featureGeneratorBytes,
        resources, sequenceCodec));
  }

  public final SequenceLabelerModel train(final TrainingParameters params) {
    if (getSequenceLabelerFactory() == null) {
      throw new IllegalStateException(
          "The SequenceLabelerFactory must be instantiated!!");
    }
    SequenceLabelerModel trainedModel = null;
    SequenceLabelerEvaluator nerEvaluator = null;
    try {
      trainedModel = SequenceLabelerME.train(lang, null, trainSamples, params,
          nameClassifierFactory);
      SequenceLabelerME nerTagger = new SequenceLabelerME(trainedModel);
      nerEvaluator = new SequenceLabelerEvaluator(nerTagger);
      nerEvaluator.evaluate(testSamples);
    } catch (IOException e) {
      System.err.println("IO error while loading traing and test sets!");
      e.printStackTrace();
      System.exit(1);
    }
    System.out.println("Final Result: \n" + nerEvaluator.getFMeasure());
    //System.out.println("Word accuracy: " + nerEvaluator.getWordAccuracy());
    return trainedModel;
  }

  /**
   * Getting the stream with the right corpus format.
   * @param inputData
   *          the input data
   * @param clearFeatures clear the features
   * @param aCorpusFormat
   *          the corpus format
   * @return the stream from the several corpus formats
   * @throws IOException
   *           the io exception
   */
  public static ObjectStream<SequenceLabelSample> getSequenceStream(final String inputData,
      final String clearFeatures, final String aCorpusFormat) throws IOException {
    ObjectStream<SequenceLabelSample> samples = null;
    if (aCorpusFormat.equalsIgnoreCase("conll03")) {
      ObjectStream<String> nameStream = IOUtils.readFileIntoMarkableStreamFactory(inputData);
      samples = new CoNLL03Format(clearFeatures, nameStream);
    } else if (aCorpusFormat.equalsIgnoreCase("conll02")) {
      ObjectStream<String> nameStream = IOUtils.readFileIntoMarkableStreamFactory(inputData);
      samples = new CoNLL02Format(clearFeatures, nameStream);
    } else if (aCorpusFormat.equalsIgnoreCase("tabulated")) {
      ObjectStream<String> nameStream = IOUtils.readFileIntoMarkableStreamFactory(inputData);
      samples = new TabulatedFormat(clearFeatures, nameStream);
    } else if (aCorpusFormat.equalsIgnoreCase("lemmatizer")) {
      ObjectStream<String> seqStream = IOUtils.readFileIntoMarkableStreamFactory(inputData);
      samples = new LemmatizerFormat(clearFeatures, seqStream);
    } else {
      System.err.println("Test set corpus format not valid!!");
      System.exit(1);
    }
    return samples;
  }
 
  /**
   * Get the features which are implemented in each of the trainers extending
   * this class.
   * @return the features
   */
  public final SequenceLabelerFactory getSequenceLabelerFactory() {
    return nameClassifierFactory;
  }
  
  public final SequenceLabelerFactory setSequenceLabelerFactory(SequenceLabelerFactory tokenNameFinderFactory) {
    this.nameClassifierFactory = tokenNameFinderFactory;
    return nameClassifierFactory;
  }
    
  /**
   * Get the Sequence codec.
   * @return the sequence codec
   */
  public final String getSequenceCodec() {
    String seqCodec = null;
    if ("BIO".equals(sequenceCodec)) {
      seqCodec = BioCodec.class.getName();
    }
    else if ("BILOU".equals(sequenceCodec)) {
      seqCodec = BilouCodec.class.getName();
    }
    return seqCodec;
  }
  
  /**
   * Set the sequence codec.
   * @param aSeqCodec the sequence codec to be set
   */
  public final void setSequenceCodec(final String aSeqCodec) {
    this.sequenceCodec = aSeqCodec;
  }
  
  public final int getBeamSize() {
    return beamSize;
  }

}

