package com.example.wingallery;

import java.io.File;
import java.util.concurrent.CompletableFuture;

import javafx.application.Platform;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

/**
 * Utility class for generating thumbnails from images and videos
 */
public class ThumbnailGenerator {
    private static final int THUMBNAIL_SIZE = 300; // Larger thumbnails for better visibility

    /**
     * Generate thumbnail for an image file
     * Uses background loading to avoid blocking and reduce memory usage
     * Creates square thumbnails by cropping to center
     */
    public static Image generateImageThumbnail(File file) {
        try {
            // Load at reduced resolution for memory efficiency
            // preserveRatio = false to create square thumbnails
            // smooth = false for faster loading and less memory
            Image image = new Image(file.toURI().toString(), THUMBNAIL_SIZE, THUMBNAIL_SIZE, false, false, true);
            return image;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Generate thumbnail for a video file (extracts first frame)
     * Uses JavaFX MediaPlayer (bundled with app)
     */
    public static CompletableFuture<Image> generateVideoThumbnail(File file) {
        CompletableFuture<Image> future = new CompletableFuture<>();
        
        // Try JavaFX MediaPlayer (bundled with app)
        tryJavaFXThumbnail(file, future);
        
        // Add timeout fallback to placeholder (5 seconds total)
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(5000);
                if (!future.isDone()) {
                    future.complete(createPlaceholderImage());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
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
                        mediaView.setPreserveRatio(false); // Square thumbnails
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
                                        finalMediaPlayer.dispose();
                                    } else {
                                        future.complete(createPlaceholderImage());
                                        finalMediaPlayer.dispose();
                                    }
                                }
                            } catch (Exception ex) {
                                future.complete(createPlaceholderImage());
                                finalMediaPlayer.dispose();
                            }
                        });
                    } catch (Exception e) {
                        finalMediaPlayer.dispose();
                        if (!future.isDone()) {
                            future.complete(createPlaceholderImage());
                        }
                    }
                });
                
                mediaPlayer.setOnError(() -> {
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
                        timeoutPlayer.dispose();
                        future.complete(createPlaceholderImage());
                    }
                });
                timeout.play();
                
            } catch (Exception e) {
                if (mediaPlayer != null) {
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
}
