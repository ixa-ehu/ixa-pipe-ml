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

package eus.ixa.ixa.pipe.ml.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.google.common.collect.ListMultimap;

import opennlp.tools.cmdline.CmdLineUtil;
import opennlp.tools.cmdline.TerminateToolException;
import opennlp.tools.ml.TrainerFactory;
import opennlp.tools.util.InputStreamFactory;
import opennlp.tools.util.MarkableFileInputStreamFactory;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.TrainingParameters;

/**
 * Utility functions to read and save ObjectStreams.
 * 
 * @author ragerri
 * @version 2016-07-13
 */
public final class IOUtils {

  public static final int BUFFER_SIZE = 65536;
  public static final String SPACE_DELIMITER = " ";
  public static final String TAB_DELIMITER = "\t";

  /**
   * Private constructor. This class should only be used statically.
   */
  private IOUtils() {
  }

  /**
   * Remove punctuation.
   * 
   * @param resource
   *          the dictionary path
   * @return the dictionary path without punctuation
   */
  public static final String normalizeLexiconName(String resource) {
    resource = resource.replaceAll("\\p{P}", "");
    return resource;
  }

  /**
   * Get an input stream from a resource name. This could be either an absolute
   * path pointing to a resource in the classpath or a file or a directory in
   * the file system. If found in the classpath that will be loaded first.
   *
   * @param resource
   *          the name of the resource (absolute path with no starting /)
   * @return the inputstream of the dictionary
   */
  public static final InputStream getDictionaryResource(final String resource) {

    InputStream dictInputStream;
    final Path resourcePath = Paths.get(resource);
    final String normalizedPath = resourcePath.toString();
    dictInputStream = getStreamFromClassPath(normalizedPath);
    if (dictInputStream == null) {
      try {
        dictInputStream = new FileInputStream(normalizedPath);
      } catch (final FileNotFoundException e) {
        e.printStackTrace();
      }
    }
    return new BufferedInputStream(dictInputStream);
  }

  /**
   * Load a resource from the classpath.
   *
   * @param normalizedPath
   *          the path normalized using {@code Paths} functions.
   * @return the input stream of the resource
   */
  private static InputStream getStreamFromClassPath(
      final String normalizedPath) {
    InputStream dictInputStream = null;
    final String[] dictPaths = normalizedPath.split("src/main/resources");
    if (dictPaths.length == 2) {
      dictInputStream = IOUtils.class.getClassLoader()
          .getResourceAsStream(dictPaths[1]);
    } else {
      final String[] windowsPaths = normalizedPath
          .split("src\\\\main\\\\resources\\\\");
      if (windowsPaths.length == 2) {
        dictInputStream = IOUtils.class.getClassLoader()
            .getResourceAsStream(windowsPaths[1]);
      }
    }
    return dictInputStream;
  }

  /**
   * Check input file integrity.
   * 
   * @param name
   *          the name of the file
   * @param inFile
   *          the file
   */
  private static void checkInputFile(final String name, final File inFile) {

    String isFailure = null;

    if (inFile.isDirectory()) {
      isFailure = "The " + name + " file is a directory!";
    } else if (!inFile.exists()) {
      isFailure = "The " + name + " file does not exist!";
    } else if (!inFile.canRead()) {
      isFailure = "No permissions to read the " + name + " file!";
    }

    if (null != isFailure) {
      throw new TerminateToolException(-1,
          isFailure + " Path: " + inFile.getAbsolutePath());
    }
  }

  /**
   * Load the parameters in the {@code TrainingParameters} file.
   * 
   * @param paramFile
   *          the training parameters file
   * @return default loading of the parameters
   */
  public static TrainingParameters loadTrainingParameters(
      final String paramFile) {
    return loadTrainingParameters(paramFile, false);
  }

  /**
   * Load the parameters in the {@code TrainingParameters} file.
   *
   * @param paramFile
   *          the parameter file
   * @param supportSequenceTraining
   *          wheter sequence training is supported
   * @return the parameters
   */
  private static TrainingParameters loadTrainingParameters(
      final String paramFile, final boolean supportSequenceTraining) {

    TrainingParameters params = null;

    if (paramFile != null) {

      checkInputFile("Training Parameter", new File(paramFile));

      InputStream paramsIn = null;
      try {
        paramsIn = new FileInputStream(new File(paramFile));

        params = new opennlp.tools.util.TrainingParameters(paramsIn);
      } catch (final IOException e) {
        throw new TerminateToolException(-1,
            "Error during parameters loading: " + e.getMessage(), e);
      } finally {
        try {
          if (paramsIn != null) {
            paramsIn.close();
          }
        } catch (final IOException e) {
          System.err.println("Error closing the input stream");
        }
      }

      if (!TrainerFactory.isValid(params.getSettings())) {
        throw new TerminateToolException(1,
            "Training parameters file '" + paramFile + "' is invalid!");
      }
    }

    return params;
  }

  /**
   * Read the file into an {@code ObjectStream}.
   *
   * @param infile
   *          the string pointing to the file
   * @return the object stream
   */
  public static ObjectStream<String> readFileIntoMarkableStreamFactory(
      final String infile) {

    InputStreamFactory inputStreamFactory = null;
    try {
      inputStreamFactory = new MarkableFileInputStreamFactory(new File(infile));
    } catch (final FileNotFoundException e) {
      e.printStackTrace();
    }
    ObjectStream<String> lineStream = null;
    try {
      lineStream = new PlainTextByLineStream(inputStreamFactory, "UTF-8");
    } catch (final IOException e) {
      CmdLineUtil.handleCreateObjectStreamError(e);
    }
    return lineStream;
  }

  public static <K, V> File writeClusterToFile(
      final Map<K, V> tokenToClusterMap, final String fileName,
      final String delimiter) throws IOException {
    final File outFile = new File(fileName);
    OutputStream outputStream = new FileOutputStream(outFile);
    if (fileName.endsWith(".gz")) {
      outputStream = new GZIPOutputStream(outputStream);
    }
    final Writer writer = new BufferedWriter(
        new OutputStreamWriter(outputStream, "UTF-8"));

    for (final Entry<K, V> entry : tokenToClusterMap.entrySet()) {
      writer.write(entry.getKey() + delimiter + entry.getValue() + "\n");
    }
    writer.close();
    return outFile;
  }

  public static <K, V> File writeMultimapToFile(
      final ListMultimap<K, V> tokenToClusterMap, final String fileName,
      final String delimiter) throws IOException {
    final File outFile = new File(fileName);
    OutputStream outputStream = new FileOutputStream(outFile);
    if (fileName.endsWith(".gz")) {
      outputStream = new GZIPOutputStream(outputStream);
    }
    final Writer writer = new BufferedWriter(
        new OutputStreamWriter(outputStream, "UTF-8"));

    for (final Map.Entry<K, V> entry : tokenToClusterMap.entries()) {
      writer.write(entry.getKey() + delimiter + entry.getValue() + "\n");
    }
    writer.close();
    return outFile;
  }

  public static File writeDictionaryLemmatizerToFile(
      final Map<List<String>, String> dictMap, final String fileName,
      final String delimiter) throws IOException {
    final File outFile = new File(fileName);
    OutputStream out = new FileOutputStream(outFile);
    final Writer writer = new BufferedWriter(new OutputStreamWriter(out));
    if (fileName.endsWith(".gz")) {
      out = new GZIPOutputStream(out);
    }
    for (final Map.Entry<List<String>, String> entry : dictMap.entrySet()) {
      writer.write(entry.getKey().get(0) + delimiter + entry.getKey().get(1)
          + delimiter + entry.getValue() + "\n");
    }
    writer.close();
    return outFile;
  }

  /**
   * Serialize java object to a file.
   * 
   * @param o
   *          the java object
   * @param fileName
   *          the name of the file
   * @return the file
   * @throws IOException
   *           if io problems
   */
  public static File writeObjectToFile(final Object o, final String fileName)
      throws IOException {
    final File outFile = new File(fileName);
    OutputStream outputStream = new FileOutputStream(outFile);
    if (fileName.endsWith(".gz")) {
      outputStream = new GZIPOutputStream(outputStream);
    }
    outputStream = new BufferedOutputStream(outputStream);
    final ObjectOutputStream oos = new ObjectOutputStream(outputStream);
    oos.writeObject(o);
    oos.close();
    return outFile;
  }

  /**
   * Serialized gzipped java object to an ObjectOutputStream. The stream remains
   * open.
   * 
   * @param o
   *          the java object
   * @param out
   *          the output stream
   */
  public static void writeGzipObjectToStream(final Object o, OutputStream out) {
    out = new BufferedOutputStream(out);
    try {
      out = new GZIPOutputStream(out, true);
      final ObjectOutputStream oos = new ObjectOutputStream(out);
      oos.writeObject(o);
      oos.flush();
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Serialize java object to an ObjectOutputStream. The stream remains open.
   * 
   * @param o
   *          the java object
   * @param out
   *          the output stream
   */
  public static void writeObjectToStream(final Object o, OutputStream out) {
    out = new BufferedOutputStream(out);
    try {
      final ObjectOutputStream oos = new ObjectOutputStream(out);
      oos.writeObject(o);
      oos.flush();
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Open file to an input stream.
   * 
   * @param file
   *          the file
   * @return the input stream
   */
  public static InputStream openFromFile(final File file) {
    try {
      InputStream is = new BufferedInputStream(new FileInputStream(file),
          BUFFER_SIZE);
      if (file.getName().endsWith(".gz") || file.getName().endsWith("gz")) {
        is = new GZIPInputStream(is, BUFFER_SIZE);
      }
      return is;
    } catch (final IOException e) {
      throw new TerminateToolException(-1,
          "File '" + file + "' cannot be found", e);
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> T readObjectFromInputStream(InputStream is)
      throws IOException, ClassNotFoundException {
    is = new BufferedInputStream(is, BUFFER_SIZE);
    final ObjectInputStream ois = new ObjectInputStream(is);
    final Object readObject = ois.readObject();
    return (T) readObject;
  }

  @SuppressWarnings("unchecked")
  public static <T> T readGzipObjectFromInputStream(InputStream is)
      throws IOException, ClassNotFoundException {
    is = new BufferedInputStream(is, BUFFER_SIZE);
    final GZIPInputStream zis = new GZIPInputStream(is, BUFFER_SIZE);
    final ObjectInputStream ois = new ObjectInputStream(zis);
    final Object readObject = ois.readObject();
    return (T) readObject;
  }
  
  public static Map<String,String> readDictIntoMap(InputStream dictionary) {
    Map<String,String> dictMap = new HashMap<>();
    final BufferedReader breader = new BufferedReader(
        new InputStreamReader(dictionary));
    String line;
    try {
      while ((line = breader.readLine()) != null) {
        if (!line.isEmpty() && !line.startsWith("#")) {
          final String[] elems = line.split("\t");
          dictMap.put(elems[0], elems[2]);
        }
      }
    } catch (final IOException e) {
      e.printStackTrace();
    }
    return dictMap;
  }
}