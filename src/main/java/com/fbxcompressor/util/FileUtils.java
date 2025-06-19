package com.fbxcompressor.util;

import java.io.*;
import java.util.zip.GZIPInputStream;

/**
 * Utility class for file operations.
 */
public class FileUtils {
    
    /**
     * Converts a file size in bytes to a human-readable string.
     * @param bytes The file size in bytes
     * @return A human-readable string representation of the file size
     */
    public static String humanReadableByteCount(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    /**
     * Validates if a file has the specified extension.
     * @param file The file to check
     * @param extension The extension to check for (without the dot)
     * @return true if the file has the specified extension (case-insensitive)
     */
    public static boolean hasExtension(File file, String extension) {
        if (file == null || !file.isFile()) {
            return false;
        }
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return false;
        }
        return fileName.substring(dotIndex + 1).equalsIgnoreCase(extension);
    }
    
    /**
     * Decompresses a GZIP compressed file.
     * @param inputFile The compressed input file
     * @param outputFile The output file to save the decompressed data
     * @throws IOException If an I/O error occurs
     */
    public static void decompressFile(File inputFile, File outputFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(inputFile);
             GZIPInputStream gzipIS = new GZIPInputStream(fis);
             FileOutputStream fos = new FileOutputStream(outputFile)) {
            
            byte[] buffer = new byte[1024 * 1024]; // 1MB buffer
            int bytesRead;
            while ((bytesRead = gzipIS.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
    }
    
    /**
     * Checks if a file is compressed with GZIP.
     * @param file The file to check
     * @return true if the file is GZIP compressed
     */
    public static boolean isGzipped(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             DataInputStream dis = new DataInputStream(fis)) {
            
            int magic = dis.readUnsignedShort();
            return magic == GZIPInputStream.GZIP_MAGIC;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Removes any padding that might have been added during compression.
     * @param data The data to process
     * @return The data without padding
     */
    public static byte[] removePadding(byte[] data) {
        // Look for our padding marker
        String paddingMarker = "[PADDING]";
        byte[] markerBytes = paddingMarker.getBytes();
        
        // Search for the marker in the data
        for (int i = 0; i <= data.length - markerBytes.length; i++) {
            boolean found = true;
            for (int j = 0; j < markerBytes.length; j++) {
                if (data[i + j] != markerBytes[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                // Return the data up to the marker
                byte[] result = new byte[i];
                System.arraycopy(data, 0, result, 0, i);
                return result;
            }
        }
        
        // If no marker found, return original data
        return data;
    }
}
