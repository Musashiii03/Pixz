package com.example.pixz;

import java.io.File;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

/**
 * MediaController — owns and manages the single active MediaPlayer instance.
 * Handles lifecycle, loading, playback control, volume, and freeze detection.
 */
public class MediaController {

    @FunctionalInterface
    interface TriConsumer<A, B, C> {
        void accept(A a, B b, C c);
    }

    private final MediaView mediaView;
    private MediaPlayer currentPlayer;
    private boolean isLoading = false;
    private volatile boolean isDisposed = false;
    private volatile long activeSessionId = 0;  // Track which session this player belongs to
    
    // Watchdog for freeze detection
    private Timeline watchdog;
    private double lastWatchdogTime = 0.0;
    
    // Volume state
    private double volumeBeforeMute = 1.0;

    public MediaController(MediaView mediaView) {
        this.mediaView = mediaView;
    }

    /**
     * Load a new video. Disposes previous player automatically.
     * isLoading guard: if already loading, the call is ignored.
     * Adds 150ms PauseTransition delay before creating new player (pipeline safety).
     */
    public void loadVideo(
            File videoFile,
            Runnable onReady,
            TriConsumer<String, String, String> onError,
            Runnable onStalled,
            Runnable onPlaying
    ) {
        // Strong load lock: prevent concurrent loads
        if (isLoading) {
            return;
        }
        
        disposeCurrentPlayer();
        isLoading = true;
        isDisposed = false; // Reset disposed flag for new load
        activeSessionId = System.currentTimeMillis(); // New session for this load
        final long currentSessionId = activeSessionId;
        final long loadStartTime = System.currentTimeMillis();
        mediaView.setMediaPlayer(null);
        
        // Hard timeout recovery: if still loading after 4 seconds, trigger error
        Thread timeoutThread = new Thread(() -> {
            try {
                Thread.sleep(4000);
                
                if (isLoading && !isDisposed && activeSessionId == currentSessionId) {
                    Platform.runLater(() -> {
                        if (isLoading && !isDisposed) {
                            isLoading = false;
                            disposeCurrentPlayer();
                            onError.accept(
                                "Playback Timeout",
                                "Video failed to initialize within 4 seconds. The file may be corrupted or use an unsupported codec.",
                                videoFile.getAbsolutePath()
                            );
                        }
                    });
                }
            } catch (InterruptedException ignored) {
            }
        });
        timeoutThread.setDaemon(true);
        timeoutThread.start();

        // 150ms delay for pipeline safety
        PauseTransition delay = new PauseTransition(Duration.millis(150));
        delay.setOnFinished(e -> {
            try {
                Media media = new Media(videoFile.toURI().toString());
                MediaPlayer player = new MediaPlayer(media);
                currentPlayer = player;
                
                // Force pipeline initialization (helps with GPU decode stability)
                player.setRate(1.0);
                player.setVolume(player.getVolume());

                // FIX #2: Stalled/Playing callbacks with disposed check
                player.setOnStalled(() -> {
                    if (isDisposed || activeSessionId != currentSessionId) return;
                    Platform.runLater(onStalled);
                });
                player.setOnPlaying(() -> {
                    if (isDisposed || activeSessionId != currentSessionId) return;
                    Platform.runLater(onPlaying);
                });

                // Ready callback with first-frame lag fix
                player.setOnReady(() -> {
                    if (isDisposed || activeSessionId != currentSessionId) return;
                    
                    Platform.runLater(() -> {
                        if (isDisposed || activeSessionId != currentSessionId) return;
                        
                        // Attach MediaView BEFORE play
                        mediaView.setMediaPlayer(player);
                        isLoading = false;
                        
                        // Force initial frame render (critical for first-frame lag)
                        player.seek(Duration.ZERO);
                        
                        // Small delay before play to let renderer stabilize
                        PauseTransition playDelay = new PauseTransition(Duration.millis(120));
                        playDelay.setOnFinished(evt -> {
                            if (isDisposed || activeSessionId != currentSessionId) return;
                            
                            player.play();
                            onReady.run();
                            startWatchdog();
                        });
                        playDelay.play();
                    });
                });

                // FIX #2: Ignore early errors before player is ready
                player.setOnError(() -> {
                    if (isDisposed || activeSessionId != currentSessionId) return;
                    
                    // Only process error if player was actually initialized (not in early loading phase)
                    if (player.getStatus() == MediaPlayer.Status.UNKNOWN) {
                        // Ignore early initialization errors
                        return;
                    }
                    
                    MediaException error = player.getError();
                    String errorMessage = error != null ? error.getMessage() : "Unknown error";
                    if (error != null) {
                        error.printStackTrace();
                    }

                    boolean isCodecError = errorMessage.contains("ERROR_MEDIA_INVALID") ||
                            errorMessage.contains("ERROR_MEDIA_UNSUPPORTED") ||
                            errorMessage.contains("MEDIA_UNSUPPORTED");

                    String title = isCodecError ? "Unsupported Video Format" : "Cannot Play Video";
                    String detail = isCodecError
                            ? "This video uses a codec JavaFX cannot decode (likely not H.264).\nTry converting to H.264 MP4."
                            : "The video may be corrupt, locked, or in an unsupported container.\nError: " + errorMessage;
                    String filePath = videoFile.getAbsolutePath();

                    isLoading = false;
                    onError.accept(title, detail, filePath);
                });

            } catch (Exception ex) {
                isLoading = false;
                onError.accept("Cannot Load Video", "Failed to create media player: " + ex.getMessage(), videoFile.getAbsolutePath());
                ex.printStackTrace();
            }
        });
        delay.play();
    }

    /**
     * FIX #1: Watchdog detects freeze via time-progression comparison.
     * Runs continuously during playback, not just at start.
     * FIX #4: Run every 1 second instead of 2 for faster detection.
     */
    private void startWatchdog() {
        if (watchdog != null) {
            watchdog.stop();
        }
        lastWatchdogTime = -1.0; // Sentinel: not yet initialized

        // FIX #4: Run every 1 second instead of 2
        watchdog = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (isDisposed) return; // Check if disposed
            if (currentPlayer == null) return;
            if (currentPlayer.getStatus() != MediaPlayer.Status.PLAYING) return;

            double current = currentPlayer.getCurrentTime().toSeconds();

            if (lastWatchdogTime >= 0) {
                double delta = Math.abs(current - lastWatchdogTime);
                // If time hasn't progressed in 1 second
                if (delta < 0.05) {
                    // Time hasn't progressed — video is frozen
                    MediaMetadataUtils.logDebug("Freeze detected (delta=" + delta + "s). Resetting MediaView visibility.");
                    if (mediaView != null) {
                        Platform.runLater(() -> {
                            if (isDisposed) return; // Double-check before UI update
                            mediaView.setVisible(false);
                            mediaView.setVisible(true);
                        });
                    }
                }
            }
            lastWatchdogTime = current;
        }));
        watchdog.setCycleCount(Timeline.INDEFINITE); // Run continuously
        watchdog.play();
    }

    /**
     * Dispose current MediaPlayer: stop() → dispose() → null out reference.
     * Also sets MediaView's player to null before disposal.
     * Also stops and nulls the watchdog Timeline.
     * Resets isLoading to false.
     * Sets isDisposed flag to block pending callbacks.
     */
    public void disposeCurrentPlayer() {
        isDisposed = true; // Set disposed flag FIRST
        isLoading = false;
        lastWatchdogTime = 0.0;
        
        if (watchdog != null) {
            watchdog.stop();
            watchdog = null;
        }
        
        if (currentPlayer != null) {
            currentPlayer.stop();
            currentPlayer.dispose();
            currentPlayer = null;
        }
        
        if (mediaView != null) {
            mediaView.setMediaPlayer(null);
        }
    }

    public void play() {
        if (currentPlayer != null) {
            currentPlayer.play();
        }
    }

    public void pause() {
        if (currentPlayer != null) {
            currentPlayer.pause();
        }
    }

    public void togglePlayPause() {
        if (currentPlayer != null) {
            if (currentPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                currentPlayer.pause();
            } else {
                currentPlayer.play();
            }
        }
    }

    public void stop() {
        if (currentPlayer != null) {
            currentPlayer.stop();
        }
    }

    public void seekTo(double seconds) {
        if (currentPlayer != null) {
            currentPlayer.seek(Duration.seconds(seconds));
        }
    }

    public void setVolume(double volume) {
        if (currentPlayer != null) {
            currentPlayer.setVolume(Math.max(0.0, Math.min(1.0, volume)));
        }
    }

    public double getVolume() {
        return currentPlayer != null ? currentPlayer.getVolume() : 0.0;
    }

    public void toggleMute() {
        if (currentPlayer != null) {
            double currentVolume = currentPlayer.getVolume();
            if (currentVolume > 0) {
                volumeBeforeMute = currentVolume;
                currentPlayer.setVolume(0.0);
            } else {
                currentPlayer.setVolume(volumeBeforeMute);
            }
        }
    }

    public boolean isMuted() {
        return currentPlayer != null && currentPlayer.getVolume() == 0.0;
    }

    public MediaPlayer getMediaPlayer() {
        return currentPlayer;
    }

    public double getCurrentTimeSeconds() {
        if (currentPlayer != null && currentPlayer.getCurrentTime() != null) {
            return currentPlayer.getCurrentTime().toSeconds();
        }
        return -1;
    }

    public double getTotalDurationSeconds() {
        if (currentPlayer != null && currentPlayer.getTotalDuration() != null) {
            return currentPlayer.getTotalDuration().toSeconds();
        }
        return -1;
    }

    public boolean isPlaying() {
        return currentPlayer != null && currentPlayer.getStatus() == MediaPlayer.Status.PLAYING;
    }

    public boolean isReady() {
        return currentPlayer != null && currentPlayer.getStatus() == MediaPlayer.Status.READY;
    }

    public boolean isCurrentlyLoading() {
        return isLoading;
    }
}
