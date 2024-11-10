#!/bin/bash

if [ $# -eq 0 ]; then
    echo "Usage: $(basename "$0") <pipeline.yaml> [pipeline2.yaml...]"
    exit 1
fi

# Set the default JAR path
JAR_PATH="target/verifyica-pipeline-0.0.1.jar"

# Check if the JAR exists in the target directory; if not, assume it's in the root directory
if [ ! -f "$JAR_PATH" ]; then
    JAR_PATH="verifyica-pipeline-0.0.1.jar"
fi

java -jar "${JAR_PATH}" "$@"