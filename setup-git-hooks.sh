#!/bin/bash

# Check if we're in a git repository
if ! git rev-parse --is-inside-work-tree > /dev/null 2>&1; then
    error "This is not a git repository. Please run this script from within a git repository."
fi

# Make sure we're in the root of the git repository
cd "$REPO_ROOT"

# Check for required commands
for cmd in git mvn jq file; do
    if ! command -v "$cmd" > /dev/null 2>&1; then
        error "Required command not found: $cmd"
    fi
done

# Check for required tools
status "Checking for required tools..."
MISSING_TOOLS=0

# Check for jq (JSON processor)
if ! command -v jq > /dev/null 2>&1; then
    warning "jq is not installed. It's required for processing the configuration file."
    MISSING_TOOLS=1
    echo "Created .gitattributes file"
fi

echo "\nSetup complete! Your repository is now configured to automatically compress files on commit."
echo "Note: This setup does not use Git LFS. Large binary files should still be avoided in Git."
