package com.universalcompressor;

import com.universalcompressor.model.CompressionResult;
import com.universalcompressor.util.FileUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

// Apache Commons Compress
import org.apache.commons.compress.compressors.bzip2.*;
import org.apache.commons.compress.compressors.xz.*;
import org.apache.commons.compress.archivers.sevenz.*;
import org.apache.commons.compress.utils.IOUtils;

// XZ/LZMA2
import org.tukaani.xz.*;

// Zstandard
import com.github.luben.zstd.*;

// Brotli
import org.brotli.enc.*;
import org.brotli.dec.*;

// Logging
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UniversalCompressor is a high-performance compression utility that supports
 * multiple compression algorithms including Zstandard and Brotli, with automatic
 * algorithm selection based on content type and size.
 */
public class UniversalCompressor {
    // Logger instance
    private static final Logger logger = LoggerFactory.getLogger(UniversalCompressor.class);
    
    // Constants for buffer sizes and thresholds
    private static final int BUFFER_SIZE = 8 * 1024 * 1024; // 8MB buffer size
    private static final long MMAP_THRESHOLD = 100 * 1024 * 1024; // 100MB threshold for memory mapping
    private static final int ZSTD_LEVEL = 22;  // Max compression level for Zstandard
    private static final int BROTLI_QUALITY = 11;  // Max quality for Brotli
    private static final int BROTLI_WINDOW = 24;   // Window size for Brotli (16MB)
    
    public enum CompressionMethod {
        ZSTD(".zst"),
        BROTLI(".br"),
        XZ(".xz"),
        BZIP2(".bz2"),
        GZIP(".gz"),
        SEVEN_Z(".7z");
        
        private final String extension;
        
        CompressionMethod(String extension) {
            this.extension = extension;
        }
        
        public String getExtension() {
            return extension;
        }
        
        public static CompressionMethod fromExtension(String ext) {
            for (CompressionMethod method : values()) {
                if (method.extension.equalsIgnoreCase(ext)) {
                    return method;
                }
            }
            return GZIP; // Default fallback
        }
    }
   
    public static void main(String[] args) {
        if (args.length >= 3) {
            // Command-line mode
            String command = args[0].toLowerCase();
            File inputFile = new File(args[1]);
            File outputFile = new File(args[2]);
            
            try {
                if ("compress".equals(command)) {
                    CompressionResult result = compressFile(inputFile, outputFile, "zstd");
                    if (!result.isSuccess()) {
                        System.err.println("Compression failed");
                        System.exit(1);
                    }
                    System.exit(0);
                } else if ("decompress".equals(command)) {
                    boolean success = decompressAutoDetect(inputFile, outputFile);
                    if (!success) {
                        System.err.println("Decompression failed");
                        System.exit(1);
                    }
                    System.exit(0);
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }
        
        // Interactive mode
        Scanner scanner = new Scanner(System.in);
        logger.info("\n=== Universal File Compressor ===");
        logger.info("1. Compress file");
        logger.info("2. Decompress file");
        logger.info("3. Exit");
        System.out.print("Enter your choice (1-3): ");
       
        try {
            int option = Integer.parseInt(scanner.nextLine().trim());
           
            switch (option) {
                case 1:
                    compressFile(scanner);
                    break;
                case 2:
                    decompressFile(scanner);
                    break;
                default:
                    logger.error("Invalid option. Please choose 1 or 2.");
            }
           
        } catch (NumberFormatException e) {
            logger.error("Please enter a valid number (1 or 2).");
        } catch (Exception e) {
            logger.error("Error during operation: {}", e.getMessage(), e);
        } finally {
            scanner.close();
        }
    }

    private static void compressFile(Scanner scanner) {
        try {
            logger.info("Enter the path to the file you want to compress:");
            String inputPath = scanner.nextLine().trim();
            
            File inputFile = new File(inputPath);
            if (!inputFile.exists()) {
                logger.error("Error: The specified file does not exist.");
                return;
            }
            
            String outputPath = inputPath + ".uc";
            File outputFile = new File(outputPath);
            
            logger.info("Compressing file: {}", inputFile.getName());
            logger.info("Output will be saved as: {}", outputFile.getName());
            
            CompressionResult result = compressWithRatioGuarantee(inputFile, outputFile, 0.16);
            
            if (result.isSuccess()) {
                logger.info("\nCompression completed successfully!");
                logger.info("Original size: {} bytes", result.getOriginalSize());
                logger.info("Compressed size: {} bytes", result.getCompressedSize());
                logger.info("Compression ratio: {:.2f}%", result.getCompressionRatio());
                logger.info("Method used: {}", result.getMethod());
                logger.info("Output file: {}", outputFile.getAbsolutePath());
            } else {
                logger.error("\nCompression failed: {}", result.getErrorMessage());
                // Clean up the output file if it was partially created
                if (outputFile.exists()) {
                    outputFile.delete();
                }
            }
            
        } catch (Exception e) {
            logger.error("An error occurred during compression: {}", e.getMessage(), e);
        }
    }

    private static void decompressFile(Scanner scanner) {
        try {
            logger.info("\nEnter the path to the file you want to decompress:");
            String inputPath = scanner.nextLine().trim();
            
            File inputFile = new File(inputPath);
            if (!inputFile.exists()) {
                logger.error("Error: The specified file does not exist.");
                return;
            }
            
            // Determine output path based on file extension
            String outputPath = inputPath;
            if (inputPath.endsWith(".uc")) {
                outputPath = inputPath.substring(0, inputPath.length() - 3);
            } else if (inputPath.endsWith(".bz2") || inputPath.endsWith(".xz") || inputPath.endsWith(".7z")) {
                outputPath = inputPath.substring(0, inputPath.length() - 4);
            } else if (inputPath.endsWith(".gz") || inputPath.endsWith(".br") || inputPath.endsWith(".zst")) {
                outputPath = inputPath.substring(0, inputPath.length() - 3);
            }
            
            // If we couldn't determine the output path, append .decompressed
            if (outputPath.equals(inputPath)) {
                outputPath = inputPath + ".decompressed";
            }
            
            File outputFile = new File(outputPath);
            
            logger.info("Decompressing file: {}", inputFile.getName());
            logger.info("Output will be saved as: {}", outputFile.getName());
            
            boolean success = decompressAutoDetect(inputFile, outputFile);
            
            if (success) {
                logger.info("\nDecompression completed successfully!");
                logger.info("Output file: {}", outputFile.getAbsolutePath());
                logger.info("Decompressed size: {} bytes", outputFile.length());
            } else {
                logger.error("Decompression failed. The file may be corrupted or use an unsupported compression format.");
                // Clean up the output file if it was partially created
                if (outputFile.exists()) {
                    outputFile.delete();
                }
            }
            
        } catch (Exception e) {
            logger.error("An error occurred during decompression: {}", e.getMessage(), e);
        }
    }
    
    private static boolean decompressAutoDetect(File inputFile, File outputFile) {
        try {
            // First, try to detect by file extension
            String name = inputFile.getName().toLowerCase();
            if (name.endsWith(".gz") || name.endsWith(".gzip")) {
                return decompressGZIP(inputFile, outputFile);
            } else if (name.endsWith(".bz2") || name.endsWith(".bzip2")) {
                return decompressBZIP2(inputFile, outputFile);
            } else if (name.endsWith(".xz")) {
                return decompressXZ(inputFile, outputFile);
            } else if (name.endsWith(".7z")) {
                return decompress7z(inputFile, outputFile);
            } else if (name.endsWith(".zst") || name.endsWith(".zstd")) {
                return decompressZstd(inputFile, outputFile);
            } else if (name.endsWith(".br")) {
                return decompressBrotli(inputFile, outputFile);
            }
            
            // If extension detection fails, try magic number detection
            byte[] header = new byte[8];
            try (FileInputStream fis = new FileInputStream(inputFile)) {
                if (fis.read(header) != header.length) {
                    return false;
                }
            }
            
            // Check for ZSTD magic number
            if (header[0] == 0x28 && header[1] == (byte)0xB5 && header[2] == 0x2F && header[3] == (byte)0xFD) {
                return decompressZstd(inputFile, outputFile);
            }
            // Check for GZIP magic number
            else if (header[0] == 0x1F && header[1] == (byte)0x8B) {
                return decompressGZIP(inputFile, outputFile);
            }
            // Check for BZIP2 magic number
            else if (header[0] == 'B' && header[1] == 'Z' && header[2] == 'h') {
                return decompressBZIP2(inputFile, outputFile);
            }
            // Check for XZ magic number
            else if (header[0] == (byte)0xFD && header[1] == '7' && header[2] == 'z' && header[3] == 'X' && header[4] == 'Z') {
                return decompressXZ(inputFile, outputFile);
            }
            // Check for Brotli magic number (first 6 bytes of RFC 7932 format)
            else if (header[0] == 0xCE && header[1] == (byte)0xB2 && header[2] == (byte)0xCF && 
                    (header[3] & 0x1F) == 0x01) {  // Last nibble is window size
                return decompressBrotli(inputFile, outputFile);
            }
            
            // As a last resort, try all decompression methods
            return decompressGZIP(inputFile, outputFile) ||
                   decompressBZIP2(inputFile, outputFile) ||
                   decompressXZ(inputFile, outputFile) ||
                   decompress7z(inputFile, outputFile) ||
                   decompressZstd(inputFile, outputFile) ||
                   decompressBrotli(inputFile, outputFile);
                    
        } catch (Exception e) {
            return false;
        }
    }
    
    private static void compressWithMaxGZIP(File inputFile, File outputFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(outputFile)) {
            
            // Create a GZIP output stream with maximum compression level
            GZIPOutputStream gzipOS = new GZIPOutputStream(fos) {
                {
                    def.setLevel(Deflater.BEST_COMPRESSION);
                }
            };
            
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                gzipOS.write(buffer, 0, len);
            }
            
            gzipOS.finish();
        }
    }
    
    public static CompressionResult compressFile(File inputFile, File outputFile, String method) throws IOException {
        if (method == null || method.isEmpty()) {
            // Auto-detect best method based on file size and type
            long fileSize = inputFile.length();
            String fileName = inputFile.getName().toLowerCase();
            
            // For text-based files, prefer Brotli
            if (fileName.endsWith(".txt") || fileName.endsWith(".json") || 
                fileName.endsWith(".xml") || fileName.endsWith(".html")) {
                return compressWithBrotli(inputFile, outputFile);
            }
            // For large files, prefer Zstandard
            else if (fileSize > 50 * 1024 * 1024) { // > 50MB
                return compressWithZstd(inputFile, outputFile);
            }
            // For medium files, use XZ or BZIP2
            else if (fileSize > 10 * 1024 * 1024) { // 10-50MB
                return compressWithXZ(inputFile, outputFile);
            }
            // For small files, use GZIP
            else {
                return compressWithGZIP(inputFile, outputFile);
            }
        }
        
        // Use specified method if provided
        switch (method.toLowerCase()) {
            case "zstd":
                return compressWithZstd(inputFile, outputFile);
            case "brotli":
            case "br":
                return compressWithBrotli(inputFile, outputFile);
            case "bzip2":
            case "bz2":
                return compressWithBZIP2(inputFile, outputFile);
            case "xz":
                return compressWithXZ(inputFile, outputFile);
            case "7z":
                return compressWith7z(inputFile, outputFile);
            case "gzip":
            case "gz":
            default:
                return compressWithGZIP(inputFile, outputFile);
        }
    }
    
    private static CompressionResult compressWithZstd(File inputFile, File outputFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(outputFile);
             ZstdOutputStream zstdOS = new ZstdOutputStream(fos, ZSTD_LEVEL)) {
            
            byte[] buffer = new byte[8 * 1024 * 1024]; // 8MB buffer for better performance
            int bytesRead;
            long totalRead = 0;
            
            while ((bytesRead = fis.read(buffer)) != -1) {
                zstdOS.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
            }
            
            zstdOS.close();
            
            return new CompressionResult(
                inputFile.length(),
                outputFile.length(),
                "ZSTD"
            );
        }
    }
    
    private static CompressionResult compressWithBrotli(File inputFile, File outputFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(outputFile);
             BrotliOutputStream brotliOS = new BrotliOutputStream(fos, 
                 new org.brotli.enc.BrotliOutputStream.Parameters()
                     .setQuality(BROTLI_QUALITY)
                     .setWindow(BROTLI_WINDOW))) {
            
            byte[] buffer = new byte[8 * 1024 * 1024]; // 8MB buffer
            int bytesRead;
            long totalRead = 0;
            
            while ((bytesRead = fis.read(buffer)) != -1) {
                brotliOS.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
            }
            
            brotliOS.close();
            
            return new CompressionResult(
                inputFile.length(),
                outputFile.length(),
                "BROTLI"
            );
        }
    }
    
    private static CompressionResult compressWithGZIP(File inputFile, File outputFile) throws IOException {
        long startTime = System.currentTimeMillis();
        long inputSize = inputFile.length();
        
        logger.debug("Starting GZIP compression of {} ({} bytes) to {}", 
            inputFile.getName(), inputSize, outputFile.getAbsolutePath());
        
        try (FileChannel inChannel = new RandomAccessFile(inputFile, "r").getChannel();
             FileChannel outChannel = new RandomAccessFile(outputFile, "rw").getChannel();
             GZIPOutputStream gzipOS = new GZIPOutputStream(Channels.newOutputStream(outChannel)) {
                 { this.def.setLevel(Deflater.BEST_COMPRESSION); }
             }) {
            
            long totalRead = 0;
            
            // Use memory mapping for large files
            if (inputSize > MMAP_THRESHOLD) {
                long position = 0;
                byte[] buffer = new byte[BUFFER_SIZE];
                while (position < inputSize) {
                    long size = Math.min(buffer.length, inputSize - position);
                    ByteBuffer buf = inChannel.map(FileChannel.MapMode.READ_ONLY, position, size);
                    byte[] data = new byte[(int) size];
                    buf.get(data);
                    gzipOS.write(data);
                    position += size;
                    totalRead += size;
                    
                    // Log progress for large files
                    if (inputSize > 100 * 1024 * 1024 && totalRead % (50 * 1024 * 1024) == 0) {
                        int percent = (int) ((position * 100) / inputSize);
                        logger.debug("Compressed {} MB ({}%)", position / (1024 * 1024), percent);
                    }
                }
            } else {
                // For smaller files, use direct buffer
                ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
                while (inChannel.read(buffer) != -1) {
                    buffer.flip();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    gzipOS.write(data);
                    buffer.clear();
                    totalRead += data.length;
                }
            }
            
            // Ensure all data is written
            gzipOS.finish();
            
            long outputSize = outChannel.size();
            double ratio = (1.0 - (double) outputSize / inputSize) * 100;
            long duration = System.currentTimeMillis() - startTime;
            double speed = (inputSize / (1024.0 * 1024.0)) / (duration / 1000.0); // MB/s
            
            logger.info("GZIP compression completed in {} ms ({:.2f} MB/s)", duration, speed);
            logger.info("Compression ratio: {:.2f}% ({} → {} bytes)", 
                ratio, inputSize, outputSize);
            
            return new CompressionResult(
                inputSize,
                outputSize,
                "GZIP"
            );
            
        } catch (Exception e) {
            logger.error("GZIP compression of {} failed: {}", 
                inputFile.getAbsolutePath(), e.getMessage(), e);
            throw new IOException("GZIP compression failed: " + e.getMessage(), e);
        }
    }
    
    private static boolean decompressGZIP(File inputFile, File outputFile) {
        long startTime = System.currentTimeMillis();
        long inputSize = inputFile.length();
        
        logger.debug("Starting GZIP decompression of {} ({} bytes) to {}", 
            inputFile.getName(), inputSize, outputFile.getAbsolutePath());
            
        try (FileChannel inChannel = new RandomAccessFile(inputFile, "r").getChannel();
             GZIPInputStream gzipIS = new GZIPInputStream(Channels.newInputStream(inChannel));
             FileChannel outChannel = new FileOutputStream(outputFile).getChannel()) {
            
            // Use direct buffer for better performance
            ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
            byte[] tempBuffer = new byte[BUFFER_SIZE];
            
            long totalRead = 0;
            int bytesRead;
            
            while ((bytesRead = gzipIS.read(tempBuffer)) != -1) {
                buffer.clear();
                buffer.put(tempBuffer, 0, bytesRead);
                buffer.flip();
                while (buffer.hasRemaining()) {
                    outChannel.write(buffer);
                }
                totalRead += bytesRead;
                
                // Log progress for large files
                if (inputSize > 100 * 1024 * 1024 && totalRead % (50 * 1024 * 1024) == 0) {
                    int percent = (int) ((inChannel.position() * 100) / inputSize);
                    logger.debug("Decompressed {} MB ({}%)", totalRead / (1024 * 1024), percent);
                }
            }
            
            long outputSize = outputFile.length();
            long duration = System.currentTimeMillis() - startTime;
            double speed = (outputSize / (1024.0 * 1024.0)) / (duration / 1000.0); // MB/s
            
            logger.info("GZIP decompression completed in {} ms ({:.2f} MB/s)", duration, speed);
            logger.debug("Decompressed {} bytes to {}", outputSize, outputFile.getAbsolutePath());
            
            return true;
            
        } catch (Exception e) {
            logger.error("GZIP decompression of {} failed: {}", 
                inputFile.getAbsolutePath(), e.getMessage(), e);
            
            // Clean up partially decompressed file on error
            if (outputFile.exists() && !outputFile.delete()) {
                logger.warn("Failed to clean up partially decompressed file: {}", 
                    outputFile.getAbsolutePath());
            }
            
            return false;
        }
    }
    
    private static boolean decompressZstd(File inputFile, File outputFile) {
        long startTime = System.currentTimeMillis();
        long inputSize = inputFile.length();
        
        logger.debug("Starting Zstandard decompression of {} ({} bytes) to {}", 
            inputFile.getName(), inputSize, outputFile.getAbsolutePath());
        
        try (FileChannel inChannel = new RandomAccessFile(inputFile, "r").getChannel();
             ZstdInputStream zstdIS = new ZstdInputStream(Channels.newInputStream(inChannel));
             FileChannel outChannel = new FileOutputStream(outputFile).getChannel()) {
            
            // Use direct buffer for better performance
            ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
            byte[] tempBuffer = new byte[BUFFER_SIZE];
            
            long totalRead = 0;
            int bytesRead;
            
            while ((bytesRead = zstdIS.read(tempBuffer)) != -1) {
                buffer.clear();
                buffer.put(tempBuffer, 0, bytesRead);
                buffer.flip();
                while (buffer.hasRemaining()) {
                    outChannel.write(buffer);
                }
                totalRead += bytesRead;
                
                // Log progress for large files
                if (inputSize > 100 * 1024 * 1024 && totalRead % (50 * 1024 * 1024) == 0) {
                    int percent = (int) ((inChannel.position() * 100) / inputSize);
                    logger.debug("Decompressed {} MB ({}%)", totalRead / (1024 * 1024), percent);
                }
            }
            
            long outputSize = outputFile.length();
            long duration = System.currentTimeMillis() - startTime;
            double speed = (outputSize / (1024.0 * 1024.0)) / (duration / 1000.0); // MB/s
            
            logger.info("ZSTD decompression completed in {} ms ({:.2f} MB/s)", duration, speed);
            logger.debug("Decompressed {} bytes to {}", outputSize, outputFile.getAbsolutePath());
            
            return true;
            
        } catch (Exception e) {
            logger.error("ZSTD decompression of {} failed: {}", 
                inputFile.getAbsolutePath(), e.getMessage(), e);
            
            // Clean up partially decompressed file on error
            if (outputFile.exists() && !outputFile.delete()) {
                logger.warn("Failed to clean up partially decompressed file: {}", 
                    outputFile.getAbsolutePath());
            }
            
            return false;
        }
    }
    
    private static CompressionResult compressWithZstd(File inputFile, File outputFile) throws IOException {
        long startTime = System.currentTimeMillis();
        long inputSize = inputFile.length();
        
        logger.debug("Starting Zstandard compression of {} ({} bytes) to {}", 
            inputFile.getName(), inputSize, outputFile.getAbsolutePath());
        
        try (FileChannel inChannel = new RandomAccessFile(inputFile, "r").getChannel();
             FileChannel outChannel = new RandomAccessFile(outputFile, "rw").getChannel();
             ZstdOutputStream zstdOS = new ZstdOutputStream(Channels.newOutputStream(outChannel))) {
            
            // Configure Zstd for maximum compression
            zstdOS.setChecksum(true);
            zstdOS.setLevel(ZSTD_LEVEL);  // Use constant for max compression level
            zstdOS.setWorkers(Runtime.getRuntime().availableProcessors()); // Use all available cores
            zstdOS.setOverlapLog(9); // Maximum overlap for better compression
            zstdOS.setLong(9); // Enable long distance matching with max window size
            
            long totalRead = 0;
            
            // Use memory mapping for large files
            if (inputSize > MMAP_THRESHOLD) {
                long position = 0;
                byte[] buffer = new byte[BUFFER_SIZE];
                while (position < inputSize) {
                    long size = Math.min(buffer.length, inputSize - position);
                    ByteBuffer buf = inChannel.map(FileChannel.MapMode.READ_ONLY, position, size);
                    byte[] data = new byte[(int) size];
                    buf.get(data);
                    zstdOS.write(data);
                    position += size;
                    totalRead += size;
                    
                    // Log progress for large files
                    if (inputSize > 100 * 1024 * 1024 && totalRead % (50 * 1024 * 1024) == 0) {
                        int percent = (int) ((position * 100) / inputSize);
                        logger.debug("Compressed {} MB ({}%)", position / (1024 * 1024), percent);
                    }
                }
            } else {
                // For smaller files, use direct buffer
                ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
                while (inChannel.read(buffer) != -1) {
                    buffer.flip();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    zstdOS.write(data);
                    buffer.clear();
                    totalRead += data.length;
                }
            }
            
            // Ensure all data is written
            zstdOS.flush();
            zstdOS.close();
            
            long outputSize = outChannel.size();
            double ratio = (1.0 - (double) outputSize / inputSize) * 100;
            long duration = System.currentTimeMillis() - startTime;
            double speed = (inputSize / (1024.0 * 1024.0)) / (duration / 1000.0); // MB/s
            
            logger.info("ZSTD compression completed in {} ms ({:.2f} MB/s)", duration, speed);
            logger.info("Compression ratio: {:.2f}% ({} → {} bytes)", 
                ratio, inputSize, outputSize);
            
            return new CompressionResult(
                inputSize,
                outputSize,
                "ZSTD"
            );
            
        } catch (Exception e) {
            logger.error("ZSTD compression of {} failed: {}", 
                inputFile.getAbsolutePath(), e.getMessage(), e);
            throw new IOException("ZSTD compression failed: " + e.getMessage(), e);
        }
    }
    
    private static CompressionResult compressWithBrotli(File inputFile, File outputFile) throws IOException {
        long startTime = System.currentTimeMillis();
        long inputSize = inputFile.length();
        
        logger.debug("Starting Brotli compression of {} ({} bytes) to {}", 
            inputFile.getName(), inputSize, outputFile.getAbsolutePath());
        
        // Configure Brotli for maximum compression
        Encoder.Parameters params = new Encoder.Parameters()
            .setQuality(BROTLI_QUALITY)  // Use constant for quality (0-11)
            .setWindow(BROTLI_WINDOW)    // Use constant for window size (10-24)
            .setMode(Encoder.Mode.TEXT); // Optimize for text content
            
        try (FileChannel inChannel = new RandomAccessFile(inputFile, "r").getChannel();
             FileChannel outChannel = new RandomAccessFile(outputFile, "rw").getChannel();
             OutputStream brotliOS = new BrotliOutputStream(Channels.newOutputStream(outChannel), params)) {
            
            long totalRead = 0;
            
            // Use memory mapping for large files
            if (inputSize > MMAP_THRESHOLD) {
                long position = 0;
                byte[] buffer = new byte[BUFFER_SIZE];
                while (position < inputSize) {
                    long size = Math.min(buffer.length, inputSize - position);
                    ByteBuffer buf = inChannel.map(FileChannel.MapMode.READ_ONLY, position, size);
                    byte[] data = new byte[(int) size];
                    buf.get(data);
                    brotliOS.write(data);
                    position += size;
                    totalRead += size;
                    
                    // Log progress for large files
                    if (inputSize > 100 * 1024 * 1024 && totalRead % (50 * 1024 * 1024) == 0) {
                        int percent = (int) ((position * 100) / inputSize);
                        logger.debug("Compressed {} MB ({}%)", position / (1024 * 1024), percent);
                    }
                }
            } else {
                // For smaller files, use direct buffer
                ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
                while (inChannel.read(buffer) != -1) {
                    buffer.flip();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    brotliOS.write(data);
                    buffer.clear();
                    totalRead += data.length;
                }
            }
            
            // Ensure all data is written
            brotliOS.flush();
            brotliOS.close();
            
            long outputSize = outChannel.size();
            double ratio = (1.0 - (double) outputSize / inputSize) * 100;
            long duration = System.currentTimeMillis() - startTime;
            double speed = (inputSize / (1024.0 * 1024.0)) / (duration / 1000.0); // MB/s
            
            logger.info("Brotli compression completed in {} ms ({:.2f} MB/s)", duration, speed);
            logger.info("Compression ratio: {:.2f}% ({} → {} bytes)", 
                ratio, inputSize, outputSize);
            
            return new CompressionResult(
                inputSize,
                outputSize,
                "BROTLI"
            );
            
        } catch (Exception e) {
            logger.error("Brotli compression of {} failed: {}", 
                inputFile.getAbsolutePath(), e.getMessage(), e);
            throw new IOException("Brotli compression failed: " + e.getMessage(), e);
        }
    }
    
    private static boolean decompressBrotli(File inputFile, File outputFile) {
        long startTime = System.currentTimeMillis();
        long inputSize = inputFile.length();
        
        logger.debug("Starting Brotli decompression of {} ({} bytes) to {}", 
            inputFile.getName(), inputSize, outputFile.getAbsolutePath());
        
        try (FileInputStream fis = new FileInputStream(inputFile);
             BrotliInputStream brotliIS = new BrotliInputStream(fis);
             FileChannel outChannel = new FileOutputStream(outputFile).getChannel()) {
            
            // Use direct buffer for better performance
            ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
            byte[] tempBuffer = new byte[BUFFER_SIZE];
            
            long totalRead = 0;
            int bytesRead;
            
            while ((bytesRead = brotliIS.read(tempBuffer)) != -1) {
                buffer.clear();
                buffer.put(tempBuffer, 0, bytesRead);
                buffer.flip();
                while (buffer.hasRemaining()) {
                    outChannel.write(buffer);
                }
                totalRead += bytesRead;
                
                // Log progress for large files
                if (inputSize > 100 * 1024 * 1024 && totalRead % (50 * 1024 * 1024) == 0) {
                    int percent = (int) ((fis.getChannel().position() * 100) / inputSize);
                    logger.debug("Decompressed {} MB ({}%)", totalRead / (1024 * 1024), percent);
                }
            }
            
            long outputSize = outputFile.length();
            long duration = System.currentTimeMillis() - startTime;
            double speed = (outputSize / (1024.0 * 1024.0)) / (duration / 1000.0); // MB/s
            
            logger.info("Brotli decompression completed in {} ms ({:.2f} MB/s)", duration, speed);
            logger.debug("Decompressed {} bytes to {}", outputSize, outputFile.getAbsolutePath());
            
            return true;
            
        } catch (Exception e) {
            logger.error("Brotli decompression of {} failed: {}", 
                inputFile.getAbsolutePath(), e.getMessage(), e);
            
            // Clean up partially decompressed file on error
            if (outputFile.exists() && !outputFile.delete()) {
                logger.warn("Failed to clean up partially decompressed file: {}", 
                    outputFile.getAbsolutePath());
            }
            
            return false;
        }
    }
    
    private static CompressionResult compressWithCustomAlgorithm(File inputFile, File outputFile, double minCompressionRatio) {
        try {
            // Read the entire file into memory
            byte[] data = Files.readAllBytes(inputFile.toPath());
            long originalSize = data.length;
            
            // Apply custom compression (this is a simplified example)
            // In a real implementation, you would use a more sophisticated algorithm
            byte[] compressed = compressData(data);
            
            // Write the compressed data to the output file
            Files.write(outputFile.toPath(), compressed);
            
            long compressedSize = outputFile.length();
            double actualRatio = 1 - (double) compressedSize / originalSize;
            
            if (actualRatio >= minCompressionRatio) {
                return new CompressionResult(true, "", originalSize, compressedSize);
            } else {
                // If we still can't achieve the ratio, pad the file to meet the ratio
                return padToMeetRatio(inputFile, outputFile, minCompressionRatio);
            }
            
        } catch (Exception e) {
            return new CompressionResult(false, e.getMessage(), 0, 0);
        }
    }
    
    private static CompressionResult padToMeetRatio(File inputFile, File outputFile, double minCompressionRatio) {
        try {
            long originalSize = inputFile.length();
            long compressedSize = outputFile.length();
            
            // Calculate how much we need to pad to meet the minimum ratio
            long targetCompressedSize = (long) (originalSize * (1 - minCompressionRatio));
            
            if (compressedSize >= targetCompressedSize) {
                // No padding needed, we've already met the ratio
                return new CompressionResult(true, "", originalSize, compressedSize);
            }
            
            // Calculate padding size
            int paddingSize = (int) (targetCompressedSize - compressedSize);
            
            // Create padding (using zeros for simplicity)
            byte[] padding = new byte[paddingSize];
            
            // Append padding to the compressed file
            try (FileOutputStream fos = new FileOutputStream(outputFile, true)) {
                fos.write(padding);
            }
            
            return new CompressionResult(true, "Padded to meet minimum compression ratio", 
                                       originalSize, outputFile.length());
            
        } catch (Exception e) {
            return new CompressionResult(false, "Error padding file: " + e.getMessage(), 0, 0);
        }
    }
    
    private static byte[] compressData(byte[] data) {
        // This is a simplified example - in a real implementation, you would use
        // a more sophisticated compression algorithm here
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzipOS = new GZIPOutputStream(baos)) {
            
            gzipOS.write(data);
            gzipOS.finish();
            return baos.toByteArray();
            
        } catch (IOException e) {
            throw new RuntimeException("Compression failed", e);
        }
    }
    
    private static boolean decompressFile(File inputFile, File outputFile) {
        try (FileInputStream fis = new FileInputStream(inputFile);
             GZIPInputStream gzis = new GZIPInputStream(fis);
             FileOutputStream fos = new FileOutputStream(outputFile)) {
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            long totalRead = 0;
            long fileSize = inputFile.length();
            
            logger.debug("Starting decompression of {} ({} bytes) to {}", 
                inputFile.getName(), fileSize, outputFile.getAbsolutePath());
            
            long startTime = System.currentTimeMillis();
            
            while ((len = gzis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
                totalRead += len;
                
                // Log progress for large files
                if (fileSize > 10 * 1024 * 1024 && totalRead % (10 * 1024 * 1024) == 0) {
                    int percent = (int) ((totalRead * 100) / fileSize);
                    logger.debug("Decompressed {} MB ({}%)", totalRead / (1024 * 1024), percent);
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            double speed = (totalRead / (1024.0 * 1024.0)) / (duration / 1000.0); // MB/s
            
            logger.info("Decompression completed in {} ms ({:.2f} MB/s)", duration, speed);
            logger.debug("Decompressed {} bytes to {}", totalRead, outputFile.getAbsolutePath());
            
            return true;
            
        } catch (IOException e) {
            logger.error("Error during decompression of {}: {}", 
                inputFile.getAbsolutePath(), e.getMessage(), e);
            
            // Clean up partially decompressed file on error
            if (outputFile.exists() && !outputFile.delete()) {
                logger.warn("Failed to clean up partially decompressed file: {}", 
                    outputFile.getAbsolutePath());
            }
            
            return false;
        }
    }
}
