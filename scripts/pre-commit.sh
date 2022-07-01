#!/bin/sh

echo '[git hook] Code Formatting: executing gradle spotlessApply before commit'

# Stash any unstaged changes
git stash -q --keep-index

# Run the spotlessApply to format the code
./gradlew spotlessApply

# Add the format changes to the git commit
git add .

# unstash the unstashed changes
git stash pop -q
