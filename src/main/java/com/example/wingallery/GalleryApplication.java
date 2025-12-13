package com.example.wingallery;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class GalleryApplication extends Application {
    private boolean isMaximized = true;
    private double restoreX, restoreY, restoreWidth, restoreHeight;

    @Override
    public void start(Stage stage) throws IOException {
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
        GalleryController controller = fxmlLoader.getController();
        controller.setCustomTitleBar(titleBar);

        // Set application icon
        try {
            javafx.scene.image.Image icon = new javafx.scene.image.Image(
                getClass().getResourceAsStream("icon.png")
            );
            stage.getIcons().add(icon);
        } catch (Exception e) {
            System.err.println("Could not load application icon: " + e.getMessage());
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

        stage.show();
    }

    private javafx.scene.layout.HBox createTitleBar(Stage stage) {
        javafx.scene.layout.HBox titleBar = new javafx.scene.layout.HBox();
        titleBar.setStyle("-fx-background-color: #0b0e14; -fx-padding: 8 10;");
        titleBar.setPrefHeight(35);
        titleBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // App icon and title
        javafx.scene.image.ImageView iconView = new javafx.scene.image.ImageView();
        try {
            javafx.scene.image.Image icon = new javafx.scene.image.Image(
                getClass().getResourceAsStream("icon.png")
            );
            iconView.setImage(icon);
            iconView.setFitWidth(20);
            iconView.setFitHeight(20);
            iconView.setPreserveRatio(true);
            iconView.setSmooth(true);
        } catch (Exception e) {
            System.err.println("Could not load title bar icon: " + e.getMessage());
        }
        javafx.scene.layout.HBox iconContainer = new javafx.scene.layout.HBox(iconView);
        iconContainer.setStyle("-fx-padding: 0 10 0 0;");
        iconContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        javafx.scene.control.Label titleLabel = new javafx.scene.control.Label("WinGallery");
        titleLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 14px; -fx-font-weight: normal;");
        titleLabel.setMinWidth(80);

        // Spacer
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // Window control buttons
        String buttonStyle = "-fx-background-color: transparent; -fx-text-fill: #ffffff; -fx-font-size: 14px; -fx-padding: 0; -fx-cursor: hand; -fx-min-width: 46px; -fx-max-width: 46px;";
        String buttonHoverStyle = "-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: #ffffff; -fx-font-size: 14px; -fx-padding: 0; -fx-cursor: hand; -fx-min-width: 46px; -fx-max-width: 46px;";
        
        javafx.scene.control.Button minimizeBtn = new javafx.scene.control.Button("─");
        minimizeBtn.setStyle(buttonStyle);
        minimizeBtn.setOnMouseEntered(e -> minimizeBtn.setStyle(buttonHoverStyle));
        minimizeBtn.setOnMouseExited(e -> minimizeBtn.setStyle(buttonStyle));
        minimizeBtn.setOnAction(e -> stage.setIconified(true));

        javafx.scene.control.Button maximizeBtn = new javafx.scene.control.Button("□");
        maximizeBtn.setStyle(buttonStyle);
        maximizeBtn.setOnMouseEntered(e -> maximizeBtn.setStyle(buttonHoverStyle));
        maximizeBtn.setOnMouseExited(e -> maximizeBtn.setStyle(buttonStyle));
        maximizeBtn.setOnAction(e -> toggleMaximize(stage));

        javafx.scene.control.Button closeBtn = new javafx.scene.control.Button("✕");
        closeBtn.setStyle(buttonStyle);
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(
                "-fx-background-color: #e81123; -fx-text-fill: #ffffff; -fx-font-size: 14px; -fx-padding: 0; -fx-cursor: hand; -fx-min-width: 46px; -fx-max-width: 46px;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(buttonStyle));
        closeBtn.setOnAction(e -> stage.close());

        titleBar.getChildren().addAll(iconContainer, titleLabel, spacer, minimizeBtn, maximizeBtn, closeBtn);

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

    public static void main(String[] args) {
        launch();
    }
}
