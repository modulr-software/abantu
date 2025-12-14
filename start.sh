#!/usr/bin/env bash

clojure -M:migrate

java -jar target/source-be-standalone.jar
