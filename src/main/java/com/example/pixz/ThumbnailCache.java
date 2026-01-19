package com.example.pixz;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

/**
 * Manages thumbnail caching to disk and memory for faster loads
 * Uses WeakReference for memory cache so GC can reclaim when needed
 * Tracks failed thumbnails to avoid repeated generation attempts
 */
public class ThumbnailCache {
    private static final String CACHE_DIR_NAME = ".wingallery/thumbnails";
    private static Path cacheDir;

    // In-memory cache with WeakReference - GC can reclaim when memory is low
    private static final ConcurrentHashMap<String, WeakReference<Image>> memoryCache = new ConcurrentHashMap<>();

    // Track files that failed thumbnail generation
    private static final Set<String> failedThumbnails = new HashSet<>();

    static {
        initializeCacheDir();
    }

    private static void initializeCacheDir() {
        String userHome = System.getProperty("user.home");
        cacheDir = Paths.get(userHome, CACHE_DIR_NAME);

        try {
            if (!Files.exists(cacheDir)) {
                Files.createDirectories(cacheDir);
            }
        } catch (IOException e) {
            // Cache directory creation failed, will work without cache
        }
    }

    /**
     * Generate cache key from file path and last modified time
     */
    private static String getCacheKey(File file) {
        try {
            String key = file.getAbsolutePath() + "_" + file.lastModified();
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(key.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString() + ".jpg";
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get cached thumbnail - checks memory cache first, then disk
     */
    public static Image getCachedThumbnail(File file) {
        String cacheKey = getCacheKey(file);
        if (cacheKey == null)
            return null;

        // Check memory cache first (fastest)
        WeakReference<Image> weakRef = memoryCache.get(cacheKey);
        if (weakRef != null) {
            Image image = weakRef.get();
            if (image != null) {
                return image; // Found in memory
            } else {
                // Reference was cleared by GC, remove from map
                memoryCache.remove(cacheKey);
            }
        }

        // Check disk cache
        Path cachedFile = cacheDir.resolve(cacheKey);
        if (Files.exists(cachedFile)) {
            try {
                // Load cached thumbnail with size constraint (should already be 300x300, but
                // enforce it)
                Image image = new Image(cachedFile.toUri().toString(), 300, 300, true, false, true);

                // Store in memory cache for next time
                memoryCache.put(cacheKey, new WeakReference<>(image));
                return image;
            } catch (Exception e) {
                // Cache file corrupted, will regenerate
                try {
                    Files.deleteIfExists(cachedFile);
                } catch (IOException ignored) {
                }
            }
        }
        return null;
    }

    /**
     * Check if an image is a placeholder (gray or black background)
     */
    public static boolean isPlaceholderImage(Image image) {
        if (image == null || image.getWidth() == 0 || image.getHeight() == 0) {
            return true;
        }

        try {
            // Placeholder images are exactly 300x300
            if (image.getWidth() != 300 || image.getHeight() != 300) {
                return false; // Real images might be different sizes
            }

            int centerX = (int) (image.getWidth() / 2);
            int centerY = (int) (image.getHeight() / 2);
            int argb = image.getPixelReader().getArgb(centerX, centerY);

            int red = (argb >> 16) & 0xFF;
            int green = (argb >> 8) & 0xFF;
            int blue = argb & 0xFF;

            // Check if it's APPROXIMATELY placeholder gray (60,60,60)
            // Allow wider tolerance for different placeholder shades
            int tolerance = 20; // Increased from 10
            boolean isGrayPlaceholder = (Math.abs(red - 60) <= tolerance &&
                    Math.abs(green - 60) <= tolerance &&
                    Math.abs(blue - 60) <= tolerance);

            if (isGrayPlaceholder) {

                return true;
            }

            // Also check for darker gray/black placeholders
            boolean isDarkPlaceholder = (red < 80 && green < 80 && blue < 80 &&
                    Math.abs(red - green) < 10 && Math.abs(green - blue) < 10);

            if (isDarkPlaceholder) {

                return true;
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isBlackPixel(int argb) {
        int red = (argb >> 16) & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = argb & 0xFF;
        return red < 5 && green < 5 && blue < 5;
    }

    /**
     * Save thumbnail to cache (both memory and disk)
     * Don't cache placeholders
     */
    public static void cacheThumbnail(File file, Image thumbnail) {
        if (thumbnail == null)
            return;

        // Don't cache placeholder images
        if (isPlaceholderImage(thumbnail)) {
            return; // Skip caching placeholders
        }

        String cacheKey = getCacheKey(file);
        if (cacheKey == null)
            return;

        // Store in memory cache with WeakReference
        memoryCache.put(cacheKey, new WeakReference<>(thumbnail));

        // Store on disk for persistence
        Path cachedFile = cacheDir.resolve(cacheKey);

        try {
            // Convert JavaFX Image to BufferedImage
            int width = (int) thumbnail.getWidth();
            int height = (int) thumbnail.getHeight();
            // Use TYPE_INT_RGB for JPEG (no alpha channel needed for thumbnails)
            BufferedImage bImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            PixelReader pixelReader = thumbnail.getPixelReader();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int argb = pixelReader.getArgb(x, y);
                    // Remove alpha channel for JPEG
                    int rgb = argb & 0x00FFFFFF;
                    bImage.setRGB(x, y, rgb);
                }
            }

            // Save as JPEG with high quality (0.85 = 85% quality, good balance)
            ImageIO.write(bImage, "jpg", cachedFile.toFile());

            // Flush BufferedImage to release native resources
            bImage.flush();
        } catch (Exception e) {
            // Failed to cache, not critical
        }
    }

    /**
     * Clear memory cache to free up RAM
     */
    public static void clearMemoryCache() {
        memoryCache.clear();
    }

    /**
     * Remove cached thumbnail for a specific file (both memory and disk)
     */
    public static void removeCachedThumbnail(File file) {
        String cacheKey = getCacheKey(file);
        if (cacheKey == null)
            return;

        // Remove from memory cache
        memoryCache.remove(cacheKey);

        // Remove from disk cache
        Path cachedFile = cacheDir.resolve(cacheKey);
        try {
            Files.deleteIfExists(cachedFile);

        } catch (IOException ignored) {

        }
    }

    /**
     * Mark a file as having failed thumbnail generation
     */
    public static void markAsFailed(File file) {
        failedThumbnails.add(file.getAbsolutePath());
    }

    /**
     * Check if a file has failed thumbnail generation
     */
    public static boolean hasFailed(File file) {
        return failedThumbnails.contains(file.getAbsolutePath());
    }

    /**
     * Remove a file from the failed list (after successful generation)
     */
    public static void removeFromFailed(File file) {
        failedThumbnails.remove(file.getAbsolutePath());
    }

    /**
     * Get all failed thumbnail file paths
     */
    public static Set<String> getFailedThumbnails() {
        return new HashSet<>(failedThumbnails);
    }

    /**
     * Clear the failed thumbnails list
     */
    public static void clearFailedList() {
        failedThumbnails.clear();
    }

    /**
     * Clear old cache entries (optional cleanup)
     */
    public static void clearOldCache(int daysOld) {
        try {
            long cutoffTime = System.currentTimeMillis() - (daysOld * 24L * 60 * 60 * 1000);
            Files.list(cacheDir)
                    .filter(path -> {
                        try {
                            return Files.getLastModifiedTime(path).toMillis() < cutoffTime;
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException e) {
            // Cleanup failed, not critical
        }
    }
}
