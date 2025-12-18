package com.example.wingallery;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class GalleryController {
    @FXML
    private MasonryPane galleryPane;

    @FXML
    private BorderPane rootPane;

    @FXML
    private ScrollPane galleryScrollPane;

    @FXML
    private TextField searchField;

    @FXML
    private Label breadcrumbLabel;

    @FXML
    private Label folderTitleLabel;

    @FXML
    private Label itemCountLabel;

    @FXML
    private VBox sidebar;

    @FXML
    private VBox folderList;

    @FXML
    private Button allMediaButton;

    @FXML
    private Button photosButton;

    @FXML
    private Button videosButton;

    @FXML
    private ComboBox<String> sortComboBox;

    @FXML
    private HBox folderFilterButtons;

    @FXML
    private Button allFoldersButton;

    @FXML
    private Button refreshButton;

    private final Set<String> selectedFolders = new HashSet<>();
    private final List<MediaItem> mediaItems = new ArrayList<>();
    private final Map<String, HBox> folderCards = new HashMap<>();
    private final Map<String, Button> folderFilterButtonsMap = new HashMap<>();

    // Filter and sort state
    private enum MediaFilter {
        ALL, PHOTOS, VIDEOS
    }

    private MediaFilter currentFilter = MediaFilter.ALL;
    private String currentSortBy = "Name";
    private String currentFolderFilter = null; // null means all folders

    // Fullscreen viewer components
    private StackPane fullscreenViewer;
    private MediaPlayer currentMediaPlayer;
    private int currentMediaIndex = -1;
    private javafx.scene.Node headerNode; // Store header to restore later

    private javafx.scene.layout.HBox customTitleBar; // Custom title bar reference

    // Sidebar animation
    private javafx.animation.TranslateTransition sidebarTransition;

    // Remember last opened folder
    private File lastOpenedFolder = null;

    // Method to set custom title bar reference
    public void setCustomTitleBar(javafx.scene.layout.HBox titleBar) {
        this.customTitleBar = titleBar;
    }

    @FXML
    public void initialize() {
        // Setup search functionality
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFiltersAndSort());

        // Setup sort dropdown
        sortComboBox.getItems().addAll("Name", "Date Modified");
        sortComboBox.setValue("Name");
        sortComboBox.setOnAction(e -> {
            currentSortBy = sortComboBox.getValue();
            applyFiltersAndSort();
        });

        // Setup sidebar auto-hide
        setupSidebar();

        // Store header reference
        headerNode = rootPane.getTop();

        // Load previous session
        loadPreviousSession();

        // Update UI
        updateHeaderInfo();

        // Show empty state if no folders
        showEmptyStateIfNeeded();
    }

    /**
     * Load folders from previous session
     */
    private void loadPreviousSession() {
        Set<String> savedFolders = SessionManager.loadSession();

        if (!savedFolders.isEmpty()) {
            // Load each folder
            for (String folderPath : savedFolders) {
                File folder = new File(folderPath);
                if (folder.exists() && folder.isDirectory()) {
                    scanFolder(folder);
                }
            }
        }
    }

    /**
     * Save current session (called on app close)
     */
    public void saveCurrentSession() {
        if (!selectedFolders.isEmpty()) {
            SessionManager.saveSession(selectedFolders);
        }
    }

    private void showEmptyStateIfNeeded() {
        if (mediaItems.isEmpty()) {
            // Create empty state UI centered in viewport
            StackPane emptyStateContainer = new StackPane();
            emptyStateContainer.setStyle("-fx-background-color: #0b0e14;");

            VBox emptyState = new VBox(20);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setStyle("-fx-padding: 40;");

            Label emptyIcon = new Label("📁");
            emptyIcon.setStyle("-fx-font-size: 80px;");

            Label emptyTitle = new Label("No Folders Added");
            emptyTitle.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 24px; -fx-font-weight: bold;");

            Label emptySubtitle = new Label("Add a folder to start viewing your photos and videos");
            emptySubtitle.setStyle("-fx-text-fill: #7a7d8a; -fx-font-size: 14px;");

            Button addFolderBtn = new Button("+ Add Folder");
            addFolderBtn.setStyle(
                    "-fx-background-color: #e6b450; -fx-text-fill: black; -fx-font-size: 16px; -fx-padding: 12 24; -fx-background-radius: 8; -fx-cursor: hand;");
            addFolderBtn.setOnAction(e -> onFolderLocationClick());

            // Hover effect
            addFolderBtn.setOnMouseEntered(e -> addFolderBtn.setStyle(
                    "-fx-background-color: #f0c060; -fx-text-fill: black; -fx-font-size: 16px; -fx-padding: 12 24; -fx-background-radius: 8; -fx-cursor: hand;"));
            addFolderBtn.setOnMouseExited(e -> addFolderBtn.setStyle(
                    "-fx-background-color: #e6b450; -fx-text-fill: black; -fx-font-size: 16px; -fx-padding: 12 24; -fx-background-radius: 8; -fx-cursor: hand;"));

            emptyState.getChildren().addAll(emptyIcon, emptyTitle, emptySubtitle, addFolderBtn);
            emptyStateContainer.getChildren().add(emptyState);

            // Replace the center content with empty state
            rootPane.setCenter(emptyStateContainer);
        }
    }

    private void setupSidebar() {
        // Create animation for sidebar
        sidebarTransition = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(300), sidebar);

        // Get the parent StackPane
        Platform.runLater(() -> {
            StackPane parent = (StackPane) rootPane.getParent();
            if (parent != null) {
                // Show sidebar on hover at left edge (only when not viewing media)
                parent.setOnMouseMoved(e -> {
                    if (fullscreenViewer == null) {
                        if (e.getSceneX() < 30) {
                            showSidebar();
                        } else if (e.getSceneX() > 320 && sidebar.getTranslateX() >= -50) {
                            hideSidebar();
                        }
                    }
                });
            }
        });

        // Keep sidebar visible when hovering over it (only when not viewing media)
        sidebar.setOnMouseEntered(e -> {
            if (fullscreenViewer == null) {
                showSidebar();
            }
        });
    }

    private void showSidebar() {
        if (sidebar.getTranslateX() < -10) {
            sidebarTransition.stop();
            sidebarTransition.setFromX(sidebar.getTranslateX());
            sidebarTransition.setToX(0);
            sidebarTransition.play();
        }
    }

    private void hideSidebar() {
        if (sidebar.getTranslateX() > -270) {
            sidebarTransition.stop();
            sidebarTransition.setFromX(sidebar.getTranslateX());
            sidebarTransition.setToX(-280);
            sidebarTransition.play();
        }
    }

    private void updateHeaderInfo() {
        itemCountLabel.setText(mediaItems.size() + " items");

        // Update header based on folder filter
        if (currentFolderFilter != null) {
            // A specific folder is selected
            String folderName = new File(currentFolderFilter).getName();
            breadcrumbLabel.setText("All Folders / " + folderName);
            folderTitleLabel.setText(folderName);
        } else if (selectedFolders.isEmpty()) {
            breadcrumbLabel.setText("All Folders");
            folderTitleLabel.setText("All Media");
        } else if (selectedFolders.size() == 1) {
            String folderPath = selectedFolders.iterator().next();
            String folderName = new File(folderPath).getName();
            breadcrumbLabel.setText("All Folders / " + folderName);
            folderTitleLabel.setText(folderName);
        } else {
            breadcrumbLabel.setText("All Folders / Multiple");
            folderTitleLabel.setText("Multiple Folders");
        }
    }

    @FXML
    protected void onFilterAll() {
        currentFilter = MediaFilter.ALL;
        updateFilterButtonStyles();
        applyFiltersAndSort();
    }

    @FXML
    protected void onFilterPhotos() {
        currentFilter = MediaFilter.PHOTOS;
        updateFilterButtonStyles();
        applyFiltersAndSort();
    }

    @FXML
    protected void onFilterVideos() {
        currentFilter = MediaFilter.VIDEOS;
        updateFilterButtonStyles();
        applyFiltersAndSort();
    }

    private void onFolderFilterClick(String folderPath) {
        currentFolderFilter = folderPath;
        updateFolderFilterButtonStyles();
        updateHeaderInfo();
        applyFiltersAndSort();
    }

    @FXML
    protected void onAllFoldersClick() {
        currentFolderFilter = null;
        updateFolderFilterButtonStyles();
        updateHeaderInfo();
        applyFiltersAndSort();
    }

    @FXML
    protected void onRefreshClick() {
        // Rescan all folders
        List<File> foldersToRescan = new ArrayList<>();
        for (String folderPath : selectedFolders) {
            foldersToRescan.add(new File(folderPath));
        }

        // Clear current media items
        mediaItems.clear();

        // Rescan each folder
        for (File folder : foldersToRescan) {
            if (folder.exists()) {
                scanFolder(folder);
            }
        }

        // Refresh the gallery
        refreshGallery();
    }

    private void updateFilterButtonStyles() {
        String activeStyle = "-fx-background-color: #e6b450; -fx-text-fill: black; -fx-font-size: 13px; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;";
        String inactiveStyle = "-fx-background-color: #2d3142; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;";

        allMediaButton.setStyle(currentFilter == MediaFilter.ALL ? activeStyle : inactiveStyle);
        photosButton.setStyle(currentFilter == MediaFilter.PHOTOS ? activeStyle : inactiveStyle);
        videosButton.setStyle(currentFilter == MediaFilter.VIDEOS ? activeStyle : inactiveStyle);
    }

    private void updateFolderFilterButtonStyles() {
        String activeStyle = "-fx-background-color: #e6b450; -fx-text-fill: black; -fx-font-size: 13px; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;";
        String inactiveStyle = "-fx-background-color: #2d3142; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;";

        // Update All Folders button
        if (allFoldersButton != null) {
            allFoldersButton.setStyle(currentFolderFilter == null ? activeStyle : inactiveStyle);
        }

        // Update individual folder buttons
        for (Map.Entry<String, Button> entry : folderFilterButtonsMap.entrySet()) {
            String folderPath = entry.getKey();
            Button button = entry.getValue();
            button.setStyle(folderPath.equals(currentFolderFilter) ? activeStyle : inactiveStyle);
        }
    }

    private void applyFiltersAndSort() {
        // Show empty state if no media items
        if (mediaItems.isEmpty()) {
            showEmptyStateIfNeeded();
            return;
        }

        // Restore gallery view if it was replaced by empty state
        if (!(rootPane.getCenter() instanceof ScrollPane)) {
            rootPane.setCenter(galleryScrollPane);
        }

        galleryPane.getChildren().clear();

        String searchText = searchField.getText();
        String lowerSearch = searchText != null ? searchText.toLowerCase() : "";

        // Filter items
        List<MediaItem> filteredItems = new ArrayList<>();
        for (MediaItem item : mediaItems) {
            // Apply media type filter
            boolean matchesFilter = false;
            switch (currentFilter) {
                case ALL:
                    matchesFilter = true;
                    break;
                case PHOTOS:
                    matchesFilter = item.getType() == MediaItem.MediaType.IMAGE;
                    break;
                case VIDEOS:
                    matchesFilter = item.getType() == MediaItem.MediaType.VIDEO;
                    break;
            }

            // Apply search filter
            boolean matchesSearch = searchText == null || searchText.trim().isEmpty() ||
                    item.getName().toLowerCase().contains(lowerSearch);

            // Apply folder filter
            boolean matchesFolderFilter = currentFolderFilter == null ||
                    item.getPath().startsWith(currentFolderFilter);

            if (matchesFilter && matchesSearch && matchesFolderFilter) {
                filteredItems.add(item);
            }
        }

        // Sort items
        if ("Name".equals(currentSortBy)) {
            filteredItems.sort(Comparator.comparing(MediaItem::getName, String.CASE_INSENSITIVE_ORDER));
        } else if ("Date Modified".equals(currentSortBy)) {
            filteredItems.sort((a, b) -> Long.compare(b.getFile().lastModified(), a.getFile().lastModified()));
        }

        // Display items or show no results message
        if (filteredItems.isEmpty()) {
            // Show no results message centered in viewport
            StackPane noResultsContainer = new StackPane();
            noResultsContainer.setStyle("-fx-background-color: #0b0e14;");

            VBox noResultsBox = new VBox(20);
            noResultsBox.setAlignment(Pos.CENTER);
            noResultsBox.setStyle("-fx-padding: 40;");

            String filterType = currentFilter == MediaFilter.PHOTOS ? "photos"
                    : currentFilter == MediaFilter.VIDEOS ? "videos" : "items";

            Label noResultsIcon = new Label(
                    currentFilter == MediaFilter.PHOTOS ? "📷" : currentFilter == MediaFilter.VIDEOS ? "🎬" : "🔍");
            noResultsIcon.setStyle("-fx-font-size: 64px;");

            Label noResultsTitle = new Label("No " + filterType + " found");
            noResultsTitle.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 20px; -fx-font-weight: bold;");

            Label noResultsSubtitle = new Label(
                    currentFolderFilter != null ? "This folder doesn't contain any " + filterType
                            : searchText != null && !searchText.trim().isEmpty() ? "Try adjusting your search"
                                    : "Try selecting a different filter");
            noResultsSubtitle.setStyle("-fx-text-fill: #7a7d8a; -fx-font-size: 14px;");

            noResultsBox.getChildren().addAll(noResultsIcon, noResultsTitle, noResultsSubtitle);
            noResultsContainer.getChildren().add(noResultsBox);

            // Replace center content with no results message
            rootPane.setCenter(noResultsContainer);
        } else {
            // Restore gallery view if needed
            if (!(rootPane.getCenter() instanceof ScrollPane)) {
                rootPane.setCenter(galleryScrollPane);
            }

            for (MediaItem item : filteredItems) {
                StackPane card = createMediaCard(item);
                galleryPane.getChildren().add(card);
            }
        }

        // Update count
        itemCountLabel.setText(filteredItems.size() + " items");
    }

    private void filterByFolder(String folderPath) {
        // Update header to show current folder
        String folderName = new File(folderPath).getName();
        breadcrumbLabel.setText("All Folders / " + folderName);
        folderTitleLabel.setText(folderName);

        // Apply filters (this will be handled by applyFiltersAndSort with folder
        // context)
        applyFiltersAndSort();
    }

    @FXML
    protected void onFolderLocationClick() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Folder");

        // Set initial directory to last opened folder if available
        if (lastOpenedFolder != null && lastOpenedFolder.exists()) {
            directoryChooser.setInitialDirectory(lastOpenedFolder);
        } else if (lastOpenedFolder != null && lastOpenedFolder.getParentFile() != null
                && lastOpenedFolder.getParentFile().exists()) {
            // If last folder doesn't exist, try its parent
            directoryChooser.setInitialDirectory(lastOpenedFolder.getParentFile());
        }

        Stage stage = (Stage) rootPane.getScene().getWindow();
        File selectedDirectory = directoryChooser.showDialog(stage);

        if (selectedDirectory != null && selectedDirectory.exists()) {
            // Remember this folder for next time
            lastOpenedFolder = selectedDirectory;

            String folderPath = selectedDirectory.getAbsolutePath();
            if (!selectedFolders.contains(folderPath)) {
                // Don't add to sidebar yet - let scanFolder add only folders with media
                scanFolder(selectedDirectory);
            }
        }
    }

    private void addFolderToSidebar(File folder) {
        String folderPath = folder.getAbsolutePath();
        String folderName = folder.getName();

        HBox folderCard = new HBox(10);
        folderCard.setAlignment(Pos.CENTER_LEFT);
        folderCard.setStyle(
                "-fx-background-color: #0b0e14; -fx-padding: 10; -fx-background-radius: 8; -fx-cursor: hand;");

        Label folderLabel = new Label("📁 " + folderName);
        folderLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 13px;");
        folderLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(folderLabel, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button removeBtn = new Button("✕");
        removeBtn.setStyle(
                "-fx-background-color: #d74545; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 4 8; -fx-background-radius: 4; -fx-cursor: hand;");
        removeBtn.setOnAction(e -> removeFolder(folderPath));

        folderCard.getChildren().addAll(folderLabel, spacer, removeBtn);

        // Click to filter by this folder
        folderCard.setOnMouseClicked(e -> {
            if (e.getTarget() != removeBtn && !removeBtn.equals(e.getTarget())) {
                filterByFolder(folderPath);
            }
        });

        // Hover effect
        folderCard.setOnMouseEntered(e -> folderCard.setStyle(
                "-fx-background-color: #2d3142; -fx-padding: 10; -fx-background-radius: 8; -fx-cursor: hand;"));
        folderCard.setOnMouseExited(e -> folderCard.setStyle(
                "-fx-background-color: #0b0e14; -fx-padding: 10; -fx-background-radius: 8; -fx-cursor: hand;"));

        folderList.getChildren().add(folderCard);
        folderCards.put(folderPath, folderCard);

        // Add folder filter button to header
        Button folderFilterButton = new Button("📁 " + folderName);
        folderFilterButton.setStyle(
                "-fx-background-color: #2d3142; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;");
        folderFilterButton.setOnAction(e -> onFolderFilterClick(folderPath));

        folderFilterButtons.getChildren().add(folderFilterButton);
        folderFilterButtonsMap.put(folderPath, folderFilterButton);
    }

    private void removeFolder(String folderPath) {
        selectedFolders.remove(folderPath);
        HBox card = folderCards.remove(folderPath);
        if (card != null) {
            folderList.getChildren().remove(card);
        }

        // Remove folder filter button
        Button filterButton = folderFilterButtonsMap.remove(folderPath);
        if (filterButton != null) {
            folderFilterButtons.getChildren().remove(filterButton);
        }

        // Reset folder filter if the removed folder was selected
        if (folderPath.equals(currentFolderFilter)) {
            currentFolderFilter = null;
        }

        // Remove media items from this folder
        mediaItems.removeIf(item -> item.getPath().startsWith(folderPath));
        refreshGallery();
        updateHeaderInfo();
    }

    private void scanFolder(File folder) {
        // Scan folder for media files in background (including subfolders)
        CompletableFuture.runAsync(() -> {
            List<MediaItem> newItems = new ArrayList<>();
            Map<String, Integer> folderMediaCount = new HashMap<>();
            scanFolderRecursive(folder, newItems, folderMediaCount);

            // Add items to gallery on UI thread
            Platform.runLater(() -> {
                mediaItems.addAll(newItems);

                // Only add folders that contain media files
                for (Map.Entry<String, Integer> entry : folderMediaCount.entrySet()) {
                    String folderPath = entry.getKey();
                    int mediaCount = entry.getValue();

                    // Only add if folder has media and isn't already added
                    if (mediaCount > 0 && !selectedFolders.contains(folderPath)) {
                        selectedFolders.add(folderPath);
                        addFolderToSidebar(new File(folderPath));
                    }
                }

                refreshGallery();
                updateHeaderInfo();
            });
        });
    }

    private void scanFolderRecursive(File folder, List<MediaItem> items, Map<String, Integer> folderMediaCount) {
        File[] files = folder.listFiles();
        int mediaFilesInThisFolder = 0;

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // Recursively scan subdirectories
                    scanFolderRecursive(file, items, folderMediaCount);
                } else if (file.isFile()) {
                    MediaItem item = null;
                    if (ThumbnailGenerator.isImageFile(file)) {
                        item = new MediaItem(file, MediaItem.MediaType.IMAGE);
                        // Generate thumbnail immediately (small size for memory efficiency)
                        Image thumbnail = ThumbnailGenerator.generateImageThumbnail(file);
                        item.setThumbnail(thumbnail);
                        items.add(item);
                        mediaFilesInThisFolder++;
                    } else if (ThumbnailGenerator.isVideoFile(file)) {
                        item = new MediaItem(file, MediaItem.MediaType.VIDEO);
                        items.add(item);
                        mediaFilesInThisFolder++;
                        
                        // Generate video thumbnail asynchronously
                        MediaItem finalItem = item;
                        ThumbnailGenerator.generateVideoThumbnail(file).thenAccept(thumbnail -> {
                            if (thumbnail != null) {
                                finalItem.setThumbnail(thumbnail);
                            }
                            Platform.runLater(() -> updateGalleryItem(finalItem));
                        });
                    }
                }
            }
        }

        // Only track this folder if it has media files
        if (mediaFilesInThisFolder > 0) {
            folderMediaCount.put(folder.getAbsolutePath(), mediaFilesInThisFolder);
        }
    }

    private void refreshGallery() {
        applyFiltersAndSort();

        // Force layout update
        galleryPane.requestLayout();
    }

    private void updateGalleryItem(MediaItem item) {
        // Find and update the card for this item
        refreshGallery();
    }
    
    private void updateCardWithThumbnail(StackPane card, MediaItem item, Image thumbnail) {
        // Clear placeholder
        card.getChildren().clear();
        card.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        
        // Add thumbnail - fill the square
        ImageView thumbnailView = new ImageView(thumbnail);
        thumbnailView.setPreserveRatio(false); // Fill square
        thumbnailView.setSmooth(false); // Faster rendering, less memory
        thumbnailView.setFitWidth(300);
        thumbnailView.setFitHeight(300);
        
        card.getChildren().add(thumbnailView);
        
        // Add play icon overlay for videos
        if (item.getType() == MediaItem.MediaType.VIDEO) {
            StackPane playIconContainer = new StackPane();
            playIconContainer.setMaxSize(40, 40);
            playIconContainer.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-background-radius: 20;");
            
            Label playIcon = new Label("▶");
            playIcon.setStyle("-fx-text-fill: rgba(255,255,255,0.9); -fx-font-size: 16px;");
            playIconContainer.getChildren().add(playIcon);
            
            card.getChildren().add(playIconContainer);
        }
        
        // Request layout update
        card.requestLayout();
    }

    private StackPane createMediaCard(MediaItem item) {
        // Square thumbnail card for uniform grid
        StackPane card = new StackPane();
        card.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        card.setPrefSize(300, 300); // Larger square size
        card.setMinSize(300, 300);
        card.setMaxSize(300, 300);

        if (item.getThumbnail() != null) {
            // Thumbnail loaded - fill the square card
            ImageView thumbnailView = new ImageView(item.getThumbnail());
            thumbnailView.setPreserveRatio(false); // Fill square
            thumbnailView.setSmooth(false); // Faster rendering, less memory
            thumbnailView.setFitWidth(300);
            thumbnailView.setFitHeight(300);

            card.getChildren().add(thumbnailView);

            // Add play icon overlay for videos
            if (item.getType() == MediaItem.MediaType.VIDEO) {
                StackPane playIconContainer = new StackPane();
                playIconContainer.setMaxSize(40, 40);
                playIconContainer.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-background-radius: 20;");

                Label playIcon = new Label("▶");
                playIcon.setStyle("-fx-text-fill: rgba(255,255,255,0.9); -fx-font-size: 16px;");
                playIconContainer.getChildren().add(playIcon);

                card.getChildren().add(playIconContainer);
            }
        } else {
            // No thumbnail yet (video loading)
            card.setStyle("-fx-background-color: #2d3142; -fx-cursor: hand;");
            Label placeholderIcon = new Label("🎬");
            placeholderIcon.setStyle("-fx-font-size: 48px;");
            card.getChildren().add(placeholderIcon);
        }

        // Hover effect - subtle opacity change
        card.setOnMouseEntered(e -> {
            card.setOpacity(0.9);
        });
        card.setOnMouseExited(e -> {
            card.setOpacity(1.0);
        });

        // Click to open fullscreen viewer
        card.setOnMouseClicked(e -> {
            currentMediaIndex = mediaItems.indexOf(item);
            showFullscreenViewer(item);
        });

        return card;
    }

    private void showFullscreenViewer(MediaItem item) {
        // Switch to fullscreen mode for media viewing
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.setFullScreen(true);

        // Create fullscreen viewer overlay
        fullscreenViewer = new StackPane();
        fullscreenViewer.setStyle("-fx-background-color: #000000;");

        if (item.getType() == MediaItem.MediaType.IMAGE) {
            setupImageViewer(fullscreenViewer, item);
        } else {
            setupVideoViewer(fullscreenViewer, item);
        }

        // Hide sidebar immediately
        hideSidebar();
        sidebar.setVisible(false);

        // Hide custom title bar
        if (customTitleBar != null) {
            customTitleBar.setVisible(false);
            customTitleBar.setManaged(false);
        }

        // Hide header and show only media
        rootPane.setTop(null);
        rootPane.setCenter(fullscreenViewer);

        // Handle keyboard events
        fullscreenViewer.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                closeFullscreenViewer();
            } else if (event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.DOWN) {
                navigateToNextMedia();
            } else if (event.getCode() == KeyCode.LEFT || event.getCode() == KeyCode.UP) {
                navigateToPreviousMedia();
            } else if (event.getCode() == KeyCode.F || event.getCode() == KeyCode.F11) {
                toggleFullscreen();
            } else if (event.getCode() == KeyCode.SPACE) {
                if (currentMediaPlayer != null) {
                    if (currentMediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                        currentMediaPlayer.pause();
                    } else {
                        currentMediaPlayer.play();
                    }
                }
            }
        });
        fullscreenViewer.requestFocus();
    }

    private void setupImageViewer(StackPane container, MediaItem item) {
        BorderPane layout = new BorderPane();
        layout.setStyle("-fx-background-color: #000000;");

        // Image in center with proper constraints
        Image image = new Image(item.getFile().toURI().toString());
        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        // Create a constrained container using Pane
        Pane imageContainer = new Pane() {
            @Override
            protected void layoutChildren() {
                super.layoutChildren();
                double containerWidth = getWidth();
                double containerHeight = getHeight();

                if (containerWidth > 0 && containerHeight > 0) {
                    double imageRatio = image.getWidth() / image.getHeight();
                    double containerRatio = containerWidth / containerHeight;

                    double newWidth, newHeight;

                    if (imageRatio > containerRatio) {
                        // Image is wider - fit to width
                        newWidth = containerWidth;
                        newHeight = containerWidth / imageRatio;
                    } else {
                        // Image is taller - fit to height
                        newHeight = containerHeight;
                        newWidth = containerHeight * imageRatio;
                    }

                    imageView.setFitWidth(newWidth);
                    imageView.setFitHeight(newHeight);

                    // Center the image
                    double x = (containerWidth - newWidth) / 2;
                    double y = (containerHeight - newHeight) / 2;
                    imageView.relocate(x, y);
                }
            }
        };
        imageContainer.getChildren().add(imageView);
        imageContainer.setStyle("-fx-background-color: #000000;");

        // Rotate button at top-right - always visible
        Button rotateButton = new Button("↻");
        rotateButton.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.7); -fx-text-fill: white; -fx-font-size: 20px; -fx-cursor: hand; -fx-padding: 10 15; -fx-background-radius: 8;");
        rotateButton.setTooltip(new Tooltip("Rotate 90°"));

        // Rotate button action with proper bounds handling
        rotateButton.setOnAction(e -> {
            double currentRotation = imageContainer.getRotate();
            double newRotation = currentRotation + 90;
            imageContainer.setRotate(newRotation);
            imageContainer.requestLayout();
        });

        // Position rotate button at top-right
        StackPane buttonContainer = new StackPane(rotateButton);
        buttonContainer.setAlignment(Pos.TOP_RIGHT);
        buttonContainer.setStyle("-fx-padding: 20;");

        // Use StackPane to overlay button on image
        StackPane imageStack = new StackPane();
        imageStack.getChildren().addAll(imageContainer, buttonContainer);

        layout.setCenter(imageStack);
        container.getChildren().add(layout);
    }

    private void setupVideoViewer(StackPane container, MediaItem item) {
        BorderPane layout = new BorderPane();
        layout.setStyle("-fx-background-color: #000000;");

        // Video in center with proper constraints
        try {
            Media media = new Media(item.getFile().toURI().toString());
            currentMediaPlayer = new MediaPlayer(media);
            MediaView mediaView = new MediaView(currentMediaPlayer);
            mediaView.setPreserveRatio(true);
            mediaView.setSmooth(true);

            // Disable looping by default (changed to 1 cycle)
            currentMediaPlayer.setCycleCount(1);

            // Track if video actually started playing
            final boolean[] hasPlayed = { false };

            currentMediaPlayer.statusProperty().addListener((obs, oldStatus, newStatus) -> {
                if (newStatus == MediaPlayer.Status.PLAYING) {
                    hasPlayed[0] = true;
                }
            });

            // Error handler - only show error if video never started playing
            currentMediaPlayer.setOnError(() -> {

                // Wait a bit to see if video recovers and starts playing
                javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(
                        javafx.util.Duration.seconds(2));
                delay.setOnFinished(e -> {
                    if (!hasPlayed[0]) {
                        // Video truly failed - show error UI
                        Platform.runLater(() -> {
                            VBox errorBox = new VBox(20);
                            errorBox.setAlignment(Pos.CENTER);
                            errorBox.setStyle("-fx-padding: 20;");

                            Label errorLabel = new Label("Cannot play this video in app");
                            errorLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 24px; -fx-font-weight: bold;");

                            Label detailLabel = new Label(item.getName());
                            detailLabel.setStyle("-fx-text-fill: #f2f2f2; -fx-font-size: 14px;");
                            detailLabel.setWrapText(true);
                            detailLabel.setMaxWidth(600);

                            Label reasonLabel = new Label(
                                    "This video codec is not supported. You can open it in your system's default video player.");
                            reasonLabel.setStyle("-fx-text-fill: #999999; -fx-font-size: 12px;");
                            reasonLabel.setWrapText(true);
                            reasonLabel.setMaxWidth(600);

                            Button openExternalBtn = new Button("🎬 Open in System Player");
                            openExternalBtn.setStyle("-fx-font-size: 14px; -fx-padding: 10 20;");
                            openExternalBtn.setOnAction(ev -> {
                                try {
                                    java.awt.Desktop.getDesktop().open(item.getFile());
                                } catch (Exception ex) {
                                    // Failed to open externally
                                }
                            });

                            errorBox.getChildren().addAll(errorLabel, detailLabel, reasonLabel, openExternalBtn);

                            container.getChildren().clear();
                            container.getChildren().add(errorBox);
                        });
                    }
                });
                delay.play();
            });

            // Create a constrained container using Pane
            Pane videoContainer = new Pane() {
                @Override
                protected void layoutChildren() {
                    super.layoutChildren();
                    double containerWidth = getWidth();
                    double containerHeight = getHeight();

                    if (containerWidth > 0 && containerHeight > 0 &&
                            currentMediaPlayer.getMedia() != null) {
                        double videoWidth = currentMediaPlayer.getMedia().getWidth();
                        double videoHeight = currentMediaPlayer.getMedia().getHeight();

                        if (videoWidth > 0 && videoHeight > 0) {
                            double videoRatio = videoWidth / videoHeight;
                            double containerRatio = containerWidth / containerHeight;

                            double newWidth, newHeight;

                            if (videoRatio > containerRatio) {
                                // Video is wider - fit to width
                                newWidth = containerWidth;
                                newHeight = containerWidth / videoRatio;
                            } else {
                                // Video is taller - fit to height
                                newHeight = containerHeight;
                                newWidth = containerHeight * videoRatio;
                            }

                            mediaView.setFitWidth(newWidth);
                            mediaView.setFitHeight(newHeight);

                            // Center the video
                            double x = (containerWidth - newWidth) / 2;
                            double y = (containerHeight - newHeight) / 2;
                            mediaView.relocate(x, y);
                        }
                    }
                }
            };
            videoContainer.getChildren().add(mediaView);
            videoContainer.setStyle("-fx-background-color: #000000;");

            // Trigger layout when video is ready
            currentMediaPlayer.setOnReady(() -> {
                videoContainer.requestLayout();
            });

            // Container for controls
            StackPane videoStack = new StackPane();
            videoStack.getChildren().add(videoContainer);

            // --- Control Bar Setup ---
            HBox controlsBar = new HBox(15);
            controlsBar.setAlignment(Pos.CENTER_LEFT);
            controlsBar.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7); -fx-padding: 10 20;");
            controlsBar.setMaxWidth(Double.MAX_VALUE); // Full width
            controlsBar.setMinHeight(60);
            controlsBar.setMaxHeight(60);
            StackPane.setAlignment(controlsBar, Pos.BOTTOM_CENTER);

            // Styles for buttons
            String btnStyle = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 16px; -fx-cursor: hand; -fx-background-radius: 5;";
            String activeBtnStyle = "-fx-background-color: rgba(255, 255, 255, 0.2); -fx-text-fill: white; -fx-font-size: 16px; -fx-cursor: hand; -fx-background-radius: 5;";

            // 1. Play/Pause Button
            Button playPauseBtn = new Button("⏸"); // Default to pause symbol as it auto-plays
            playPauseBtn.setStyle(btnStyle);
            playPauseBtn.setMinWidth(40);

            Runnable updatePlayBtn = () -> {
                if (currentMediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                    playPauseBtn.setText("⏸");
                } else {
                    playPauseBtn.setText("▶");
                }
            };

            playPauseBtn.setOnAction(e -> {
                if (currentMediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                    currentMediaPlayer.pause();
                } else {
                    currentMediaPlayer.play();
                }
            });

            currentMediaPlayer.statusProperty().addListener((obs, old, newVal) -> updatePlayBtn.run());

            // 2. Seek Slider
            javafx.scene.control.Slider seekSlider = new javafx.scene.control.Slider();
            seekSlider.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(seekSlider, Priority.ALWAYS); // Grow to fill space
            seekSlider.setDisable(true); // Initially disabled

            // Slider Logic
            final boolean[] isUpdatingFromPlayer = { false };

            currentMediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                if (!seekSlider.isValueChanging()) {
                    isUpdatingFromPlayer[0] = true;
                    seekSlider.setValue(newTime.toSeconds());
                    isUpdatingFromPlayer[0] = false;
                }
            });

            Runnable onReady = () -> {
                javafx.util.Duration total = currentMediaPlayer.getTotalDuration();
                if (total != null && !total.isIndefinite() && !total.isUnknown()) {
                    seekSlider.setMax(total.toSeconds());
                    seekSlider.setDisable(false);
                }
            };

            if (currentMediaPlayer.getStatus() == MediaPlayer.Status.READY) {
                onReady.run();
            } else {
                currentMediaPlayer.setOnReady(() -> {
                    onReady.run();
                    videoContainer.requestLayout();
                });
            }

            currentMediaPlayer.totalDurationProperty().addListener((obs, oldD, newD) -> {
                if (newD != null && !newD.isIndefinite() && !newD.isUnknown()) {
                    seekSlider.setMax(newD.toSeconds());
                    seekSlider.setDisable(false);
                }
            });

            // Reset at end
            currentMediaPlayer.setOnEndOfMedia(() -> {
                seekSlider.setValue(seekSlider.getMax());
                playPauseBtn.setText("▶"); // Show play button at end
            });

            seekSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (!isUpdatingFromPlayer[0] && !seekSlider.isValueChanging()) {
                    currentMediaPlayer.seek(javafx.util.Duration.seconds(newVal.doubleValue()));
                }
            });

            seekSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
                if (!isChanging) {
                    currentMediaPlayer.seek(javafx.util.Duration.seconds(seekSlider.getValue()));
                }
            });

            // 3. Loop Button
            Button loopBtn = new Button("🔁");
            loopBtn.setTooltip(new Tooltip("Toggle Loop"));
            loopBtn.setStyle(btnStyle); // Default off

            loopBtn.setOnAction(e -> {
                if (currentMediaPlayer.getCycleCount() == 1) {
                    currentMediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                    loopBtn.setStyle(activeBtnStyle); // Visual feedback
                } else {
                    currentMediaPlayer.setCycleCount(1);
                    loopBtn.setStyle(btnStyle);
                }
            });

            // 4. Rotate Button
            Button rotateBtn = new Button("↻");
            rotateBtn.setTooltip(new Tooltip("Rotate Video"));
            rotateBtn.setStyle(btnStyle);

            rotateBtn.setOnAction(e -> {
                double currentRotation = videoContainer.getRotate();
                double newRotation = currentRotation + 90;
                videoContainer.setRotate(newRotation);
                videoContainer.requestLayout();
            });

            // Add all to bar
            controlsBar.getChildren().addAll(playPauseBtn, seekSlider, loopBtn, rotateBtn);

            // Add bar to stack
            videoStack.getChildren().add(controlsBar);

            // --- Auto-Hide Logic ---
            controlsBar.setOpacity(1.0); // Start visible

            javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(500), controlsBar);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(200), controlsBar);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);

            javafx.animation.PauseTransition idleTimer = new javafx.animation.PauseTransition(
                    javafx.util.Duration.seconds(3));
            idleTimer.setOnFinished(ev -> fadeOut.play());

            // Reset timer on movement
            videoStack.setOnMouseMoved(e -> {
                if (controlsBar.getOpacity() < 1.0) {
                    fadeIn.play();
                }
                controlsBar.setOpacity(1.0); // Ensure visible
                idleTimer.playFromStart(); // Reset timer
            });

            // Also show on click
            videoStack.setOnMouseClicked(e -> {
                if (controlsBar.getOpacity() < 1.0) {
                    fadeIn.play();
                    idleTimer.playFromStart();
                } else {
                    // If clicking not on controls, toggle play/pause
                    if (!controlsBar.contains(controlsBar.sceneToLocal(e.getSceneX(), e.getSceneY()))) {
                        if (currentMediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                            currentMediaPlayer.pause();
                        } else {
                            currentMediaPlayer.play();
                        }
                    }
                }
            });

            // Start timer initially
            idleTimer.play();

            layout.setCenter(videoStack);
            container.getChildren().add(layout);

            // Auto-play
            currentMediaPlayer.setAutoPlay(true);

        } catch (Exception e) {

            // Show error message
            VBox errorBox = new VBox(20);
            errorBox.setAlignment(Pos.CENTER);
            errorBox.setStyle("-fx-padding: 20;");

            Label errorLabel = new Label("Error loading video: " + item.getName());
            errorLabel.setStyle("-fx-text-fill: #ff0000; -fx-font-size: 18px;");

            Button closeButton = new Button("✕ Close");
            closeButton.setOnAction(ev -> closeFullscreenViewer());

            errorBox.getChildren().addAll(errorLabel, closeButton);

            layout.setCenter(errorBox);
            container.getChildren().add(layout);
        }
    }

    private void navigateToNextMedia() {
        if (currentMediaIndex < mediaItems.size() - 1) {
            currentMediaIndex++;
            switchToMedia(mediaItems.get(currentMediaIndex));
        }
    }

    private void navigateToPreviousMedia() {
        if (currentMediaIndex > 0) {
            currentMediaIndex--;
            switchToMedia(mediaItems.get(currentMediaIndex));
        }
    }

    private void switchToMedia(MediaItem item) {
        // Stop current media player if exists
        if (currentMediaPlayer != null) {
            currentMediaPlayer.stop();
            currentMediaPlayer.dispose();
            currentMediaPlayer = null;
        }

        // Clear and rebuild the fullscreen viewer content
        fullscreenViewer.getChildren().clear();

        if (item.getType() == MediaItem.MediaType.IMAGE) {
            setupImageViewer(fullscreenViewer, item);
        } else {
            setupVideoViewer(fullscreenViewer, item);
        }

        // Ensure focus for keyboard events
        fullscreenViewer.requestFocus();
    }

    private void toggleFullscreen() {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.setFullScreen(!stage.isFullScreen());
    }

    private void closeFullscreenViewer() {
        // Stop and dispose media player
        if (currentMediaPlayer != null) {
            currentMediaPlayer.stop();
            currentMediaPlayer.dispose();
            currentMediaPlayer = null;
        }

        // Restore custom title bar
        if (customTitleBar != null) {
            customTitleBar.setVisible(true);
            customTitleBar.setManaged(true);
        }

        // Restore sidebar visibility
        sidebar.setVisible(true);

        // Restore header and gallery view
        rootPane.setTop(headerNode);
        rootPane.setCenter(galleryScrollPane);

        // Clear fullscreen viewer
        fullscreenViewer = null;

        // Exit fullscreen mode
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.setFullScreen(false);
    }

}
