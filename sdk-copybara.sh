#!/bin/bash

# Bazel Java SDK Vendoring Script

# This script does not actually use copybara, but it is named as such because
# most people will understand the use case. This is a sample script for
# vendoring the SDK into Bazel Eclipse using simple file copy primitives.

# SOURCE
if [ -z "${SDK_DIR+xxx}" ]; then
  echo "Please set the location of Bazel Java SDK on your filesystem using the SDK_DIR env variable."
  echo " This is the absolute path of the directory that contains the WORKSPACE file of bazel-java-sdk"
  exit 1
fi
sdk_root=$SDK_DIR


# DESTINATION
if [ ! -f CODE_OF_CONDUCT.md ]; then
  echo "This script must be run from the root of the Bazel Eclipse repository."
  exit 1
fi
bls_root=$(pwd)

# SDK
# sdk
#  bazel-java-sdk
#    aspect/*
#    src/main/java/* (aspect, command, index, lang, logging, model, project, workspace)
#    src/test/java/**/*Test.java
#  bazel-java-sdk-test-framework
#    src/main/java/**/test/*.java (mocks)

# BLS
# com.salesforce.b2eclipse.jdt.ls
#    resources/*
#    src/main/java/* (aspect, command, index, lang, logging, model, project, workspace)

# Mapping Rules

# Aspect (TODO)
#cp -R $sdk_root/sdk/bazel-java-sdk/aspect/* $bls_root/com.salesforce.b2eclipse.jdt.ls/resources

# SDK Java Classes
cp -R $sdk_root/sdk/bazel-java-sdk/src/main/java/* $bls_root/com.salesforce.b2eclipse.jdt.ls/src/main/java
