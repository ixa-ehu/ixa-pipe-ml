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

import java.util.Arrays;


public class HeadRule {

  private boolean leftToRight;
  private String[] tags;

  public HeadRule(final boolean l2r, final String[] tags) {
    this.setLeftToRight(l2r);

    for (final String tag : tags) {
      if (tag == null) {
        throw new IllegalArgumentException(
            "tags must not contain null values!");
      }
    }

    this.setTags(tags);
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof HeadRule) {
      final HeadRule rule = (HeadRule) obj;

      return rule.isLeftToRight() == this.isLeftToRight()
          && Arrays.equals(rule.getTags(), this.getTags());
    } else {
      return false;
    }
  }

  /**
   * @return the tags
   */
  public String[] getTags() {
    return tags;
  }

  /**
   * @param tags the tags to set
   */
  public void setTags(String[] tags) {
    this.tags = tags;
  }

  /**
   * @return the leftToRight
   */
  public boolean isLeftToRight() {
    return leftToRight;
  }

  /**
   * @param leftToRight the leftToRight to set
   */
  public void setLeftToRight(boolean leftToRight) {
    this.leftToRight = leftToRight;
  }
}
