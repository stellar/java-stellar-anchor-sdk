#!/bin/sh

# find staged files
stagedFiles=$(git diff --staged --name-only)

# run spotlessApply
echo "Running spotlessApply. Formatting code..."
./gradlew spotlessApply

# add staged files
for file in $stagedFiles; do
  if test -f "$file"; then
    git add $file
  fi
done
