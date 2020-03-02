package eus.ixa.ixa.pipe.ml.pos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eus.ixa.ixa.pipe.ml.lemma.MorfologikLemmatizer;
import eus.ixa.ixa.pipe.ml.utils.IOUtils;

public class CliticSplitter {

  private List<Clitic> cliticPatternDict;
  private Map<String, String> pronounsDict;

  public CliticSplitter(final InputStream cliticPatternDictStream,
      final InputStream pronounsDictStream) {
    cliticPatternDict = readCliticPatternDict(cliticPatternDictStream);
    pronounsDict = IOUtils.readDictIntoMap(pronounsDictStream);
  }

  private static List<Clitic> readCliticPatternDict(InputStream dictionary) {
    List<Clitic> cliticPatterns = new ArrayList<>();
    final BufferedReader breader = new BufferedReader(
        new InputStreamReader(dictionary));
    String line;
    try {
      while ((line = breader.readLine()) != null) {
        if (!line.isEmpty() && !line.startsWith("#")) {
          final String[] elems = line.split("\t");
          Clitic tmp = new Clitic(elems[0], elems[1], elems[2], elems[3]);
          cliticPatterns.add(tmp);
        }
      }
    } catch (final IOException e) {
      e.printStackTrace();
    }
    return cliticPatterns;
  }

  /**
   * Takes word, posTag and lemma as input and finds if any clitic pronouns are
   * in verbs.
   * 
   * @param word
   *          the original word possibly containing clitic pronoun
   * @param posTag
   *          the postag
   * @param lemma
   *          the lemma
   * @param lemmatizer
   *          the lemmatizer
   * @return the new pos and lemma after clitic splitting
   */
  public List<String> tagClitics(String word, String posTag, String lemma,
      MorfologikLemmatizer lemmatizer) {
    List<String> newCliticElems = new ArrayList<>();
    Clitic match = cliticMatcher(word, posTag);
    if (match != null) {
      addCliticComponents(word, posTag, lemmatizer, match, newCliticElems);
    }
    return newCliticElems;
  }

  private Clitic cliticMatcher(String word, String posTag) {
    /*
     * We can't just exit from the loop as soon as we find a clitic that matches
     * the verb. We must check every clitic, as some of them are repeated and in
     * case the is more than one occurrence of one, we should take the latest
     * match. In case we check the clitic of the word "vete", there are two
     * cases that matches it: - te * ^V.[GNM] 1 $$+te:$$+PP (line 23) - te *
     * ^VMM02S0 0 $$+te:$$+PP (line 91) As the second match is more specific, we
     * take that.
     */
    Clitic match = null;
    for (Clitic cliticElem : cliticPatternDict) {
      if (cliticElem.containsClitic(word) && cliticElem.matchesPosTag(posTag)) {
        match = cliticElem;
      }
    }
    return match;
  }

  private void addCliticComponents(String word, String posTag,
      MorfologikLemmatizer lemmatizer, Clitic clitic,
      List<String> newCliticComponents) {
    String[] origWordComponents = clitic.getOrigWordPatternComponents();
    for (String origWordComponent : origWordComponents) {
      if (origWordComponent.equals("$$")) {
        String newWord = rebuildRoot(word, clitic);
        String newLemma = lemmatizer.apply(newWord, posTag);
        newCliticComponents.add(posTag);
        newCliticComponents.add(newLemma);
      } else {
        String newTag = pronounsDict.get(origWordComponent);
        newCliticComponents.add(origWordComponent);
        newCliticComponents.add(newTag);
      }
    }
  }

  private String rebuildRoot(String origWord, Clitic clitic) {
    // Remove affix
    String result = clitic.removeClitic(origWord);
    // In case of need to build the resulting with an affix
    result = clitic.addAffixForRoot(result);
    // In case the enclitic affix has added acccents
    result = clitic.removeAccents(result);
    return result;
  }

}
