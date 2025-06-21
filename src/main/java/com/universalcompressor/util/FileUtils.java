package com.universalcompressor.util;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    public static String formatFileSize(long bytes) {
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
     * Decompresses a file.
     * @param inputFile The compressed input file
     * @param outputFile The output file to save the decompressed data
     * @return true if decompression was successful, false otherwise
     */
    public static boolean decompressFile(File inputFile, File outputFile) {
        try (FileInputStream fis = new FileInputStream(inputFile);
             GZIPInputStream gzipIS = new GZIPInputStream(fis);
             FileOutputStream fos = new FileOutputStream(outputFile)) {
            
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipIS.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            return true;
            
        } catch (IOException e) {
            System.err.println("Error decompressing file: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Creates a unique filename by appending a number if the file already exists.
     * @param originalPath The original file path
     * @return A unique file path
     */
    public static String getUniqueFilename(String originalPath) {
        Path path = Paths.get(originalPath);
        String parent = path.getParent() != null ? path.getParent().toString() : ".";
        String fileName = path.getFileName().toString();
        String baseName = fileName;
        String extension = "";
        
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = fileName.substring(0, dotIndex);
            extension = fileName.substring(dotIndex);
        }
        
        File file = new File(originalPath);
        int counter = 1;
        
        while (file.exists()) {
            String newName = String.format("%s (%d)%s", baseName, counter++, extension);
            file = new File(parent, newName);
        }
        
        return file.getPath();
    }
}
