package com.example.pixz;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
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

        // Close button
        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> close());
        HBox buttonBox = new HBox(closeButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setStyle("-fx-padding: 10; -fx-background-color: rgba(0,0,0,0.5);");
        buttonBox.setMaxHeight(50);

        root.getChildren().add(buttonBox);
        StackPane.setAlignment(buttonBox, Pos.BOTTOM_CENTER);
    }

    private void setupVideoViewer(StackPane root, MediaItem mediaItem) {
        Media media = new Media(mediaItem.getFile().toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        MediaView mediaView = new MediaView(mediaPlayer);

        mediaView.setPreserveRatio(true);
        // DEBUG: Shrink video to verify if controls are hidden behind it or just not
        // there
        mediaView.setFitWidth(200);
        mediaView.setFitHeight(200);

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

        // Video controls
        VBox controlsBox = createVideoControls();
        root.getChildren().add(controlsBox);
        StackPane.setAlignment(controlsBox, Pos.BOTTOM_CENTER);
        controlsBox.toFront();

        // DEBUG: Overlay Test
        Label overlayTest = new Label("OVERLAY TEST");
        overlayTest.setStyle("-fx-font-size: 50px; -fx-text-fill: red;");
        root.getChildren().add(overlayTest);
        overlayTest.toFront();

        // Auto-play
        mediaPlayer.setAutoPlay(true);
    }

    private VBox createVideoControls() {
        VBox controlsBox = new VBox(10);
        // DEBUG STYLE
        controlsBox.setStyle(
                "-fx-padding: 15; -fx-background-color: #00FF00; -fx-background-radius: 10; -fx-border-color: red; -fx-border-width: 5;");
        controlsBox.setAlignment(Pos.CENTER);
        controlsBox.setMaxWidth(800);
        controlsBox.setMinHeight(100);
        StackPane.setMargin(controlsBox, new javafx.geometry.Insets(0, 0, 50, 0));

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

        controlsBox.getChildren().add(seekSlider);

        return controlsBox;
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
