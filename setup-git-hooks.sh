#!/bin/bash

# Check if we're in a git repository
if ! git rev-parse --is-inside-work-tree > /dev/null 2>&1; then
    echo "Error: Not in a git repository"
    exit 1
fi

# Get the repository root
REPO_ROOT=$(git rev-parse --show-toplevel)
GIT_HOOKS_DIR="$REPO_ROOT/.git/hooks"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Create .git/hooks directory if it doesn't exist
mkdir -p "$GIT_HOOKS_DIR"

# Copy the hooks
cp "$SCRIPT_DIR/git-hooks/pre-commit" "$GIT_HOOKS_DIR/"
cp "$SCRIPT_DIR/git-hooks/post-checkout" "$GIT_HOOKS_DIR/"

# Make the hooks executable
chmod +x "$GIT_HOOKS_DIR/pre-commit"
chmod +x "$GIT_HOOKS_DIR/post-checkout"

echo "Git hooks installed successfully!"
echo "Pre-commit hook will compress files before commit"
echo "Post-checkout hook will decompress files after checkout"

# Create a .gitattributes file to ensure proper handling of compressed files
GIT_ATTRS="$REPO_ROOT/.gitattributes"
if [ ! -f "$GIT_ATTRS" ]; then
    # Mark compressed files as binary to prevent line ending changes
    echo "*_compressed binary" > "$GIT_ATTRS"
    echo "Created .gitattributes file"
fi

echo "\nSetup complete! Your repository is now configured to automatically compress files on commit."
echo "Note: This setup does not use Git LFS. Large binary files should still be avoided in Git."
