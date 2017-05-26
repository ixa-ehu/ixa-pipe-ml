package eus.ixa.ixa.pipe.ml;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import eus.ixa.ixa.pipe.ml.document.DocumentClassifierME;
import eus.ixa.ixa.pipe.ml.document.DocumentClassifierModel;
import eus.ixa.ixa.pipe.ml.utils.StringUtils;

public class StatisticalDocumentClassifier {
    
    private static final char tabDelimiter = '\t';
    private static final char newLineDelimiter = '\n';
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
    
    public String classify(final String[] document) {
        double[] outcomes = docClassifier.classifyProb(document);
        String category = docClassifier.getBestLabel(outcomes);
        return StringUtils.getStringFromTokens(document) + tabDelimiter + category + newLineDelimiter;
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
