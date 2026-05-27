package com.example.pixz;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;

/**
 * ZoomManager - handles zoom and pan functionality for images and videos.
 * Supports mouse wheel zoom, button zoom, and drag-to-pan when zoomed.
 */
public class ZoomManager {
    
    private final Node target;
    private final Pane container;
    
    private double currentZoom = 1.0;
    private static final double MIN_ZOOM = 0.5;  // 50% (can zoom out to see more context)
    private static final double MAX_ZOOM = 5.0;  // 500%
    private static final double ZOOM_STEP = 0.2; // 20% per step
    
    // Pan state
    private double lastMouseX = 0;
    private double lastMouseY = 0;
    private boolean isPanning = false;
    
    public ZoomManager(Node target, Pane container) {
        this.target = target;
        this.container = container;
        
        setupZoomHandlers();
        setupPanHandlers();
    }
    
    /**
     * Setup mouse wheel zoom handler using event filter.
     */
    private void setupZoomHandlers() {
        // Use event filter to capture before other handlers
        container.addEventFilter(ScrollEvent.SCROLL, event -> {
            // Ctrl+Scroll for zoom
            if (event.isControlDown()) {
                event.consume();
                
                // Get mouse position relative to target
                Bounds targetBounds = target.getBoundsInParent();
                double mouseX = event.getX() - targetBounds.getMinX();
                double mouseY = event.getY() - targetBounds.getMinY();
                
                // Zoom in or out
                if (event.getDeltaY() > 0) {
                    zoomIn(mouseX, mouseY);
                } else {
                    zoomOut(mouseX, mouseY);
                }
            }
        });
    }
    
    /**
     * Setup pan (drag) handlers for when zoomed (in or out).
     */
    private void setupPanHandlers() {
        // Use event filters to capture before other handlers
        container.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            // Allow panning at any zoom level except exactly 100%
            if (event.getButton() == MouseButton.PRIMARY && currentZoom != 1.0) {
                isPanning = true;
                lastMouseX = event.getSceneX();
                lastMouseY = event.getSceneY();
                container.setCursor(javafx.scene.Cursor.CLOSED_HAND);
                event.consume();
            }
        });
        
        container.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (isPanning && currentZoom != 1.0) {
                double deltaX = event.getSceneX() - lastMouseX;
                double deltaY = event.getSceneY() - lastMouseY;
                
                // Update translate
                target.setTranslateX(target.getTranslateX() + deltaX);
                target.setTranslateY(target.getTranslateY() + deltaY);
                
                lastMouseX = event.getSceneX();
                lastMouseY = event.getSceneY();
                event.consume();
            }
        });
        
        container.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (event.getButton() == MouseButton.PRIMARY && isPanning) {
                isPanning = false;
                container.setCursor(currentZoom != 1.0 ? javafx.scene.Cursor.OPEN_HAND : javafx.scene.Cursor.DEFAULT);
                event.consume();
            }
        });
        
        // Update cursor when mouse enters/exits
        container.addEventFilter(MouseEvent.MOUSE_ENTERED, event -> {
            if (currentZoom != 1.0 && !isPanning) {
                container.setCursor(javafx.scene.Cursor.OPEN_HAND);
            }
        });
        
        container.addEventFilter(MouseEvent.MOUSE_EXITED, event -> {
            if (!isPanning) {
                container.setCursor(javafx.scene.Cursor.DEFAULT);
            }
        });
    }
    
    /**
     * Zoom in by one step, centered on the given point.
     */
    public void zoomIn(double pivotX, double pivotY) {
        double newZoom = Math.min(currentZoom + ZOOM_STEP, MAX_ZOOM);
        applyZoom(newZoom, pivotX, pivotY);
    }
    
    /**
     * Zoom in centered on the target center.
     */
    public void zoomIn() {
        Bounds bounds = target.getBoundsInLocal();
        zoomIn(bounds.getWidth() / 2, bounds.getHeight() / 2);
    }
    
    /**
     * Zoom out by one step, centered on the given point.
     */
    public void zoomOut(double pivotX, double pivotY) {
        double newZoom = Math.max(currentZoom - ZOOM_STEP, MIN_ZOOM);
        applyZoom(newZoom, pivotX, pivotY);
    }
    
    /**
     * Zoom out centered on the target center.
     */
    public void zoomOut() {
        Bounds bounds = target.getBoundsInLocal();
        zoomOut(bounds.getWidth() / 2, bounds.getHeight() / 2);
    }
    
    /**
     * Reset zoom to 100% (fit to screen) and center.
     */
    public void resetZoom() {
        currentZoom = 1.0;  // Reset to 100%, not MIN_ZOOM
        target.setScaleX(1.0);
        target.setScaleY(1.0);
        target.setTranslateX(0);
        target.setTranslateY(0);
        container.setCursor(javafx.scene.Cursor.DEFAULT);
        isPanning = false;
    }
    
    /**
     * Apply zoom with pivot point.
     */
    private void applyZoom(double newZoom, double pivotX, double pivotY) {
        if (newZoom == currentZoom) return;
        
        // Special case: when zooming to/from below 100%, center the media
        if (newZoom < 1.0 || currentZoom < 1.0) {
            currentZoom = newZoom;
            target.setScaleX(currentZoom);
            target.setScaleY(currentZoom);
            
            // Keep centered when below 100%
            if (newZoom < 1.0) {
                target.setTranslateX(0);
                target.setTranslateY(0);
            }
            
            // Update cursor
            if (currentZoom == 1.0) {
                container.setCursor(javafx.scene.Cursor.DEFAULT);
            } else {
                container.setCursor(javafx.scene.Cursor.OPEN_HAND);
            }
            return;
        }
        
        // For zoom >= 100%, use pivot point to keep the zoomed area stable
        double scaleFactor = newZoom / currentZoom;
        
        // Get current translate
        double oldTranslateX = target.getTranslateX();
        double oldTranslateY = target.getTranslateY();
        
        // Get the bounds of the target in parent coordinates
        Bounds bounds = target.getBoundsInParent();
        double centerX = bounds.getMinX() + bounds.getWidth() / 2;
        double centerY = bounds.getMinY() + bounds.getHeight() / 2;
        
        // Calculate offset from center to pivot point
        double offsetX = pivotX - centerX;
        double offsetY = pivotY - centerY;
        
        // Calculate new translate to keep pivot point fixed
        double newTranslateX = oldTranslateX - offsetX * (scaleFactor - 1);
        double newTranslateY = oldTranslateY - offsetY * (scaleFactor - 1);
        
        // Apply zoom
        currentZoom = newZoom;
        target.setScaleX(currentZoom);
        target.setScaleY(currentZoom);
        target.setTranslateX(newTranslateX);
        target.setTranslateY(newTranslateY);
        
        // Update cursor
        container.setCursor(javafx.scene.Cursor.OPEN_HAND);
    }
    
    /**
     * Get current zoom level.
     */
    public double getCurrentZoom() {
        return currentZoom;
    }
    
    /**
     * Check if currently zoomed in.
     */
    public boolean isZoomedIn() {
        return currentZoom > MIN_ZOOM;
    }
    
    /**
     * Cleanup - remove all handlers.
     */
    public void cleanup() {
        // Event filters are automatically removed when container is removed from scene
        resetZoom();
    }
}
