#!/bin/bash

if [ $# -ne 1 ]; then
    echo "Usage: $0 <filename>"
    exit 1
fi

filename="$1"

if [ ! -f "$filename" ] || [ ! -r "$filename" ]; then
    echo "Error: File '$filename' not found or not readable!"
    exit 1
fi

while IFS= read -r line || [[ -n "$line" ]]; do
    line=$(echo "$line" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')

    if [[ $line != \#* ]]; then
        image_name="${line#\#}"
        echo "Pulling Docker image [$image_name]"

        docker pull "$image_name"

        if [ $? -ne 0 ]; then
            echo "Error puling Docker image [$image_name]"
            exit 1
        fi

        echo "Docker image pulled successfully [$image_name]"
    fi
done < "$filename"

echo "All Docker images pulled successfully"
exit 0
