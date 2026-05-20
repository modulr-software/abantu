#!/usr/bin/env bash
export $(grep '.*' staging.env | xargs)

export JAVA_CMD="$JAVA_HOME/bin/java"
clojure -M:migrate

/home/merv/.jenv/shims/java -jar target/source-be-standalone.jar
