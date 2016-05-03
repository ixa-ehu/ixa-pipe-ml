package eus.ixa.ixa.pipe.ml.sequence;

import opennlp.tools.util.BeamSearchContextGenerator;
import opennlp.tools.util.featuregen.AdaptiveFeatureGenerator;

/**
 * Interface for generating the context for a sequence labeler by specifying a
 * set of feature generators.
 * 
 */
public interface SequenceLabelerContextGenerator extends
    BeamSearchContextGenerator<String> {

  /**
   * Adds a feature generator to this set of feature generators.
   * 
   * @param generator
   *          The feature generator to add.
   */
  public void addFeatureGenerator(AdaptiveFeatureGenerator generator);

  /**
   * Informs all the feature generators for a sequence labeler that the
   * specified tokens have been classified with the corresponding set of
   * specified outcomes.
   * 
   * @param tokens
   *          The tokens of the sentence or other text unit which has been
   *          processed.
   * @param outcomes
   *          The outcomes associated with the specified tokens.
   */
  public void updateAdaptiveData(String[] tokens, String[] outcomes);

  /**
   * Informs all the feature generators for a name finder that the context of
   * the adaptive data (typically a document) is no longer valid.
   */
  public void clearAdaptiveData();

}
