#!/usr/bin/bash

export $(grep '.*' .env | xargs)


export JAVA_CMD="/home/merv/.jenv/shims/java"

echo "Starting compilation..."
clojure -T:build uber
