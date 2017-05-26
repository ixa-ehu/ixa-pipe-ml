package eus.ixa.ixa.pipe.ml;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;

public class DocClassifier {
    
    private static final char tabDelimiter = '\t';
    private static final char newLineDelimiter = '\n';
    private static final ConcurrentHashMap<String, DoccatModel> docClassifierModels = new ConcurrentHashMap<>();
    private final DocumentCategorizerME docClassifier;
    
    public DocClassifier(final Properties props) {
      String language = props.getProperty("language");
      String inputModel = props.getProperty("model");
      Boolean useModelCache = Boolean.valueOf(props.getProperty("useModelCache", "true"));
      final DoccatModel docClassModel = loadModel(inputModel, language, useModelCache);
      this.docClassifier = new DocumentCategorizerME(docClassModel);
    }
    
    public DocClassifier(final String inputModel, String language) throws IOException {
        final DoccatModel docClassModel = loadModel(inputModel, language, true);
        this.docClassifier = new DocumentCategorizerME(docClassModel);
    }
    
    public String classify(final String document) {
        double[] outcomes = docClassifier.categorize(document);
        String category = docClassifier.getBestCategory(outcomes);
        return document + tabDelimiter + category + newLineDelimiter;
    }

    private DoccatModel loadModel(final String lang,
        final String modelName, final Boolean useModelCache) {
      final long lStartTime = new Date().getTime();
      DoccatModel model = null;
      try {
        if (useModelCache) {
          synchronized (docClassifierModels) {
            if (!docClassifierModels.containsKey(lang + modelName)) {
              model = new DoccatModel(new FileInputStream(modelName));
              docClassifierModels.put(lang + modelName, model);
            }
          }
        } else {
          model = new DoccatModel(new FileInputStream(modelName));
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
