package com.example.pixz;

import java.util.List;

import javafx.scene.Node;
import javafx.scene.layout.Pane;

/**
 * Custom layout pane that arranges children in a uniform grid
 */
public class MasonryPane extends Pane {
    private static final double CELL_SIZE = 300; // Size of each thumbnail
    private static final double GAP = 2; // 2px gap between items
    private int numColumns = 3;

    public MasonryPane() {
        super();
        // Set black background so gaps appear as thin lines
        setStyle("-fx-background-color: #000000;");
        // Listen to width changes to recalculate columns
        widthProperty().addListener((obs, oldVal, newVal) -> {
            calculateColumns();
            requestLayout();
        });
    }

    private void calculateColumns() {
        double availableWidth = getWidth() - getInsets().getLeft() - getInsets().getRight();
        if (availableWidth > 0) {
            // Calculate how many columns fit: (width + gap) / (cellSize + gap)
            numColumns = Math.max(1, (int) ((availableWidth + GAP) / (CELL_SIZE + GAP)));
        }
    }

    @Override
    protected void layoutChildren() {
        calculateColumns();
        
        List<Node> managed = getManagedChildren();
        if (managed.isEmpty()) {
            return;
        }

        double leftInset = getInsets().getLeft();
        double topInset = getInsets().getTop();
        
        // Track the height of each column
        double[] columnHeights = new double[numColumns];
        
        // Layout each child in a grid
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
            
            // Calculate exact position with gap
            double x = leftInset + shortestColumn * (CELL_SIZE + GAP);
            double y = topInset + columnHeights[shortestColumn];
            
            // Position the child as exact square
            child.resizeRelocate(x, y, CELL_SIZE, CELL_SIZE);
            
            // Update column height: add cell size + gap
            columnHeights[shortestColumn] += CELL_SIZE + GAP;
        }
        
        // Set the preferred height of the pane to the tallest column
        double maxHeight = 0;
        for (double height : columnHeights) {
            maxHeight = Math.max(maxHeight, height);
        }
        // Subtract one GAP at the end since we don't need gap after last row
        setPrefHeight(maxHeight - GAP + topInset + getInsets().getBottom());
    }

    @Override
    protected double computePrefWidth(double height) {
        return numColumns * (CELL_SIZE + GAP) - GAP + getInsets().getLeft() + getInsets().getRight();
    }

    @Override
    protected double computePrefHeight(double width) {
        return getPrefHeight();
    }
}
