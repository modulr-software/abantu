#!/usr/bin/env bash
export $(grep '.*' .env | xargs)

export JAVA_CMD="$JAVA_HOME/bin"
clojure -M:migrate

/home/merv/.jenv/shims/java -jar target/source-be-standalone.jar
