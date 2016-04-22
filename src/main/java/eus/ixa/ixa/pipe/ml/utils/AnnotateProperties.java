package eus.ixa.ixa.pipe.ml.utils;

import java.util.Properties;

/**
 * Properties for annotation. Language and model parameters are compulsory.
 * <ol>
 * <li>Language: ca, de, en, es, eu, fr, gl, it, nl, pt, ru.
 * <li>Model: the model name.
 * </ol>
 * @author ragerri
 * @version 2016-04-22
 */
public class AnnotateProperties {

  /**
   * Choose corpus conventions for normalization of punctuation.
   * See eus.ixa.ixa.pipe.ml.tok.Normalizer class.
   */
  public static final String DEFAULT_NORMALIZE = "default";
  /**
   * Choose between 'yes' and 'no'.
   */
  public static final String DEFAULT_UNTOKENIZABLE_STRING = "no";
  /**
   * Choose between 'yes' and 'no'.
   */
  public static final String DEFAULT_HARD_PARAGRAPH = "no";
  /**
   * Detect multiwords.
   */
  public static final String DEFAULT_MULTIWORDS = "false";
  /**
   * Correct statistical POS tagger output with monosemic dictionary.
   */
  public static final String DEFAULT_DICTAG = "false";
  /**
   * Output all POS and Lemma analysis before disambiguation.
   */
  public static final String DEFAULT_ALL_MORPHOLOGY = "false";
  
  
  /**
   * This class is not to be instantiated.
   */
  private AnnotateProperties() {

  }
  
  /**
   * Creates the Properties object required to construct a Sentence
   * Segmenter and a Tokenizer.
   * @param lang it is required to provide a language code
   * @param normalize the normalization option
   * @param untokenizable print untokenizable tokens
   * @param hardParagraph do not segment paragraph marks
   * @return the properties object
   */
  public static Properties setTokenizeProperties(final String lang, final String normalize, final String untokenizable, final String hardParagraph) {
    final Properties annotateProperties = new Properties();
    annotateProperties.setProperty("language", lang);
    annotateProperties.setProperty("normalize", normalize);
    annotateProperties.setProperty("untokenizable", untokenizable);
    annotateProperties.setProperty("hardParagraph", hardParagraph);
    return annotateProperties;
  }
  
  /**
   * Generate Properties object for POS tagging and Lemmatizing.
   * Language, model and lemmatizerModel are compulsory.
   * @param model the pos tagger model
   * @param lemmatizerModel the lemmatizer model
   * @param language the language
   * @param multiwords whether multiwords are to be detected
   * @param dictag whether tagging from a dictionary is activated
   * @param allMorphology whether to disclose all pos tags and lemmas before disambiguation
   * @return a properties object
   */
  public static Properties setPOSLemmaProperties(final String model, final String lemmatizerModel,
      final String language, final String multiwords, final String dictag, final String allMorphology) {
    final Properties annotateProperties = new Properties();
    annotateProperties.setProperty("model", model);
    annotateProperties.setProperty("lemmatizerModel", lemmatizerModel);
    annotateProperties.setProperty("language", language);
    annotateProperties.setProperty("multiwords", multiwords);
    annotateProperties.setProperty("dictag", dictag);
    annotateProperties.setProperty("allMorphology", allMorphology);
    return annotateProperties;
  }
  
  /**
   * Generate Properties object for chunking. Both parameters are
   * compulsory.
   * @param model
   *          the model to perform the annotation
   * @param language
   *          the language
   * @return a properties object
   */
  public Properties setChunkingProperties(final String model, final String language) {
    final Properties annotateProperties = new Properties();
    annotateProperties.setProperty("model", model);
    annotateProperties.setProperty("language", language);
    return annotateProperties;
  }

}
