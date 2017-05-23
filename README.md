
ixa-pipe-ml
=============
[![Build Status](https://travis-ci.org/ixa-ehu/ixa-pipe-ml.svg?branch=master)](https://travis-ci.org/ixa-ehu/ixa-pipe-ml)
[![GitHub license](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://raw.githubusercontent.com/apache/opennlp/master/LICENSE)

ixa-pipe-ml is the Machine Learning Component of IXA pipes. ixa-pipe-ml allows to
train and evaluate the models for every IXA-pipe available.

ixa-pipe-ml is part of IXA pipes, a multilingual set of NLP tools developed
by the IXA NLP Group [http://ixa2.si.ehu.es/ixa-pipes].

Please go to [http://ixa2.si.ehu.es/ixa-pipes] for general information about the IXA
pipes tools but also for **official releases, including source code and binary
packages for all the tools in the IXA pipes toolkit**.

This document is intended to be the **usage guide of ixa-pipe-ml**. If you really need to clone
and install this repository instead of using the releases provided in
[http://ixa2.si.ehu.es/ixa-pipes], please scroll down to the end of the document for
the [installation instructions](#installation).

**NOTICE!!**: ixa-pipe-ml is now in [Maven Central](http://search.maven.org/).

## TABLE OF CONTENTS

1. [Overview of ixa-pipe-ml](#overview)
2. [Usage of ixa-pipe-ml](#cli-usage)
  + [Available features](#features)
  + [Training your own models](#training)
  + [Evaluation](#evaluation)
3. [API via Maven Dependency](#api)
4. [Git installation](#installation)

## OVERVIEW

ixa-pipe-ml provides the training and evaluation functionalities required to train and evaluate sequence labeling and constituent parsing models to be used in the ixa pipes modules (http://ixa2.si.ehu.es/ixa-pipes/).

Every model trained with ixa-pipe-ml is self-contained, that is, the properties files are not needed to use them.
Please see the sequenceTrainer.properties template file for all available training options and documentation.

ixa-pipe-ml provides competitive models based on robust local features and exploiting unlabeled data
via clustering features. The clustering features are based on Brown, Clark (2003) and Word2Vec clustering plus some gazetteers in some cases. The procedure to obtain such clusters is described in (https://github.com/ragerri/cluster-preprocessing).
To avoid duplication of efforts, we use and contribute to the API provided by the
[Apache OpenNLP project](http://opennlp.apache.org) with our own custom developed features.

## CLI-USAGE

ixa-pipe-ml provides the following command-line basic functionalities:

1. **sequenceTrainer**: trains sequence labeling models for POS tagging, Lemmatization, Named Entity Recognition, Opinion Target Extraction, Chunking and Supersense tagging.
2. **parserTrainer**: trains constituent parsing models.
3. **eval**: evaluates a trained sequence labeling model with a given test set. Accuracy or F1 can be chosen.
4. **parseval**: evaluates a trained parsing model with parseval metric.
5. **cross**: it runs cross validation training.

Each of these functionalities are accessible by as subcommands to the ixa-pipe-ml-$version.jar. Please read below and check the -help parameter:

````shell
java -jar target/ixa-pipe-ml-$version-exec.jar (sequenceTrainer|parserTrainer|eval|parseval|cross) -help

### Features

**A description of every feature is provided in the sequenceTrainer.properties properties
file** distributed with ixa-pipe-ml. As the training functionality is configured in
properties files, please do check this document.

### Training

To train a new model you just need to pass a training parameters file as an
argument. As it has been already said, the options are documented in the
template trainParams.properties file.

**Example**:

````shell
java -jar target/ixa.pipe.ml-$version-exec.jar train -p sequenceTrainer.properties
````
**Training with Features using External Resources**: For training with dictionary or clustering
based features (Brown, Clark and Word2Vec) you need to pass the lexicon as
value of the respective feature in the properties file. This is only for training, as
for tagging or evaluation the model is serialized with all resources included.

### Evaluation

You can evaluate a trained model or a prediction data against a reference data
or testset.

+ **language**: provide the language.
+ **metric**: Choose the metric: accuracy or F1.
+ **model**: the model to be evaluated.
+ **testset**: the testset or reference set.
+ **corpusFormat**: the format of the reference set and of the prediction set
  if --prediction option is chosen.
+ **evalReport**: detail of the evaluation report
  + **brief**: just the F1, precision and recall scores
  + **detailed**, the F1, precision and recall per class
  + **error**: the list of false positives and negatives

**Example**:

````shell
java -jar target/ixa.pipe.nerc-$version-exec.jar eval -m en-local-conll03.bin -l en -f conll03 -t conll03.testb
````

## API

The easiest way to use ixa-pipe-ml programatically is via Apache Maven. Add
this dependency to your pom.xml:

````shell
<dependency>
    <groupId>eus.ixa</groupId>
    <artifactId>ixa-pipe-ml</artifactId>
    <version>0.0.4</version>
</dependency>
````

## JAVADOC

The javadoc of the module is located here:

````shell
ixa-pipe-ml/target/ixa-pipe-ml-$version-javadoc.jar
````

## Module contents

The contents of the module are the following:

    + formatter.xml           Apache OpenNLP code formatter for Eclipse SDK
    + pom.xml                 maven pom file which deals with everything related to compilation and execution of the module
    + src/                    java source code of the module and required resources
    + Furthermore, the installation process, as described in the README.md, will generate another directory:
    target/                 it contains binary executable and other directories
    + trainParams.properties      A template properties file containing documention
    for every available option


## INSTALLATION

Installing the ixa-pipe-ml requires the following steps:

If you already have installed in your machine the Java 1.7+ and MAVEN 3, please go to step 3
directly. Otherwise, follow these steps:

### 1. Install JDK 1.7 or JDK 1.8

If you do not install JDK 1.7+ in a default location, you will probably need to configure the PATH in .bashrc or .bash_profile:

````shell
export JAVA_HOME=/yourpath/local/java7
export PATH=${JAVA_HOME}/bin:${PATH}
````

If you use tcsh you will need to specify it in your .login as follows:

````shell
setenv JAVA_HOME /usr/java/java17
setenv PATH ${JAVA_HOME}/bin:${PATH}
````

If you re-login into your shell and run the command

````shell
java -version
````

You should now see that your JDK is 1.7 or 1.8.

### 2. Install MAVEN 3

Download MAVEN 3 from

````shell
wget http://apache.rediris.es/maven/maven-3/3.0.5/binaries/apache-maven-3.0.5-bin.tar.gz
````
Now you need to configure the PATH. For Bash Shell:

````shell
export MAVEN_HOME=/home/ragerri/local/apache-maven-3.0.5
export PATH=${MAVEN_HOME}/bin:${PATH}
````

For tcsh shell:

````shell
setenv MAVEN3_HOME ~/local/apache-maven-3.0.5
setenv PATH ${MAVEN3}/bin:{PATH}
````

If you re-login into your shell and run the command

````shell
mvn -version
````

You should see reference to the MAVEN version you have just installed plus the JDK that is using.

### 3. Get module source code

If you must get the module source code from here do this:

````shell
git clone https://github.com/ixa-ehu/ixa-pipe-ml
````

### 4. Compile

Execute this command to compile ixa-pipe-ml:

````shell
cd ixa-pipe-ml
mvn clean package
````
This step will create a directory called target/ which contains various directories and files.
Most importantly, there you will find the module executable:

ixa-pipe-ml-$version.jar

This executable contains every dependency the module needs, so it is completely portable as long
as you have a JVM 1.7 installed.

To install the module in the local maven repository, usually located in ~/.m2/, execute:

````shell
mvn clean install
````

## Contact information

````shell
Rodrigo Agerri
IXA NLP Group
University of the Basque Country (UPV/EHU)
E-20018 Donostia-San Sebastián
rodrigo.agerri@ehu.eus
````
