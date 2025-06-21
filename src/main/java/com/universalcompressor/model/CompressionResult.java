package com.universalcompressor.model;

/**
 * Represents the result of a compression operation.
 */
public class CompressionResult {
    private final boolean success;
    private final String errorMessage;
    private final long originalSize;     // in bytes
    private final long compressedSize;    // in bytes

    public CompressionResult(boolean success, String errorMessage, 
                           long originalSize, long compressedSize) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.originalSize = originalSize;
        this.compressedSize = compressedSize;
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
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
