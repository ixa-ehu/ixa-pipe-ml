package eus.ixa.ixa.pipe.ml.pos;

/**
 * Class to store information about clitic pronouns and how it affects the verb
 * it is attached to.
 *
 * @author iortiz
 * @version 2016-05-12
 *
 */
public class Clitic {

  private String affix;
  private String affixForRoot;
  private String posTagPattern;
  private String originalWordPattern;
  private String retokenizedPattern;

  public Clitic(String affix, String affForRoot, String tagRegExp,
      String retokPattern) {
    this.affix = affix;
    this.affixForRoot = affForRoot;
    this.posTagPattern = tagRegExp;
    String[] splitted = retokPattern.split(":");
    originalWordPattern = splitted[0];
    retokenizedPattern = splitted[1];
  }

  /**
   * Find if a token contains a clitic pronoun.
   *
   * @param word
   *          string of the word
   *
   * @return true if the token contains a clitic pronoun
   *
   */
  public boolean containsClitic(String word) {
    if (word.endsWith(affix)) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Check if a postag matches with the tag pattern to trigger the clitic analysis.
   *
   * @param wordTag
   *          the token
   *
   * @return true if the regular expression matches
   *
   */
  public boolean matchesPosTag(String wordTag) {
    if (wordTag.matches(posTagPattern + ".*")) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Return the components of the original word form pattern.
   *
   * @return the original word components
   *
   */
  public String[] getOrigWordPatternComponents() {
    return this.originalWordPattern.split("\\+");
  }

  /**
   * Removes clitic pronoun(s) from a token.
   *
   * @param word
   *          the string
   *
   * @return the word without the clitic pronouns
   *
   */
  public String removeClitic(String word) {
    String result = word;
    int endInd = result.lastIndexOf(this.affix);
    return result.substring(0, endInd);
  }

  /**
   * Given a word, adds an affix to build the root, if necessary.
   *
   * @param word
   *         the token
   *
   * @return the resulting token
   *
   */
  public String addAffixForRoot(String word) {
    String result = word;
    if (!this.affixForRoot.equals("*")) {
      result = result + this.affixForRoot;
    }
    return result;
  }

  /**
   * Remove diacritics from root token (usually a verb).
   *
   * @param word
   *         the token
   *
   * @return the word
   *
   */
  public String removeAccents(String word) {
    char[] temp = word.toCharArray();
    for (int i = 0; i < temp.length; i++) {
      if (temp[i] == 'á')
        temp[i] = 'a';
      else if (temp[i] == 'é')
        temp[i] = 'e';
      else if (temp[i] == 'í')
        temp[i] = 'i';
      else if (temp[i] == 'ó')
        temp[i] = 'o';
      else if (temp[i] == 'ú')
        temp[i] = 'u';
    }
    return new String(temp);
  }
}
