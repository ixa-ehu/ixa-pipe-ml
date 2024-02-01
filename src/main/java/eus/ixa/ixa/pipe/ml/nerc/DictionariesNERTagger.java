/*
 *Copyright 2014 Rodrigo Agerri

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

package eus.ixa.ixa.pipe.ml.nerc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eus.ixa.ixa.pipe.ml.StatisticalSequenceLabeler;
import eus.ixa.ixa.pipe.ml.resources.Dictionaries;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabel;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelFactory;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerME;
import eus.ixa.ixa.pipe.ml.utils.Span;
import eus.ixa.ixa.pipe.ml.utils.StringUtils;

/**
 * Named Entity Recognition module based on {@link Dictionaries} objects This
 * class provides the following functionalities:
 *
 * <ol>
 * <li>string matching against of a string (typically tokens) against a
 * Dictionary containing names. This function is also used to implement
 * Dictionary based features in the training package.
 * <li>tag: Provided a Dictionaries it tags only the names it matches against it
 * <li>post: This function checks for names in the Dictionary that have not been
 * detected by a {@link StatisticalSequenceLabeler}; it also corrects the Name
 * type for those detected by a {@link StatisticalSequenceLabeler} but also
 * present in a dictionary.
 * </ol>
 *
 * @author ragerri 2014/03/14
 *
 */

public class DictionariesNERTagger {

  /**
   * The name factory to create Name objects.
   */
  private final SequenceLabelFactory nameFactory;
  /**
   * The dictionary to find the names.
   */
  private final Dictionaries dictionaries;
  /**
   * Debugging switch.
   */
  private final boolean debug = false;

  /**
   * Construct a DictionariesNameFinder with a dictionary, a type and a name
   * factory.
   *
   * @param aDictionaries
   *          the dictionaries
   */
  public DictionariesNERTagger(final Dictionaries aDictionaries) {
    this.dictionaries = aDictionaries;
    this.nameFactory = new SequenceLabelFactory();
  }

  /**
   * {@link Dictionaries} based Named Entity Detection and Classification.
   *
   * @param tokens
   *          the tokenized sentence
   * @return a list of detected {@link SequenceLabel} objects
   */
  public final List<SequenceLabel> getNames(final String[] tokens) {

    final Span[] origSpans = nercToSpans(tokens);
    final Span[] neSpans = SequenceLabelerME.dropOverlappingSpans(origSpans);
    return getNamesFromSpans(neSpans, tokens);
  }

  /**
   * Detects Named Entities in a {@link Dictionaries} by NE type ignoring case.
   *
   * @param tokens
   *          the tokenized sentence
   * @return spans of the Named Entities
   */
  public final Span[] nercToSpans(final String[] tokens) {
    final List<Span> neSpans = new ArrayList<>();
    for (final Map<String, String> neDict : this.dictionaries
        .getIgnoreCaseDictionaries()) {
      for (final Map.Entry<String, String> neEntry : neDict.entrySet()) {
        final String neForm = neEntry.getKey();
        final String neType = neEntry.getValue();
        final List<Integer> neIds = StringUtils
            .exactTokenFinderIgnoreCase(neForm, tokens);
        if (!neIds.isEmpty()) {
          for (int i = 0; i < neIds.size(); i += 2) {
            final Span neSpan = new Span(neIds.get(i), neIds.get(i + 1),
                neType);
            neSpans.add(neSpan);
            if (this.debug) {
              System.err.println(neSpans.toString());
            }
          }
        }
      }
    }
    return neSpans.toArray(new Span[neSpans.size()]);
  }

  /**
   * Detects Named Entities in a {@link Dictionaries} by NE type This method is
   * case sensitive.
   *
   * @param tokens
   *          the tokenized sentence
   * @return spans of the Named Entities all
   */
  public final Span[] nercToSpansExact(final String[] tokens) {
    final List<Span> neSpans = new ArrayList<>();
    for (final Map<String, String> neDict : this.dictionaries
        .getDictionaries()) {
      for (final Map.Entry<String, String> neEntry : neDict.entrySet()) {
        final String neForm = neEntry.getKey();
        final String neType = neEntry.getValue();
        final List<Integer> neIds = StringUtils.exactTokenFinder(neForm,
            tokens);
        if (!neIds.isEmpty()) {
          for (int i = 0; i < neIds.size(); i += 2) {
            final Span neSpan = new Span(neIds.get(i), neIds.get(i + 1),
                neType);
            neSpans.add(neSpan);
            if (this.debug) {
              System.err.println(neSpans.toString());
            }
          }
        }
      }
    }
    return neSpans.toArray(new Span[neSpans.size()]);
  }

  /**
   * Creates a list of {@link SequenceLabel} objects from spans and tokens.
   *
   * @param neSpans
   *          the spans of the entities in the sentence
   * @param tokens
   *          the tokenized sentence
   * @return a list of {@link SequenceLabel} objects
   */
  public final List<SequenceLabel> getNamesFromSpans(final Span[] neSpans,
      final String[] tokens) {
    final List<SequenceLabel> names = new ArrayList<>();
    for (final Span neSpan : neSpans) {
      final String nameString = neSpan.getCoveredText(tokens);
      final String neType = neSpan.getType();
      final SequenceLabel name = this.nameFactory.createSequence(nameString,
          neType, neSpan);
      names.add(name);
    }
    return names;
  }

  /**
   * Clear the adaptiveData for each document.
   */
  public void clearAdaptiveData() {
    // nothing to clear
  }

}
