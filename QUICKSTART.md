# Quick Start Guide: Git LFS Replacement with Universal Compressor

This guide will help you quickly set up and use the Universal Compressor as a Git LFS replacement in your project.

## Prerequisites

- Java 11 or higher
- Git
- Maven (for building from source)
- `jq` (for JSON processing in hooks)

## Setup Instructions

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/FBX-Compressor.git
cd FBX-Compressor
```

### 2. Build the Project

```bash
mvn clean package
```

### 3. Initialize Your Git Repository

If you haven't already:

```bash
git init  # Skip if already a git repository
```

### 4. Install Git Hooks

```bash
# Make the setup script executable (Linux/macOS)
chmod +x setup-git-hooks.sh

# Run the setup script
./setup-git-hooks.sh
```

### 5. Configure Compression (Optional)

Edit `compression-config.json` to customize:
- File patterns to compress
- Compression methods
- Minimum file sizes
- Exclusion patterns

Example configuration:

```json
{
  "compression": {
    "min_file_size": 1024,
    "min_compression_ratio": 0.05,
    "max_compression_time_ms": 300000,
    "buffer_size_mb": 8
  },
  "file_patterns": {
    "compress": ["**/*.fbx", "**/*.obj", "**/*.blend"],
    "exclude": ["**/node_modules/**", "**/target/**"]
  },
  "compression_methods": [
    {
      "name": "gzip",
      "extension": ".gz",
      "min_size": 1024,
      "max_size": 10485760,
      "level": 9
    },
    {
      "name": "bzip2",
      "extension": ".bz2",
      "min_size": 1048576,
      "max_size": 52428800,
      "level": 9
    },
    {
      "name": "xz",
      "extension": ".xz",
      "min_size": 5242880,
      "level": 9
    }
  ]
}
```

## Basic Usage

### Adding Large Files

1. Add your large files as normal:
   ```bash
   git add large_model.fbx
   ```

2. Commit your changes:
   ```bash
   git commit -m "Add 3D model"
   ```
   The pre-commit hook will automatically compress the file.

### Cloning a Repository

1. Clone the repository as usual:
   ```bash
   git clone your-repo.git
   cd your-repo
   ```

2. The post-checkout hook will automatically decompress any compressed files.

### Manual Operations

#### Compress a File

```bash
java -jar target/universal-compressor-1.0-SNAPSHOT.jar compress input.fbx output.fbx.gz
```

#### Decompress a File

```bash
java -jar target/universal-compressor-1.0-SNAPSHOT.jar decompress input.fbx.gz output.fbx
```

## Common Workflows

### Adding New Large Files

1. Add the file to Git:
   ```bash
   git add new_model.obj
   ```

2. Commit the file (it will be compressed automatically):
   ```bash
   git commit -m "Add new 3D model"
   ```

### Updating Large Files

1. Make your changes to the file
2. Add and commit as normal - the file will be recompressed

### Viewing Compressed Files

1. Check the `.compressed_files` file to see which files are being tracked
2. The original files are automatically decompressed when you check out a branch

## Troubleshooting

### Hooks Not Running

- Ensure hooks are executable:
  ```bash
  chmod +x .git/hooks/*
  ```

### File Not Being Compressed

- Check if the file matches any patterns in `compression-config.json`
- Verify the file size is above the minimum threshold
- Check the file isn't excluded

### Getting Help

For additional help, refer to the full documentation in README.md or open an issue in the repository.
