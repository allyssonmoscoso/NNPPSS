package com.squarepeace.nnppss.fx.controllers;

import com.squarepeace.nnppss.fx.components.GlobalProgressIndicator;
import com.squarepeace.nnppss.fx.components.NotificationPane;
import com.squarepeace.nnppss.model.Console;
import com.squarepeace.nnppss.model.Game;
import com.squarepeace.nnppss.service.*;
import com.squarepeace.nnppss.util.I18n;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MainController {
    
    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @FXML private StackPane rootStack;
    @FXML private HBox consoleBox;
    @FXML private RadioButton rbVita;
    @FXML private RadioButton rbPsp;
    @FXML private RadioButton rbPsx;
    @FXML private ToggleGroup consoleGroup; // Manually managed in initialize if needed
    
    @FXML private ComboBox<String> comboRegion;
    @FXML private ComboBox<String> comboSize;
    @FXML private TextField txtSearch;
    @FXML private Button btnRefresh;
    @FXML private Button btnSettings;
    @FXML private Button btnDownloadList;
    
    @FXML private TableView<Game> tableGames;
    @FXML private TableColumn<Game, String> colTitle;
    @FXML private TableColumn<Game, String> colRegion;
    @FXML private TableColumn<Game, String> colId;
    @FXML private TableColumn<Game, Long> colSize; // Will need custom cell factory for formatting
    @FXML private TableColumn<Game, String> colConsole;
    
    @FXML private Button btnResumePause;
    @FXML private Button btnRetry;
    @FXML private VBox progressContainer;
    
    private GlobalProgressIndicator globalProgress;
    private NotificationPane notificationPane;
    
    private ConfigManager configManager;
    private GameRepository gameRepository;
    private DownloadService downloadService;
    private PackageService packageService;
    private DownloadStateManager downloadStateManager;
    private DatabaseManager databaseManager;
    private DownloadQueueManager downloadQueueManager;
    
    private ObservableList<Game> masterData = FXCollections.observableArrayList();
    private ObservableList<Game> downloadQueue;
    private FilteredList<Game> filteredData;
    
    public void setServices(ConfigManager configManager, GameRepository gameRepository, 
                          DownloadService downloadService, PackageService packageService, 
                          DownloadStateManager downloadStateManager, DatabaseManager databaseManager,
                          DownloadQueueManager downloadQueueManager) {
        this.configManager = configManager;
        this.gameRepository = gameRepository;
        this.downloadService = downloadService;
        this.packageService = packageService;
        this.downloadStateManager = downloadStateManager;
        this.databaseManager = databaseManager;
        this.downloadQueueManager = downloadQueueManager;
        
        // Initialize queue
        this.downloadQueue = FXCollections.observableArrayList(downloadQueueManager.loadQueue());

        // Setup Database Manager Listener
        databaseManager.addListener(new DatabaseManager.DatabaseListener() {
            @Override
            public void onAvailabilityChanged(Console console, boolean available) {
                // Should update UI indicators if we had them (e.g. enable/disable radio buttons?)
            }

            @Override
            public void onDownloadComplete(Console console, boolean success) {
                Platform.runLater(() -> {
                    if (success) {
                        notificationPane.showNotification("Database for " + console + " downloaded.", NotificationPane.NotificationType.SUCCESS);
                        // Reload if this is the current console
                        Console selected = rbVita.isSelected() ? Console.PSVITA : (rbPsp.isSelected() ? Console.PSP : Console.PSX);
                        if (console == selected) {
                            loadGames(console);
                        }
                    } else {
                        notificationPane.showNotification("Failed to download database for " + console, NotificationPane.NotificationType.ERROR);
                    }
                });
            }

            @Override
            public void onDownloadProgress(Console console, String message) {
                // Log progress instead of showing notification for every update
                log.debug("Database download progress for {}: {}", console, message);
            }
        });
        
        // Initial load using availability check
        databaseManager.checkAllConsolesAvailability().thenRun(() -> {
             Platform.runLater(() -> {
                 // Trigger download for missing databases
                 databaseManager.downloadAllDatabases();
                 
                 // If PSVITA is available, load it immediately.
                 // Otherwise, wait for the onDownloadComplete listener to fire.
                 if (databaseManager.isConsoleAvailable(Console.PSVITA)) {
                    loadGames(Console.PSVITA);
                 } else {
                    notificationPane.showNotification("Downloading game database...", NotificationPane.NotificationType.INFO);
                 }
             });
        });
    }

    @FXML
    public void initialize() {
        // Setup Components
        setupComponents();
        
        // Setup Table
        setupTable();
        
        // Setup Listeners
        setupListeners();
    }
    
    private void setupComponents() {
        // Toggle Group for Radios
        ToggleGroup group = new ToggleGroup();
        rbVita.setToggleGroup(group);
        rbPsp.setToggleGroup(group);
        rbPsx.setToggleGroup(group);
        
        // Setup Global Progress
        globalProgress = new GlobalProgressIndicator();
        progressContainer.getChildren().add(globalProgress);
        
        // Setup NotificationPane
        notificationPane = new NotificationPane();
        rootStack.getChildren().add(notificationPane); // Add on top
        
        // Combos
        comboRegion.getItems().addAll("All", "US", "EUR", "JPN", "ASIA");
        comboRegion.getSelectionModel().selectFirst();
        
        comboSize.getItems().addAll("All sizes", "< 1GB", "1-5GB", "> 5GB");
        comboSize.getSelectionModel().selectFirst();
    }
    
    private void setupTable() {
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colRegion.setCellValueFactory(new PropertyValueFactory<>("region"));
        colId.setCellValueFactory(new PropertyValueFactory<>("contentId"));
        colConsole.setCellValueFactory(new PropertyValueFactory<>("console"));
        
        // Custom cell for Size
        colSize.setCellValueFactory(new PropertyValueFactory<>("fileSize"));
        colSize.setCellFactory(column -> new TableCell<Game, Long>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatSize(item));
                }
            }
        });
        
        filteredData = new FilteredList<>(masterData, p -> true);
        tableGames.setItems(filteredData);
        
        // Double click to download
        tableGames.setRowFactory(tv -> {
            TableRow<Game> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY 
                     && event.getClickCount() == 2) {
                    Game clickedRow = row.getItem();
                    handleGameSelection(clickedRow);
                }
            });
            return row;
        });
    }
    
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private void setupListeners() {
        rbVita.setOnAction(e -> loadGames(Console.PSVITA));
        rbPsp.setOnAction(e -> loadGames(Console.PSP));
        rbPsx.setOnAction(e -> loadGames(Console.PSX));
        
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> updateFilter());
        comboRegion.valueProperty().addListener((obs, oldVal, newVal) -> updateFilter());
        comboSize.valueProperty().addListener((obs, oldVal, newVal) -> updateFilter());
    }
    

    private void updateFilter() {
        if (filteredData == null) return;
        
        filteredData.setPredicate(game -> {
            // Search Text
            String search = txtSearch.getText().toLowerCase();
            if (search != null && !search.isEmpty()) {
                boolean matchesTitle = game.getTitle().toLowerCase().contains(search);
                boolean matchesId = game.getContentId() != null && game.getContentId().toLowerCase().contains(search);
                if (!matchesTitle && !matchesId) return false; 
            }
            
            // Region
            String region = comboRegion.getValue();
            if (region != null && !"All".equals(region)) {
                if (game.getRegion() == null || !game.getRegion().contains(region)) return false;
            }
            
            // Size
            String sizeFilter = comboSize.getValue();
            if (sizeFilter != null && !"All sizes".equals(sizeFilter)) {
                long size = game.getFileSize();
                double sizeGB = size / (1024.0 * 1024.0 * 1024.0);
                if ("< 1GB".equals(sizeFilter) && sizeGB >= 1.0) return false;
                if ("1-5GB".equals(sizeFilter) && (sizeGB < 1.0 || sizeGB > 5.0)) return false;
                if ("> 5GB".equals(sizeFilter) && sizeGB <= 5.0) return false;
            }
            
            return true;
        });
    }
    
    private void loadGames(Console console) {
        if (configManager == null) return; // Services not ready
        
        Task<List<Game>> loadTask = new Task<List<Game>>() {
            @Override
            protected List<Game> call() throws Exception {
                // Fetch games from repository
                return gameRepository.loadGames(console);
            }
        };
        
        loadTask.setOnSucceeded(e -> {
            masterData.setAll(loadTask.getValue());
            updateRegionCombo();
            log.info("Loaded {} games for {}", masterData.size(), console);
        });
        
        loadTask.setOnFailed(e -> {
            notificationPane.showNotification("Failed to load games: " + loadTask.getException().getMessage(), NotificationPane.NotificationType.ERROR);
            log.error("Load failed", loadTask.getException());
        });
        
        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void updateRegionCombo() {
        // Capture current selection to restore if still valid
        String currentSelection = comboRegion.getValue();
        
        // Collect unique regions from data, preserving order or sorting
        java.util.Set<String> regions = new java.util.TreeSet<>();
        for (Game game : masterData) {
            String r = game.getRegion();
            if (r != null && !r.isEmpty()) {
                regions.add(r);
            }
        }
        
        // Update items on UI thread (already here since called from setOnSucceeded)
        comboRegion.getItems().clear();
        comboRegion.getItems().add("All");
        comboRegion.getItems().addAll(regions);
        
        // Restore selection or select first
        if (currentSelection != null && comboRegion.getItems().contains(currentSelection)) {
             comboRegion.setValue(currentSelection);
        } else {
             comboRegion.getSelectionModel().selectFirst();
        }
    }
    
    private void handleGameSelection(Game game) {
        if (downloadQueue == null) {
            notificationPane.showNotification("Download service not ready", NotificationPane.NotificationType.ERROR);
            return;
        }
        
        // Check if already in queue
        boolean alreadyInQueue = downloadQueue.stream().anyMatch(g -> g.getPkgUrl().equals(game.getPkgUrl()));
        if (alreadyInQueue) {
            notificationPane.showNotification("Game already in download queue", NotificationPane.NotificationType.WARNING);
            return;
        }
        
        downloadQueue.add(game);
        downloadQueueManager.saveQueue(new java.util.ArrayList<>(downloadQueue));
        notificationPane.showNotification("Added to Download List: " + game.getTitle(), NotificationPane.NotificationType.SUCCESS);
    }
    
    @FXML
    private void handleOpenDownloadList() {
        if (downloadQueue == null) return;
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DownloadListView.fxml"));
            Parent root = loader.load();
            
            DownloadListController controller = loader.getController();
            controller.setQueueData(downloadQueue, downloadQueueManager);
            controller.setOnStartDownloads(this::startDownloadsFromQueue);
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Download List");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
        } catch (IOException e) {
            log.error("Failed to open download list", e);
            notificationPane.showNotification("Error opening download list", NotificationPane.NotificationType.ERROR);
        }
    }
    
    private void startDownloadsFromQueue() {
        if (downloadQueue.isEmpty()) return;
        
        List<Game> gamesToStart = new java.util.ArrayList<>(downloadQueue);
        int count = gamesToStart.size();
        
        for (Game game : gamesToStart) {
             // In a real app we might want to verify destination path, etc.
             // Using default logic from Frame.java (simplified)
             String destPath = "packages/" + game.getFileName();
             
             // Check if already downloading? (DownloadService handles this somewhat)
             // We just fire them off. DownloadService manages the queue/threads.
             downloadService.downloadFile(game.getPkgUrl(), destPath, null); // Listener is handled globally or we could add one here
        }
        
        // Clear queue after starting
        downloadQueue.clear();
        downloadQueueManager.clearQueue();
        
        notificationPane.showNotification("Started " + count + " downloads", NotificationPane.NotificationType.SUCCESS);
    }

    @FXML
    private void handleRefresh() {
        Console selected = rbVita.isSelected() ? Console.PSVITA : (rbPsp.isSelected() ? Console.PSP : Console.PSX);
        loadGames(selected);
    }

    @FXML
    private void handleOpenSettings() {
        if (configManager == null) return;
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ConfigView.fxml"));
            Parent root = loader.load();
            
            ConfigController controller = loader.getController();
            controller.setConfigManager(configManager);
            controller.setOnSaveCallback(() -> {
                handleRefresh();
            });
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Settings");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
        } catch (IOException e) {
            log.error("Failed to open settings", e);
            notificationPane.showNotification("Error opening settings", NotificationPane.NotificationType.ERROR);
        }
    }
    @FXML
    private void handleResumePause() {
        // Implementation
    }
    
    @FXML
    private void handleRetryFailed() {
        // Implementation
    }
}
