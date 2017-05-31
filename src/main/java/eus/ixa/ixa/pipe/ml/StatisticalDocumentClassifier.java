package eus.ixa.ixa.pipe.ml;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;

import eus.ixa.ixa.pipe.ml.document.DocumentClassifierME;
import eus.ixa.ixa.pipe.ml.document.DocumentClassifierModel;

public class StatisticalDocumentClassifier {
    
    private static final ConcurrentHashMap<String, DocumentClassifierModel> docClassifierModels = new ConcurrentHashMap<>();
    private final DocumentClassifierME docClassifier;
    
    public StatisticalDocumentClassifier(final Properties props) {
      String language = props.getProperty("language");
      String inputModel = props.getProperty("model");
      Boolean useModelCache = Boolean.valueOf(props.getProperty("useModelCache", "true"));
      final DocumentClassifierModel docClassModel = loadModel(inputModel, language, useModelCache);
      this.docClassifier = new DocumentClassifierME(docClassModel);
    }
    
    public StatisticalDocumentClassifier(final String inputModel, String language) throws IOException {
        final DocumentClassifierModel docClassModel = loadModel(inputModel, language, true);
        this.docClassifier = new DocumentClassifierME(docClassModel);
    }
    
    /**
     * Classifies the given text, provided in separate tokens.
     * @param text the tokens of text to classify
     * @return the best label found
     */
    public String classify(final String[] document) {
        double[] outcomes = docClassifier.classifyProb(document);
        String category = docClassifier.getBestLabel(outcomes);
        return category;
    }
    
    /**
     * Classifies the given text, provided in separate tokens.
     * @param text the tokens of text to classify
     * @return probabilities per label
     */
    public double[] classifyProb(final String[] document) {
      return docClassifier.classifyProb(document);
    }
    
    /**
     * Get a map of the scores sorted in ascending order together with their associated labels.
     * Many labels can have the same score, hence the Set as value.
     *
     * @param text the input text to classify
     * @return a map with the score as a key. The value is a Set of labels with their score.
     */
    public SortedMap<Double, Set<String>> classifySortedScoreMap(final String[] document) {
      return docClassifier.sortedScoreMap(document);
    }
    
    /**
     * Forgets all adaptive data which was collected during previous calls to one
     * of the find methods. This method is typically called at the end of a
     * document.
     */
    public final void clearFeatureData() {
      this.docClassifier.clearFeatureData();
    }

    private DocumentClassifierModel loadModel(final String lang,
        final String modelName, final Boolean useModelCache) {
      final long lStartTime = new Date().getTime();
      DocumentClassifierModel model = null;
      try {
        if (useModelCache) {
          synchronized (docClassifierModels) {
            if (!docClassifierModels.containsKey(lang + modelName)) {
              model = new DocumentClassifierModel(new FileInputStream(modelName));
              docClassifierModels.put(lang + modelName, model);
            }
          }
        } else {
          model = new DocumentClassifierModel(new FileInputStream(modelName));
        }
      } catch (final IOException e) {
        e.printStackTrace();
      }
      final long lEndTime = new Date().getTime();
      final long difference = lEndTime - lStartTime;
      System.err.println("IXA pipes Document Classifier model loaded in: " + difference
          + " miliseconds ... [DONE]");
      return model;
    }
}
