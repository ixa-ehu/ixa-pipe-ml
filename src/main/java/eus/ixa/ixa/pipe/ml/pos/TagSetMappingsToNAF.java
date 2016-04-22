package eus.ixa.ixa.pipe.ml.pos;

/**
 * Mappings from POS tagsets to NAF pos attribute.
 * @author ragerri
 * @version 2016-04-22
 */
public class TagSetMappingsToNAF {
  
  /**
   * This class is not be instantiated.
   */
  private TagSetMappingsToNAF() {
  }
  
  /**
   * Set the term type attribute based on the pos value.
   * 
   * @param postag
   *          the postag
   * @return the type
   */
  public static String setTermTypeNAF(final String postag) {
    if (postag.startsWith("N") || postag.startsWith("V")
        || postag.startsWith("G") || postag.startsWith("A")) {
      return "open";
    } else {
      return "close";
    }
  }
  
  /**
   * Obtain the appropriate tagset according to language and postag.
   * 
   * @param postag
   *          the postag
   * @param lang the language
   * @return the mapped tag
   */
  public static String getNAFTagSet(final String postag, final String lang) {
    
    String tag = null;
    if (lang.equalsIgnoreCase("de")) {
      tag = mapGermanCoNLL09TagSetToNAF(postag);
    } else if (lang.equalsIgnoreCase("en")) {
      tag = mapEnglishPennTagSetToNAF(postag);
    } else if (lang.equalsIgnoreCase("es")) {
      tag = mapSpanishAncoraTagSetToNAF(postag);
    } else if (lang.equalsIgnoreCase("eu")) {
      tag = mapUDTagSetToNAF(postag);
    } else if (lang.equalsIgnoreCase("gl")) {
      tag = mapGalicianCTAGTagSetToNAF(postag);
    } else if (lang.equalsIgnoreCase("fr")) {
      tag = mapFrenchCCTagSetToNAF(postag);
    } else if (lang.equalsIgnoreCase("it")) {
      tag = mapUDTagSetToNAF(postag);
    } else if (lang.equalsIgnoreCase("nl")) {
      tag = mapWotanTagSetToNAF(postag);
    } else {
      tag = "O";
    }
    return tag;
  }
  
  /**
   * Mapping between CoNLL 2009 German tagset and NAF tagset.
   * Based on the Stuttgart-Tuebingen tagset.
   * 
   * @param postag the postag
   * @return NAF POS tag
   */
  private static String mapGermanCoNLL09TagSetToNAF(final String postag) {
    if (postag.startsWith("ADV")) {
      return "A"; // adverb
    } else if (postag.startsWith("KO")) {
      return "C"; // conjunction
    } else if (postag.equalsIgnoreCase("ART")) {
      return "D"; // determiner and predeterminer
    } else if (postag.startsWith("ADJ")) {
      return "G"; // adjective
    } else if (postag.equalsIgnoreCase("NN")) {
      return "N"; // common noun
    } else if (postag.startsWith("NE")) {
      return "R"; // proper noun
    } else if (postag.startsWith("AP")) {
      return "P"; // preposition
    } else if (postag.startsWith("PD") || postag.startsWith("PI") || postag.startsWith("PP") || postag.startsWith("PR")
        || postag.startsWith("PW") || postag.startsWith("PA")) {
      return "Q"; // pronoun
    } else if (postag.startsWith("V")) {
      return "V"; // verb
    } else {
      return "O"; // other
    }
  }
  /**
   * Mapping between Penn Treebank tagset and NAF tagset.
   * 
   * @param postag
   *          treebank postag
   * @return NAF POS tag
   */
  private static String mapEnglishPennTagSetToNAF(final String postag) {
    if (postag.startsWith("RB")) {
      return "A"; // adverb
    } else if (postag.equalsIgnoreCase("CC")) {
      return "C"; // conjunction
    } else if (postag.startsWith("D") || postag.equalsIgnoreCase("PDT")) {
      return "D"; // determiner and predeterminer
    } else if (postag.startsWith("J")) {
      return "G"; // adjective
    } else if (postag.equalsIgnoreCase("NN") || postag.equalsIgnoreCase("NNS")) {
      return "N"; // common noun
    } else if (postag.startsWith("NNP")) {
      return "R"; // proper noun
    } else if (postag.equalsIgnoreCase("TO") || postag.equalsIgnoreCase("IN")) {
      return "P"; // preposition
    } else if (postag.startsWith("PRP") || postag.startsWith("WP")) {
      return "Q"; // pronoun
    } else if (postag.startsWith("V")) {
      return "V"; // verb
    } else {
      return "O"; // other
    }
  }

  /**
   * Mapping between EAGLES PAROLE Ancora tagset and NAF.
   * 
   * @param postag
   *          the postag
   * @return the mapping to NAF pos tagset
   */
  private static String mapSpanishAncoraTagSetToNAF(final String postag) {
    if (postag.equalsIgnoreCase("RG") || postag.equalsIgnoreCase("RN")) {
      return "A"; // adverb
    } else if (postag.equalsIgnoreCase("CC") || postag.equalsIgnoreCase("CS")) {
      return "C"; // conjunction
    } else if (postag.startsWith("D")) {
      return "D"; // det predeterminer
    } else if (postag.startsWith("A")) {
      return "G"; // adjective
    } else if (postag.startsWith("NC")) {
      return "N"; // common noun
    } else if (postag.startsWith("NP")) {
      return "R"; // proper noun
    } else if (postag.startsWith("SP")) {
      return "P"; // preposition
    } else if (postag.startsWith("P")) {
      return "Q"; // pronoun
    } else if (postag.startsWith("V")) {
      return "V"; // verb
    } else {
      return "O"; // other
    }
  }
  
  /**
   * Mapping between Universal POS tags and NAF.
   * 
   * @param postag
   *          the postag
   * @return the mapping to NAF pos tagset
   */
  private static String mapUDTagSetToNAF(final String postag) {
    if (postag.equalsIgnoreCase("ADV")) {
      return "A"; // adverb
    } else if (postag.equalsIgnoreCase("CONJ") || postag.equalsIgnoreCase("SCONJ")) {
      return "C"; // conjunction
    } else if (postag.equalsIgnoreCase("DET")) {
      return "D"; // det predeterminer
    } else if (postag.equalsIgnoreCase("ADJ")) {
      return "G"; // adjective
    } else if (postag.equalsIgnoreCase("NOUN")) {
      return "N"; // common noun
    } else if (postag.equalsIgnoreCase("PROPN")) {
      return "R"; // proper noun
    } else if (postag.equalsIgnoreCase("ADP")) {
      return "P"; // preposition
    } else if (postag.equalsIgnoreCase("PRON")) {
      return "Q"; // pronoun
    } else if (postag.startsWith("V")) {
      return "V"; // verb
    } else {
      return "O"; // other
    }
  }
  
  /**
   * Mapping between CC tagset and NAF.
   * 
   * @param postag
   *          the postag
   * @return the mapping to NAF pos tagset
   */
  private static String mapFrenchCCTagSetToNAF(final String postag) {
    if (postag.startsWith("ADV")) {
      return "A"; // adverb
    } else if (postag.equalsIgnoreCase("CC") || postag.equalsIgnoreCase("CS")) {
      return "C"; // conjunction
    } else if (postag.startsWith("D") || postag.startsWith("I")) {
      return "D"; // det predeterminer
    } else if (postag.startsWith("ADJ")) {
      return "G"; // adjective
    } else if (postag.startsWith("NC")) {
      return "N"; // common noun
    } else if (postag.startsWith("NPP")) {
      return "R"; // proper noun
    } else if (postag.startsWith("PRO") || postag.startsWith("CL")) {
      return "Q"; // pronoun
    } else if (postag.equalsIgnoreCase("P") || postag.equalsIgnoreCase("P+D")
        || postag.equalsIgnoreCase("P+PRO")) {
      return "P"; // preposition
    } else if (postag.startsWith("V")) {
      return "V"; // verb
    } else {
      return "O"; // other
    }
  }

  /**
   * Mapping between CTAG tagset and NAF.
   * 
   * @param postag
   *          the postag
   * @return the mapping to NAF pos tagset
   */
  private static String mapGalicianCTAGTagSetToNAF(final String postag) {
    if (postag.startsWith("R")) {
      return "A"; // adverb
    } else if (postag.equalsIgnoreCase("CC") || postag.equalsIgnoreCase("CS")) {
      return "C"; // conjunction
    } else if (postag.startsWith("D") || postag.startsWith("G")
        || postag.startsWith("X") || postag.startsWith("Q")
        || postag.startsWith("T") || postag.startsWith("I")
        || postag.startsWith("M")) {
      return "D"; // det predeterminer
    } else if (postag.startsWith("A")) {
      return "G"; // adjective
    } else if (postag.startsWith("NC")) {
      return "N"; // common noun
    } else if (postag.startsWith("NP")) {
      return "R"; // proper noun
    } else if (postag.startsWith("S")) {
      return "P"; // preposition9434233199310741
    } else if (postag.startsWith("P")) {
      return "Q"; // pronoun
    } else if (postag.startsWith("V")) {
      return "V"; // verb
    } else {
      return "O"; // other
    }
  }
  
  /**
   * Mapping between Wotan (Alpino) tagset and NAF.
   * 
   * @param postag
   *          the postag
   * @return the mapping to NAF pos tagset
   */
  private static String mapWotanTagSetToNAF(final String postag) {
    if (postag.equalsIgnoreCase("ADV")) {
      return "A"; // adverb
    } else if (postag.equalsIgnoreCase("CONJ") || postag.equalsIgnoreCase("SCONJ")) {
      return "C"; // conjunction
    } else if (postag.equalsIgnoreCase("Art")) {
      return "D"; // det predeterminer
    } else if (postag.equalsIgnoreCase("ADJ")) {
      return "G"; // adjective
    } else if (postag.equalsIgnoreCase("N")) {
      return "N"; // common noun
    } else if (postag.equalsIgnoreCase("PROPN")) {
      return "R"; // proper noun
    } else if (postag.equalsIgnoreCase("Prep")) {
      return "P"; // preposition
    } else if (postag.equalsIgnoreCase("PRON")) {
      return "Q"; // pronoun
    } else if (postag.startsWith("V")) {
      return "V"; // verb
    } else {
      return "O"; // other
    }
  }
}
