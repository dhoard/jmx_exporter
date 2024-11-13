#!/bin/bash

# Check if a file argument is passed
if [ -z "$1" ]; then
    echo "Usage: $0 <file>"
    exit 1
fi

# Check if the file exists
if [ ! -f "$1" ]; then
    echo "Error: File '$1' not found."
    exit 1
fi

# Read the file line by line
while IFS= read -r line; do
    # Skip lines that start with '#'
    if [[ "$line" =~ ^# ]] || [[ -z "$line" ]]; then
        continue
    fi

    # Run the docker pull command
    echo "Pulling image [$line]"
    docker pull "$line"
    exit_code=$?

    # Check if the command was successful
    if [ $exit_code -ne 0 ]; then
        echo "Failed to pull image [$line] with exit code [$exit_code]"
    else
        echo "Successfully pulled image [$line]"
    fi
done < "$1"

