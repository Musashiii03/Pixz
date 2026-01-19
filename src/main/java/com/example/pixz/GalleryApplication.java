package com.example.pixz;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GalleryApplication extends Application {
    private boolean isMaximized = true;
    private double restoreX, restoreY, restoreWidth, restoreHeight;
    private GalleryController controller; // Store controller reference for cleanup

    @Override
    public void start(Stage stage) throws IOException {
        // Add shutdown hook to ensure cleanup on unexpected exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {

        }));

        // Remove default window decorations
        stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
        stage.setFullScreenExitHint("");

        FXMLLoader fxmlLoader = new FXMLLoader(GalleryApplication.class.getResource("gallery-view.fxml"));
        javafx.scene.Parent root = fxmlLoader.load();

        // Create custom title bar
        javafx.scene.layout.HBox titleBar = createTitleBar(stage);
        titleBar.setId("customTitleBar"); // Add ID so controller can find it

        // Wrap content with title bar
        javafx.scene.layout.BorderPane mainContainer = new javafx.scene.layout.BorderPane();
        mainContainer.setTop(titleBar);
        mainContainer.setCenter(root);

        Scene scene = new Scene(mainContainer, 1280, 720);
        scene.getStylesheets().add(getClass().getResource("dark-theme.css").toExternalForm());

        // Pass title bar reference to controller
        controller = fxmlLoader.getController();
        controller.setCustomTitleBar(titleBar);

        // Set application icon with multiple sizes for better taskbar display
        try {
            // Load multiple PNG icon sizes - prioritize larger sizes first for better Windows taskbar display
            stage.getIcons().addAll(
                    new javafx.scene.image.Image(getClass().getResourceAsStream("pixz_512x512.png")),
                    new javafx.scene.image.Image(getClass().getResourceAsStream("pixz_256x256.png")),
                    new javafx.scene.image.Image(getClass().getResourceAsStream("pixz_128x128.png")),
                    new javafx.scene.image.Image(getClass().getResourceAsStream("pixz_96x96.png")),
                    new javafx.scene.image.Image(getClass().getResourceAsStream("pixz_64x64.png")),
                    new javafx.scene.image.Image(getClass().getResourceAsStream("pixz_48x48.png")),
                    new javafx.scene.image.Image(getClass().getResourceAsStream("pixz_32x32.png")),
                    new javafx.scene.image.Image(getClass().getResourceAsStream("pixz_24x24.png")),
                    new javafx.scene.image.Image(getClass().getResourceAsStream("pixz_16x16.png"))
            );

        } catch (Exception e) {
            System.err.println("Error loading icon: " + e.getMessage());
            e.printStackTrace();
        }

        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.setScene(scene);

        // Initialize restore values for a centered window
        javafx.geometry.Rectangle2D visualBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        restoreWidth = 1280;
        restoreHeight = 720;
        restoreX = visualBounds.getMinX() + (visualBounds.getWidth() - restoreWidth) / 2;
        restoreY = visualBounds.getMinY() + (visualBounds.getHeight() - restoreHeight) / 2;

        // Maximize to visual bounds (respecting taskbar)
        stage.setX(visualBounds.getMinX());
        stage.setY(visualBounds.getMinY());
        stage.setWidth(visualBounds.getWidth());
        stage.setHeight(visualBounds.getHeight());

        // Save session on window close
        stage.setOnCloseRequest(event -> {

            // Save session first
            controller.saveCurrentSession();

            // Shutdown controller resources (media players, etc.)
            controller.shutdown();

            // Force exit after a short delay to ensure cleanup
            javafx.animation.PauseTransition exitDelay = new javafx.animation.PauseTransition(
                    javafx.util.Duration.millis(500));
            exitDelay.setOnFinished(e -> {

                javafx.application.Platform.exit();
                System.exit(0);
            });
            exitDelay.play();
        });

        stage.show();
    }

    private javafx.scene.layout.HBox createTitleBar(Stage stage) {
        javafx.scene.layout.HBox titleBar = new javafx.scene.layout.HBox();
        titleBar.setStyle("-fx-background-color: #000000; -fx-padding: 0;");
        titleBar.setPrefHeight(35);
        titleBar.setMinHeight(35);
        titleBar.setMaxHeight(35);
        titleBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // App icon and title container
        javafx.scene.layout.HBox leftContainer = new javafx.scene.layout.HBox(8);
        leftContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        leftContainer.setStyle("-fx-padding: 0 0 0 10;");

        // App icon
        javafx.scene.image.ImageView iconView = new javafx.scene.image.ImageView();
        try {
            // Use 64x64 source for better quality when scaled to 20x20
            javafx.scene.image.Image icon = new javafx.scene.image.Image(
                    getClass().getResourceAsStream("pixz_64x64.png")
            );
            iconView.setImage(icon);
            iconView.setFitWidth(20);
            iconView.setFitHeight(20);
            iconView.setPreserveRatio(true);
            iconView.setSmooth(true); // Keep smooth for downscaling
        } catch (Exception e) {
            // Could not load icon, try fallback
            try {
                javafx.scene.image.Image icon = new javafx.scene.image.Image(
                        getClass().getResourceAsStream("pixz_48x48.png"));
                iconView.setImage(icon);
                iconView.setFitWidth(20);
                iconView.setFitHeight(20);
                iconView.setPreserveRatio(true);
                iconView.setSmooth(true);
            } catch (Exception ex) {
                // No icon available
                System.err.println("Could not load any icon: " + ex.getMessage());
            }
        }

        javafx.scene.control.Label titleLabel = new javafx.scene.control.Label("Pixz");
        titleLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 13px; -fx-font-weight: normal;");

        leftContainer.getChildren().addAll(iconView, titleLabel);

        // Spacer
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // Window control buttons - use CSS class for proper styling
        javafx.scene.control.Button minimizeBtn = new javafx.scene.control.Button("─");
        minimizeBtn.getStyleClass().add("title-bar-button");
        minimizeBtn.setMaxHeight(Double.MAX_VALUE);
        minimizeBtn.setOnAction(e -> stage.setIconified(true));

        javafx.scene.control.Button maximizeBtn = new javafx.scene.control.Button("⬜");
        maximizeBtn.getStyleClass().add("title-bar-button");
        maximizeBtn.setMaxHeight(Double.MAX_VALUE);
        maximizeBtn.setOnAction(e -> toggleMaximize(stage));

        javafx.scene.control.Button closeBtn = new javafx.scene.control.Button("✕");
        closeBtn.getStyleClass().addAll("title-bar-button", "close-button");
        closeBtn.setMaxHeight(Double.MAX_VALUE);
        closeBtn.setOnAction(e -> stage.close());

        titleBar.getChildren().addAll(leftContainer, spacer, minimizeBtn, maximizeBtn, closeBtn);

        // Make title bar draggable
        final double[] xOffset = { 0 };
        final double[] yOffset = { 0 };

        titleBar.setOnMousePressed(event -> {
            xOffset[0] = event.getSceneX();
            yOffset[0] = event.getSceneY();
        });

        titleBar.setOnMouseDragged(event -> {
            if (!isMaximized) {
                stage.setX(event.getScreenX() - xOffset[0]);
                stage.setY(event.getScreenY() - yOffset[0]);
            }
        });

        // Double-click to maximize/restore
        titleBar.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                toggleMaximize(stage);
            }
        });

        return titleBar;
    }

    private void toggleMaximize(Stage stage) {
        javafx.geometry.Rectangle2D visualBounds = javafx.stage.Screen.getPrimary().getVisualBounds();

        if (isMaximized) {
            // Restore to previous size
            stage.setX(restoreX);
            stage.setY(restoreY);
            stage.setWidth(restoreWidth);
            stage.setHeight(restoreHeight);
            isMaximized = false;
        } else {
            // Save current position and size before maximizing
            restoreX = stage.getX();
            restoreY = stage.getY();
            restoreWidth = stage.getWidth();
            restoreHeight = stage.getHeight();

            // Maximize to visual bounds (respecting taskbar)
            stage.setX(visualBounds.getMinX());
            stage.setY(visualBounds.getMinY());
            stage.setWidth(visualBounds.getWidth());
            stage.setHeight(visualBounds.getHeight());
            isMaximized = true;
        }
    }

    @Override
    public void stop() throws Exception {

        // Save session before exit
        if (controller != null) {
            controller.saveCurrentSession();
            controller.shutdown();
        }

        super.stop();
        // Force exit to ensure all threads are terminated
        System.exit(0);
    }

    public static void main(String[] args) {
        launch();
    }
}
