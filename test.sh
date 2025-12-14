#!/usr/bin/env bash

export $(grep '.*' .env | xargs)

clj -X:test
