package com.fbxcompressor.model;

/**
 * Represents the result of a compression operation.
 */
public class CompressionResult {
    private final String originalFileName;
    private final String compressedFileName;
    private final long originalSize;     // in bytes
    private final long compressedSize;    // in bytes

    public CompressionResult(String originalFileName, String compressedFileName, 
                           long originalSize, long compressedSize) {
        this.originalFileName = originalFileName;
        this.compressedFileName = compressedFileName;
        this.originalSize = originalSize;
        this.compressedSize = compressedSize;
    }

    // Getters
    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getCompressedFileName() {
        return compressedFileName;
    }

    public long getOriginalSize() {
        return originalSize;
    }

    public long getCompressedSize() {
        return compressedSize;
    }

    /**
     * Calculates the compression ratio as a percentage.
     * @return The reduction percentage (0-100)
     */
    public double getReductionPercentage() {
        if (originalSize == 0) return 0;
        return 100.0 * (originalSize - compressedSize) / originalSize;
    }
}
