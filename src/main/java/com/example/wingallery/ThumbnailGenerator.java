package com.example.wingallery;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import javafx.application.Platform;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

/**
 * Utility class for generating thumbnails from images and videos
 * Uses bounded thread pool and semaphore for memory-safe concurrent generation
 */
public class ThumbnailGenerator {
    private static final int THUMBNAIL_SIZE = 300; // Larger thumbnails for better visibility
    
    // Bounded thread pool - prevents decode storms
    private static final ExecutorService thumbnailExecutor = Executors.newFixedThreadPool(4);
    
    // Semaphore to limit concurrent thumbnail generation (4 in-flight max)
    private static final Semaphore generationSemaphore = new Semaphore(4);

    /**
     * Generate thumbnail for an image file with caching
     * Checks cache first, generates only if needed
     * Uses semaphore to limit concurrent generation
     */
    public static CompletableFuture<Image> generateImageThumbnail(File file) {
        // Check cache first
        Image cached = ThumbnailCache.getCachedThumbnail(file);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        
        // Generate with throttling
        CompletableFuture<Image> future = new CompletableFuture<>();
        
        thumbnailExecutor.submit(() -> {
            try {
                generationSemaphore.acquire(); // Throttle concurrent generation
                try {
                    // Load at reduced resolution for memory efficiency
                    // preserveRatio = true to avoid squeezing
                    // smooth = false for faster loading and less memory
                    // backgroundLoading = false to ensure image is fully loaded
                    Image image = new Image(file.toURI().toString(), THUMBNAIL_SIZE, THUMBNAIL_SIZE, true, false, false);
                    
                    // Complete with image (even if error, let display code handle it)
                    future.complete(image);
                } finally {
                    generationSemaphore.release();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                future.complete(null);
            } catch (Exception e) {
                future.complete(null);
            }
        });
        
        // Cache to disk asynchronously (doesn't block thumbnail display)
        future.thenAccept(thumbnail -> {
            if (thumbnail != null) {
                ThumbnailCache.cacheThumbnail(file, thumbnail);
            }
        });
        
        return future;
    }

    /**
     * Generate thumbnail for a video file with caching
     * Checks cache first, generates only if needed
     * Uses semaphore to limit concurrent generation
     */
    public static CompletableFuture<Image> generateVideoThumbnail(File file) {
        // Check cache first
        Image cached = ThumbnailCache.getCachedThumbnail(file);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        
        CompletableFuture<Image> future = new CompletableFuture<>();
        
        // Acquire semaphore before starting generation
        thumbnailExecutor.submit(() -> {
            try {
                generationSemaphore.acquire(); // Throttle concurrent generation
                
                // Try JavaFX MediaPlayer (bundled with app)
                tryJavaFXThumbnail(file, future);
                
                // Add timeout fallback to placeholder (5 seconds total)
                thumbnailExecutor.submit(() -> {
                    try {
                        Thread.sleep(5000);
                        if (!future.isDone()) {
                            Image placeholder = createPlaceholderImage();
                            future.complete(placeholder);
                            // Cache the placeholder too
                            if (placeholder != null) {
                                ThumbnailCache.cacheThumbnail(file, placeholder);
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        if (!future.isDone()) {
                            future.complete(createPlaceholderImage());
                        }
                    }
                    // Note: Semaphore released in whenComplete callback
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                future.complete(createPlaceholderImage());
            } catch (Exception e) {
                // Ensure future is completed even on unexpected errors
                if (!future.isDone()) {
                    future.complete(createPlaceholderImage());
                }
            }
        });
        
        // Cache result and release semaphore when complete
        future.whenComplete((thumbnail, throwable) -> {
            // Release semaphore when done (success or failure)
            generationSemaphore.release();
            
            // Cache the result
            if (thumbnail != null) {
                ThumbnailCache.cacheThumbnail(file, thumbnail);
            }
        });
        
        return future;
    }
    
    /**
     * Create a placeholder image for videos that can't generate thumbnails
     */
    private static Image createPlaceholderImage() {
        try {
            // Create a simple colored rectangle as placeholder
            javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(THUMBNAIL_SIZE, THUMBNAIL_SIZE);
            javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
            
            // Dark gray background
            gc.setFill(javafx.scene.paint.Color.rgb(60, 60, 60));
            gc.fillRect(0, 0, THUMBNAIL_SIZE, THUMBNAIL_SIZE);
            
            // Play icon
            gc.setFill(javafx.scene.paint.Color.rgb(200, 200, 200));
            double centerX = THUMBNAIL_SIZE / 2.0;
            double centerY = THUMBNAIL_SIZE / 2.0;
            double size = 40; // Smaller for reduced thumbnail size
            
            // Triangle play button
            gc.fillPolygon(
                new double[]{centerX - size/2, centerX + size/2, centerX - size/2},
                new double[]{centerY - size/2, centerY, centerY + size/2},
                3
            );
            
            // Take snapshot
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(javafx.scene.paint.Color.TRANSPARENT);
            WritableImage placeholder = canvas.snapshot(params, null);
            
            return placeholder;
        } catch (Exception e) {
            return null;
        }
    }
    

    /**
     * Try to generate thumbnail using JavaFX MediaPlayer
     */
    private static void tryJavaFXThumbnail(File file, CompletableFuture<Image> future) {
        Platform.runLater(() -> {
            MediaPlayer mediaPlayer = null;
            try {
                Media media = new Media(file.toURI().toString());
                mediaPlayer = new MediaPlayer(media);
                MediaPlayer finalMediaPlayer = mediaPlayer;
                
                // Flag to track if snapshot was taken
                final boolean[] snapshotTaken = {false};
                final MediaView[] mediaViewHolder = {null};
                
                mediaPlayer.setOnReady(() -> {
                    try {
                        // Create MediaView only when ready
                        MediaView mediaView = new MediaView(finalMediaPlayer);
                        mediaView.setFitWidth(THUMBNAIL_SIZE);
                        mediaView.setFitHeight(THUMBNAIL_SIZE);
                        mediaView.setPreserveRatio(true); // Don't squeeze, will be cropped in display
                        mediaViewHolder[0] = mediaView;
                        
                        // Try to take snapshot immediately without seeking
                        Platform.runLater(() -> {
                            try {
                                if (!snapshotTaken[0] && mediaViewHolder[0] != null) {
                                    snapshotTaken[0] = true;
                                    SnapshotParameters params = new SnapshotParameters();
                                    WritableImage snapshot = mediaViewHolder[0].snapshot(params, null);
                                    
                                    if (snapshot != null && snapshot.getWidth() > 0 && snapshot.getHeight() > 0) {
                                        future.complete(snapshot);
                                        finalMediaPlayer.stop();
                                        finalMediaPlayer.dispose();
                                    } else {
                                        future.complete(createPlaceholderImage());
                                        finalMediaPlayer.stop();
                                        finalMediaPlayer.dispose();
                                    }
                                }
                            } catch (Exception ex) {
                                future.complete(createPlaceholderImage());
                                finalMediaPlayer.stop();
                                finalMediaPlayer.dispose();
                            }
                        });
                    } catch (Exception e) {
                        finalMediaPlayer.stop();
                        finalMediaPlayer.dispose();
                        if (!future.isDone()) {
                            future.complete(createPlaceholderImage());
                        }
                    }
                });
                
                mediaPlayer.setOnError(() -> {
                    finalMediaPlayer.stop();
                    finalMediaPlayer.dispose();
                    if (!future.isDone()) {
                        future.complete(createPlaceholderImage());
                    }
                });
                
                // Timeout fallback (4 seconds for JavaFX attempt)
                MediaPlayer timeoutPlayer = mediaPlayer;
                javafx.animation.PauseTransition timeout = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(4));
                timeout.setOnFinished(e -> {
                    if (!future.isDone()) {
                        timeoutPlayer.stop();
                        timeoutPlayer.dispose();
                        future.complete(createPlaceholderImage());
                    }
                });
                timeout.play();
                
            } catch (Exception e) {
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.dispose();
                }
                if (!future.isDone()) {
                    future.complete(createPlaceholderImage());
                }
            }
        });
    }

    /**
     * Check if file is a supported image format
     */
    public static boolean isImageFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || 
               name.endsWith(".png") || name.endsWith(".gif") || 
               name.endsWith(".bmp");
    }

    /**
     * Check if file is a supported video format
     */
    public static boolean isVideoFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".mp4") || name.endsWith(".avi") || 
               name.endsWith(".mov") || name.endsWith(".mkv") ||
               name.endsWith(".m4v") || name.endsWith(".flv");
    }
    
    /**
     * Shutdown the executor service (call on app exit)
     */
    public static void shutdown() {
        thumbnailExecutor.shutdown();
    }
}
