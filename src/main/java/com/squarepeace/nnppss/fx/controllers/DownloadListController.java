package com.squarepeace.nnppss.fx.controllers;

import com.squarepeace.nnppss.model.Game;
import com.squarepeace.nnppss.service.DownloadQueueManager;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class DownloadListController {
    
    private static final Logger log = LoggerFactory.getLogger(DownloadListController.class);

    @FXML private TableView<Game> tableQueue;
    @FXML private TableColumn<Game, String> colTitle;
    @FXML private TableColumn<Game, String> colRegion;
    @FXML private TableColumn<Game, String> colConsole;
    @FXML private TableColumn<Game, Long> colSize;
    
    @FXML private Button btnStart;
    @FXML private Button btnRemove;
    @FXML private Button btnClose;
    
    private ObservableList<Game> downloadQueue;
    private DownloadQueueManager queueManager;
    private Runnable onStartDownloadsCallback;
    
    @FXML
    public void initialize() {
        setupTable();
    }
    
    public void setQueueData(ObservableList<Game> downloadQueue, DownloadQueueManager queueManager) {
        this.downloadQueue = downloadQueue;
        this.queueManager = queueManager;
        tableQueue.setItems(downloadQueue);
        updateButtonState();
    }
    
    public void setOnStartDownloads(Runnable callback) {
        this.onStartDownloadsCallback = callback;
    }
    
    private void setupTable() {
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colRegion.setCellValueFactory(new PropertyValueFactory<>("region"));
        colConsole.setCellValueFactory(new PropertyValueFactory<>("console"));
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
        
        // Listen to selection changes to update button state
        tableQueue.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableQueue.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updateButtonState());
    }
    
    private void updateButtonState() {
        boolean hasItems = downloadQueue != null && !downloadQueue.isEmpty();
        boolean hasSelection = !tableQueue.getSelectionModel().getSelectedItems().isEmpty();
        
        btnStart.setDisable(!hasItems);
        btnRemove.setDisable(!hasSelection);
    }
    
    @FXML
    private void handleRemoveSelected() {
        var selected = new ArrayList<>(tableQueue.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) return;
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Remove from Queue");
        alert.setHeaderText("Remove " + selected.size() + " games from queue?");
        alert.setContentText("This will remove the selected games from your download list.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                downloadQueue.removeAll(selected);
                queueManager.saveQueue(new ArrayList<>(downloadQueue));
                updateButtonState();
            }
        });
    }
    
    @FXML
    private void handleStartDownloads() {
        if (onStartDownloadsCallback != null) {
            onStartDownloadsCallback.run();
        }
        closeWindow();
    }
    
    @FXML
    private void handleClose() {
        closeWindow();
    }
    
    private void closeWindow() {
        Stage stage = (Stage) btnClose.getScene().getWindow();
        stage.close();
    }
    
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
