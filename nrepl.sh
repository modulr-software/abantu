#!/usr/bin/env bash

export $(grep '.*' .env | xargs)

clojure -M:nrepl
