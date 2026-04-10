package com.example.pixz;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GalleryApplication extends Application {
    private GalleryController controller; // Store controller reference for cleanup

    @Override
    public void start(Stage stage) throws IOException {
        try {
            // Add shutdown hook to ensure cleanup on unexpected exit
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {

            }));

            // Remove default window decorations
            stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            stage.setFullScreenExitHint("");

            FXMLLoader fxmlLoader = new FXMLLoader(GalleryApplication.class.getResource("gallery-view.fxml"));
            javafx.scene.Parent root = fxmlLoader.load();

            // Create main container without floating controls overlay
            Scene scene = new Scene(root, 1280, 720);
            scene.getStylesheets().add(getClass().getResource("dark-theme.css").toExternalForm());

            // Pass null for window controls reference (no longer using floating controls)
            controller = fxmlLoader.getController();
            controller.setCustomTitleBar(null);
            
            // Enable window dragging from top area
            setupWindowDragging(scene, stage);
            
            // Enable window dragging from anywhere on the window
            setupWindowDragging(scene, stage);

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

        // Start maximized to visual bounds (respecting taskbar)
        javafx.geometry.Rectangle2D visualBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
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
        } catch (Exception e) {
            System.err.println("ERROR in start method:");
            e.printStackTrace();
            throw e;
        }
    }

    private void setupWindowDragging(Scene scene, Stage stage) {
        final double[] xOffset = { 0 };
        final double[] yOffset = { 0 };
        
        scene.setOnMousePressed(event -> {
            // Only allow dragging from top area (first 50px) and not when maximized
            if (event.getSceneY() < 50 && !stage.isMaximized()) {
                xOffset[0] = event.getSceneX();
                yOffset[0] = event.getSceneY();
            }
        });
        
        scene.setOnMouseDragged(event -> {
            // Only drag if we started from top area and not maximized
            if (event.getSceneY() < 100 && !stage.isMaximized() && xOffset[0] != 0) {
                stage.setX(event.getScreenX() - xOffset[0]);
                stage.setY(event.getScreenY() - yOffset[0]);
            }
        });
        
        scene.setOnMouseReleased(event -> {
            xOffset[0] = 0;
            yOffset[0] = 0;
        });
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
        try {
            launch();
        } catch (Exception e) {
            System.err.println("ERROR in main:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
