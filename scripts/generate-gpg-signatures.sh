#!/bin/bash

if [ -z "$1" ]; then
    echo "Usage: $0 <directory>"
    exit 1
fi

DIR="$1"

for FILE in "$DIR"/*; do
    if [ -f "$FILE" ]; then
        gpg --batch --yes --detach-sign --armor -o "$FILE.asc" "$FILE"
    fi
done
