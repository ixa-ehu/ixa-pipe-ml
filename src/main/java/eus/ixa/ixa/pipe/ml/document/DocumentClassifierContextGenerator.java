/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eus.ixa.ixa.pipe.ml.document;

/**
 * Context generator for the Document Classifier.
 */
public interface DocumentClassifierContextGenerator {

  /**
   * Adds a feature generator to this set of feature generators.
   *
   * @param generator
   *          The feature generator to add.
   */
  public void addFeatureGenerator(DocumentFeatureGenerator generator);

  /**
   * Informs all the feature generators for a document classifier that the context of
   * the feature data (typically a document) is no longer valid.
   */
  public void clearFeatureData();
}
