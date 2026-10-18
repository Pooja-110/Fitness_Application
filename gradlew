#!/bin/sh
#
# Copyright 2015 the original author or authors.
# Gradle wrapper script for Unix
#

APP_HOME=$(cd "$(dirname "$0")" && pwd)
APP_NAME="Gradle"

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
exec java -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
