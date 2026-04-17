#!/usr/bin/env bash

export $(grep '.*' .env | xargs)

echo "Starting compilation..."
clojure -T:build uber
