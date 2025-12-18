package com.example.wingallery;

import java.util.List;

import javafx.scene.Node;
import javafx.scene.layout.Pane;

/**
 * Custom layout pane that arranges children in a uniform grid
 */
public class MasonryPane extends Pane {
    private static final double COLUMN_WIDTH = 300; // Larger thumbnails
    private static final double GAP = 0; // No gap between items
    private int numColumns = 3;

    public MasonryPane() {
        super();
        // Listen to width changes to recalculate columns
        widthProperty().addListener((obs, oldVal, newVal) -> {
            calculateColumns();
            requestLayout();
        });
    }

    private void calculateColumns() {
        double availableWidth = getWidth() - getInsets().getLeft() - getInsets().getRight();
        if (availableWidth > 0) {
            numColumns = Math.max(1, (int) ((availableWidth + GAP) / (COLUMN_WIDTH + GAP)));
        }
    }
    
    private double getActualColumnWidth() {
        double availableWidth = getWidth() - getInsets().getLeft() - getInsets().getRight();
        double totalGapWidth = (numColumns - 1) * GAP;
        return (availableWidth - totalGapWidth) / numColumns;
    }

    @Override
    protected void layoutChildren() {
        calculateColumns();
        
        List<Node> managed = getManagedChildren();
        if (managed.isEmpty()) {
            return;
        }

        double columnWidth = getActualColumnWidth();
        double leftInset = getInsets().getLeft();
        double topInset = getInsets().getTop();
        
        // Track the height of each column
        double[] columnHeights = new double[numColumns];
        
        // Layout each child
        for (Node child : managed) {
            // Find the shortest column
            int shortestColumn = 0;
            double minHeight = columnHeights[0];
            for (int i = 1; i < numColumns; i++) {
                if (columnHeights[i] < minHeight) {
                    minHeight = columnHeights[i];
                    shortestColumn = i;
                }
            }
            
            // Calculate position
            double x = leftInset + shortestColumn * (columnWidth + GAP);
            double y = topInset + columnHeights[shortestColumn];
            
            // Position the child
            child.resizeRelocate(x, y, columnWidth, child.prefHeight(columnWidth));
            
            // Update column height
            columnHeights[shortestColumn] += child.prefHeight(columnWidth) + GAP;
        }
        
        // Set the preferred height of the pane to the tallest column
        double maxHeight = 0;
        for (double height : columnHeights) {
            maxHeight = Math.max(maxHeight, height);
        }
        setPrefHeight(maxHeight + topInset + getInsets().getBottom());
    }

    @Override
    protected double computePrefWidth(double height) {
        return numColumns * (COLUMN_WIDTH + GAP) - GAP;
    }

    @Override
    protected double computePrefHeight(double width) {
        // This will be calculated during layout
        return getPrefHeight();
    }
}
