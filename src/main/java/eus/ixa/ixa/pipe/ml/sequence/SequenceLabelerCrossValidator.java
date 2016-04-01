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
import opennlp.tools.util.SequenceCodec;
import opennlp.tools.util.TrainingParameters;
import opennlp.tools.util.eval.CrossValidationPartitioner;
import opennlp.tools.util.eval.FMeasure;

public class SequenceLabelerCrossValidator {

	  private class DocumentSample {

	    private SequenceSample samples[];

	    DocumentSample(SequenceSample samples[]) {
	      this.samples = samples;
	    }

	    private SequenceSample[] getSamples() {
	      return samples;
	    }
	  }

	  /**
	   * Reads Name Samples to group them as a document based on the clear adaptive data flag.
	   */
	  private class NameToDocumentSampleStream extends FilterObjectStream<SequenceSample, DocumentSample> {

	    private SequenceSample beginSample;

	    protected NameToDocumentSampleStream(ObjectStream<SequenceSample> samples) {
	      super(samples);
	    }

	    public DocumentSample read() throws IOException {

	      List<SequenceSample> document = new ArrayList<SequenceSample>();

	      if (beginSample == null) {
	        // Assume that the clear flag is set
	        beginSample = samples.read();
	      }

	      // Underlying stream is exhausted!
	      if (beginSample == null) {
	        return null;
	      }

	      document.add(beginSample);

	      SequenceSample sample;
	      while ((sample = samples.read()) != null) {

	        if (sample.isClearAdaptiveDataSet()) {
	          beginSample = sample;
	          break;
	        }

	        document.add(sample);
	      }

	      // Underlying stream is exhausted,
	      // next call must return null
	      if (sample == null) {
	        beginSample = null;
	      }

	      return new DocumentSample(document.toArray(new SequenceSample[document.size()]));
	    }

	    @Override
	    public void reset() throws IOException, UnsupportedOperationException {
	      super.reset();

	      beginSample = null;
	    }
	  }

	  /**
	   * Splits DocumentSample into NameSamples.
	   */
	  private class DocumentToNameSampleStream extends FilterObjectStream<DocumentSample, SequenceSample>{

	    protected DocumentToNameSampleStream(ObjectStream<DocumentSample> samples) {
	      super(samples);
	    }

	    private Iterator<SequenceSample> documentSamples = Collections.<SequenceSample>emptyList().iterator();

	    public SequenceSample read() throws IOException {

	      // Note: Empty document samples should be skipped

	      if (documentSamples.hasNext()) {
	        return documentSamples.next();
	      }
	      else {
	        DocumentSample docSample = samples.read();

	        if (docSample != null) {
	          documentSamples = Arrays.asList(docSample.getSamples()).iterator();

	          return read();
	        }
	        else {
	          return null;
	        }
	      }
	    }
	  }

	  private final String languageCode;
	  private final TrainingParameters params;
	  private final String type;
	  private SequenceLabelerEvaluationMonitor[] listeners;

	  private FMeasure fmeasure = new FMeasure();
	  private SequenceLabelerFactory factory;

	  
	  /** SequenceLabeler Cross Validator.
	 * @param languageCode the language
	 * @param type the class
	 * @param trainParams the parameters files
	 * @param featureGeneratorBytes the feature descriptor
	 * @param resources the external resources
	 * @param codec the encoding
	 * @param listeners the listeners
	 */
	public SequenceLabelerCrossValidator(String languageCode, String type,
	      TrainingParameters trainParams, byte[] featureGeneratorBytes,
	      Map<String, Object> resources, SequenceCodec<String> codec,
	      SequenceLabelerEvaluationMonitor... listeners) {

	    this.languageCode = languageCode;
	    this.type = type;
	    this.params = trainParams;

	    this.listeners = listeners;
	  }

	  public SequenceLabelerCrossValidator(String languageCode, String type,
	      TrainingParameters trainParams, byte[] featureGeneratorBytes,
	      Map<String, Object> resources,
	      SequenceLabelerEvaluationMonitor... listeners) {
	    this(languageCode, type, trainParams, featureGeneratorBytes, resources, new BioCodec(), listeners);
	  }

	  public SequenceLabelerCrossValidator(String languageCode, String type,
	      TrainingParameters trainParams, SequenceLabelerFactory factory,
	      SequenceLabelerEvaluationMonitor... listeners) {
	    this.languageCode = languageCode;
	    this.type = type;
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
	   * @throws IOException if io errors
	   */
	  public void evaluate(ObjectStream<SequenceSample> samples, int nFolds)
	      throws IOException {

	    // Note: The name samples need to be grouped on a document basis.

	    CrossValidationPartitioner<DocumentSample> partitioner = new CrossValidationPartitioner<DocumentSample>(
	        new NameToDocumentSampleStream(samples), nFolds);

	    while (partitioner.hasNext()) {

	      CrossValidationPartitioner.TrainingSampleStream<DocumentSample> trainingSampleStream = partitioner
	          .next();

	      SequenceLabelerModel model = null;
	      if (factory != null) {
	        model = SequenceLabelerME.train(languageCode, type, new DocumentToNameSampleStream(trainingSampleStream), params, factory);
	      }
	      else {
	        System.err.println("You need to implement a SequenceLabelerFactory!");
	        System.exit(1);
	      }

	      // do testing
	      SequenceLabelerEvaluator evaluator = new SequenceLabelerEvaluator(
	          new SequenceLabelerME(model), listeners);

	      evaluator.evaluate(new DocumentToNameSampleStream(trainingSampleStream.getTestSampleStream()));

	      fmeasure.mergeInto(evaluator.getFMeasure());
	    }
	  }

	  public FMeasure getFMeasure() {
	    return fmeasure;
	  }
	}

