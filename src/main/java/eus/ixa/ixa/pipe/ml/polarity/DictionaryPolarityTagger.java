package eus.ixa.ixa.pipe.ml.polarity;

import java.io.IOException;
import java.io.InputStream;

import eus.ixa.ixa.pipe.ml.resources.Dictionary;

public class DictionaryPolarityTagger {
  
  private final Dictionary polarityTagger;
  
  public DictionaryPolarityTagger(InputStream in) throws IOException {
    this.polarityTagger = new Dictionary(in);
  }
  
  public String tag(String gazEntry) {
    String polarityClass = null;
    String gazResult = polarityTagger.lookup(gazEntry);
    if (gazResult != null) {
      polarityClass = gazResult;
    } else {
      polarityClass = "O";
    }
    return polarityClass;
  }

}
