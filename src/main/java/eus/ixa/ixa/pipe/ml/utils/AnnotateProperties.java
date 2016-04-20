package eus.ixa.ixa.pipe.ml.utils;

import java.util.Properties;

public class AnnotateProperties {

  /**
   * Choose language: ca, de, en, es, eu, fr, gl, it, nl, pt, ru.
   */
  public static final String DEFAULT_LANGUAGE = null;
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
  
  
  private AnnotateProperties() {

  }
  
  /**
   * Creates the Properties object required to construct a Sentence
   * Segmenter and a Tokenizer.
   * @param lang the language
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

}
