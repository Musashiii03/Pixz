package com.example.pixz;

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
    private static final ExecutorService thumbnailExecutor = Executors.newFixedThreadPool(8);

    // Semaphore to limit concurrent thumbnail generation (8 in-flight max for
    // faster refresh)
    private static final Semaphore generationSemaphore = new Semaphore(8);

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
                    // Read orientation
                    int rotation = MediaMetadataUtils.getRotation(file);

                    if (rotation == 0) {
                        // Normal loading
                        Image image = new Image(file.toURI().toString(), THUMBNAIL_SIZE, THUMBNAIL_SIZE, true, false,
                                false);

                        // Check if image loaded successfully
                        if (image.isError() || image.getWidth() == 0 || image.getHeight() == 0) {
                            future.complete(null);
                        } else {
                            future.complete(image);
                        }
                    } else {
                        // Load and rotate on FX thread
                        Platform.runLater(() -> {
                            try {
                                Image sourceImage = new Image(file.toURI().toString(), THUMBNAIL_SIZE, THUMBNAIL_SIZE,
                                        true,
                                        false, false);
                                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(sourceImage);
                                iv.setRotate(rotation);
                                SnapshotParameters params = new SnapshotParameters();
                                params.setFill(javafx.scene.paint.Color.TRANSPARENT);
                                WritableImage rotated = iv.snapshot(params, null);
                                future.complete(rotated);
                            } catch (Exception e) {
                                future.complete(null);
                            }
                        });
                    }
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

                // Add timeout fallback to placeholder (1.5 seconds - balance between speed and
                // quality)
                thumbnailExecutor.submit(() -> {
                    try {
                        Thread.sleep(1500);
                        if (!future.isDone()) {

                            Image placeholder = createPlaceholderImage();
                            future.complete(placeholder);
                            // Don't cache placeholder - will be retried on refresh
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
     * Returns null to indicate no thumbnail - UI will show text instead
     */
    private static Image createPlaceholderImage() {
        // Return null instead of creating a placeholder image
        // The UI will display "No thumbnail generated" text
        return null;
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
                final boolean[] snapshotTaken = { false };
                final MediaView[] mediaViewHolder = { null };

                mediaPlayer.setOnReady(() -> {
                    try {
                        // Create MediaView only when ready
                        MediaView mediaView = new MediaView(finalMediaPlayer);
                        mediaView.setFitWidth(THUMBNAIL_SIZE);
                        mediaView.setFitHeight(THUMBNAIL_SIZE);
                        mediaView.setPreserveRatio(true); // Don't squeeze, will be cropped in display

                        // Apply rotation if needed
                        int rotation = MediaMetadataUtils.getRotation(file);
                        mediaView.setRotate(rotation);

                        mediaViewHolder[0] = mediaView;

                        // Seek to 1 second or 10% of duration to avoid black intro frames
                        javafx.util.Duration duration = finalMediaPlayer.getTotalDuration();
                        javafx.util.Duration seekTime;
                        if (duration != null && duration.toSeconds() > 2) {
                            // Seek to 10% of video or 1 second, whichever is less
                            double seekSeconds = Math.min(duration.toSeconds() * 0.1, 1.0);
                            seekTime = javafx.util.Duration.seconds(seekSeconds);
                        } else {
                            // Short video, seek to 0.5 seconds
                            seekTime = javafx.util.Duration.seconds(0.5);
                        }

                        finalMediaPlayer.seek(seekTime);

                        // Wait a bit after seeking for frame to load, then take snapshot
                        Platform.runLater(() -> {
                            // Add small delay to ensure frame is rendered after seek
                            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(
                                    javafx.util.Duration.millis(200));
                            delay.setOnFinished(e -> {
                                try {
                                    if (!snapshotTaken[0] && mediaViewHolder[0] != null) {
                                        snapshotTaken[0] = true;
                                        SnapshotParameters params = new SnapshotParameters();
                                        params.setFill(javafx.scene.paint.Color.BLACK);
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
                            delay.play();
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
                javafx.animation.PauseTransition timeout = new javafx.animation.PauseTransition(
                        javafx.util.Duration.seconds(4));
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
     * Forces immediate shutdown of all thumbnail generation threads
     */
    public static void shutdown() {

        try {
            // Try graceful shutdown first
            thumbnailExecutor.shutdown();

            // Wait up to 2 seconds for tasks to complete
            if (!thumbnailExecutor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {

                // Force shutdown if tasks don't complete
                thumbnailExecutor.shutdownNow();

                // Wait a bit more for forced shutdown
                if (!thumbnailExecutor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
                    System.err.println("Thumbnail executor did not terminate");
                }
            }

        } catch (InterruptedException e) {
            System.err.println("Shutdown interrupted, forcing...");
            thumbnailExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
