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
