package com.example.pixz;

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
    private Button addFolderButton;
    
    @FXML
    private Button homeButton;
    
    @FXML
    private Button favoritesButton;

    @FXML
    private Button allMediaButton;

    @FXML
    private Button photosButton;

    @FXML
    private Button videosButton;

    @FXML
    private Button sortButton;

    @FXML
    private Button refreshButton;

    // Top navigation bar buttons
    @FXML
    private Button galleryTab;

    @FXML
    private Button collectionsTab;

    @FXML
    private Button archiveTab;

    @FXML
    private Button notificationButton;

    @FXML
    private Button settingsButton;

    @FXML
    private Button macCloseButton;

    @FXML
    private Button macMinimizeButton;

    @FXML
    private Button macMaximizeButton;

    private final Set<String> selectedFolders = new HashSet<>();
    private final Set<String> favoritePaths = new HashSet<>();
    private final List<MediaItem> mediaItems = new ArrayList<>();
    private final List<MediaItem> displayedItems = new ArrayList<>();
    private final Map<String, HBox> folderCards = new HashMap<>();

    // Filter and sort state
    private enum MediaFilter {
        ALL, PHOTOS, VIDEOS
    }

    private MediaFilter currentFilter = MediaFilter.ALL;
    private boolean showOnlyFavorites = false;
    private String currentSortBy = "Name";
    private String currentFolderFilter = null; // null means all folders
    
    // Flag to control mouse moved event response (prevent showing controls on keyboard navigation)
    private boolean respondToMouseMoved = true;
    private double savedScrollPosition = 0.0; // Save scroll position when opening fullscreen

    // Playback retry state
    private MediaItem currentAttemptingItem = null;
    private int currentRetryAttempts = 0;
    private static final int MAX_RETRIES = 2;

    // Fullscreen viewer components
    private StackPane fullscreenViewer;
    private MediaPlayer currentMediaPlayer;
    private int currentMediaIndex = -1;
    private javafx.scene.Node headerNode; // Store header to restore later

    private javafx.scene.layout.HBox customTitleBar; // Custom title bar reference

    // Sidebar is now permanently visible (no animation needed)

    // Remember last opened folder
    private File lastOpenedFolder = null;

    // Track seekbar interaction for context-aware keyboard navigation
    private boolean seekbarInteracted = false;

    // Throttle navigation to prevent rapid switching issues
    private long lastSwitchTime = 0;
    private static final long SWITCH_THROTTLE_MS = 200;

    // Method to set custom title bar reference
    public void setCustomTitleBar(javafx.scene.layout.HBox titleBar) {
        this.customTitleBar = titleBar;
    }

    @FXML
    public void initialize() {
        // Setup top navigation bar
        setupTopNavigationBar();

        // Setup add folder button hover effect
        setupAddFolderButtonHover();

        // Setup search functionality
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFiltersAndSort());

        // Enable scroll past end - add extra padding at bottom
        setupScrollPastEnd();

        // Sidebar is now permanently visible - no setup needed

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
     * Setup top navigation bar with hover effects and tab functionality
     */
    private void setupTopNavigationBar() {
        // Gallery tab is active by default
        // Collections and Archive tabs are disabled (functionality ignored as requested)
        
        // Add hover effects for navigation tabs
        setupTabHoverEffect(collectionsTab);
        setupTabHoverEffect(archiveTab);
        
        // Add hover effects for icon buttons
        setupIconButtonHoverEffect(notificationButton, "transparent");
        setupIconButtonHoverEffect(settingsButton, "transparent");
        
        // Setup macOS-style window control buttons
        setupMacOSWindowControls();
        
        // Disable functionality for non-gallery tabs (as requested)
        collectionsTab.setOnAction(e -> {
            // Functionality ignored
        });
        archiveTab.setOnAction(e -> {
            // Functionality ignored
        });
        
        // Disable functionality for right side buttons (as requested)
        notificationButton.setOnAction(e -> {
            // Functionality ignored
        });
        settingsButton.setOnAction(e -> {
            // Functionality ignored
        });
    }
    
    /**
     * Setup hover effect for add folder button
     */
    private void setupAddFolderButtonHover() {
        String normalStyle = "-fx-background-color: #2d3142; -fx-text-fill: #89b4fa; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 6; -fx-cursor: hand; -fx-border-width: 0;";
        String hoverStyle = "-fx-background-color: #3a4a5a; -fx-text-fill: #b4befe; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 6; -fx-cursor: hand; -fx-border-width: 0;";
        
        addFolderButton.setOnMouseEntered(e -> addFolderButton.setStyle(hoverStyle));
        addFolderButton.setOnMouseExited(e -> addFolderButton.setStyle(normalStyle));
    }
    
    /**
     * Setup macOS-style window control buttons
     */
    private void setupMacOSWindowControls() {
        // Get the stage reference
        Platform.runLater(() -> {
            javafx.stage.Stage stage = (javafx.stage.Stage) rootPane.getScene().getWindow();
            
            // Close button (red) - closes the window
            macCloseButton.setOnAction(e -> stage.close());
            
            // Maximize button (yellow) - toggles maximize/restore
            macMaximizeButton.setOnAction(e -> {
                if (stage.isMaximized()) {
                    stage.setMaximized(false);
                } else {
                    stage.setMaximized(true);
                }
            });
            
            // Minimize button (green) - minimizes the window
            macMinimizeButton.setOnAction(e -> stage.setIconified(true));
            
            // Add hover effects to show symbols on macOS buttons
            setupMacButtonHoverEffect(macCloseButton, "×", "#ff5f57", "#e04b43");
            setupMacButtonHoverEffect(macMaximizeButton, "□", "#ffbd2e", "#e5a81e");
            setupMacButtonHoverEffect(macMinimizeButton, "−", "#28c840", "#1fb032");
        });
    }
    
    /**
     * Setup hover effect for macOS-style buttons to show symbols
     */
    private void setupMacButtonHoverEffect(Button button, String symbol, String normalColor, String hoverColor) {
        String normalStyle = "-fx-background-color: " + normalColor + "; -fx-min-width: 14; -fx-max-width: 14; -fx-min-height: 14; -fx-max-height: 14; -fx-background-radius: 50%; -fx-cursor: hand; -fx-border-width: 0; -fx-padding: 0; -fx-text-fill: transparent; -fx-font-size: 10px; -fx-font-weight: bold;";
        String hoverStyle = "-fx-background-color: " + hoverColor + "; -fx-min-width: 14; -fx-max-width: 14; -fx-min-height: 14; -fx-max-height: 14; -fx-background-radius: 50%; -fx-cursor: hand; -fx-border-width: 0; -fx-padding: 0; -fx-text-fill: #000000; -fx-font-size: 10px; -fx-font-weight: bold;";
        
        button.setText(symbol);
        button.setStyle(normalStyle);
        
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(normalStyle));
    }
    
    /**
     * Setup hover effect for navigation tabs
     */
    private void setupTabHoverEffect(Button tab) {
        String normalStyle = "-fx-background-color: transparent; -fx-text-fill: #7a7d8a; -fx-font-size: 14px; -fx-padding: 8 0; -fx-cursor: hand; -fx-background-radius: 0;";
        String hoverStyle = "-fx-background-color: transparent; -fx-text-fill: #cdd6f4; -fx-font-size: 14px; -fx-padding: 8 0; -fx-cursor: hand; -fx-background-radius: 0;";
        
        tab.setOnMouseEntered(e -> tab.setStyle(hoverStyle));
        tab.setOnMouseExited(e -> tab.setStyle(normalStyle));
    }
    
    /**
     * Setup hover effect for icon buttons
     */
    private void setupIconButtonHoverEffect(Button button, String baseColor) {
        // Notification button has smaller font size (14px), settings button has 16px
        String fontSize = button == notificationButton ? "14px" : "16px";
        String normalStyle = "-fx-background-color: " + baseColor + "; -fx-text-fill: #cdd6f4; -fx-font-size: " + fontSize + "; -fx-padding: 6; -fx-cursor: hand; -fx-background-radius: 6; -fx-min-width: 32; -fx-min-height: 32;";
        String hoverColor = baseColor.equals("transparent") ? "#2d3142" : "#fab4c8";
        String hoverStyle = "-fx-background-color: " + hoverColor + "; -fx-text-fill: #ffffff; -fx-font-size: " + fontSize + "; -fx-padding: 6; -fx-cursor: hand; -fx-background-radius: 6; -fx-min-width: 32; -fx-min-height: 32;";
        
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(normalStyle));
    }

    /**
     * Setup scroll past end functionality
     * Adds extra padding at bottom so user can scroll past the last item
     */
    private void setupScrollPastEnd() {
        Platform.runLater(() -> {
            // Add listener to viewport height to calculate padding
            galleryScrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
                if (newBounds != null) {
                    // Add padding equal to 70% of viewport height
                    double extraPadding = newBounds.getHeight() * 0.7;
                    galleryPane.setStyle("-fx-background-color: #000000; -fx-padding: 0 0 " + extraPadding + " 0;");
                }
            });
        });
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
        
        // Load favorites
        favoritePaths.addAll(SessionManager.loadFavorites());
        
        // Mark items as favorites
        for (MediaItem item : mediaItems) {
            if (favoritePaths.contains(item.getPath())) {
                item.setFavorite(true);
            }
        }
    }

    /**
     * Save current session (called on app close)
     */
    public void saveCurrentSession() {

        SessionManager.saveSession(selectedFolders);
        SessionManager.saveFavorites(favoritePaths);
    }

    private void showEmptyStateIfNeeded() {
        if (mediaItems.isEmpty()) {
            // Create empty state UI centered in viewport
            StackPane emptyStateContainer = new StackPane();
            emptyStateContainer.setStyle("-fx-background-color: #000000;");

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
                    "-fx-background-color: #89b4fa; -fx-text-fill: #1e1e2e; -fx-font-size: 16px; -fx-padding: 12 24; -fx-background-radius: 8; -fx-cursor: hand;");
            addFolderBtn.setOnAction(e -> onFolderLocationClick());

            // Hover effect
            addFolderBtn.setOnMouseEntered(e -> addFolderBtn.setStyle(
                    "-fx-background-color: #b4befe; -fx-text-fill: #1e1e2e; -fx-font-size: 16px; -fx-padding: 12 24; -fx-background-radius: 8; -fx-cursor: hand;"));
            addFolderBtn.setOnMouseExited(e -> addFolderBtn.setStyle(
                    "-fx-background-color: #89b4fa; -fx-text-fill: #1e1e2e; -fx-font-size: 16px; -fx-padding: 12 24; -fx-background-radius: 8; -fx-cursor: hand;"));

            emptyState.getChildren().addAll(emptyIcon, emptyTitle, emptySubtitle, addFolderBtn);
            emptyStateContainer.getChildren().add(emptyState);

            // Replace the center content with empty state
            rootPane.setCenter(emptyStateContainer);
        }
    }

    // Sidebar is now permanently visible - no animation methods needed

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
        updateSidebarButtonStyles();
        applyFiltersAndSort();
    }

    @FXML
    protected void onFilterPhotos() {
        currentFilter = MediaFilter.PHOTOS;
        updateFilterButtonStyles();
        updateSidebarButtonStyles();
        applyFiltersAndSort();
    }

    @FXML
    protected void onFilterVideos() {
        currentFilter = MediaFilter.VIDEOS;
        updateFilterButtonStyles();
        updateSidebarButtonStyles();
        applyFiltersAndSort();
    }
    
    @FXML
    protected void onHomeClick() {
        currentFilter = MediaFilter.ALL;
        showOnlyFavorites = false;
        currentFolderFilter = null;
        updateFilterButtonStyles();
        updateSidebarButtonStyles();
        updateHeaderInfo();
        applyFiltersAndSort();
    }
    
    @FXML
    protected void onFavoritesClick() {
        showOnlyFavorites = true;
        // Keep current media type filter (ALL, PHOTOS, or VIDEOS)
        // Don't reset folder filter - keep it if one is selected
        updateFilterButtonStyles();
        updateSidebarButtonStyles();
        
        // Update breadcrumb based on folder filter
        if (currentFolderFilter != null) {
            String folderName = new File(currentFolderFilter).getName();
            breadcrumbLabel.setText("Favorites / " + folderName);
            folderTitleLabel.setText("Favorites in " + folderName);
        } else {
            breadcrumbLabel.setText("Favorites");
            folderTitleLabel.setText("Favorite Media");
        }
        
        applyFiltersAndSort();
    }

    @FXML
    protected void onSortClick() {
        // Toggle between Name and Date Modified
        if ("Name".equals(currentSortBy)) {
            currentSortBy = "Date Modified";
            sortButton.setText("≡ Sort by Name");
        } else {
            currentSortBy = "Name";
            sortButton.setText("≡ Sort by Date");
        }
        applyFiltersAndSort();
    }

    @FXML
    protected void onRefreshClick() {
        // Step 1: Identify items without thumbnails (need regeneration)
        List<MediaItem> itemsToRegenerate = new ArrayList<>();

        for (MediaItem item : mediaItems) {
            Image thumbnail = item.getThumbnail();

            if (thumbnail == null) {
                // No thumbnail - needs regeneration
                itemsToRegenerate.add(item);
            }
        }

        // Step 2: Regenerate thumbnails only for items without them
        for (MediaItem item : itemsToRegenerate) {
            ThumbnailCache.removeCachedThumbnail(item.getFile());

            // Regenerate thumbnail asynchronously based on type
            CompletableFuture<Image> thumbnailFuture;
            if (item.getType() == MediaItem.MediaType.IMAGE) {
                thumbnailFuture = ThumbnailGenerator.generateImageThumbnail(item.getFile());
            } else {
                thumbnailFuture = ThumbnailGenerator.generateVideoThumbnail(item.getFile());
            }

            // Capture item in final variable for lambda
            final MediaItem currentItem = item;
            thumbnailFuture.thenAccept(newThumbnail -> {
                Platform.runLater(() -> {
                    currentItem.setThumbnail(newThumbnail);
                    updateGalleryItem(currentItem);
                });
            });
        }

        // Step 3: Rescan folders to detect new/deleted files
        if (currentFolderFilter != null) {
            // Refresh specific folder
            File folderToRefresh = new File(currentFolderFilter);
            List<MediaItem> itemsToKeep = new ArrayList<>();
            for (MediaItem item : mediaItems) {
                if (!item.getPath().startsWith(currentFolderFilter)) {
                    itemsToKeep.add(item);
                }
            }
            mediaItems.clear();
            mediaItems.addAll(itemsToKeep);

            if (folderToRefresh.exists()) {
                scanFolder(folderToRefresh);
            } else {
                refreshGallery();
            }
        } else {
            // Refresh all folders
            List<File> foldersToRescan = new ArrayList<>();
            for (String folderPath : selectedFolders) {
                foldersToRescan.add(new File(folderPath));
            }
            mediaItems.clear();
            for (File folder : foldersToRescan) {
                if (folder.exists()) {
                    scanFolder(folder);
                }
            }
        }
    }

    private void updateFilterButtonStyles() {
        // Active button style - primary color background with black text
        String activeStyle = "-fx-background-color: #90CAF9; -fx-text-fill: #000000; -fx-font-size: 13px; -fx-padding: 8 20; -fx-background-radius: 8; -fx-cursor: hand; -fx-border-width: 0;";
        // Inactive button style - transparent background with gray text
        String inactiveStyle = "-fx-background-color: transparent; -fx-text-fill: #7a7d8a; -fx-font-size: 13px; -fx-padding: 8 20; -fx-background-radius: 8; -fx-cursor: hand; -fx-border-width: 0;";

        allMediaButton.setStyle(currentFilter == MediaFilter.ALL ? activeStyle : inactiveStyle);
        photosButton.setStyle(currentFilter == MediaFilter.PHOTOS ? activeStyle : inactiveStyle);
        videosButton.setStyle(currentFilter == MediaFilter.VIDEOS ? activeStyle : inactiveStyle);
    }
    
    private void updateSidebarButtonStyles() {
        // Active button style - primary color background with black text
        String activeStyle = "-fx-background-color: #90CAF9; -fx-text-fill: #000000; -fx-font-size: 15px; -fx-padding: 12 16; -fx-background-radius: 8; -fx-cursor: hand; -fx-alignment: CENTER-LEFT; -fx-border-width: 0;";
        // Inactive button style - transparent background with gray text
        String inactiveStyle = "-fx-background-color: transparent; -fx-text-fill: #7a7d8a; -fx-font-size: 15px; -fx-padding: 12 16; -fx-background-radius: 8; -fx-cursor: hand; -fx-alignment: CENTER-LEFT; -fx-border-width: 0;";

        homeButton.setStyle(!showOnlyFavorites && currentFolderFilter == null ? activeStyle : inactiveStyle);
        favoritesButton.setStyle(showOnlyFavorites ? activeStyle : inactiveStyle);
    }

    /**
     * Check if a file is a direct child of a folder (not in subfolders)
     */
    private boolean isDirectChildOf(File file, File folder) {
        File parent = file.getParentFile();
        return parent != null && parent.getAbsolutePath().equals(folder.getAbsolutePath());
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

        // Clear ImageViews properly before clearing children
        clearGalleryImageViews();
        galleryPane.getChildren().clear();

        String searchText = searchField.getText();
        String lowerSearch = searchText != null ? searchText.toLowerCase() : "";

        // Filter items
        displayedItems.clear();
        for (MediaItem item : mediaItems) {
            // Apply favorites filter first
            if (showOnlyFavorites && !item.isFavorite()) {
                continue;
            }
            
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

            // Apply folder filter - only show direct children of the selected folder
            boolean matchesFolderFilter = currentFolderFilter == null ||
                    isDirectChildOf(item.getFile(), new File(currentFolderFilter));

            if (matchesFilter && matchesSearch && matchesFolderFilter) {
                displayedItems.add(item);
            }
        }

        // Sort items
        if ("Name".equals(currentSortBy)) {
            displayedItems.sort(Comparator.comparing(MediaItem::getName, String.CASE_INSENSITIVE_ORDER));
        } else if ("Date Modified".equals(currentSortBy)) {
            displayedItems.sort((a, b) -> Long.compare(b.getFile().lastModified(), a.getFile().lastModified()));
        }

        // Display items or show no results message
        if (displayedItems.isEmpty()) {
            // Show no results message centered in viewport
            StackPane noResultsContainer = new StackPane();
            noResultsContainer.setStyle("-fx-background-color: #000000;");

            VBox noResultsBox = new VBox(20);
            noResultsBox.setAlignment(Pos.CENTER);
            noResultsBox.setStyle("-fx-padding: 40;");

            String filterType = showOnlyFavorites ? "favorites" 
                    : currentFilter == MediaFilter.PHOTOS ? "photos"
                    : currentFilter == MediaFilter.VIDEOS ? "videos" : "items";

            Label noResultsIcon = new Label(
                    showOnlyFavorites ? "♥"
                    : currentFilter == MediaFilter.PHOTOS ? "📷" 
                    : currentFilter == MediaFilter.VIDEOS ? "🎬" : "🔍");
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

            for (MediaItem item : displayedItems) {
                StackPane card = createMediaCard(item);
                galleryPane.getChildren().add(card);
            }
        }

        // Update count
        itemCountLabel.setText(displayedItems.size() + " items");
    }

    private void filterByFolder(String folderPath) {
        // Set the current folder filter
        currentFolderFilter = folderPath;
        
        // Update header to show current folder
        String folderName = new File(folderPath).getName();
        
        if (showOnlyFavorites) {
            breadcrumbLabel.setText("Favorites / " + folderName);
            folderTitleLabel.setText("Favorites in " + folderName);
        } else {
            breadcrumbLabel.setText("All Folders / " + folderName);
            folderTitleLabel.setText(folderName);
        }

        // Apply filters (this will now filter by the currentFolderFilter)
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

        HBox folderCard = new HBox(12);
        folderCard.setAlignment(Pos.CENTER_LEFT);
        folderCard.setStyle(
                "-fx-background-color: transparent; -fx-padding: 12 16; -fx-background-radius: 8; -fx-cursor: hand;");

        Label folderIcon = new Label("📁");
        folderIcon.setStyle("-fx-text-fill: #7a7d8a; -fx-font-size: 18px;");

        Label folderLabel = new Label(folderName);
        folderLabel.setStyle("-fx-text-fill: #7a7d8a; -fx-font-size: 14px;");
        folderLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(folderLabel, Priority.ALWAYS);

        folderCard.getChildren().addAll(folderIcon, folderLabel);

        // Single click to filter by this folder
        folderCard.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1) {
                filterByFolder(folderPath);
            } else if (e.getClickCount() == 2) {
                // Double click to remove/close folder
                removeFolder(folderPath);
            }
        });

        // Hover effect with #37474F color
        folderCard.setOnMouseEntered(e -> {
            folderCard.setStyle(
                "-fx-background-color: #37474F; -fx-padding: 12 16; -fx-background-radius: 8; -fx-cursor: hand;");
            folderLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 14px;");
            folderIcon.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 18px;");
        });
        folderCard.setOnMouseExited(e -> {
            folderCard.setStyle(
                "-fx-background-color: transparent; -fx-padding: 12 16; -fx-background-radius: 8; -fx-cursor: hand;");
            folderLabel.setStyle("-fx-text-fill: #7a7d8a; -fx-font-size: 14px;");
            folderIcon.setStyle("-fx-text-fill: #7a7d8a; -fx-font-size: 18px;");
        });

        folderList.getChildren().add(folderCard);
        folderCards.put(folderPath, folderCard);
    }
    
    private void addAllFoldersOption() {
        // Check if "All Folders" already exists
        if (folderCards.containsKey("ALL_FOLDERS")) {
            return;
        }
        
        HBox allFoldersCard = new HBox(12);
        allFoldersCard.setAlignment(Pos.CENTER_LEFT);
        allFoldersCard.setStyle(
                "-fx-background-color: transparent; -fx-padding: 12 16; -fx-background-radius: 8; -fx-cursor: hand;");

        Label folderIcon = new Label("📂");
        folderIcon.setStyle("-fx-text-fill: #7a7d8a; -fx-font-size: 18px;");

        Label folderLabel = new Label("All Folders");
        folderLabel.setStyle("-fx-text-fill: #7a7d8a; -fx-font-size: 14px; -fx-font-weight: bold;");
        folderLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(folderLabel, Priority.ALWAYS);

        allFoldersCard.getChildren().addAll(folderIcon, folderLabel);

        // Click to show all folders
        allFoldersCard.setOnMouseClicked(e -> {
            currentFolderFilter = null;
            updateHeaderInfo();
            applyFiltersAndSort();
        });

        // Hover effect with #37474F color
        allFoldersCard.setOnMouseEntered(e -> {
            allFoldersCard.setStyle(
                "-fx-background-color: #37474F; -fx-padding: 12 16; -fx-background-radius: 8; -fx-cursor: hand;");
            folderLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 14px; -fx-font-weight: bold;");
            folderIcon.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 18px;");
        });
        allFoldersCard.setOnMouseExited(e -> {
            allFoldersCard.setStyle(
                "-fx-background-color: transparent; -fx-padding: 12 16; -fx-background-radius: 8; -fx-cursor: hand;");
            folderLabel.setStyle("-fx-text-fill: #7a7d8a; -fx-font-size: 14px; -fx-font-weight: bold;");
            folderIcon.setStyle("-fx-text-fill: #7a7d8a; -fx-font-size: 18px;");
        });

        // Add at the beginning of the folder list
        folderList.getChildren().add(0, allFoldersCard);
        folderCards.put("ALL_FOLDERS", allFoldersCard);
    }

    private void removeFolder(String folderPath) {
        selectedFolders.remove(folderPath);
        HBox card = folderCards.remove(folderPath);
        if (card != null) {
            folderList.getChildren().remove(card);
        }

        // Reset folder filter if the removed folder was selected
        if (folderPath.equals(currentFolderFilter)) {
            currentFolderFilter = null;
        }

        // Remove media items from this folder and clear their thumbnails
        mediaItems.removeIf(item -> {
            if (item.getPath().startsWith(folderPath)) {
                item.setThumbnail(null); // Release thumbnail reference
                return true;
            }
            return false;
        });

        refreshGallery();
        updateHeaderInfo();
    }

    private void scanFolder(File folder) {
        // Scan folder sequentially to prevent memory spikes
        // Uses bounded thread pool internally for thumbnail generation
        CompletableFuture.runAsync(() -> {
            List<MediaItem> newItems = new ArrayList<>();
            Map<String, Integer> folderMediaCount = new HashMap<>();

            // Scan files first (fast, no I/O)
            scanFolderRecursive(folder, newItems, folderMediaCount);

            // Add items to gallery on UI thread
            Platform.runLater(() -> {
                // Add only new items that don't already exist (prevent duplicates)
                for (MediaItem newItem : newItems) {
                    boolean alreadyExists = false;
                    for (MediaItem existingItem : mediaItems) {
                        if (existingItem.getPath().equals(newItem.getPath())) {
                            alreadyExists = true;
                            break;
                        }
                    }
                    if (!alreadyExists) {
                        mediaItems.add(newItem);
                        // Check if this item is in favorites
                        if (favoritePaths.contains(newItem.getPath())) {
                            newItem.setFavorite(true);
                        }
                    }
                }

                // Only add folders that contain media files
                boolean isFirstFolder = selectedFolders.isEmpty();
                for (Map.Entry<String, Integer> entry : folderMediaCount.entrySet()) {
                    String folderPath = entry.getKey();
                    int mediaCount = entry.getValue();

                    // Only add if folder has media and isn't already added
                    if (mediaCount > 0 && !selectedFolders.contains(folderPath)) {
                        selectedFolders.add(folderPath);
                        addFolderToSidebar(new File(folderPath));
                    }
                }
                
                // Add "All Folders" option if this is the first folder or if it doesn't exist yet
                if (isFirstFolder && !selectedFolders.isEmpty()) {
                    addAllFoldersOption();
                }

                refreshGallery();
                updateHeaderInfo();

                // Now progressively generate thumbnails (throttled by semaphore)
                generateThumbnailsProgressively(newItems);
            });
        });
    }

    /**
     * Generate thumbnails progressively to avoid memory spikes
     * Thumbnails are generated with bounded thread pool and semaphore throttling
     */
    private void generateThumbnailsProgressively(List<MediaItem> items) {
        for (MediaItem item : items) {
            if (item.getType() == MediaItem.MediaType.IMAGE) {
                ThumbnailGenerator.generateImageThumbnail(item.getFile())
                        .thenAccept(thumbnail -> {
                            if (thumbnail != null) {
                                item.setThumbnail(thumbnail);
                                Platform.runLater(() -> updateGalleryItem(item));
                            }
                        });
            } else if (item.getType() == MediaItem.MediaType.VIDEO) {
                ThumbnailGenerator.generateVideoThumbnail(item.getFile())
                        .thenAccept(thumbnail -> {
                            if (thumbnail != null) {
                                item.setThumbnail(thumbnail);
                                Platform.runLater(() -> updateGalleryItem(item));
                            }
                        });
            }
        }
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
                    // Just identify media files, don't generate thumbnails yet
                    if (ThumbnailGenerator.isImageFile(file)) {
                        MediaItem item = new MediaItem(file, MediaItem.MediaType.IMAGE);
                        items.add(item);
                        mediaFilesInThisFolder++;
                    } else if (ThumbnailGenerator.isVideoFile(file)) {
                        MediaItem item = new MediaItem(file, MediaItem.MediaType.VIDEO);
                        items.add(item);
                        mediaFilesInThisFolder++;
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
        // Find and update the specific card for this item (efficient update)
        for (javafx.scene.Node node : galleryPane.getChildren()) {
            if (node instanceof StackPane) {
                StackPane card = (StackPane) node;
                // Check if this card belongs to the updated item
                if (card.getUserData() == item) {
                    updateCardWithThumbnail(card, item, item.getThumbnail());
                    return;
                }
            }
        }
    }

    private void updateCardWithThumbnail(StackPane card, MediaItem item, Image thumbnail) {
        // Clear old content
        clearImageViewsRecursive(card);
        card.getChildren().clear();

        // Reset card style
        card.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

        // If thumbnail was reclaimed by GC, reload from cache
        if (thumbnail == null) {
            thumbnail = ThumbnailCache.getCachedThumbnail(item.getFile());
            if (thumbnail != null) {
                item.setThumbnail(thumbnail); // Restore WeakReference
            }
        }

        if (thumbnail != null) {
            // Add thumbnail - fill cell like CSS object-fit: cover
            ImageView thumbnailView = new ImageView(thumbnail);
            thumbnailView.setPreserveRatio(true); // Don't squeeze
            thumbnailView.setSmooth(false); // Faster rendering, less memory

            // Calculate size to fill the cell (cover behavior) - 298x298
            double imageWidth = thumbnail.getWidth();
            double imageHeight = thumbnail.getHeight();
            double imageRatio = imageWidth / imageHeight;
            double cellRatio = 1.0; // Square cell

            if (imageRatio > cellRatio) {
                // Image is wider - fit to height, overflow width
                thumbnailView.setFitHeight(300);
                thumbnailView.setFitWidth(300 * imageRatio);
            } else {
                // Image is taller - fit to width, overflow height
                thumbnailView.setFitWidth(300);
                thumbnailView.setFitHeight(300 / imageRatio);
            }

            // Clip to square bounds
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(300, 300);
            card.setClip(clip);

            // Start invisible for fade-in animation
            thumbnailView.setOpacity(0.0);
            card.getChildren().add(thumbnailView);

            // Add play icon overlay for videos
            if (item.getType() == MediaItem.MediaType.VIDEO) {
                StackPane playIconContainer = new StackPane();
                playIconContainer.setMaxSize(40, 40);
                playIconContainer.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-background-radius: 20;");

                Label playIcon = new Label("▶");
                playIcon.setStyle("-fx-text-fill: rgba(255,255,255,0.9); -fx-font-size: 16px;");
                playIconContainer.getChildren().add(playIcon);

                playIconContainer.setOpacity(0.0);
                card.getChildren().add(playIconContainer);
            }

            // Smooth fade-in animation
            javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(200), thumbnailView);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();

            // Fade in play icon if present
            if (item.getType() == MediaItem.MediaType.VIDEO && card.getChildren().size() > 1) {
                javafx.scene.Node playIconContainer = card.getChildren().get(1);
                javafx.animation.FadeTransition playIconFade = new javafx.animation.FadeTransition(
                        javafx.util.Duration.millis(200), playIconContainer);
                playIconFade.setFromValue(0.0);
                playIconFade.setToValue(1.0);
                playIconFade.play();
            }
        } else {
            // Thumbnail not available - show text message
            card.setStyle("-fx-background-color: #2d3142; -fx-cursor: hand;");

            VBox placeholderContent = new VBox(10);
            placeholderContent.setAlignment(javafx.geometry.Pos.CENTER);

            Label icon = new Label(item.getType() == MediaItem.MediaType.VIDEO ? "🎬" : "📷");
            icon.setStyle("-fx-font-size: 48px;");

            Label message = new Label("No thumbnail\ngenerated");
            message.setStyle("-fx-text-fill: #888888; -fx-font-size: 12px; -fx-text-alignment: center;");
            message.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

            placeholderContent.getChildren().addAll(icon, message);
            card.getChildren().add(placeholderContent);
        }

        // Request layout update
        card.requestLayout();
    }

    private StackPane createMediaCard(MediaItem item) {
        // Card is exactly 300x300, gap of 1px creates thin uniform spacing
        StackPane card = new StackPane();
        card.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        card.setPrefSize(300, 300);
        card.setMinSize(300, 300);
        card.setMaxSize(300, 300);
        card.setFocusTraversable(false);

        // Store reference to item for efficient updates
        card.setUserData(item);

        // Try to get thumbnail - check cache if null
        Image thumbnail = item.getThumbnail();
        if (thumbnail == null) {
            thumbnail = ThumbnailCache.getCachedThumbnail(item.getFile());
            if (thumbnail != null) {
                item.setThumbnail(thumbnail); // Restore from cache
            }
        }

        if (thumbnail != null) {
            // Thumbnail loaded - fill cell like CSS object-fit: cover
            ImageView thumbnailView = new ImageView(thumbnail);
            thumbnailView.setPreserveRatio(true); // Don't squeeze
            thumbnailView.setSmooth(false); // Faster rendering, less memory

            // Calculate size to fill the cell (cover behavior) - 298x298
            double imageWidth = thumbnail.getWidth();
            double imageHeight = thumbnail.getHeight();
            double imageRatio = imageWidth / imageHeight;
            double cellRatio = 1.0; // Square cell

            if (imageRatio > cellRatio) {
                // Image is wider - fit to height, overflow width
                thumbnailView.setFitHeight(300);
                thumbnailView.setFitWidth(300 * imageRatio);
            } else {
                // Image is taller - fit to width, overflow height
                thumbnailView.setFitWidth(300);
                thumbnailView.setFitHeight(300 / imageRatio);
            }

            // Clip to square bounds (300x300)
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(300, 300);
            card.setClip(clip);

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
            // No thumbnail yet - show text message
            card.setStyle("-fx-background-color: #2d3142; -fx-cursor: hand;");

            VBox placeholderContent = new VBox(10);
            placeholderContent.setAlignment(javafx.geometry.Pos.CENTER);

            Label icon = new Label(item.getType() == MediaItem.MediaType.VIDEO ? "🎬" : "📷");
            icon.setStyle("-fx-font-size: 48px;");

            Label message = new Label("No thumbnail\ngenerated");
            message.setStyle("-fx-text-fill: #888888; -fx-font-size: 12px; -fx-text-alignment: center;");
            message.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

            placeholderContent.getChildren().addAll(icon, message);
            card.getChildren().add(placeholderContent);
        }

        // Hover effect - subtle opacity change
        card.setOnMouseEntered(e -> {
            card.setOpacity(0.9);
        });
        card.setOnMouseExited(e -> {
            card.setOpacity(1.0);
        });

        // Prevent ScrollPane from reacting to mouse press (e.g. scrolling to focus)
        card.setOnMousePressed(e -> {
            e.consume();
        });

        // Click to open fullscreen viewer
        card.setOnMouseClicked(e -> {
            e.consume();
            currentMediaIndex = displayedItems.indexOf(item);
            showFullscreenViewer(item);
        });

        return card;
    }

    private void showFullscreenViewer(MediaItem item) {
        // Save current scroll position
        savedScrollPosition = galleryScrollPane.getVvalue();

        // Reset seekbar interaction flag for new viewer session
        seekbarInteracted = false;

        // Switch to fullscreen mode for media viewing
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.setFullScreen(true);

        // Create fullscreen viewer overlay
        fullscreenViewer = new StackPane();
        fullscreenViewer.setStyle("-fx-background-color: #000000;");

        if (item.getType() == MediaItem.MediaType.IMAGE) {
            // Create rotate action for images
            Runnable rotateAction = () -> {
                // Get the imageContainer from userData
                Object userData = fullscreenViewer.getUserData();
                if (userData instanceof Pane) {
                    Pane imageContainer = (Pane) userData;
                    double currentRotation = imageContainer.getRotate();
                    double newRotation = currentRotation + 90;
                    imageContainer.setRotate(newRotation);
                    imageContainer.requestLayout();
                }
            };
            HBox topBar = createTopBar(item, rotateAction);
            int initialRotation = MediaMetadataUtils.getRotation(item.getFile());
            setupImageViewer(fullscreenViewer, item, topBar, initialRotation);
        } else {
            // Create rotate action for videos
            Runnable rotateAction = () -> {
                // Get the videoContainer from userData
                Object userData = fullscreenViewer.getUserData();
                if (userData instanceof Pane) {
                    Pane videoContainer = (Pane) userData;
                    double currentRotation = videoContainer.getRotate();
                    double newRotation = currentRotation + 90;
                    videoContainer.setRotate(newRotation);
                    videoContainer.requestLayout();
                }
            };
            HBox topBar = createTopBar(item, rotateAction);
            int initialRotation = MediaMetadataUtils.getRotation(item.getFile());
            setupVideoViewer(fullscreenViewer, item, topBar, initialRotation);
        }

        // Hide sidebar when viewing media
        sidebar.setVisible(false);
        sidebar.setManaged(false);

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
            } else if (event.getCode() == KeyCode.M) {
                // Mute/unmute with M key
                if (currentMediaPlayer != null) {
                    if (currentMediaPlayer.getVolume() > 0) {
                        currentMediaPlayer.setVolume(0);
                    } else {
                        currentMediaPlayer.setVolume(1.0);
                    }
                }
            } else if (event.getCode() == KeyCode.R) {
                // Rotate with R key
                Object userData = fullscreenViewer.getUserData();
                if (userData instanceof Pane) {
                    Pane container = (Pane) userData;
                    double currentRotation = container.getRotate();
                    double newRotation = currentRotation + 90;
                    container.setRotate(newRotation);
                    container.requestLayout();
                }
            } else {
                // Media Type specific navigation/seeking
                // Fix: Use current item from list instead of captured 'item' variable which is
                // stale after navigation
                MediaItem currentItem = displayedItems.get(currentMediaIndex);

                if (currentItem.getType() == MediaItem.MediaType.VIDEO) {
                    if (event.isShiftDown()) {
                        // Shift+N/P always navigate between videos
                        if (event.getCode() == KeyCode.N) {
                            respondToMouseMoved = false; // Disable mouse moved response during keyboard navigation
                            navigateToNextMedia();
                        } else if (event.getCode() == KeyCode.P) {
                            respondToMouseMoved = false; // Disable mouse moved response during keyboard navigation
                            navigateToPreviousMedia();
                        }
                    } else if (seekbarInteracted) {
                        // Seekbar mode: Arrow keys seek within video
                        if (event.getCode() == KeyCode.RIGHT) {
                            if (currentMediaPlayer != null) {
                                currentMediaPlayer
                                        .seek(currentMediaPlayer.getCurrentTime().add(javafx.util.Duration.seconds(5)));
                            }
                        } else if (event.getCode() == KeyCode.LEFT) {
                            if (currentMediaPlayer != null) {
                                currentMediaPlayer.seek(
                                        currentMediaPlayer.getCurrentTime().subtract(javafx.util.Duration.seconds(5)));
                            }
                        }
                    } else {
                        // Default mode: Arrow keys navigate between videos (like images)
                        if (event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.DOWN) {
                            respondToMouseMoved = false; // Disable mouse moved response during keyboard navigation
                            navigateToNextMedia();
                        } else if (event.getCode() == KeyCode.LEFT || event.getCode() == KeyCode.UP) {
                            respondToMouseMoved = false; // Disable mouse moved response during keyboard navigation
                            navigateToPreviousMedia();
                        }
                    }
                } else {
                    // IMAGE navigation
                    if (event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.DOWN) {
                        respondToMouseMoved = false; // Disable mouse moved response during keyboard navigation
                        navigateToNextMedia();
                    } else if (event.getCode() == KeyCode.LEFT || event.getCode() == KeyCode.UP) {
                        respondToMouseMoved = false; // Disable mouse moved response during keyboard navigation
                        navigateToPreviousMedia();
                    }
                }
            }
        });
        Platform.runLater(() -> {
            fullscreenViewer.requestFocus();
        });
    }

    private void setupImageViewer(StackPane container, MediaItem item, HBox topBar, int initialRotation) {
        BorderPane layout = new BorderPane();
        layout.setStyle("-fx-background-color: #000000;");

        // Load full-quality image for fullscreen viewing
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
                    double imageWidth = image.getWidth();
                    double imageHeight = image.getHeight();

                    // Check if rotated 90 or 270 degrees
                    double currentRotation = getRotate();
                    boolean isRotated90 = (Math.abs(currentRotation % 180) == 90);

                    if (isRotated90) {
                        // Swap dimensions for aspect ratio calculation
                        double temp = imageWidth;
                        imageWidth = imageHeight;
                        imageHeight = temp;
                    }

                    double imageRatio = imageWidth / imageHeight;
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

                    if (isRotated90) {
                        // If rotated, we need to size the ImageView such that when rotated it fits
                        // The logic above calculated the BOUNDING BOX size of the rotated image
                        // So for the actual ImageView:
                        // width -> newHeight
                        // height -> newWidth
                        imageView.setFitWidth(newHeight);
                        imageView.setFitHeight(newWidth);
                    } else {
                        imageView.setFitWidth(newWidth);
                        imageView.setFitHeight(newHeight);
                    }

                    // Center the image
                    double x = (containerWidth - imageView.getFitWidth()) / 2;
                    double y = (containerHeight - imageView.getFitHeight()) / 2;
                    imageView.relocate(x, y);
                }
            }
        };
        imageContainer.getChildren().add(imageView);
        imageContainer.setStyle("-fx-background-color: #000000;");

        // Use StackPane to overlay top bar on image
        StackPane imageStack = new StackPane();
        imageStack.getChildren().add(imageContainer);

        // Add top bar overlay
        StackPane.setAlignment(topBar, Pos.TOP_CENTER);
        imageStack.getChildren().add(topBar);

        layout.setCenter(imageStack);
        container.getChildren().add(layout);

        // Auto-hide top bar with fade in/out (similar to video viewer)
        topBar.setOpacity(1.0); // Start visible

        javafx.animation.FadeTransition fadeOutTop = new javafx.animation.FadeTransition(
                javafx.util.Duration.millis(500), topBar);
        fadeOutTop.setFromValue(1.0);
        fadeOutTop.setToValue(0.0);
        fadeOutTop.setOnFinished(ev -> {
            // Hide cursor when bars are hidden
            imageStack.setCursor(javafx.scene.Cursor.NONE);
            topBar.setMouseTransparent(true);
        });

        javafx.animation.FadeTransition fadeInTop = new javafx.animation.FadeTransition(
                javafx.util.Duration.millis(200), topBar);
        fadeInTop.setFromValue(0.0);
        fadeInTop.setToValue(1.0);
        fadeInTop.setOnFinished(ev -> {
            // Show cursor when bars are visible
            imageStack.setCursor(javafx.scene.Cursor.DEFAULT);
            topBar.setMouseTransparent(false);
        });

        javafx.animation.PauseTransition idleTimer = new javafx.animation.PauseTransition(
                javafx.util.Duration.seconds(3));
        idleTimer.setOnFinished(ev -> fadeOutTop.play());

        // Only respond to actual mouse movement, not synthetic events from keyboard navigation
        imageStack.setOnMouseMoved(e -> {
            if (respondToMouseMoved) {
                if (topBar.getOpacity() < 1.0) {
                    fadeInTop.play();
                    topBar.setMouseTransparent(false);
                }
                topBar.setOpacity(1.0);
                imageStack.setCursor(javafx.scene.Cursor.DEFAULT);
                idleTimer.playFromStart();
            }
        });

        // Re-enable mouse moved response on actual mouse interaction
        imageStack.setOnMouseEntered(e -> {
            respondToMouseMoved = true;
        });

        imageStack.setOnMouseDragged(e -> {
            respondToMouseMoved = true;
            if (topBar.getOpacity() < 1.0) {
                fadeInTop.play();
                topBar.setMouseTransparent(false);
            }
            topBar.setOpacity(1.0);
            imageStack.setCursor(javafx.scene.Cursor.DEFAULT);
            idleTimer.playFromStart();
        });

        // Start timer initially
        idleTimer.play();

        // Set initial rotation
        imageContainer.setRotate(initialRotation);

        // Store reference to imageContainer for rotation
        container.setUserData(imageContainer);
    }

    private void setupVideoViewer(StackPane container, MediaItem item, HBox topBar, int initialRotation) {
        // Video in center with proper constraints
        try {
            Media media = new Media(item.getFile().toURI().toString());
            currentMediaPlayer = new MediaPlayer(media);
            MediaView mediaView = new MediaView(currentMediaPlayer);
            
            mediaView.setPreserveRatio(true);
            mediaView.setSmooth(true);

            // Ensure MediaView is visible and rendered
            mediaView.setVisible(true);
            mediaView.setManaged(true);

            // Disable looping by default (1 cycle)
            currentMediaPlayer.setCycleCount(1);

            // Track if video actually started playing
            final boolean[] hasPlayed = { false };
            final boolean[] videoFrameRendered = { false };

            currentMediaPlayer.statusProperty().addListener((obs, oldStatus, newStatus) -> {
                if (newStatus == MediaPlayer.Status.PLAYING) {
                    hasPlayed[0] = true;
                }

                // Debug: Log status changes
                MediaMetadataUtils.logDebug("Video status changed: " + oldStatus + " -> " + newStatus);
            });

            // Monitor current time to detect if video is actually progressing
            currentMediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                if (newTime.greaterThan(javafx.util.Duration.millis(100))) {
                    videoFrameRendered[0] = true;
                }
            });

            // Error handler - detect codec errors and handle appropriately
            currentMediaPlayer.setOnError(() -> {
                javafx.scene.media.MediaException error = currentMediaPlayer.getError();
                String errorMessage = error != null ? error.getMessage() : "Unknown error";

                // Check if it's a codec/format error (no point retrying)
                boolean isCodecError = errorMessage.contains("ERROR_MEDIA_INVALID") ||
                        errorMessage.contains("ERROR_MEDIA_UNSUPPORTED") ||
                        errorMessage.contains("MEDIA_UNSUPPORTED");

                if (isCodecError) {
                    // Codec error - show error immediately, don't retry

                    showVideoErrorUI(item, "Unsupported video format",
                            "This video uses a codec that JavaFX cannot play. You can open it in your system's video player.");
                    return;
                }

                // For other errors, try retry logic
                if (currentRetryAttempts < MAX_RETRIES) {

                    currentRetryAttempts++;

                    // Run on next pulse to ensure clean state
                    Platform.runLater(() -> switchToMedia(item));
                    return;
                }

                // Wait a bit to see if video recovers and starts playing
                javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(
                        javafx.util.Duration.seconds(1));
                delay.setOnFinished(e -> {
                    if (!hasPlayed[0]) {
                        // Video truly failed - show error UI
                        showVideoErrorUI(item, "Cannot play this video",
                                "This video format is not supported or the file may be corrupted.");
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
                            currentMediaPlayer != null && currentMediaPlayer.getMedia() != null) {
                        double videoWidth = currentMediaPlayer.getMedia().getWidth();
                        double videoHeight = currentMediaPlayer.getMedia().getHeight();

                        if (videoWidth > 0 && videoHeight > 0) {

                            // Check if rotated 90 or 270 degrees
                            double currentRotation = getRotate();
                            boolean isRotated90 = (Math.abs(currentRotation % 180) == 90);

                            if (isRotated90) {
                                // Swap dimensions for aspect ratio calculation
                                double temp = videoWidth;
                                videoWidth = videoHeight;
                                videoHeight = temp;
                            }

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

                            if (isRotated90) {
                                mediaView.setFitWidth(newHeight);
                                mediaView.setFitHeight(newWidth);
                            } else {
                                mediaView.setFitWidth(newWidth);
                                mediaView.setFitHeight(newHeight);
                            }

                            // Center the video
                            double x = (containerWidth - mediaView.getFitWidth()) / 2;
                            double y = (containerHeight - mediaView.getFitHeight()) / 2;
                            mediaView.relocate(x, y);
                        }
                    }
                }
            };
            videoContainer.getChildren().add(mediaView);
            videoContainer.setStyle("-fx-background-color: #000000;");
            videoContainer.setVisible(true);
            videoContainer.setManaged(true);

            // Wait for media to be ready, then play with slight delay
            currentMediaPlayer.setOnReady(() -> {
                videoContainer.requestLayout();

                // Small delay before playing for stability
                javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(
                        javafx.util.Duration.millis(300));
                delay.setOnFinished(e -> {
                    currentMediaPlayer.play();
                    currentMediaPlayer.setVolume(1.0);
                    videoContainer.requestLayout();
                });
                delay.play();
            });

            // Container for controls
            StackPane videoStack = new StackPane();
            videoStack.getChildren().add(videoContainer);

            // --- Control Bar Setup ---
            // Create a VBox to stack progress bar on top and controls below
            VBox controlsContainer = new VBox(0);
            controlsContainer.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8);");
            controlsContainer.setMaxWidth(Double.MAX_VALUE);
            controlsContainer.setMaxHeight(Region.USE_PREF_SIZE); // Don't expand vertically

            // Progress bar container with hover preview
            StackPane progressContainer = new StackPane();
            progressContainer.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
            progressContainer.setMaxWidth(Double.MAX_VALUE);
            progressContainer.setMinHeight(8);
            progressContainer.setMaxHeight(8);

            // Seek Slider (styled as progress bar)
            javafx.scene.control.Slider seekSlider = new javafx.scene.control.Slider();
            seekSlider.setMaxWidth(Double.MAX_VALUE);
            seekSlider.setStyle("-fx-padding: 0;");
            seekSlider.setDisable(true); // Initially disabled
            seekSlider.setFocusTraversable(false); // logic for focus stealing prevention

            progressContainer.getChildren().add(seekSlider);

            // Add hover effect to slider - make it pop out and brighter
            seekSlider.setOnMouseEntered(e -> {
                seekSlider.setScaleY(1.8); // Make it bigger vertically (pop out effect)
                seekSlider.setStyle("-fx-padding: 0; -fx-opacity: 1.0;"); // Brighter
            });

            seekSlider.setOnMouseExited(e -> {
                seekSlider.setScaleY(1.0); // Return to normal size
                seekSlider.setStyle("-fx-padding: 0; -fx-opacity: 0.8;"); // Normal brightness
            });

            // Set initial opacity
            seekSlider.setOpacity(0.8);

            // Controls bar with buttons and time
            HBox controlsBar = new HBox(15);
            controlsBar.setAlignment(Pos.CENTER_LEFT);
            controlsBar.setStyle("-fx-background-color: transparent; -fx-padding: 10 20;");
            controlsBar.setMaxWidth(Double.MAX_VALUE);
            controlsBar.setMinHeight(50);

            // Styles for buttons
            String btnStyle = "-fx-background-color: transparent; -fx-text-fill: #cdd6f4; -fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 5;";
            String activeBtnStyle = "-fx-background-color: #89b4fa; -fx-text-fill: #1e1e2e; -fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 5; -fx-background-radius: 4;";

            // Capture player in local variable to prevent NPE if field is nulled
            MediaPlayer player = currentMediaPlayer;

            // 1. Play/Pause Button Logic
            Runnable togglePlayPause = () -> {
                if (player.getStatus() == MediaPlayer.Status.PLAYING) {
                    player.pause();
                } else {
                    player.play();
                }
            };

            Button playPauseBtn = new Button("⏸"); // Default to pause symbol as it auto-plays
            playPauseBtn.setStyle(btnStyle);
            playPauseBtn.setMinWidth(30);
            playPauseBtn.setFocusTraversable(false);
            playPauseBtn.setOnAction(e -> togglePlayPause.run());

            Runnable updatePlayBtn = () -> {
                if (player.getStatus() == MediaPlayer.Status.PLAYING) {
                    playPauseBtn.setText("⏸");
                } else {
                    playPauseBtn.setText("▶");
                }
            };

            player.statusProperty().addListener((obs, old, newVal) -> updatePlayBtn.run());

            // Request focus on container to ensure key events are caught
            Platform.runLater(container::requestFocus);

            // 2. Volume Button with mute/unmute toggle
            Button volumeBtn = new Button("🔊");
            volumeBtn.setStyle(btnStyle);
            volumeBtn.setMinWidth(30);
            volumeBtn.setFocusTraversable(false);

            // Toggle mute/unmute
            Runnable toggleMute = () -> {
                if (currentMediaPlayer.getVolume() > 0) {
                    currentMediaPlayer.setVolume(0);
                    volumeBtn.setText("🔇");
                } else {
                    currentMediaPlayer.setVolume(1.0);
                    volumeBtn.setText("🔊");
                }
            };

            volumeBtn.setOnAction(e -> toggleMute.run());

            // Left spacer to push time to center
            Region leftSpacer = new Region();
            HBox.setHgrow(leftSpacer, Priority.ALWAYS);

            // 3. Time Labels (centered)
            Label currentTimeLabel = new Label("00:00");
            currentTimeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");

            Label separatorLabel = new Label("/");
            separatorLabel.setStyle("-fx-text-fill: #999999; -fx-font-size: 13px;");

            Label totalTimeLabel = new Label("00:00");
            totalTimeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");

            // Right spacer to keep time centered
            Region rightSpacer = new Region();
            HBox.setHgrow(rightSpacer, Priority.ALWAYS);

            // 4. Loop Button (on the right) - starts OFF by default
            Button loopBtn = new Button("🔁");
            loopBtn.setTooltip(new Tooltip("Toggle Loop (Off)"));
            loopBtn.setStyle(btnStyle); // Inactive style by default
            loopBtn.setMinWidth(30);
            loopBtn.setFocusTraversable(false);

            // Track looping state manually for better control - starts as FALSE (off)
            final boolean[] isLooping = { false };

            loopBtn.setOnAction(e -> {
                isLooping[0] = !isLooping[0];
                MediaMetadataUtils.logDebug("Loop toggled: " + isLooping[0]);
                if (isLooping[0]) {
                    // Loop turned ON
                    player.setCycleCount(MediaPlayer.INDEFINITE);
                    loopBtn.setStyle(activeBtnStyle);
                    loopBtn.setTooltip(new Tooltip("Toggle Loop (On)"));

                    // If video was already at end, restart it
                    if (player.getStatus() == MediaPlayer.Status.STOPPED ||
                            player.getCurrentTime().greaterThanOrEqualTo(
                                    player.getTotalDuration().subtract(javafx.util.Duration.millis(100)))) {
                        player.seek(javafx.util.Duration.ZERO);
                        player.play();
                        playPauseBtn.setText("⏸");
                    }
                } else {
                    // Loop turned OFF
                    player.setCycleCount(1);
                    loopBtn.setStyle(btnStyle);
                    loopBtn.setTooltip(new Tooltip("Toggle Loop (Off)"));
                }
            });

            // Robust loop fallback - only loops when isLooping[0] is true
            // End of media handler
            player.setOnEndOfMedia(() -> {
                // Update slider to show end position
                seekSlider.setValue(seekSlider.getMax());

                if (!isLooping[0]) {
                    // Loop is OFF - pause at end and reset to beginning
                    MediaMetadataUtils.logDebug("EndOfMedia: Pausing (Loop OFF)");
                    player.pause();
                    player.seek(javafx.util.Duration.ZERO);
                    playPauseBtn.setText("▶");
                } else {
                    // Loop is ON - restart video
                    MediaMetadataUtils.logDebug("EndOfMedia: Forcing restart (Loop ON)");
                    player.seek(javafx.util.Duration.ZERO);
                    // Set cycle count again just in case
                    player.setCycleCount(MediaPlayer.INDEFINITE);
                    javafx.application.Platform.runLater(() -> player.play());
                }
            });

            // Watch for status changes to catch "stuck" states - only restart if loop is ON
            player.statusProperty().addListener((obs, oldStatus, newStatus) -> {
                if (isLooping[0]
                        && (newStatus == MediaPlayer.Status.PAUSED || newStatus == MediaPlayer.Status.STOPPED)) {
                    // Check if we are at the end
                    if (player.getCurrentTime().greaterThanOrEqualTo(
                            player.getTotalDuration().subtract(javafx.util.Duration.millis(100)))) {
                        MediaMetadataUtils
                                .logDebug("Status " + newStatus + " at end with Loop ON. Forcing loop restart.");
                        player.seek(javafx.util.Duration.ZERO);
                        player.play();
                    }
                }
            });
            controlsBar.getChildren().addAll(playPauseBtn, volumeBtn, leftSpacer, currentTimeLabel, separatorLabel,
                    totalTimeLabel, rightSpacer, loopBtn);

            // Add progress bar and controls to container
            controlsContainer.getChildren().addAll(progressContainer, controlsBar);

            // Slider Logic
            final boolean[] isUpdatingFromPlayer = { false };

            // Format time helper
            java.util.function.Function<Double, String> formatTime = (seconds) -> {
                int mins = (int) (seconds / 60);
                int secs = (int) (seconds % 60);
                return String.format("%02d:%02d", mins, secs);
            };

            currentMediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                if (seekSlider != null && !seekSlider.isValueChanging()) {
                    isUpdatingFromPlayer[0] = true;
                    seekSlider.setValue(newTime.toSeconds());
                    currentTimeLabel.setText(formatTime.apply(newTime.toSeconds()));
                    isUpdatingFromPlayer[0] = false;
                }
            });

            Runnable onReady = () -> {
                javafx.util.Duration total = currentMediaPlayer.getTotalDuration();
                if (total != null && !total.isIndefinite() && !total.isUnknown()) {
                    seekSlider.setMax(total.toSeconds());
                    seekSlider.setDisable(false);
                    totalTimeLabel.setText(formatTime.apply(total.toSeconds()));
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
                    totalTimeLabel.setText(formatTime.apply(newD.toSeconds()));
                }
            });

            // Note: EndOfMedia handler is already set above with loop logic
            // Don't override it here

            // Detect seekbar interaction (mouse click or drag)
            seekSlider.setOnMousePressed(e -> {
                seekbarInteracted = true;
            });

            seekSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (!isUpdatingFromPlayer[0] && !seekSlider.isValueChanging()) {
                    currentMediaPlayer.seek(javafx.util.Duration.seconds(newVal.doubleValue()));
                }
            });

            seekSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
                if (isChanging) {
                    // User started dragging
                    seekbarInteracted = true;
                }
                if (!isChanging) {
                    currentMediaPlayer.seek(javafx.util.Duration.seconds(seekSlider.getValue()));
                }
            });

            // Add controls container to stack
            videoStack.getChildren().add(controlsContainer);
            StackPane.setAlignment(controlsContainer, Pos.BOTTOM_CENTER);

            // Add top bar overlay
            videoStack.getChildren().add(topBar);
            StackPane.setAlignment(topBar, Pos.TOP_CENTER);

            // --- Auto-Hide Logic for both top and bottom bars ---
            controlsContainer.setOpacity(1.0); // Start visible
            topBar.setOpacity(1.0); // Start visible

            javafx.animation.FadeTransition fadeOutControls = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(500), controlsContainer);
            fadeOutControls.setFromValue(1.0);
            fadeOutControls.setToValue(0.0);

            javafx.animation.FadeTransition fadeOutTop = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(500), topBar);
            fadeOutTop.setFromValue(1.0);
            fadeOutTop.setToValue(0.0);
            fadeOutTop.setOnFinished(ev -> {
                // Hide cursor when bars are hidden
                videoStack.setCursor(javafx.scene.Cursor.NONE);
                controlsContainer.setMouseTransparent(true);
                topBar.setMouseTransparent(true);
            });

            javafx.animation.FadeTransition fadeInControls = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(200), controlsContainer);
            fadeInControls.setFromValue(0.0);
            fadeInControls.setToValue(1.0);

            javafx.animation.FadeTransition fadeInTop = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(200), topBar);
            fadeInTop.setFromValue(0.0);
            fadeInTop.setToValue(1.0);
            fadeInTop.setOnFinished(ev -> {
                // Show cursor when bars are visible
                videoStack.setCursor(javafx.scene.Cursor.DEFAULT);
                controlsContainer.setMouseTransparent(false);
                topBar.setMouseTransparent(false);
            });

            javafx.animation.PauseTransition idleTimer = new javafx.animation.PauseTransition(
                    javafx.util.Duration.seconds(3));
            idleTimer.setOnFinished(ev -> {
                fadeOutControls.play();
                fadeOutTop.play();
            });

            // Only respond to actual mouse movement, not synthetic events from keyboard navigation
            videoStack.setOnMouseMoved(e -> {
                if (respondToMouseMoved) {
                    if (controlsContainer.getOpacity() < 1.0) {
                        fadeInControls.play();
                        fadeInTop.play();
                        controlsContainer.setMouseTransparent(false);
                        topBar.setMouseTransparent(false);
                    }
                    controlsContainer.setOpacity(1.0);
                    topBar.setOpacity(1.0);
                    videoStack.setCursor(javafx.scene.Cursor.DEFAULT);
                    idleTimer.playFromStart();
                }
            });

            // Re-enable mouse moved response on actual mouse interaction
            videoStack.setOnMouseEntered(e -> {
                respondToMouseMoved = true;
            });

            videoStack.setOnMouseDragged(e -> {
                respondToMouseMoved = true;
                if (controlsContainer.getOpacity() < 1.0) {
                    fadeInControls.play();
                    fadeInTop.play();
                    controlsContainer.setMouseTransparent(false);
                    topBar.setMouseTransparent(false);
                }
                controlsContainer.setOpacity(1.0);
                topBar.setOpacity(1.0);
                videoStack.setCursor(javafx.scene.Cursor.DEFAULT);
                idleTimer.playFromStart();
            });

            // Also show on click
            videoStack.setOnMouseClicked(e -> {
                respondToMouseMoved = true; // Re-enable on click
                if (controlsContainer.getOpacity() < 1.0) {
                    fadeInControls.play();
                    fadeInTop.play();
                    idleTimer.playFromStart();
                } else {
                    // If clicking not on controls, toggle play/pause
                    if (!controlsContainer.contains(controlsContainer.sceneToLocal(e.getSceneX(), e.getSceneY()))) {
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

            container.getChildren().add(videoStack);

            // Set initial rotation
            videoContainer.setRotate(initialRotation);

            // Store reference to videoContainer for rotation
            container.setUserData(videoContainer);

            // Auto-play
            currentMediaPlayer.setAutoPlay(true);

            // Watchdog: If video is playing but time isn't progressing, try to restart
            javafx.animation.Timeline watchdog = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2), e -> {
                        if (currentMediaPlayer != null &&
                                currentMediaPlayer.getStatus() == MediaPlayer.Status.PLAYING &&
                                !videoFrameRendered[0]) {
                            MediaMetadataUtils
                                    .logDebug("Video stuck - audio playing but no frames. Attempting restart...");
                            // Video is stuck - try to restart
                            currentMediaPlayer.stop();
                            currentMediaPlayer.seek(javafx.util.Duration.ZERO);
                            currentMediaPlayer.play();
                        }
                    }));
            watchdog.setCycleCount(1);
            watchdog.play();

        } catch (Exception e) {

            // Show error message
            VBox errorBox = new VBox(20);
            errorBox.setAlignment(Pos.CENTER);
            errorBox.setStyle("-fx-padding: 20; -fx-background-color: #000000;");

            Label errorLabel = new Label("Error loading video: " + item.getName());
            errorLabel.setStyle("-fx-text-fill: #f38ba8; -fx-font-size: 18px;");

            Button closeButton = new Button("✕ Close");
            closeButton.setStyle(
                    "-fx-background-color: #45475a; -fx-text-fill: #cdd6f4; -fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
            closeButton.setOnAction(ev -> closeFullscreenViewer());

            errorBox.getChildren().addAll(errorLabel, closeButton);

            container.getChildren().add(errorBox);
        }
    }

    private HBox createTopBar(MediaItem item, Runnable rotateAction) {
        HBox topBar = new HBox(15);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8); -fx-padding: 15 20;");
        topBar.setMaxWidth(Double.MAX_VALUE); // Full width
        topBar.setMinHeight(60);
        topBar.setMaxHeight(60);

        // Close button (left side)
        Button closeButton = new Button("✕");
        closeButton.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #cdd6f4; -fx-font-size: 20px; -fx-cursor: hand; -fx-padding: 5 10;");
        closeButton.setFocusTraversable(false);
        closeButton.setOnAction(e -> closeFullscreenViewer());
        closeButton.setOnMouseEntered(e -> closeButton.setStyle(
                "-fx-background-color: #45475a; -fx-text-fill: #cdd6f4; -fx-font-size: 20px; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 4;"));
        closeButton.setOnMouseExited(e -> closeButton.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #cdd6f4; -fx-font-size: 20px; -fx-cursor: hand; -fx-padding: 5 10;"));

        // Spacer to push filename to center
        Region leftSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);

        // Filename label (centered)
        Label filenameLabel = new Label(item.getName());
        filenameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        // Spacer to keep filename centered
        Region rightSpacer = new Region();
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        // Favorite button
        Button favoriteButton = new Button(item.isFavorite() ? "♥" : "♡");
        favoriteButton.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: " + (item.isFavorite() ? "#90CAF9" : "#cdd6f4") + "; -fx-font-size: 20px; -fx-cursor: hand; -fx-padding: 5 10;");
        favoriteButton.setTooltip(new Tooltip(item.isFavorite() ? "Remove from Favorites" : "Add to Favorites"));
        favoriteButton.setFocusTraversable(false);
        favoriteButton.setOnAction(e -> {
            item.setFavorite(!item.isFavorite());
            if (item.isFavorite()) {
                favoritePaths.add(item.getPath());
                favoriteButton.setText("♥");
                favoriteButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #90CAF9; -fx-font-size: 20px; -fx-cursor: hand; -fx-padding: 5 10;");
                favoriteButton.setTooltip(new Tooltip("Remove from Favorites"));
            } else {
                favoritePaths.remove(item.getPath());
                favoriteButton.setText("♡");
                favoriteButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #cdd6f4; -fx-font-size: 20px; -fx-cursor: hand; -fx-padding: 5 10;");
                favoriteButton.setTooltip(new Tooltip("Add to Favorites"));
            }
            SessionManager.saveFavorites(favoritePaths);
        });
        favoriteButton.setOnMouseEntered(e -> {
            if (item.isFavorite()) {
                favoriteButton.setStyle("-fx-background-color: #45475a; -fx-text-fill: #90CAF9; -fx-font-size: 20px; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 4;");
            } else {
                favoriteButton.setStyle("-fx-background-color: #45475a; -fx-text-fill: #cdd6f4; -fx-font-size: 20px; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 4;");
            }
        });
        favoriteButton.setOnMouseExited(e -> {
            if (item.isFavorite()) {
                favoriteButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #90CAF9; -fx-font-size: 20px; -fx-cursor: hand; -fx-padding: 5 10;");
            } else {
                favoriteButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #cdd6f4; -fx-font-size: 20px; -fx-cursor: hand; -fx-padding: 5 10;");
            }
        });

        // Rotate button (right side, after filename)
        Button rotateButton = new Button("↻");
        rotateButton.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #cdd6f4; -fx-font-size: 20px; -fx-cursor: hand; -fx-padding: 5 10;");
        rotateButton.setTooltip(new Tooltip("Rotate 90°"));
        rotateButton.setFocusTraversable(false);
        rotateButton.setOnMouseEntered(e -> rotateButton.setStyle(
                "-fx-background-color: #45475a; -fx-text-fill: #cdd6f4; -fx-font-size: 20px; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 4;"));
        rotateButton.setOnMouseExited(e -> rotateButton.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #cdd6f4; -fx-font-size: 20px; -fx-cursor: hand; -fx-padding: 5 10;"));
        rotateButton.setOnAction(e -> rotateAction.run());

        topBar.getChildren().addAll(closeButton, leftSpacer, filenameLabel, rightSpacer, favoriteButton, rotateButton);
        return topBar;
    }

    private void navigateToNextMedia() {
        // Throttle rapid navigation
        long now = System.currentTimeMillis();
        if (now - lastSwitchTime < SWITCH_THROTTLE_MS) {
            return; // Ignore fast key presses
        }
        lastSwitchTime = now;

        if (currentMediaIndex < displayedItems.size() - 1) {
            currentMediaIndex++;
            seekbarInteracted = false; // Reset to default navigation mode
            switchToMedia(displayedItems.get(currentMediaIndex));
        }
    }

    private void navigateToPreviousMedia() {
        // Throttle rapid navigation
        long now = System.currentTimeMillis();
        if (now - lastSwitchTime < SWITCH_THROTTLE_MS) {
            return; // Ignore fast key presses
        }
        lastSwitchTime = now;

        if (currentMediaIndex > 0) {
            currentMediaIndex--;
            seekbarInteracted = false; // Reset to default navigation mode
            switchToMedia(displayedItems.get(currentMediaIndex));
        }
    }

    private void switchToMedia(MediaItem item) {
        // Manage retry state
        if (item != currentAttemptingItem) {
            currentAttemptingItem = item;
            currentRetryAttempts = 0;
        }

        // FULLY stop and dispose current media player
        if (currentMediaPlayer != null) {
            currentMediaPlayer.stop();
            currentMediaPlayer.dispose();
            currentMediaPlayer = null;
        }

        // Clear ImageViews before clearing children
        if (fullscreenViewer != null) {
            clearImageViewsRecursive(fullscreenViewer);
        }

        // Clear and rebuild the fullscreen viewer content
        fullscreenViewer.getChildren().clear();

        // Add delay before loading new media to prevent pipeline overlap
        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(
                javafx.util.Duration.millis(150));
        delay.setOnFinished(e -> {
            if (item.getType() == MediaItem.MediaType.IMAGE) {
                // Create rotate action for images
                Runnable rotateAction = () -> {
                    // Get the imageContainer from userData
                    Object userData = fullscreenViewer.getUserData();
                    if (userData instanceof Pane) {
                        Pane imageContainer = (Pane) userData;
                        double currentRotation = imageContainer.getRotate();
                        double newRotation = currentRotation + 90;
                        imageContainer.setRotate(newRotation);
                        imageContainer.requestLayout();
                    }
                };
                HBox topBar = createTopBar(item, rotateAction);
                int initialRotation = MediaMetadataUtils.getRotation(item.getFile());
                setupImageViewer(fullscreenViewer, item, topBar, initialRotation);
            } else {
                // Create rotate action for videos
                Runnable rotateAction = () -> {
                    // Get the videoContainer from userData
                    Object userData = fullscreenViewer.getUserData();
                    if (userData instanceof Pane) {
                        Pane videoContainer = (Pane) userData;
                        double currentRotation = videoContainer.getRotate();
                        double newRotation = currentRotation + 90;
                        videoContainer.setRotate(newRotation);
                        videoContainer.requestLayout();
                    }
                };
                HBox topBar = createTopBar(item, rotateAction);
                int initialRotation = MediaMetadataUtils.getRotation(item.getFile());
                setupVideoViewer(fullscreenViewer, item, topBar, initialRotation);
            }

            // Ensure focus for keyboard events
            Platform.runLater(() -> {
                fullscreenViewer.requestFocus();
            });
        });
        delay.play();
    }

    private void showVideoErrorUI(MediaItem item, String title, String message) {
        Platform.runLater(() -> {
            // Check if we're still viewing the same item - prevent showing error after navigation
            if (fullscreenViewer == null || currentAttemptingItem != item)
                return;

            VBox errorBox = new VBox(20);
            errorBox.setAlignment(Pos.CENTER);
            errorBox.setStyle("-fx-padding: 20; -fx-background-color: #000000;");

            Label errorLabel = new Label(title);
            errorLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 24px; -fx-font-weight: bold;");

            Label detailLabel = new Label(item.getName());
            detailLabel.setStyle("-fx-text-fill: #f2f2f2; -fx-font-size: 14px;");
            detailLabel.setWrapText(true);
            detailLabel.setMaxWidth(600);

            Label reasonLabel = new Label(message);
            reasonLabel.setStyle("-fx-text-fill: #999999; -fx-font-size: 12px;");
            reasonLabel.setWrapText(true);
            reasonLabel.setMaxWidth(600);

            Button openExternalBtn = new Button("🎬 Open in System Player");
            openExternalBtn.setStyle(
                    "-fx-background-color: #89b4fa; -fx-text-fill: #1e1e2e; -fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
            openExternalBtn.setOnAction(ev -> {
                try {
                    java.awt.Desktop.getDesktop().open(item.getFile());
                } catch (Exception ex) {
                    System.err.println("Failed to open video externally: " + ex.getMessage());
                }
            });

            Button closeBtn = new Button("✕ Close");
            closeBtn.setStyle(
                    "-fx-background-color: #45475a; -fx-text-fill: #cdd6f4; -fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
            closeBtn.setOnAction(ev -> closeFullscreenViewer());

            HBox buttonBox = new HBox(10, openExternalBtn, closeBtn);
            buttonBox.setAlignment(Pos.CENTER);

            errorBox.getChildren().addAll(errorLabel, detailLabel, reasonLabel, buttonBox);

            fullscreenViewer.getChildren().clear();
            fullscreenViewer.getChildren().add(errorBox);
        });
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

        // Clear all ImageViews in fullscreen viewer
        if (fullscreenViewer != null) {
            clearImageViewsRecursive(fullscreenViewer);
        }

        // Restore custom title bar
        if (customTitleBar != null) {
            customTitleBar.setVisible(true);
            customTitleBar.setManaged(true);
        }

        // Restore sidebar visibility
        sidebar.setVisible(true);
        sidebar.setManaged(true);

        // Restore header and gallery view
        rootPane.setTop(headerNode);
        rootPane.setCenter(galleryScrollPane);

        // Restore scroll position after a short delay to ensure layout is complete
        Platform.runLater(() -> {
            galleryScrollPane.setVvalue(savedScrollPosition);
        });

        // Clear fullscreen viewer
        fullscreenViewer = null;

        // Exit fullscreen mode
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.setFullScreen(false);

        // Clear retry state
        currentAttemptingItem = null;
        currentRetryAttempts = 0;
    }

    /**
     * Clear all ImageViews in the gallery to release image references
     * This allows GC to reclaim memory when gallery is refreshed
     */
    private void clearGalleryImageViews() {
        for (javafx.scene.Node node : galleryPane.getChildren()) {
            clearImageViewsRecursive(node);
        }
    }

    /**
     * Recursively clear all ImageView references in a node tree
     * Critical for memory management - releases Image references so GC can reclaim
     */
    private void clearImageViewsRecursive(javafx.scene.Node node) {
        if (node instanceof ImageView) {
            ((ImageView) node).setImage(null);
        } else if (node instanceof javafx.scene.Parent) {
            for (javafx.scene.Node child : ((javafx.scene.Parent) node).getChildrenUnmodifiable()) {
                clearImageViewsRecursive(child);
            }
        }
    }

    public void shutdown() {

        // Stop and dispose current media player
        if (currentMediaPlayer != null) {
            try {
                currentMediaPlayer.stop();
                currentMediaPlayer.dispose();
                currentMediaPlayer = null;

            } catch (Exception e) {
                System.err.println("Error disposing media player: " + e.getMessage());
            }
        }

        // Clear all ImageViews to release image references
        try {
            clearGalleryImageViews();

        } catch (Exception e) {
            System.err.println("Error clearing gallery: " + e.getMessage());
        }

        // Clear media items
        try {
            mediaItems.clear();

        } catch (Exception e) {
            System.err.println("Error clearing media items: " + e.getMessage());
        }

        // Shutdown thumbnail generator (stops thread pool)
        try {
            ThumbnailGenerator.shutdown();

        } catch (Exception e) {
            System.err.println("Error shutting down thumbnail generator: " + e.getMessage());
        }

    }

}
