package com.example.wingallery;

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

        BorderPane root = new BorderPane();
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

    private void setupImageViewer(BorderPane root, MediaItem mediaItem) {
        Image image = new Image(mediaItem.getFile().toURI().toString());
        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(980);
        imageView.setFitHeight(650);

        StackPane imageContainer = new StackPane(imageView);
        imageContainer.setStyle("-fx-padding: 10;");
        root.setCenter(imageContainer);

        // Close button
        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> close());
        HBox buttonBox = new HBox(closeButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setStyle("-fx-padding: 10;");
        root.setBottom(buttonBox);
    }

    private void setupVideoViewer(BorderPane root, MediaItem mediaItem) {
        Media media = new Media(mediaItem.getFile().toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        MediaView mediaView = new MediaView(mediaPlayer);

        mediaView.setPreserveRatio(true);
        mediaView.setFitWidth(980);
        mediaView.setFitHeight(600);

        StackPane videoContainer = new StackPane(mediaView);
        videoContainer.setStyle("-fx-padding: 10;");
        root.setCenter(videoContainer);

        // Video controls
        VBox controlsBox = createVideoControls();
        root.setBottom(controlsBox);

        // Auto-play
        mediaPlayer.setAutoPlay(true);
    }

    private VBox createVideoControls() {
        VBox controlsBox = new VBox(10);
        controlsBox.setStyle("-fx-padding: 10; -fx-background-color: #0a0a0a;");
        controlsBox.setAlignment(Pos.CENTER);

        // Playback controls
        HBox playbackControls = new HBox(10);
        playbackControls.setAlignment(Pos.CENTER);

        Button playPauseButton = new Button("Play");
        Button stopButton = new Button("Stop");
        Label timeLabel = new Label("00:00 / 00:00");
        timeLabel.setStyle("-fx-text-fill: #f2f2f2;");

        playPauseButton.setOnAction(e -> {
            if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();
                playPauseButton.setText("Play");
            } else {
                mediaPlayer.play();
                playPauseButton.setText("Pause");
            }
        });

        stopButton.setOnAction(e -> {
            mediaPlayer.stop();
            playPauseButton.setText("Play");
        });

        // Seek slider
        Slider seekSlider = new Slider();
        seekSlider.setMaxWidth(400);
        seekSlider.setDisable(true);

        // Flag to prevent feedback loop when updating slider from player
        final boolean[] isUpdatingFromPlayer = { false };

        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (!seekSlider.isValueChanging()) {
                isUpdatingFromPlayer[0] = true;
                seekSlider.setValue(newTime.toSeconds());
                isUpdatingFromPlayer[0] = false;
            }
            updateTimeLabel(timeLabel, newTime, mediaPlayer.getTotalDuration());
        });

        mediaPlayer.setOnReady(() -> {
            Duration total = mediaPlayer.getTotalDuration();
            seekSlider.setMax(total.toSeconds());
            seekSlider.setDisable(false);
            updateTimeLabel(timeLabel, mediaPlayer.getCurrentTime(), total);
        });

        // Handle dynamic duration changes (common in some formats)
        mediaPlayer.totalDurationProperty().addListener((obs, oldDuration, newDuration) -> {
            seekSlider.setMax(newDuration.toSeconds());
            updateTimeLabel(timeLabel, mediaPlayer.getCurrentTime(), newDuration);
        });

        // Ensure slider reaches the end when media finishes
        mediaPlayer.setOnEndOfMedia(() -> {
            seekSlider.setValue(seekSlider.getMax());
            updateTimeLabel(timeLabel, mediaPlayer.getTotalDuration(), mediaPlayer.getTotalDuration());
            playPauseButton.setText("Play");
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

        // Volume slider
        Slider volumeSlider = new Slider(0, 1, 0.5);
        volumeSlider.setMaxWidth(150);
        Label volumeLabel = new Label("Volume");
        volumeLabel.setStyle("-fx-text-fill: #f2f2f2;");

        mediaPlayer.volumeProperty().bind(volumeSlider.valueProperty());

        playbackControls.getChildren().addAll(playPauseButton, stopButton, timeLabel);

        HBox seekBox = new HBox(10, new Label("Seek:"), seekSlider);
        seekBox.setAlignment(Pos.CENTER);
        ((Label) seekBox.getChildren().get(0)).setStyle("-fx-text-fill: #f2f2f2;");

        HBox volumeBox = new HBox(10, volumeLabel, volumeSlider);
        volumeBox.setAlignment(Pos.CENTER);

        // Close button
        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> close());

        controlsBox.getChildren().addAll(playbackControls, seekBox, volumeBox, closeButton);

        return controlsBox;
    }

    private void updateTimeLabel(Label label, Duration current, Duration total) {
        if (current != null && total != null) {
            label.setText(formatDuration(current) + " / " + formatDuration(total));
        }
    }

    private String formatDuration(Duration duration) {
        if (duration == null || duration.isUnknown() || duration.isIndefinite()) {
            return "--:--";
        }
        double totalSeconds = duration.toSeconds();
        if (totalSeconds >= Integer.MAX_VALUE || totalSeconds < 0 || Double.isNaN(totalSeconds)
                || Double.isInfinite(totalSeconds)) {
            return "--:--";
        }
        int seconds = (int) totalSeconds;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
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
