package com.example.pixz;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

/**
 * VideoControlsBar — reusable video HUD with seek bar, play/pause, volume, mute, time, loop.
 * Includes aspect-ratio binding for MediaView and buffering overlay.
 */
public class VideoControlsBar {

    private final MediaController mediaController;
    private final VBox root;
    
    // Controls
    private Slider seekSlider;
    private Button playPauseButton;
    private Slider volumeSlider;
    private Button muteButton;
    private Label timeLabel;
    private Button loopButton;
    
    // Buffering overlay
    private Label bufferingLabel;
    
    // State tracking
    private boolean seekbarInteracted = false;
    private boolean[] isUpdatingFromPlayer = {false}; // Guard against feedback loops
    private boolean isUserSeeking = false; // Flag to block updates during seek operations
    
    // Frame seeking (YouTube-like smooth seeking)
    private boolean isFrameSeeking = false;
    private Timeline frameSeekTimeline;
    private static final double FRAME_STEP = 0.04; // ~25 FPS (40ms per frame)
    private double frameSeekPosition = -1; // Internal position tracker for smooth seeking
    private long lastSeekUpdate = 0; // Throttle actual MediaPlayer seeks
    private boolean isSeekInProgress = false; // Prevent overlapping seeks
    
    // Auto-hide
    private PauseTransition idleTimer;
    
    // Current rotation for sizing calculations
    private int currentRotation = 0;

    public VideoControlsBar(MediaController mediaController) {
        this.mediaController = mediaController;
        
        this.root = buildUI();
        setupAutoHide();
    }

    private VBox buildUI() {
        VBox container = new VBox();
        container.setAlignment(Pos.BOTTOM_CENTER);
        container.setFillWidth(true);
        container.setPickOnBounds(false);
        
        // Seek bar container with hover effect
        StackPane seekBarContainer = new StackPane();
        seekBarContainer.setPrefHeight(8);
        seekBarContainer.setMaxHeight(8);
        seekBarContainer.setStyle("-fx-background-color: transparent;");
        
        seekSlider = new Slider(0, 100, 0);
        seekSlider.getStyleClass().add("seek-slider");
        seekSlider.setMaxWidth(Double.MAX_VALUE);
        seekSlider.setPrefHeight(8);
        seekSlider.setFocusTraversable(true); // Make seekbar focusable

        // Seek slider interaction
        seekSlider.setOnMousePressed(e -> {
            isUserSeeking = true;
            seekbarInteracted = true;
            seekSlider.requestFocus(); // Request focus when clicked
        });
        seekSlider.setOnMouseReleased(e -> {
            seekbarInteracted = false;
            if (mediaController.getMediaPlayer() != null) {
                double totalDuration = mediaController.getTotalDurationSeconds();
                if (totalDuration > 0) {
                    double seekTime = (seekSlider.getValue() / 100.0) * totalDuration;
                    mediaController.seekTo(seekTime);
                }
            }
            // Delay resetting the seeking flag to prevent snap-back (increased to 400ms)
            PauseTransition delay = new PauseTransition(Duration.millis(400));
            delay.setOnFinished(evt -> isUserSeeking = false);
            delay.play();
        });
        
        seekBarContainer.getChildren().add(seekSlider);
        
        // Controls bar
        HBox controlsBar = new HBox(10);
        controlsBar.setAlignment(Pos.CENTER_LEFT);
        controlsBar.setPadding(new Insets(15, 20, 15, 20));
        controlsBar.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8);");
        
        // Play/Pause button
        playPauseButton = new Button("▶");
        playPauseButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand;");
        playPauseButton.setOnAction(e -> mediaController.togglePlayPause());
        
        // Volume slider
        volumeSlider = new Slider(0.0, 1.0, 1.0);
        volumeSlider.getStyleClass().add("volume-slider");
        volumeSlider.setPrefWidth(80);
        volumeSlider.setMaxWidth(80);
        
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!isUpdatingFromPlayer[0]) {
                mediaController.setVolume(newVal.doubleValue());
            }
        });
        
        // Mute button
        muteButton = new Button("🔊");
        muteButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand;");
        muteButton.setOnAction(e -> {
            mediaController.toggleMute();
            updateMuteButton();
        });
        
        // Time label
        timeLabel = new Label("00:00 / 00:00");
        timeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
        
        // Loop button
        loopButton = new Button("🔁");
        loopButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand;");
        loopButton.setOnAction(e -> toggleLoop());
        
        // Spacers
        Region leftSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);
        
        Region rightSpacer = new Region();
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);
        
        controlsBar.getChildren().addAll(
            playPauseButton,
            volumeSlider,
            muteButton,
            leftSpacer,
            timeLabel,
            rightSpacer,
            loopButton
        );
        
        container.getChildren().addAll(seekBarContainer, controlsBar);
        
        // FIX #2: Buffering overlay
        bufferingLabel = new Label("⏳ Buffering...");
        bufferingLabel.setStyle(
            "-fx-text-fill: white; " +
            "-fx-font-size: 16px; " +
            "-fx-background-color: rgba(0, 0, 0, 0.6); " +
            "-fx-padding: 10px 20px; " +
            "-fx-background-radius: 5px;"
        );
        bufferingLabel.setVisible(false);
        
        return container;
    }

    private void setupAutoHide() {
        // Idle timer: 3 seconds
        idleTimer = new PauseTransition(Duration.seconds(3));
        idleTimer.setOnFinished(e -> {
            // Just hide without fade to prevent flickering
            root.setVisible(false);
            root.setMouseTransparent(true);
        });
        
        root.setVisible(true);
        root.setMouseTransparent(false);
    }

    private void fadeOutControls() {
        // Simplified - just hide
        root.setVisible(false);
        root.setMouseTransparent(true);
    }

    private void fadeInControls() {
        // Simplified - just show
        root.setVisible(true);
        root.setMouseTransparent(false);
    }

    public void resetIdleTimer() {
        fadeInControls();
        idleTimer.playFromStart();
    }

    /**
     * Bind all listeners to the current MediaPlayer.
     * Call this after loadVideo completes successfully.
     */
    public void bindToPlayer() {
        MediaPlayer player = mediaController.getMediaPlayer();
        if (player == null) return;
        
        // Enable loop by default for videos
        player.setCycleCount(MediaPlayer.INDEFINITE);
        
        // Update play/pause button
        player.statusProperty().addListener((obs, oldStatus, newStatus) -> 
            Platform.runLater(this::updatePlayPauseButton)
        );
        
        // Update seek slider and time label
        player.currentTimeProperty().addListener((obs, oldTime, newTime) -> 
            Platform.runLater(() -> {
                // Block updates during user seeking (mouse, keyboard, or frame seeking)
                if (!isUserSeeking && !seekbarInteracted && !isFrameSeeking) {
                    updateSeekSlider();
                    updateTimeLabel();
                }
            })
        );
        
        // Update volume slider
        player.volumeProperty().addListener((obs, oldVol, newVol) -> 
            Platform.runLater(() -> {
                isUpdatingFromPlayer[0] = true;
                volumeSlider.setValue(newVol.doubleValue());
                isUpdatingFromPlayer[0] = false;
                updateMuteButton();
            })
        );
        
        // Initial state
        Platform.runLater(() -> {
            updatePlayPauseButton();
            updateMuteButton();
            updateLoopButton();
            isUpdatingFromPlayer[0] = true;
            volumeSlider.setValue(player.getVolume());
            isUpdatingFromPlayer[0] = false;
        });
    }

    /**
     * Remove all listeners before disposal.
     */
    public void unbindFromPlayer() {
        // JavaFX listeners are weak references, but we reset UI state
        Platform.runLater(() -> {
            seekSlider.setValue(0);
            timeLabel.setText("00:00 / 00:00");
            playPauseButton.setText("▶");
            volumeSlider.setValue(1.0);
            muteButton.setText("🔊");
        });
    }

    private void updatePlayPauseButton() {
        if (mediaController.isPlaying()) {
            playPauseButton.setText("⏸");
        } else {
            playPauseButton.setText("▶");
        }
    }

    private void updateSeekSlider() {
        double current = mediaController.getCurrentTimeSeconds();
        double total = mediaController.getTotalDurationSeconds();
        if (total > 0) {
            double percentage = (current / total) * 100.0;
            seekSlider.setValue(percentage);
        }
    }

    private void updateTimeLabel() {
        double current = mediaController.getCurrentTimeSeconds();
        double total = mediaController.getTotalDurationSeconds();
        
        String currentStr = formatTime(current);
        String totalStr = formatTime(total);
        
        timeLabel.setText(currentStr + " / " + totalStr);
    }

    private String formatTime(double seconds) {
        if (seconds < 0) return "00:00";
        
        int totalSecs = (int) seconds;
        int hours = totalSecs / 3600;
        int minutes = (totalSecs % 3600) / 60;
        int secs = totalSecs % 60;
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format("%02d:%02d", minutes, secs);
        }
    }

    private void updateMuteButton() {
        if (mediaController.isMuted()) {
            muteButton.setText("🔇");
        } else {
            muteButton.setText("🔊");
        }
    }

    private void toggleLoop() {
        MediaPlayer player = mediaController.getMediaPlayer();
        if (player != null) {
            int currentCount = player.getCycleCount();
            if (currentCount == MediaPlayer.INDEFINITE) {
                player.setCycleCount(1);
            } else {
                player.setCycleCount(MediaPlayer.INDEFINITE);
            }
            updateLoopButton();
        }
    }

    private void updateLoopButton() {
        MediaPlayer player = mediaController.getMediaPlayer();
        if (player != null && player.getCycleCount() == MediaPlayer.INDEFINITE) {
            loopButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #89b4fa; -fx-cursor: hand;");
        } else {
            loopButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand;");
        }
    }

    // FIX #2: Buffering overlay methods
    public void showBuffering() {
        Platform.runLater(() -> bufferingLabel.setVisible(true));
    }

    public void hideBuffering() {
        Platform.runLater(() -> bufferingLabel.setVisible(false));
    }

    public Label getBufferingLabel() {
        return bufferingLabel;
    }

    public VBox getRoot() {
        return root;
    }

    public boolean isSeekbarFocused() {
        return seekSlider != null && seekSlider.isFocused();
    }
    
    /**
     * Perform keyboard seek: immediately updates the UI to target position,
     * then tells the player to seek. The isUserSeeking flag blocks the player's
     * currentTimeProperty listener so the slider stays at the target position
     * rather than snapping back to the pre-seek time.
     */
    public void seekWithKeyboard(double seconds) {
        isUserSeeking = true;

        // Clamp to valid range
        double totalDuration = mediaController.getTotalDurationSeconds();
        double clampedTime = Math.max(0, Math.min(totalDuration, seconds));

        // Immediately update the UI to the target position (prevents snap-back)
        updateSeekUI(clampedTime, totalDuration);

        // Tell the player to seek
        mediaController.seekTo(clampedTime);

        // Reset the flag after the player has had time to process the seek
        PauseTransition delay = new PauseTransition(Duration.millis(500));
        delay.setOnFinished(evt -> {
            isUserSeeking = false;
            // Sync UI once more with the actual player position
            updateSeekSlider();
            updateTimeLabel();
        });
        delay.play();
    }

    public void setVisible(boolean visible) {
        root.setVisible(visible);
    }

    public void setOpacity(double opacity) {
        root.setOpacity(opacity);
    }

    public void setMouseTransparent(boolean transparent) {
        root.setMouseTransparent(transparent);
    }
    
    /**
     * Start smooth frame-by-frame seeking (YouTube-like behavior).
     * @param direction +1 for forward, -1 for backward
     */
    public void startFrameSeek(double direction) {
        // Stop any existing frame seek timeline
        if (frameSeekTimeline != null) {
            frameSeekTimeline.stop();
        }
        
        isFrameSeeking = true;
        
        // Initialize internal seek position ONLY once when starting
        if (frameSeekPosition < 0) {
            frameSeekPosition = mediaController.getCurrentTimeSeconds();
        }
        
        double totalDuration = mediaController.getTotalDurationSeconds();
        
        // Create timeline that runs every ~40ms (25 FPS)
        frameSeekTimeline = new Timeline(new KeyFrame(Duration.millis(40), e -> {
            // Update internal position (DO NOT read from MediaPlayer)
            frameSeekPosition += (FRAME_STEP * direction);
            
            // Clamp to valid range
            frameSeekPosition = Math.max(0, Math.min(totalDuration, frameSeekPosition));
            
            // Throttle MediaPlayer seeks to ~120ms + prevent overlapping seeks
            long now = System.currentTimeMillis();
            if (!isSeekInProgress && now - lastSeekUpdate > 120) {
                isSeekInProgress = true;
                mediaController.seekTo(frameSeekPosition);
                lastSeekUpdate = now;
                
                // Unlock after small delay (let player process)
                PauseTransition unlock = new PauseTransition(Duration.millis(100));
                unlock.setOnFinished(evt -> isSeekInProgress = false);
                unlock.play();
            }
            
            // Update UI every frame (instant smooth feedback)
            updateSeekUI(frameSeekPosition, totalDuration);
        }));
        
        frameSeekTimeline.setCycleCount(Animation.INDEFINITE);
        frameSeekTimeline.play();
    }
    
    /**
     * Stop smooth frame seeking.
     */
    public void stopFrameSeek() {
        if (frameSeekTimeline != null) {
            frameSeekTimeline.stop();
            frameSeekTimeline = null;
        }
        isFrameSeeking = false;
        
        // Force final accurate seek to sync player with UI
        if (frameSeekPosition >= 0) {
            mediaController.seekTo(frameSeekPosition);
        }
        
        // Reset state
        frameSeekPosition = -1;
        isSeekInProgress = false;
        lastSeekUpdate = 0;
    }
    
    /**
     * Update seek UI manually during frame seeking.
     * @param time Current time in seconds
     * @param totalDuration Total duration in seconds
     */
    private void updateSeekUI(double time, double totalDuration) {
        // Update seekbar position
        if (totalDuration > 0) {
            double percentage = (time / totalDuration) * 100.0;
            seekSlider.setValue(percentage);
        }
        
        // Update time label
        String currentStr = formatTime(time);
        String totalStr = formatTime(totalDuration);
        timeLabel.setText(currentStr + " / " + totalStr);
    }
    
    /**
     * Update video fit based on container size and rotation.
     * Implements contain behavior (always fully visible, no cropping).
     * 
     * IMPORTANT: Videos with rotation metadata are stored rotated in the file.
     * We only swap dimensions for sizing, but DON'T apply visual rotation.
     * Visual rotation is only applied for manual user rotation (R key).
     * 
     * @param mediaView The actual MediaView being displayed
     * @param containerW Container width
     * @param containerH Container height
     * @param rotation Current rotation angle (0, 90, 180, 270) - for dimension calculation only
     */
    public void updateVideoFit(MediaView mediaView, double containerW, double containerH, int rotation) {
        if (mediaView == null) {
            return;
        }
        
        if (containerW <= 0 || containerH <= 0) {
            return;
        }
        
        MediaPlayer player = mediaController.getMediaPlayer();
        if (player == null || player.getMedia() == null) {
            return;
        }
        
        double mediaW = player.getMedia().getWidth();
        double mediaH = player.getMedia().getHeight();
        
        if (mediaW <= 0 || mediaH <= 0) {
            return;
        }
        
        // CRITICAL: Clear viewport to prevent cropping
        mediaView.setViewport(null);
        
        // Get current visual rotation (from manual rotation, not metadata)
        double currentVisualRotation = mediaView.getRotate();
        
        // Use actual stored dimensions (video file dimensions)
        double calcMediaW = mediaW;
        double calcMediaH = mediaH;
        
        // For manual rotation: swap CONTAINER dimensions, not media dimensions
        // This is because the MediaView is rotated, so it sees the container as rotated
        double calcContainerW = containerW;
        double calcContainerH = containerH;
        
        boolean manuallyRotated = (((int)currentVisualRotation) % 180 != 0);
        if (manuallyRotated) {
            // Swap container dimensions
            double temp = calcContainerW;
            calcContainerW = calcContainerH;
            calcContainerH = temp;
        }
        
        // CONTAIN: fit inside container, no cropping
        double scaleX = calcContainerW / calcMediaW;
        double scaleY = calcContainerH / calcMediaH;
        double scale = Math.min(scaleX, scaleY);
        
        // Calculate the actual display size
        double displayW = calcMediaW * scale;
        double displayH = calcMediaH * scale;
        
        // Set BOTH dimensions explicitly for contain behavior
        mediaView.setFitWidth(displayW);
        mediaView.setFitHeight(displayH);
        
        System.out.println("[updateVideoFit] Media: " + mediaW + "x" + mediaH + 
                         ", Visual rotation: " + currentVisualRotation +
                         ", Container (original): " + containerW + "x" + containerH +
                         ", Container (calc): " + calcContainerW + "x" + calcContainerH +
                         ", Scale: " + scale +
                         ", Display size: " + displayW + "x" + displayH);
    }
    
    /**
     * Set current rotation for sizing calculations.
     * @param rotation Rotation angle (0, 90, 180, 270)
     */
    public void setRotation(int rotation) {
        this.currentRotation = rotation;
    }
}
