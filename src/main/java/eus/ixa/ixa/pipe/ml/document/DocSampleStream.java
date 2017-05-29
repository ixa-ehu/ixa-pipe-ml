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

import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import eus.ixa.ixa.pipe.ml.utils.StringUtils;
import opennlp.tools.tokenize.WhitespaceTokenizer;
import opennlp.tools.util.FilterObjectStream;
import opennlp.tools.util.InputStreamFactory;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.StringUtil;

/**
 * This class reads in string encoded training samples, parses them and
 * outputs {@link DocSample} objects.
 * <p>
 * Format:<br>
 * Each line contains one sample document.<br>
 * The category is the first string in the line followed by a tab and whitespace
 * separated document tokens.<br>
 * Sample line: category-string tab-char whitespace-separated-tokens line-break-char(s)<br>
 */
public class DocSampleStream implements ObjectStream<DocSample> {
  
  /**
   * The stream.
   */
  private final ObjectStream<String> docStream;
  /**
   * Whether the features are to be reset or not.
   */
  private final String clearFeatures;


  public DocSampleStream(String clearFeatures, ObjectStream<String> samples) {
    this.clearFeatures = clearFeatures;
    this.docStream = samples;
  }
  
  public DocSampleStream(String clearFeatures, InputStreamFactory in) throws IOException {
    this.clearFeatures = clearFeatures;
    try {
      this.docStream = new PlainTextByLineStream(in, "UTF-8");
      System.setOut(new PrintStream(System.out, true, "UTF-8"));
    } catch (final UnsupportedEncodingException e) {
      // UTF-8 is available on all JVMs, will never happen
      throw new IllegalStateException(e);
    }
  }

  public DocSample read() throws IOException {
    
    List<String> tokens = new ArrayList<>();
    String label = null;
    boolean isClearAdaptiveData = false;
    //empty line indicates new document
    String doc;
    while ((doc = this.docStream.read()) != null
        && !StringUtil.isEmpty(doc)) {
      // clear adaptive data if document mark appears following
      if (this.clearFeatures.equalsIgnoreCase("docstart")
          && doc.startsWith("-DOCSTART-")) {
        isClearAdaptiveData = true;
        final String emptyLine = this.docStream.read();
        if (!StringUtil.isEmpty(emptyLine)) {
          throw new IOException(
              "Empty line after -DOCSTART- not empty: '" + emptyLine + "'!");
        }
        continue;
      }
      final String[] fields = doc.split("\t");
      if (fields.length == 2) {
        tokens.add(e)
        
      } else {
        throw new IOException(
            "Expected two fields per doc in training data, got "
                + tokens.length + " for doc '" + doc + "'!");
      }
    }
    // check if we need to clear features every sentence
    if (this.clearFeatures.equalsIgnoreCase("yes")) {
      isClearAdaptiveData = true;
    }
    if ()
  }
}
