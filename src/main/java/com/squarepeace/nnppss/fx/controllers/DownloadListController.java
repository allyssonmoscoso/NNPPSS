package com.squarepeace.nnppss.fx.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.squarepeace.nnppss.app.AppPaths;
import com.squarepeace.nnppss.model.Game;
import com.squarepeace.nnppss.service.DownloadQueueManager;
import com.squarepeace.nnppss.util.I18n;
import com.squarepeace.nnppss.util.JsonStore;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

public class DownloadListController {

    private static final Logger log = LoggerFactory.getLogger(DownloadListController.class);

    @FXML private TableView<Game> tableQueue;
    @FXML private TableColumn<Game, Void> colIndex;
    @FXML private TableColumn<Game, String> colTitle;
    @FXML private TableColumn<Game, String> colRegion;
    @FXML private TableColumn<Game, String> colConsole;
    @FXML private TableColumn<Game, Long> colSize;

    @FXML private Label statsLabel;
    @FXML private Button btnSelectAll;
    @FXML private Button btnDeselectAll;
    @FXML private Button btnMoveUp;
    @FXML private Button btnMoveDown;
    @FXML private Button btnRemove;
    @FXML private Button btnClear;
    @FXML private Button btnExport;
    @FXML private Button btnImport;
    @FXML private Button btnStart;
    @FXML private Button btnClose;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private ObservableList<Game> downloadQueue;
    private DownloadQueueManager queueManager;
    private BooleanSupplier onStartDownloadsCallback;
    private Runnable onClose;
    private ListChangeListener<Game> queueListener;

    @FXML
    public void initialize() {
        setupTable();
    }

    public void setQueueData(ObservableList<Game> downloadQueue, DownloadQueueManager queueManager) {
        this.downloadQueue = downloadQueue;
        this.queueManager = queueManager;
        tableQueue.setItems(downloadQueue);

        if (queueListener != null) {
            downloadQueue.removeListener(queueListener);
        }
        queueListener = change -> {
            updateStats();
            updateButtonState();
        };
        downloadQueue.addListener(queueListener);

        updateStats();
        updateButtonState();
    }

    /** Refreshes stats and buttons when the drawer is reopened. */
    public void refresh() {
        updateStats();
        updateButtonState();
        if (tableQueue != null) {
            tableQueue.refresh();
        }
    }

    public void focusTable() {
        if (tableQueue != null) {
            tableQueue.requestFocus();
        }
    }

    private void detachQueueListener() {
        if (downloadQueue != null && queueListener != null) {
            downloadQueue.removeListener(queueListener);
            queueListener = null;
        }
    }

    /** Releases the queue listener when the drawer is disposed. */
    public void detach() {
        detachQueueListener();
    }

    public void setOnStartDownloads(BooleanSupplier callback) {
        this.onStartDownloadsCallback = callback;
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    private void setupTable() {
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colRegion.setCellValueFactory(new PropertyValueFactory<>("region"));
        colConsole.setCellValueFactory(new PropertyValueFactory<>("console"));
        colSize.setCellValueFactory(new PropertyValueFactory<>("fileSize"));

        colIndex.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : String.valueOf(getIndex() + 1));
            }
        });

        colSize.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatSize(item));
            }
        });

        tableQueue.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableQueue.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> updateButtonState());
    }

    private void updateStats() {
        if (statsLabel == null) {
            return;
        }
        if (downloadQueue == null || downloadQueue.isEmpty()) {
            statsLabel.setText("");
            statsLabel.getStyleClass().remove("stats-warning");
            return;
        }

        long totalBytes = downloadQueue.stream().mapToLong(Game::getFileSize).sum();
        Path packagesDir = AppPaths.resolve("packages");
        File spaceDir = Files.isDirectory(packagesDir)
                ? packagesDir.toFile()
                : AppPaths.dataDir().toFile();
        long freeBytes = spaceDir.getUsableSpace();
        boolean insufficient = totalBytes > freeBytes;

        String totalGb = String.format(Locale.ROOT, "%.2f",
                totalBytes / (1024.0 * 1024.0 * 1024.0));
        String freeGb = String.format(Locale.ROOT, "%.2f",
                freeBytes / (1024.0 * 1024.0 * 1024.0));

        statsLabel.setText(I18n.get(
                insufficient ? "queue.stats.insufficient" : "queue.stats",
                downloadQueue.size(), totalGb, freeGb));
        statsLabel.getStyleClass().remove("stats-warning");
        if (insufficient) {
            statsLabel.getStyleClass().add("stats-warning");
        }
    }

    private void updateButtonState() {
        boolean empty = downloadQueue == null || downloadQueue.isEmpty();
        var selected = tableQueue.getSelectionModel().getSelectedIndices();
        boolean hasSelection = !selected.isEmpty();

        btnStart.setDisable(empty);
        btnClear.setDisable(empty);
        btnExport.setDisable(empty);
        btnSelectAll.setDisable(empty);
        btnRemove.setDisable(!hasSelection);
        btnDeselectAll.setDisable(!hasSelection);

        boolean canMoveUp = hasSelection && selected.stream().anyMatch(index -> index > 0);
        boolean canMoveDown = hasSelection && !empty
                && selected.stream().anyMatch(index -> index < downloadQueue.size() - 1);
        btnMoveUp.setDisable(!canMoveUp);
        btnMoveDown.setDisable(!canMoveDown);
    }

    @FXML
    private void handleSelectAll() {
        tableQueue.getSelectionModel().selectAll();
        updateButtonState();
    }

    @FXML
    private void handleDeselectAll() {
        tableQueue.getSelectionModel().clearSelection();
        updateButtonState();
    }

    @FXML
    private void handleMoveUp() {
        moveSelected(-1);
    }

    @FXML
    private void handleMoveDown() {
        moveSelected(1);
    }

    private void moveSelected(int delta) {
        if (downloadQueue == null || downloadQueue.isEmpty()) {
            return;
        }
        List<Integer> indices = new ArrayList<>(tableQueue.getSelectionModel().getSelectedIndices());
        if (indices.isEmpty()) {
            return;
        }
        indices.sort(delta < 0 ? Comparator.naturalOrder() : Comparator.reverseOrder());

        for (int index : indices) {
            int target = index + delta;
            if (target < 0 || target >= downloadQueue.size()) {
                continue;
            }
            Game game = downloadQueue.remove(index);
            downloadQueue.add(target, game);
        }

        tableQueue.getSelectionModel().clearSelection();
        for (int index : indices) {
            int target = index + delta;
            if (target >= 0 && target < downloadQueue.size()) {
                tableQueue.getSelectionModel().select(target);
            }
        }
        saveQueue();
        tableQueue.refresh();
        updateButtonState();
    }

    @FXML
    private void handleRemoveSelected() {
        if (downloadQueue == null) {
            return;
        }
        var selected = new ArrayList<>(tableQueue.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) {
            return;
        }
        if (!confirm("dialog.confirmremove.title",
                I18n.get("dialog.confirmremove.message", selected.size()))) {
            return;
        }
        downloadQueue.removeAll(selected);
        saveQueue();
        updateStats();
        updateButtonState();
    }

    @FXML
    private void handleClearAll() {
        if (downloadQueue == null || downloadQueue.isEmpty()) {
            return;
        }
        if (!confirm("dialog.confirmclear.title",
                I18n.get("dialog.confirmclear.message", downloadQueue.size()))) {
            return;
        }
        downloadQueue.clear();
        saveQueue();
        updateStats();
        updateButtonState();
        showInfo(I18n.get("notify.queuecleared"));
    }

    @FXML
    private void handleExport() {
        if (downloadQueue == null || downloadQueue.isEmpty()) {
            showWarning(I18n.get("notify.queueemptynoexport"));
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.get("dialog.export.title"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON files (*.json)", "*.json"));
        chooser.setInitialFileName("download-queue-export.json");
        File file = chooser.showSaveDialog(ownerWindow());
        if (file == null) {
            return;
        }
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".json")) {
            file = new File(file.getAbsolutePath() + ".json");
        }

        try {
            JsonStore.write(file.toPath(), new ArrayList<>(downloadQueue), gson);
            showInfo(I18n.get("notify.queueexported", file.getName()));
            log.info("Queue exported to {}", file.getAbsolutePath());
        } catch (IOException e) {
            log.error("Error exporting queue", e);
            showError(I18n.get("notify.exportfailed", e.getMessage()));
        }
    }

    @FXML
    private void handleImport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.get("dialog.import.title"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON files (*.json)", "*.json"));
        File file = chooser.showOpenDialog(ownerWindow());
        if (file == null) {
            return;
        }

        try {
            Type listType = new TypeToken<List<Game>>() {
            }.getType();
            List<Game> imported = JsonStore.read(file.toPath(), listType, gson);
            if (imported == null || imported.isEmpty()) {
                showWarning(I18n.get("notify.nogamesinfile"));
                return;
            }

            int added = 0;
            for (Game game : imported) {
                if (game == null || game.getPkgUrl() == null) {
                    continue;
                }
                boolean exists = downloadQueue.stream()
                        .anyMatch(queued -> game.getPkgUrl().equals(queued.getPkgUrl()));
                if (!exists) {
                    downloadQueue.add(game);
                    added++;
                }
            }
            if (added > 0) {
                saveQueue();
                updateStats();
                showInfo(I18n.get("notify.queueimported", added));
                log.info("Imported {} games from {}", added, file.getAbsolutePath());
            } else {
                showInfo(I18n.get("notify.alreadyinqueue"));
            }
        } catch (IOException | RuntimeException e) {
            log.error("Error importing queue", e);
            showError(I18n.get("notify.importfailed", e.getMessage()));
        }
        updateButtonState();
    }

    @FXML
    private void handleStartDownloads() {
        if (downloadQueue == null || downloadQueue.isEmpty()) {
            showWarning(I18n.get("notify.queueempty"));
            return;
        }
        if (onStartDownloadsCallback != null && onStartDownloadsCallback.getAsBoolean()) {
            closeWindow();
        }
    }

    @FXML
    private void handleClose() {
        closeWindow();
    }

    private void saveQueue() {
        if (queueManager != null && downloadQueue != null) {
            queueManager.saveQueue(new ArrayList<>(downloadQueue));
        }
    }

    private void closeWindow() {
        if (onClose != null) {
            onClose.run();
            return;
        }
        Stage stage = (Stage) btnClose.getScene().getWindow();
        stage.close();
    }

    private javafx.stage.Window ownerWindow() {
        return btnClose.getScene() == null ? null : btnClose.getScene().getWindow();
    }

    private boolean confirm(String titleKey, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18n.get(titleKey));
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private void showInfo(String message) {
        showAlert(Alert.AlertType.INFORMATION, message);
    }

    private void showWarning(String message) {
        showAlert(Alert.AlertType.WARNING, message);
    }

    private void showError(String message) {
        showAlert(Alert.AlertType.ERROR, message);
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(I18n.get("dialog.downloadqueue.title"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
