/*
 * Copyright 2015 Rodrigo Agerri

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

package eus.ixa.ixa.pipe.ml.tok;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eus.ixa.ixa.pipe.ml.utils.StringUtils;

/**
 * This class implements exceptions for periods as sentence breakers and tokens.
 * It decides when a period induces a new sentence or a new token and when it
 * does not.
 *
 * @author ragerri
 * @version 2015-04-04
 */
public class NonPeriodBreaker {

  /**
   * Non segmented words, candidates for sentence breaking.
   */
  public static Pattern nonSegmentedWords = Pattern.compile(
      "([\\p{Alnum}.\\-]*)(" + RuleBasedSegmenter.FINAL_PUNCT + "*)(\\.+)$",
      Pattern.UNICODE_CHARACTER_CLASS);
  /**
   * Next word wrt to the candidate to indicate sentence breaker.
   */
  public static Pattern nextCandidateWord = Pattern.compile("([ ]*"
      + RuleBasedSegmenter.INITIAL_PUNCT + "*[ ]*[\\p{Lu}\\p{Digit}])",
      Pattern.UNICODE_CHARACTER_CLASS);
  /**
   * Do not split dot after these words if followed by number.
   */
  public static String NON_BREAKER_DIGITS = "(al|[Aa]rt|ca|figs?|[Nn]os?|[Nn]rs?|op|p|pp|[Pp]ág)";
  /**
   * General acronyms.
   */
  public static Pattern acronym = Pattern.compile("(\\.)[\\p{Lu}\\-]+([.]+)$",
      Pattern.UNICODE_CHARACTER_CLASS);
  /**
   * Do not segment numbers like 11.1.
   */
  public static Pattern numbers = Pattern.compile(
      "(\\p{Digit}+[.])[ ]*(\\p{Digit}+)", Pattern.UNICODE_CHARACTER_CLASS);
  /**
   * Any non white space followed by a period.
   */
  public static Pattern wordDot = Pattern.compile("^(\\S+)\\.$");
  /**
   * Any alphabetic character.
   */
  public static Pattern alphabetic = Pattern.compile("\\p{Alpha}",
      Pattern.UNICODE_CHARACTER_CLASS);
  /**
   * Starts with a lowercase.
   */
  public static Pattern startLower = Pattern.compile("^\\p{Lower}+",
      Pattern.UNICODE_CHARACTER_CLASS);
  /**
   * Starts with punctuation that is not beginning of sentence marker.
   */
  public static Pattern startPunct = Pattern
      .compile("^[!#$%&()*+,-/:;=>?@\\[\\\\\\]^{|}~]");
  /**
   * Starts with a digit.
   */
  public static Pattern startDigit = Pattern.compile("^\\p{Digit}+",
      Pattern.UNICODE_CHARACTER_CLASS);
  /**
   * Non breaker prefix read from the files in resources.
   */
  private String NON_BREAKER = null;

  /**
   *
   * This constructor reads some non breaking prefixes files in resources to
   * create exceptions of segmentation and tokenization.
   *
   * @param properties
   *          the options
   */
  public NonPeriodBreaker(final Properties properties) {
    loadNonBreaker(properties);
  }

  private void loadNonBreaker(final Properties properties) {
    final String lang = properties.getProperty("language");
    if (this.NON_BREAKER == null) {
      createNonBreaker(lang);
    }
  }

  private void createNonBreaker(final String lang) {
    final List<String> nonBreakerList = new ArrayList<>();

    final InputStream nonBreakerInputStream = getNonBreakerInputStream(lang);
    if (nonBreakerInputStream == null) {
      System.err.println("WARNING: No exceptions file for language " + lang
          + " in ixa-pipe-ml/src/main/resources/tokenizer/!!");
      System.exit(1);
    }
    final BufferedReader breader = new BufferedReader(
        new InputStreamReader(nonBreakerInputStream));
    String line;
    try {
      while ((line = breader.readLine()) != null) {
        line = line.trim();
        if (!line.startsWith("#")) {
          nonBreakerList.add(line);
        }
      }
    } catch (final IOException e) {
      e.printStackTrace();
    }
    this.NON_BREAKER = StringUtils.createDisjunctRegexFromList(nonBreakerList);
  }

  private InputStream getNonBreakerInputStream(final String lang) {
    InputStream nonBreakerInputStream = null;
    if (lang.equalsIgnoreCase("ca")) {
      nonBreakerInputStream = getClass()
          .getResourceAsStream("/tokenizer/ca-exceptions.txt");
    } else if (lang.equalsIgnoreCase("de")) {
      nonBreakerInputStream = getClass()
          .getResourceAsStream("/tokenizer/de-exceptions.txt");
    } else if (lang.equalsIgnoreCase("en")) {
      nonBreakerInputStream = getClass()
          .getResourceAsStream("/tokenizer/en-exceptions.txt");
    } else if (lang.equalsIgnoreCase("es")) {
      nonBreakerInputStream = getClass()
          .getResourceAsStream("/tokenizer/es-exceptions.txt");
    } else if (lang.equalsIgnoreCase("eu")) {
      nonBreakerInputStream = getClass()
          .getResourceAsStream("/tokenizer/eu-exceptions.txt");
    } else if (lang.equalsIgnoreCase("fr")) {
      nonBreakerInputStream = getClass()
          .getResourceAsStream("/tokenizer/fr-exceptions.txt");
    } else if (lang.equalsIgnoreCase("gl")) {
      nonBreakerInputStream = getClass()
          .getResourceAsStream("/tokenizer/gl-exceptions.txt");
    } else if (lang.equalsIgnoreCase("it")) {
      nonBreakerInputStream = getClass()
          .getResourceAsStream("/tokenizer/it-exceptions.txt");
    } else if (lang.equalsIgnoreCase("nl")) {
      nonBreakerInputStream = getClass()
          .getResourceAsStream("/tokenizer/nl-exceptions.txt");
    } else if (lang.equalsIgnoreCase("pt")) {
      nonBreakerInputStream = getClass()
          .getResourceAsStream("/tokenizer/pt-exceptions.txt");
    } else if (lang.equalsIgnoreCase("ru")) {
      nonBreakerInputStream = getClass()
          .getResourceAsStream("/tokenizer/ru-exceptions.txt");
    }
    return nonBreakerInputStream;
  }

  /**
   * Segment the rest of the text taking into account some exceptions for
   * periods as sentence breakers. It decides when a period marks an end of
   * sentence.
   *
   * @param lines
   *          the segmented sentences so far
   * @return all the segmented sentences
   */
  public String[] segmenterExceptions(final String[] lines) {
    final List<String> sentences = new ArrayList<>();
    for (final String line : lines) {
      final String segmentedLine = segmenterNonBreaker(line);
      final String[] lineSentences = segmentedLine.split("\n");
      Collections.addAll(sentences, lineSentences);
    }
    return sentences.toArray(new String[0]);
  }

  /**
   * This function implements exceptions for periods as sentence breakers. It
   * decides when a period induces a new sentence or not.
   *
   * @param line
   *          the text to be processed
   * @return segmented text (with newlines included)
   */
  private String segmenterNonBreaker(String line) {

    // these are fine because they do not affect offsets
    line = line.trim();
    line = RuleBasedTokenizer.doubleSpaces.matcher(line).replaceAll(" ");
    final StringBuilder sb = new StringBuilder();
    String segmentedText = "";
    int i;
    final String[] words = line.split(" ");
    // iterate over the words
    for (i = 0; i < words.length - 1; i++) {
      final Matcher nonSegmentedWordMatcher = nonSegmentedWords
          .matcher(words[i]);
      // candidate word to be segmented found:
      if (nonSegmentedWordMatcher.find()) {
        final String curWord = nonSegmentedWordMatcher.replaceAll("$1");
        final String finalPunct = nonSegmentedWordMatcher.replaceAll("$2");
        if (!curWord.isEmpty() && curWord.matches("(" + this.NON_BREAKER + ")")
            && finalPunct.isEmpty()) {
          // if current word is not empty and is a no breaker and there is not
          // final punctuation
        } else if (acronym.matcher(words[i]).find()) {
          // if acronym
        } else if (nextCandidateWord.matcher(words[i + 1]).find()) {
          // if next word contains initial punctuation and then uppercase or
          // digit do:
          if (!(!curWord.isEmpty() && curWord.matches(NON_BREAKER_DIGITS)
              && finalPunct.isEmpty()
              && startDigit.matcher(words[i + 1]).find())) {
            // segment unless current word is a non breaker digit and next word
            // is not final punctuation or does not start with a number
            words[i] = words[i] + "\n";
          }
        }
      }
      sb.append(words[i]).append(" ");
      segmentedText = sb.toString();
    }
    // add last index of words array removed for easy look ahead
    segmentedText = segmentedText + words[i];
    return segmentedText;
  }

  /**
   * It decides when periods do not need to be tokenized.
   *
   * @param line
   *          the sentence to be tokenized
   * @return line
   */
  public String TokenizerNonBreaker(String line) {

    // these are fine because they do not affect offsets
    line = line.trim();
    line = RuleBasedTokenizer.doubleSpaces.matcher(line).replaceAll(" ");
    final StringBuilder sb = new StringBuilder();
    String tokenizedText = "";
    int i;
    final String[] words = line.split(" ");

    for (i = 0; i < words.length; i++) {
      final Matcher wordDotMatcher = wordDot.matcher(words[i]);

      // find anything non-whitespace finishing with a period
      if (wordDotMatcher.find()) {
        final String curWord = wordDotMatcher.replaceAll("$1");

        if (curWord.contains(".") && alphabetic.matcher(curWord).find()
            || curWord.matches("(" + this.NON_BREAKER + ")")
            || i < words.length - 1 && (startLower.matcher(words[i + 1]).find()
                || startPunct.matcher(words[i + 1]).find())) {
          // do not tokenize if (word contains a period and is alphabetic) OR
          // word is a non breaker OR (word is a non breaker and next is
          // (lowercase or starts with punctuation that is end of sentence
          // marker))
        } else if (curWord.matches(NON_BREAKER_DIGITS) && i < words.length - 1
            && startDigit.matcher(words[i + 1]).find()) {
          // do not tokenize if word is a nonbreaker digit AND next word starts
          // with a digit
        } else {
          words[i] = curWord + " .";
        }
      }
      sb.append(words[i]).append(" ");
      tokenizedText = sb.toString();
    }
    return tokenizedText;
  }

}
