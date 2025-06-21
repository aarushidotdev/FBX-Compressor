#!/bin/bash

# Create necessary directories
echo "Creating directories..."
mkdir -p CompressedAssets
mkdir -p Content

# Make hooks executable
echo "Setting up git hooks..."
chmod +x .git-hooks/*

# Set up git hooks path
git config core.hooksPath .git-hooks

# Add common Unreal Engine file patterns to .gitignore
if ! grep -q "^# Unreal Engine" .gitignore 2>/dev/null; then
    echo -e "\n# Unreal Engine Content" >> .gitignore
    echo "Content/" >> .gitignore
    echo "# Common Unreal file types" >> .gitignore
    echo "*.uasset" >> .gitignore
    echo "*.umap" >> .gitignore
    echo "*.uexp" >> .gitignore
    echo "*.ubulk" >> .gitignore
    echo "# Binary files" >> .gitignore
    echo "*.bin" >> .gitignore
    echo "# Intermediate files" >> .gitignore
    echo "Intermediate/" >> .gitignore
    echo "Saved/" >> .gitignore
    echo "Binaries/" >> .gitignore
    echo "Build/" >> .gitignore
    echo "DerivedDataCache/" >> .gitignore
    echo "*.sln" >> .gitignore
    echo "*.sdf" >> .gitignore
    echo "*.suo" >> .gitignore
    echo "*.opensdf" >> .gitignore
    echo "*.opendb" >> .gitignore
    echo "*.VC.db" >> .gitignore
    echo "*.VC.VC.opendb" >> .gitignore
    echo "*.VC.VC.opendb.*" >> .gitignore
    echo "*.VC.VC.db" >> .gitignore
    echo "*.VC.VC.db.*" >> .gitignore
    echo "*.vs/" >> .gitignore
    echo "# Local files" >> .gitignore
    echo ".vs/" >> .gitignore
    echo ".vscode/" >> .gitignore
    echo ".idea/" >> .gitignore
    echo "# Build results" >> .gitignore
    echo "*.o" >> .gitignore
    echo "*.obj" >> .gitignore
    echo "*.lib" >> .gitignore
    echo "*.dll" >> .gitignore
    echo "*.exe" >> .gitignore
    echo "*.dylib" >> .gitignore
    echo "*.so" >> .gitignore
    echo "*.app" >> .gitignore
    echo "# Logs and databases" >> .gitignore
    echo "*.log" >> .gitignore
    echo "*.sqlite" >> .gitignore
    echo "*.sql" >> .gitignore
    echo "# Project files" >> .gitignore
    echo "*.uproject" >> .gitignore
    echo "*.uprojectdirs" >> .gitignore
    echo "# OS generated files" >> .gitignore
    echo ".DS_Store" >> .gitignore
    echo ".DS_Store?" >> .gitignore
    echo "*~" >> .gitignore
    echo "*.swp" >> .gitignore
    echo "*.swo" >> .gitignore
    echo "# Unreal generated files" >> .gitignore
    echo "*.generated.h" >> .gitignore
    echo "*.generated.cpp" >> .gitignore
    echo "*.generated.manifest" >> .gitignore
    echo "# Visual Studio files" >> .gitignore
    echo "*.vcxproj" >> .gitignore
    echo "*.vcxproj.filters" >> .gitignore
    echo "*.vcxproj.user" >> .gitignore
    echo "# Xcode files" >> .gitignore
    echo "*.xcodeproj/" >> .gitignore
    echo "*.xcworkspace/" >> .gitignore
    echo "xcuserdata/" >> .gitignore
fi

echo "=== Setup complete ==="
echo "1. Place your Unreal assets in the 'Content' directory"
echo "2. Run 'git add .' to stage your changes"
echo "3. Run 'git commit' to compress and commit your assets"
echo "4. Push your changes with 'git push'"
echo "\nOn other machines, run 'git pull' to automatically decompress assets"

exit 0
