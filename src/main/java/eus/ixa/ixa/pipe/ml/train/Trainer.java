/*
 *  Copyright 2016 Rodrigo Agerri

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

package eus.ixa.ixa.pipe.ml.train;

import opennlp.tools.util.TrainingParameters;
import opennlp.tools.util.model.BaseModel;

/**
 * /**
 * Trainer based on Apache OpenNLP Machine Learning API. This class creates
 * a feature set based on the features activated in the trainParams.properties
 * files:
 * <ol>
 * <li>Window: specify left and right window lengths.
 * <li>TokenFeatures: tokens as features in a window length.
 * <li>TokenClassFeatures: token shape features in a window length.
 * <li>WordShapeSuperSenseFeatures: token shape features from Ciaramita and Altun (2006).
 * <li>OutcomePriorFeatures: take into account previous outcomes.
 * <li>PreviousMapFeatures: add features based on tokens and previous decisions.
 * <li>SentenceFeatures: add beginning and end of sentence words.
 * <li>PrefixFeatures: first 4 characters in current token.
 * <li>SuffixFeatures: last 4 characters in current token.
 * <li>BigramClassFeatures: bigrams of tokens and token class.
 * <li>TrigramClassFeatures: trigrams of token and token class.
 * <li>FourgramClassFeatures: fourgrams of token and token class.
 * <li>FivegramClassFeatures: fivegrams of token and token class.
 * <li>CharNgramFeatures: character ngram features of current token.
 * <li>DictionaryFeatures: check if current token appears in some gazetteer.
 * <li>ClarkClusterFeatures: use the clustering class of a token as a feature.
 * <li>BrownClusterFeatures: use brown clusters as features for each feature
 * containing a token.
 * <li>Word2VecClusterFeatures: use the word2vec clustering class of a token as
 * a feature.
 * <li>POSTagModelFeatures: use pos tags, pos tag class as features.
 * <li>LemmaModelFeatures: use lemma as features.
 * <li>LemmaDictionaryFeatures: use lemma from a dictionary as features.
 * <li>MFSFeatures: Most Frequent sense feature.
 * <li>SuperSenseFeatures: Ciaramita and Altun (2006) features for super sense tagging.
 * </ol>
 * 
 * This interface defines the feature creation, the training method and the
 * evaluation of the trained model.
 * @author ragerri
 * @version 2014-07-11
 */
public interface Trainer {
 
  /**
   * Generate {@link BaseModel} models.
   * @param params
   *          the training parameters file
   * @return the model
   */
  BaseModel train(TrainingParameters params);

}

