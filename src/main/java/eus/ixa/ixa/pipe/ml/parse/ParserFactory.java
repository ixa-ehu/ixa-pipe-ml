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

package eus.ixa.ixa.pipe.ml.parse;

import java.util.Map;

import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.util.BaseToolFactory;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.ext.ExtensionLoader;

public class ParserFactory extends BaseToolFactory {
  
  private static final String AUTO_DICTIONARY_ENTRY_NAME = "autodict";
  private Map<String, Object> resources;
  private Dictionary autoDict;
  
  public ParserFactory() {
  }
  
  public ParserFactory(Dictionary autoDict, Map<String, Object> resources) {
    this.init(autoDict, resources);
  }
  
  private void init(Dictionary autoDict, Map<String, Object> resources) {
    this.autoDict = autoDict;
    this.resources = resources;
  }
  
  public Dictionary getDictionary() {
    if(this.autoDict == null && artifactProvider != null)
      this.autoDict = artifactProvider.getArtifact(AUTO_DICTIONARY_ENTRY_NAME);
    return this.autoDict;
  }
  
  public void setDictionary(Dictionary autoDict) {
    if (artifactProvider != null) {
      throw new IllegalStateException(
          "Can not set ngram dictionary while using artifact provider.");
    }
    this.autoDict = autoDict;
  }
  
  protected Map<String, Object> getResources() {
    return resources;
  }

  public static ParserFactory create(String subclassName, Dictionary autoDict,
      final Map<String, Object> resources)
      throws InvalidFormatException {
    if (subclassName == null) {
      // will create the default factory
      return new ParserFactory();
    }
    try {
      ParserFactory theFactory = ExtensionLoader.instantiateExtension(
          ParserFactory.class, subclassName);
      theFactory.init(autoDict, resources);
      return theFactory;
    } catch (Exception e) {
      String msg = "Could not instantiate the " + subclassName
          + ". The initialization throw an exception.";
      System.err.println(msg);
      e.printStackTrace();
      throw new InvalidFormatException(msg, e);
    }
  }
  
  public BuildContextGenerator createBuildContextGenerator() {
    return new BuildContextGenerator(getDictionary(), getResources());
  }
  
  public CheckContextGenerator createCheckContextGenerator() {
    return new CheckContextGenerator(getResources());
  }
  
  @Override
  public Map<String, Object> createArtifactMap() {
    Map<String, Object> artifactMap = super.createArtifactMap();

    if (autoDict != null)
      artifactMap.put(AUTO_DICTIONARY_ENTRY_NAME, autoDict);

    return artifactMap;
  }
  
  @Override
  public void validateArtifactMap() throws InvalidFormatException {
    Object ngramDictEntry = this.artifactProvider
        .getArtifact(AUTO_DICTIONARY_ENTRY_NAME);

    if (ngramDictEntry != null && !(ngramDictEntry instanceof Dictionary)) {
      throw new InvalidFormatException("NGram dictionary has wrong type!");
    }
  }

  
}

