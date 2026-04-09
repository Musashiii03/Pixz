package com.example.pixz;

import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Popup window for viewing full-size images and videos
 */
public class MediaPopup {
    private final Stage stage;
    private MediaPlayer mediaPlayer;
    private PauseTransition hideControlsTimer;
    private VBox controlsBox;
    private HBox topBar;

    public MediaPopup(MediaItem mediaItem, Stage owner) {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.setTitle(mediaItem.getName());

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #000000;");

        if (mediaItem.getType() == MediaItem.MediaType.IMAGE) {
            setupImageViewer(root, mediaItem);
        } else {
            setupVideoViewer(root, mediaItem);
        }

        Scene scene = new Scene(root, 1000, 700);
        scene.getStylesheets().add(getClass().getResource("dark-theme.css").toExternalForm());

        // Setup auto-hide timer
        setupAutoHideControls(scene);

        // Close on ESC key
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
            }
        });

        stage.setScene(scene);
    }

    private void setupImageViewer(StackPane root, MediaItem mediaItem) {
        Image image = new Image(mediaItem.getFile().toURI().toString());
        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(980);
        imageView.setFitHeight(650);

        StackPane imageContainer = new StackPane(imageView);
        imageContainer.setStyle("-fx-padding: 10;");
        root.getChildren().add(imageContainer);
        StackPane.setAlignment(imageContainer, Pos.CENTER);

        // Top bar with title
        Label titleLabel = new Label(mediaItem.getName());
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        topBar = new HBox(titleLabel);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-padding: 10; -fx-background-color: rgba(0,0,0,0.7);");
        topBar.setMaxHeight(50);
        root.getChildren().add(topBar);
        StackPane.setAlignment(topBar, Pos.TOP_CENTER);

        // Close button
        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> close());
        HBox buttonBox = new HBox(closeButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setStyle("-fx-padding: 10; -fx-background-color: rgba(0,0,0,0.7);");
        buttonBox.setMaxHeight(50);

        root.getChildren().add(buttonBox);
        StackPane.setAlignment(buttonBox, Pos.BOTTOM_CENTER);
        
        // Store reference for auto-hide
        controlsBox = new VBox(buttonBox);
        controlsBox.setStyle("-fx-background-color: transparent;");
    }

    private void setupVideoViewer(StackPane root, MediaItem mediaItem) {
        // Dispose old MediaPlayer if exists
        if (mediaPlayer != null) {
            mediaPlayer.dispose();
        }

        // Create new Media and MediaPlayer
        Media media = new Media(mediaItem.getFile().toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        MediaView mediaView = new MediaView();

        mediaView.setPreserveRatio(true);
        mediaView.setFitWidth(980);
        mediaView.setFitHeight(650);

        StackPane videoContainer = new StackPane(mediaView);
        videoContainer.setStyle("-fx-padding: 10;");
        root.getChildren().add(videoContainer);
        StackPane.setAlignment(videoContainer, Pos.CENTER);

        // Click to toggle play/pause
        videoContainer.setOnMouseClicked(event -> {
            if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();
            } else {
                mediaPlayer.play();
            }
        });

        // Top bar with title
        Label titleLabel = new Label(mediaItem.getName());
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        topBar = new HBox(titleLabel);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-padding: 10; -fx-background-color: rgba(0,0,0,0.7);");
        topBar.setMaxHeight(50);
        root.getChildren().add(topBar);
        StackPane.setAlignment(topBar, Pos.TOP_CENTER);

        // Video controls
        controlsBox = createVideoControls();
        root.getChildren().add(controlsBox);
        StackPane.setAlignment(controlsBox, Pos.BOTTOM_CENTER);
        controlsBox.toFront();

        // Wait for media to be READY before setting MediaPlayer and playing
        mediaPlayer.setOnReady(() -> {
            javafx.application.Platform.runLater(() -> {
                mediaView.setMediaPlayer(mediaPlayer);
                
                // Add slight delay before playing for stability
                PauseTransition delay = new PauseTransition(Duration.millis(300));
                delay.setOnFinished(e -> mediaPlayer.play());
                delay.play();
            });
        });

        // Error handling
        mediaPlayer.setOnError(() -> {
            System.err.println("MediaPlayer Error: " + mediaPlayer.getError());
            if (mediaPlayer.getError() != null) {
                mediaPlayer.getError().printStackTrace();
            }
        });
    }

    private VBox createVideoControls() {
        VBox controls = new VBox(10);
        controls.setStyle(
                "-fx-padding: 15; -fx-background-color: rgba(0,0,0,0.7); -fx-background-radius: 10;");
        controls.setAlignment(Pos.CENTER);
        controls.setMaxWidth(800);
        controls.setMinHeight(100);
        StackPane.setMargin(controls, new javafx.geometry.Insets(0, 0, 20, 0));

        // Seek slider
        Slider seekSlider = new Slider();
        seekSlider.setMaxWidth(Double.MAX_VALUE); // Fill width
        // HBox.setHgrow only works if parent is HBox. Since parent is VBox, we rely on
        // setMaxWidth.
        // But to be safe and cleaner, we can wrap it in an HBox if we wanted, or just
        // trust setMaxWidth.
        // Actually, VBox children align center by default here. To stretch, we might
        // need FillWidth.
        // By default VBox fills width if not restricted.
        seekSlider.setDisable(true);

        // Flag to prevent feedback loop when updating slider from player
        final boolean[] isUpdatingFromPlayer = { false };

        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (!seekSlider.isValueChanging()) {
                isUpdatingFromPlayer[0] = true;
                seekSlider.setValue(newTime.toSeconds());
                isUpdatingFromPlayer[0] = false;
            }
        });

        mediaPlayer.setOnReady(() -> {
            Duration total = mediaPlayer.getTotalDuration();
            seekSlider.setMax(total.toSeconds());
            seekSlider.setDisable(false);
        });

        // Handle dynamic duration changes
        mediaPlayer.totalDurationProperty().addListener((obs, oldDuration, newDuration) -> {
            seekSlider.setMax(newDuration.toSeconds());
        });

        // Ensure slider reaches the end when media finishes
        mediaPlayer.setOnEndOfMedia(() -> {
            seekSlider.setValue(seekSlider.getMax());
        });

        // Handle clicks and programmatic changes
        seekSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!isUpdatingFromPlayer[0] && !seekSlider.isValueChanging()) {
                mediaPlayer.seek(Duration.seconds(newVal.doubleValue()));
            }
        });

        // Handle drag release
        seekSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            if (!isChanging) {
                mediaPlayer.seek(Duration.seconds(seekSlider.getValue()));
            }
        });

        controls.getChildren().add(seekSlider);

        return controls;
    }

    private void setupAutoHideControls(Scene scene) {
        // Initialize timer for auto-hiding controls
        hideControlsTimer = new PauseTransition(Duration.seconds(3));
        hideControlsTimer.setOnFinished(event -> hideControls());

        // Show controls and reset timer only on mouse movement
        scene.setOnMouseMoved(event -> {
            showControls();
            hideControlsTimer.playFromStart();
        });

        // Don't show controls on key press (arrow keys for navigation)
        // The controls will remain hidden if navigating with keyboard

        // Start the timer initially
        hideControlsTimer.playFromStart();
    }

    private void showControls() {
        if (topBar != null) {
            topBar.setVisible(true);
        }
        if (controlsBox != null) {
            controlsBox.setVisible(true);
        }
        stage.getScene().setCursor(Cursor.DEFAULT);
    }

    private void hideControls() {
        if (topBar != null) {
            topBar.setVisible(false);
        }
        if (controlsBox != null) {
            controlsBox.setVisible(false);
        }
        stage.getScene().setCursor(Cursor.NONE);
    }

    public void show() {
        stage.show();
    }

    public void close() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
        stage.close();
    }
}
