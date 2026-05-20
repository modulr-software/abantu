#!/bin/bash
cd /home/merv/Developer/abantu-be-staging

export $(grep '.*' .env | xargs)

export JAVA_CMD="$JAVA_HOME/bin/java"
./migrate.sh up

/home/merv/.jenv/shims/java -jar target/abantu-api-standalone.jar
