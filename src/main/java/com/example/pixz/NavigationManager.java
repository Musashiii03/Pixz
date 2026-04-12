package com.example.pixz;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * NavigationManager — handles image ↔ video switching with proper cleanup.
 * Manages navigation, keyboard input, and metadata preloading.
 */
public class NavigationManager {

    private final List<MediaItem> displayedItems;
    private final StackPane fullscreenViewer;
    private final MediaController mediaController;
    private final VideoControlsBar controlsBar;
    private final Runnable onTopBarRebuild;
    private final Runnable onClose;
    private final Runnable onToggleFullscreen;
    private final java.util.function.BooleanSupplier isViewerActive;
    private final long sessionId;  // Session ID for this viewer instance
    private final GalleryController galleryController;  // Reference to validate session

    private int currentIndex = 0;
    private long lastSwitchTime = 0;
    private static final long THROTTLE_MS = 200;

    // Current container and rotatable content for rotation support
    private StackPane currentContainer;
    private javafx.scene.Node currentRotatableContent; // ImageView or MediaView
    private double currentRotation = 0;

    public NavigationManager(
            List<MediaItem> displayedItems,
            StackPane fullscreenViewer,
            MediaController mediaController,
            VideoControlsBar controlsBar,
            Runnable onTopBarRebuild,
            Runnable onClose,
            Runnable onToggleFullscreen,
            java.util.function.BooleanSupplier isViewerActive,
            long sessionId,
            GalleryController galleryController
    ) {
        this.displayedItems = displayedItems;
        this.fullscreenViewer = fullscreenViewer;
        this.mediaController = mediaController;
        this.controlsBar = controlsBar;
        this.onTopBarRebuild = onTopBarRebuild;
        this.onClose = onClose;
        this.onToggleFullscreen = onToggleFullscreen;
        this.isViewerActive = isViewerActive;
        this.sessionId = sessionId;
        this.galleryController = galleryController;
    }
    
    /**
     * Validate that this session is still active.
     * Returns false if the viewer has been closed and reopened.
     */
    private boolean isValidSession() {
        return sessionId == galleryController.getFullscreenSessionId();
    }

    public void setCurrentIndex(int index) {
        this.currentIndex = index;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public MediaItem getCurrentItem() {
        if (currentIndex >= 0 && currentIndex < displayedItems.size()) {
            return displayedItems.get(currentIndex);
        }
        return null;
    }

    public void navigateNext() {
        if (currentIndex < displayedItems.size() - 1) {
            currentIndex++;
            switchToMedia(displayedItems.get(currentIndex));
        }
    }

    public void navigatePrevious() {
        if (currentIndex > 0) {
            currentIndex--;
            switchToMedia(displayedItems.get(currentIndex));
        }
    }

    /**
     * Main switching logic: handles image ↔ video transitions with proper cleanup.
     */
    public void switchToMedia(MediaItem item) {
        // Validate session before proceeding
        if (!isValidSession()) {
            return;
        }
        
        // Throttle navigation
        long now = System.currentTimeMillis();
        if (now - lastSwitchTime < THROTTLE_MS) {
            return;
        }
        lastSwitchTime = now;

        // Save last playback position
        MediaItem currentItem = getCurrentItem();
        if (currentItem != null && currentItem.getType() == MediaItem.MediaType.VIDEO) {
            double currentTime = mediaController.getCurrentTimeSeconds();
            if (currentTime > 0) {
                currentItem.setLastPlaybackPosition(currentTime);
            }
        }

        // Reset rotation
        currentRotation = 0;

        if (item.getType() == MediaItem.MediaType.IMAGE) {
            switchToImage(item);
        } else {
            switchToVideo(item);
        }

        // Rebuild top bar AFTER switching to ensure it's on top
        Platform.runLater(() -> {
            onTopBarRebuild.run();
        });

        // Request focus
        Platform.runLater(() -> fullscreenViewer.requestFocus());

        // FIX #6: Preload next video metadata
        preloadNextVideoMetadata();
    }

    private void switchToImage(MediaItem item) {
        // Dispose video player
        mediaController.disposeCurrentPlayer();
        controlsBar.unbindFromPlayer();

        // Clear viewer
        fullscreenViewer.getChildren().clear();

        // Build image container
        StackPane imageContainer = new StackPane();
        imageContainer.setStyle("-fx-background-color: black;");
        imageContainer.setAlignment(Pos.CENTER);

        ImageView imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.fitWidthProperty().bind(fullscreenViewer.widthProperty());
        imageView.fitHeightProperty().bind(fullscreenViewer.heightProperty());

        // Load image
        try {
            Image image = new Image(item.getFile().toURI().toString(), true);
            imageView.setImage(image);
        } catch (Exception e) {
            Label errorLabel = new Label("Cannot load image:\n" + e.getMessage());
            errorLabel.setTextFill(Color.WHITE);
            errorLabel.setFont(Font.font(14));
            imageContainer.getChildren().add(errorLabel);
        }

        imageContainer.getChildren().add(imageView);
        currentContainer = imageContainer;
        currentRotatableContent = imageView; // Track ImageView for rotation

        // Add to viewer
        fullscreenViewer.getChildren().add(imageContainer);

        // Hide controls
        controlsBar.setVisible(false);

        // Add mouse movement handler for top bar auto-hide
        final javafx.animation.PauseTransition[] hideTimer = {null};
        imageContainer.setOnMouseMoved(evt -> {
            // Show top bar and cursor
            imageContainer.setCursor(javafx.scene.Cursor.DEFAULT);
            fullscreenViewer.getChildren().stream()
                .filter(node -> node instanceof HBox)
                .forEach(node -> {
                    node.setVisible(true);
                    node.setOpacity(1.0);
                });
            
            // Reset hide timer
            if (hideTimer[0] != null) {
                hideTimer[0].stop();
            }
            hideTimer[0] = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(3));
            hideTimer[0].setOnFinished(hideEvt -> {
                // Hide top bar and cursor after 3 seconds of no movement
                imageContainer.setCursor(javafx.scene.Cursor.NONE);
                fullscreenViewer.getChildren().stream()
                    .filter(node -> node instanceof HBox)
                    .forEach(node -> node.setOpacity(0.0));
            });
            hideTimer[0].play();
        });

        // Initial hide after 3 seconds
        javafx.animation.PauseTransition initialHide = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(3));
        initialHide.setOnFinished(initHideEvt -> {
            imageContainer.setCursor(javafx.scene.Cursor.NONE);
            fullscreenViewer.getChildren().stream()
                .filter(node -> node instanceof HBox)
                .forEach(node -> node.setOpacity(0.0));
        });
        initialHide.play();
    }

    private void switchToVideo(MediaItem item) {
        // Unbind previous player
        controlsBar.unbindFromPlayer();
        
        // FIX #3: Ensure previous player fully disposed
        mediaController.disposeCurrentPlayer();

        // Clear viewer and show loading spinner
        fullscreenViewer.getChildren().clear();
        StackPane loadingSpinner = createLoadingSpinner();
        fullscreenViewer.getChildren().add(loadingSpinner);

        // FIX #3: Add 150ms delay to prevent race condition
        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(150));
        delay.setOnFinished(e -> {
            // 🔥 CRITICAL GUARD - Validate session BEFORE loadVideo
            if (!isValidSession() || !isViewerActive.getAsBoolean()) {
                return;
            }
            
            // Load video
            File videoFile = item.getFile();
            mediaController.loadVideo(
                videoFile,
                // onReady
                () -> {
                    // Validate session before any UI updates
                    if (!isValidSession()) {
                        return;
                    }
                    
                    // Check if viewer is still active (not being closed)
                    if (!isViewerActive.getAsBoolean()) {
                        return;
                    }
                    
                    // Guard against loading spinner already removed
                    if (!fullscreenViewer.getChildren().contains(loadingSpinner)) {
                        return;
                    }
                    
                    fullscreenViewer.getChildren().remove(loadingSpinner);
                    
                    // Create video container that fills the fullscreen viewer
                    StackPane videoContainer = new StackPane();
                    videoContainer.setStyle("-fx-background-color: black;");
                    videoContainer.setAlignment(Pos.CENTER);
                    // Make container fill the parent
                    videoContainer.prefWidthProperty().bind(fullscreenViewer.widthProperty());
                    videoContainer.prefHeightProperty().bind(fullscreenViewer.heightProperty());

                    // Create MediaView with proper size binding to prevent zoom/crop
                    MediaView mediaView = new MediaView();
                    mediaView.setMediaPlayer(mediaController.getMediaPlayer());
                    mediaView.setPreserveRatio(true);
                    // Bind to fullscreenViewer for proper sizing
                    mediaView.fitWidthProperty().bind(fullscreenViewer.widthProperty());
                    mediaView.fitHeightProperty().bind(fullscreenViewer.heightProperty());

                    // Create overlay for buffering label
                    StackPane overlay = new StackPane();
                    overlay.setAlignment(Pos.CENTER);
                    overlay.setPickOnBounds(false);
                    overlay.getChildren().add(controlsBar.getBufferingLabel());

                    videoContainer.getChildren().addAll(mediaView, overlay, controlsBar.getRoot());
                    currentContainer = videoContainer;
                    currentRotatableContent = mediaView; // Track MediaView for rotation

                    fullscreenViewer.getChildren().add(videoContainer);
                    
                    // Add click-to-pause/play functionality
                    videoContainer.setOnMouseClicked(evt -> {
                        mediaController.togglePlayPause();
                        evt.consume();
                    });

                    // Rebuild top bar to ensure it's on top of video
                    Platform.runLater(() -> {
                        onTopBarRebuild.run();
                    });

                    // Add mouse movement handler for top bar and controls auto-hide (same as images)
                    final javafx.animation.PauseTransition[] hideTimer = {null};
                    videoContainer.setOnMouseMoved(evt -> {
                        // Show cursor
                        videoContainer.setCursor(javafx.scene.Cursor.DEFAULT);
                        
                        // Show top bar
                        fullscreenViewer.getChildren().stream()
                            .filter(node -> node instanceof HBox)
                            .forEach(node -> {
                                node.setVisible(true);
                                node.setOpacity(1.0);
                            });
                        
                        // Show bottom controls
                        controlsBar.resetIdleTimer();
                        
                        // Reset hide timer
                        if (hideTimer[0] != null) {
                            hideTimer[0].stop();
                        }
                        hideTimer[0] = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(3));
                        hideTimer[0].setOnFinished(hideEvt -> {
                            // Hide cursor and top bar after 3 seconds of no movement
                            videoContainer.setCursor(javafx.scene.Cursor.NONE);
                            fullscreenViewer.getChildren().stream()
                                .filter(node -> node instanceof HBox)
                                .forEach(node -> node.setOpacity(0.0));
                        });
                        hideTimer[0].play();
                    });

                    // Initial hide after 3 seconds
                    javafx.animation.PauseTransition initialHide = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(3));
                    initialHide.setOnFinished(initHideEvt -> {
                        videoContainer.setCursor(javafx.scene.Cursor.NONE);
                        fullscreenViewer.getChildren().stream()
                            .filter(node -> node instanceof HBox)
                            .forEach(node -> node.setOpacity(0.0));
                    });
                    initialHide.play();

                    // Bind controls and play
                    controlsBar.bindToPlayer();
                    controlsBar.setVisible(true);
                    controlsBar.resetIdleTimer();
                    mediaController.play();
                },
                // onError
                (title, detail, filePath) -> {
                    // Validate session before any UI updates
                    if (!isValidSession()) {
                        return;
                    }
                    
                    // Check if viewer is still active (not being closed)
                    if (!isViewerActive.getAsBoolean()) {
                        return;
                    }
                    fullscreenViewer.getChildren().remove(loadingSpinner);
                    showVideoErrorUI(title, detail, filePath);
                },
                // onStalled
                () -> {
                    // Validate session before any UI updates
                    if (!isValidSession()) {
                        return;
                    }
                    
                    // Check if viewer is still active (not being closed)
                    if (!isViewerActive.getAsBoolean()) {
                        return;
                    }
                    controlsBar.showBuffering();
                },
                // onPlaying
                () -> {
                    // Validate session before any UI updates
                    if (!isValidSession()) {
                        return;
                    }
                    
                    // Check if viewer is still active (not being closed)
                    if (!isViewerActive.getAsBoolean()) {
                        return;
                    }
                    controlsBar.hideBuffering();
                }
            );
        });
        delay.play();
    }

    private StackPane createLoadingSpinner() {
        StackPane spinner = new StackPane();
        spinner.setStyle("-fx-background-color: black;");
        spinner.setAlignment(Pos.CENTER);

        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);

        ProgressIndicator progress = new ProgressIndicator();
        progress.getStyleClass().add("loading-indicator");
        progress.setPrefSize(60, 60);

        Label label = new Label("Loading video...");
        label.setTextFill(Color.WHITE);
        label.setFont(Font.font(14));

        content.getChildren().addAll(progress, label);
        spinner.getChildren().add(content);

        return spinner;
    }

    private void showVideoErrorUI(String title, String detail, String filePath) {
        VBox errorBox = new VBox(20);
        errorBox.setAlignment(Pos.CENTER);
        errorBox.setStyle("-fx-background-color: black; -fx-padding: 40px;");

        Label titleLabel = new Label(title);
        titleLabel.setTextFill(Color.web("#f38ba8"));
        titleLabel.setFont(Font.font(18));
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(600);

        Label detailLabel = new Label(detail);
        detailLabel.setTextFill(Color.WHITE);
        detailLabel.setFont(Font.font(14));
        detailLabel.setWrapText(true);
        detailLabel.setMaxWidth(600);

        Label pathLabel = new Label("File: " + filePath);
        pathLabel.setTextFill(Color.web("#a6adc8"));
        pathLabel.setFont(Font.font(12));
        pathLabel.setWrapText(true);
        pathLabel.setMaxWidth(600);

        Button openButton = new Button("Open in System Player");
        openButton.setStyle(
            "-fx-background-color: #89b4fa; " +
            "-fx-text-fill: black; " +
            "-fx-font-size: 14px; " +
            "-fx-padding: 10px 20px; " +
            "-fx-cursor: hand;"
        );
        openButton.setOnAction(e -> {
            try {
                java.awt.Desktop.getDesktop().open(new File(filePath));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Button closeButton = new Button("Close");
        closeButton.setStyle(
            "-fx-background-color: #45475a; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 14px; " +
            "-fx-padding: 10px 20px; " +
            "-fx-cursor: hand;"
        );
        closeButton.setOnAction(e -> onClose.run());

        HBox buttons = new HBox(10, openButton, closeButton);
        buttons.setAlignment(Pos.CENTER);

        errorBox.getChildren().addAll(titleLabel, detailLabel, pathLabel, buttons);

        fullscreenViewer.getChildren().clear();
        fullscreenViewer.getChildren().add(errorBox);
        controlsBar.setVisible(false);
    }

    /**
     * FIX #6: Preload next video metadata on background thread.
     */
    private void preloadNextVideoMetadata() {
        int nextIdx = currentIndex + 1;
        if (nextIdx < displayedItems.size()) {
            MediaItem nextItem = displayedItems.get(nextIdx);
            if (nextItem.getType() == MediaItem.MediaType.VIDEO) {
                CompletableFuture.runAsync(() -> {
                    try {
                        // Just construct Media — reads metadata without rendering
                        new Media(nextItem.getFile().toURI().toString());
                        // Discard — purpose is OS file cache warming only
                    } catch (Exception ignored) {
                        // Silently ignore preload failures
                    }
                });
            }
        }
    }

    /**
     * Handle keyboard input for navigation and playback control.
     * Task 13: All keyboard shortcuts verified and implemented.
     * 
     * Supported shortcuts:
     * - SPACE: Play/Pause
     * - LEFT/RIGHT: Seek backward/forward (video) or navigate prev/next (image)
     * - UP/DOWN: Navigate prev/next (both media types)
     * - M: Toggle mute
     * - F/F11: Toggle fullscreen
     * - R: Rotate 90°
     * - ESC: Close viewer
     * - SHIFT+N: Navigate next (always)
     * - SHIFT+P: Navigate prev (always)
     */
    public void handleKeyPress(KeyCode code, boolean isShiftDown) {
        switch (code) {
            case ESCAPE:
                onClose.run();
                break;

            case SPACE:
                mediaController.togglePlayPause();
                break;

            case M:
                mediaController.toggleMute();
                break;

            case F:
            case F11:
                onToggleFullscreen.run();
                break;

            case R:
                rotateCurrentContainer();
                break;

            case RIGHT:
                // RIGHT: Navigate next (image) or seek +15s (video + seekbar focused)
                handleRightArrow();
                break;

            case DOWN:
                // DOWN: Navigate next (both media types)
                navigateNext();
                break;

            case LEFT:
                // LEFT: Navigate prev (image) or seek -15s (video + seekbar focused)
                handleLeftArrow();
                break;

            case UP:
                // UP: Navigate prev (both media types)
                navigatePrevious();
                break;

            case N:
                // SHIFT+N: Navigate next (always)
                if (isShiftDown) {
                    navigateNext();
                }
                break;

            case P:
                // SHIFT+P: Navigate prev (always)
                if (isShiftDown) {
                    navigatePrevious();
                }
                break;

            default:
                // Ignore other keys
                break;
        }
    }
    
    /**
     * Handle key release events for frame seeking.
     * Stops smooth frame seeking when LEFT or RIGHT arrow is released.
     */
    public void handleKeyRelease(KeyCode code) {
        // No-op: arrow key seeks are now instant single-press jumps, no hold-seek to stop
    }

    private void handleRightArrow() {
        MediaItem current = getCurrentItem();
        if (current != null && current.getType() == MediaItem.MediaType.VIDEO) {
            // Video: seek forward 5 seconds
            double newTime = mediaController.getCurrentTimeSeconds() + 5.0;
            controlsBar.seekWithKeyboard(newTime);
        } else {
            // Image: navigate next
            navigateNext();
        }
    }

    private void handleLeftArrow() {
        MediaItem current = getCurrentItem();
        if (current != null && current.getType() == MediaItem.MediaType.VIDEO) {
            // Video: seek backward 5 seconds
            double newTime = mediaController.getCurrentTimeSeconds() - 5.0;
            controlsBar.seekWithKeyboard(newTime);
        } else {
            // Image: navigate previous
            navigatePrevious();
        }
    }

    private void rotateCurrentContainer() {
        if (currentRotatableContent != null) {
            currentRotation = (currentRotation + 90) % 360;
            currentRotatableContent.setRotate(currentRotation);
        }
    }
    
    /**
     * Public method to rotate current content (called from UI buttons).
     */
    public void rotateContent() {
        rotateCurrentContainer();
    }
}
