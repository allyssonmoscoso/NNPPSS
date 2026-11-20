package com.squarepeace.nnppss.fx.controllers;

import com.squarepeace.nnppss.app.AppContext;
import com.squarepeace.nnppss.app.ThemeManager;
import com.squarepeace.nnppss.fx.components.DownloadProgressItem;
import com.squarepeace.nnppss.fx.components.GlobalProgressIndicator;
import com.squarepeace.nnppss.fx.components.NotificationPane;
import com.squarepeace.nnppss.fx.util.GameFilter;
import com.squarepeace.nnppss.model.Console;
import com.squarepeace.nnppss.model.DownloadHistory;
import com.squarepeace.nnppss.model.DownloadState;
import com.squarepeace.nnppss.model.Game;
import com.squarepeace.nnppss.model.download.Segment;
import com.squarepeace.nnppss.service.ConfigManager;
import com.squarepeace.nnppss.service.DatabaseManager;
import com.squarepeace.nnppss.service.DownloadCoordinator;
import com.squarepeace.nnppss.service.DownloadHistoryManager;
import com.squarepeace.nnppss.service.DownloadQueueManager;
import com.squarepeace.nnppss.service.DownloadService;
import com.squarepeace.nnppss.service.DownloadStateManager;
import com.squarepeace.nnppss.service.GameRepository;
import com.squarepeace.nnppss.service.PackageService;
import com.squarepeace.nnppss.service.SystemValidator;
import com.squarepeace.nnppss.service.download.DownloadTaskState;
import com.squarepeace.nnppss.util.I18n;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @FXML private StackPane rootStack;
    @FXML private HBox consoleBox;
    @FXML private RadioButton rbVita;
    @FXML private RadioButton rbPsp;
    @FXML private RadioButton rbPsx;

    @FXML private ComboBox<String> comboRegion;
    @FXML private ComboBox<String> comboSize;
    @FXML private TextField txtSearch;
    @FXML private Button btnRefresh;
    @FXML private Button btnSettings;
    @FXML private Button btnDownloadList;
    @FXML private Button btnAdd;
    @FXML private Button btnTheme;
    @FXML private Label resultCountLabel;

    @FXML private TableView<Game> tableGames;
    @FXML private TableColumn<Game, String> colTitle;
    @FXML private TableColumn<Game, String> colRegion;
    @FXML private TableColumn<Game, String> colId;
    @FXML private TableColumn<Game, Long> colSize;
    @FXML private TableColumn<Game, String> colConsole;

    @FXML private Button btnResumePause;
    @FXML private Button btnRetry;
    @FXML private VBox progressContainer;
    @FXML private Label statusLabel;
    @FXML private VBox queueDrawer;

    @FXML private VBox detailPane;
    @FXML private Label detailTitle;
    @FXML private Label detailMeta;
    @FXML private Label detailStatus;
    @FXML private Label detailContentId;
    @FXML private Label detailHint;
    @FXML private Button btnDownloadNow;
    @FXML private Button btnAddToQueue;

    private GlobalProgressIndicator globalProgress;
    private NotificationPane notificationPane;

    private AppContext context;
    private ConfigManager configManager;
    private GameRepository gameRepository;
    private DownloadService downloadService;
    private PackageService packageService;
    private DownloadStateManager downloadStateManager;
    private DatabaseManager databaseManager;
    private DownloadQueueManager downloadQueueManager;
    private DownloadHistoryManager downloadHistoryManager;
    private SystemValidator systemValidator;
    private DownloadCoordinator downloadCoordinator;
    private DownloadListController queueController;
    private Parent queueRoot;

    private ObservableList<Game> masterData = FXCollections.observableArrayList();
    private ObservableList<Game> downloadQueue;
    private FilteredList<Game> filteredData;
    private final Map<String, DownloadHistory> historyByUrl = new HashMap<>();
    private final Map<String, DownloadProgressItem> downloadItems = new LinkedHashMap<>();
    private final List<String> downloadOrder = new ArrayList<>();
    private final Set<String> selectedDownloadUrls = new LinkedHashSet<>();
    private final Map<String, Long> lastProgressUiMs = new HashMap<>();
    private final PauseTransition filterDebounce = new PauseTransition(Duration.millis(250));
    private Task<List<Game>> currentLoadTask;
    private String lastSelectionAnchor;
    private int loadGeneration;
    private boolean loading;
    private boolean started;
    private boolean restorePrompted;
    private boolean retryingFailed;

    /**
     * Receives the application services. It does not start any background work:
     * {@link #start()} does that once the configuration is known to be valid.
     */
    public void setContext(AppContext context) {
        this.context = context;
        this.configManager = context.getConfigManager();
        this.gameRepository = context.getGameRepository();
        this.downloadService = context.getDownloadService();
        this.packageService = context.getPackageService();
        this.downloadStateManager = context.getDownloadStateManager();
        this.databaseManager = context.getDatabaseManager();
        this.downloadQueueManager = context.getDownloadQueueManager();
        this.downloadHistoryManager = context.getDownloadHistoryManager();
        this.systemValidator = context.getSystemValidator();

        this.downloadCoordinator = new DownloadCoordinator(configManager, downloadService,
                packageService, downloadStateManager, downloadHistoryManager,
                systemValidator, gameRepository);
        this.downloadCoordinator.setListener(new CoordinatorListener());

        this.downloadQueue = FXCollections.observableArrayList(downloadQueueManager.loadQueue());
        this.downloadQueue.addListener((javafx.collections.ListChangeListener<Game>) change -> {
            updateQueueButton();
            Game selected = tableGames.getSelectionModel().getSelectedItem();
            updateDetailStatus(selected);
            updateAddToQueueButton(selected);
        });
        refreshHistoryCache();
        updateRetryButton();
        updateQueueButton();
        updateThemeButton();
        log.info("Controller ready; {} game(s) in the saved queue", downloadQueue.size());
    }

    /** True when no database URL has been configured yet. */
    public boolean needsConfiguration() {
        if (configManager == null) {
            return true;
        }
        return isBlank(configManager.getPsVitaUrl())
                && isBlank(configManager.getPspUrl())
                && isBlank(configManager.getPsxUrl());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * Starts the first-run configuration wizard. Returns after the dialog is
     * closed, so the caller can decide whether the application is usable.
     */
    public void showInitialConfiguration() {
        log.info("No database URLs configured; showing the initial configuration wizard");
        openConfigWindow(true);
        if (!started && !needsConfiguration()) {
            start();
        }
        if (!started) {
            statusLabel.setText(I18n.get("status.ready"));
            notificationPane.showNotification(I18n.get("notify.configrequired"),
                    NotificationPane.NotificationType.WARNING, 8000);
        }
    }

    /** Starts database checks, downloads and the initial game load. */
    public void start() {
        if (started) {
            return;
        }
        if (context == null) {
            log.warn("start() called before setContext(); ignoring");
            return;
        }
        started = true;
        setupDatabaseListener();
        refreshConsoleAvailability();
        installShortcuts();
        restoreSavedDownloads();
    }

    @FXML
    public void initialize() {
        filterDebounce.setOnFinished(event -> updateFilter());
        setupComponents();
        setupTable();
        setupListeners();
        if (statusLabel != null) {
            statusLabel.setText(I18n.get("status.ready"));
        }
        if (resultCountLabel != null) {
            resultCountLabel.setText(I18n.get("results.count", 0));
        }
    }

    private void setupComponents() {
        ToggleGroup group = new ToggleGroup();
        rbVita.setToggleGroup(group);
        rbPsp.setToggleGroup(group);
        rbPsx.setToggleGroup(group);

        globalProgress = new GlobalProgressIndicator();
        progressContainer.getChildren().add(globalProgress);
        // The download dock only takes space while a session is visible.
        progressContainer.visibleProperty().bind(globalProgress.visibleProperty());
        progressContainer.managedProperty().bind(globalProgress.visibleProperty());

        notificationPane = new NotificationPane();
        rootStack.getChildren().add(notificationPane);

        comboRegion.getItems().add(I18n.get("filter.allregions"));
        comboRegion.getSelectionModel().selectFirst();

        comboSize.getItems().addAll(
                I18n.get("filter.size.all"),
                I18n.get("filter.size.lt1"),
                I18n.get("filter.size.1to5"),
                I18n.get("filter.size.gt5"));
        comboSize.getSelectionModel().selectFirst();

        if (btnDownloadNow != null) {
            btnDownloadNow.setMaxWidth(Double.MAX_VALUE);
        }
        if (btnAddToQueue != null) {
            btnAddToQueue.setMaxWidth(Double.MAX_VALUE);
        }
    }

    private void setupTable() {
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colRegion.setCellValueFactory(new PropertyValueFactory<>("region"));
        if (colId != null) {
            colId.setCellValueFactory(new PropertyValueFactory<>("contentId"));
        }
        colConsole.setCellValueFactory(data ->
                new javafx.beans.property.ReadOnlyStringWrapper(
                        consoleLabel(data.getValue().getConsole())));

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

        tableGames.setRowFactory(tv -> {
            TableRow<Game> row = new TableRow<>() {
                @Override
                protected void updateItem(Game game, boolean empty) {
                    super.updateItem(game, empty);
                    getStyleClass().removeAll("row-completed", "row-paused", "row-failed");
                    if (!empty && game != null) {
                        String status = historyStatus(game.getPkgUrl());
                        if ("completed".equals(status)) {
                            getStyleClass().add("row-completed");
                        } else if ("paused".equals(status)) {
                            getStyleClass().add("row-paused");
                        } else if ("failed".equals(status)) {
                            getStyleClass().add("row-failed");
                        }
                    }
                }
            };
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY
                        && event.getClickCount() == 2) {
                    downloadNow(row.getItem());
                }
            });
            return row;
        });

        tableGames.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                downloadNow(tableGames.getSelectionModel().getSelectedItem());
            }
        });

        tableGames.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldGame, newGame) -> showGameDetail(newGame));

        if (btnAdd != null) {
            btnAdd.disableProperty().bind(
                    tableGames.getSelectionModel().selectedItemProperty().isNull());
        }

        showGameDetail(null);
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private void setupListeners() {
        rbVita.setOnAction(e -> loadGames(Console.PSVITA));
        rbPsp.setOnAction(e -> loadGames(Console.PSP));
        rbPsx.setOnAction(e -> loadGames(Console.PSX));

        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> scheduleFilter());
        comboRegion.valueProperty().addListener((obs, oldVal, newVal) -> scheduleFilter());
        comboSize.valueProperty().addListener((obs, oldVal, newVal) -> scheduleFilter());
    }

    private void scheduleFilter() {
        filterDebounce.playFromStart();
    }

    private void updateFilter() {
        if (filteredData == null) return;

        filteredData.setPredicate(GameFilter.predicate(
                txtSearch.getText(),
                comboRegion.getValue(),
                I18n.get("filter.allregions"),
                comboSize.getValue(),
                I18n.get("filter.size.all"),
                I18n.get("filter.size.lt1"),
                I18n.get("filter.size.1to5"),
                I18n.get("filter.size.gt5")));

        if (resultCountLabel != null) {
            resultCountLabel.setText(I18n.get("results.count", filteredData.size()));
        }
    }

    private void loadGames(Console console) {
        if (configManager == null || console == null) {
            return;
        }
        if (databaseManager != null && !databaseManager.isConsoleAvailable(console)) {
            notificationPane.showNotification(
                    I18n.get("status.database.unavailable", console.getDisplayName()),
                    NotificationPane.NotificationType.WARNING);
            return;
        }

        if (currentLoadTask != null && !currentLoadTask.isDone()) {
            currentLoadTask.cancel();
        }

        final int generation = ++loadGeneration;
        setLoading(true);
        statusLabel.setText(I18n.get("status.loading"));

        Task<List<Game>> loadTask = new Task<>() {
            @Override
            protected List<Game> call() throws Exception {
                return gameRepository.loadGames(console);
            }
        };
        currentLoadTask = loadTask;

        loadTask.setOnSucceeded(e -> {
            if (generation != loadGeneration) {
                return;
            }
            masterData.setAll(loadTask.getValue());
            tableGames.getSelectionModel().clearSelection();
            refreshHistoryCache();
            updateRegionCombo();
            updateFilter();
            statusLabel.setText(I18n.get("status.loaded", masterData.size()));
            setLoading(false);
            log.info("Loaded {} games for {}", masterData.size(), console);
        });

        loadTask.setOnFailed(e -> {
            if (generation != loadGeneration) {
                return;
            }
            statusLabel.setText(I18n.get("status.ready"));
            setLoading(false);
            notificationPane.showNotification(
                    I18n.get("notify.errorloading", loadTask.getException().getMessage()),
                    NotificationPane.NotificationType.ERROR);
            log.error("Load failed", loadTask.getException());
        });

        Thread thread = new Thread(loadTask, "game-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private void setLoading(boolean value) {
        loading = value;
        if (btnRefresh != null) {
            btnRefresh.setDisable(value);
        }
        for (Console console : Console.values()) {
            RadioButton button = getConsoleButton(console);
            if (button != null) {
                button.setDisable(value || !isConsoleAvailable(console));
            }
        }
        if (rootStack != null && rootStack.getScene() != null) {
            rootStack.setCursor(value ? Cursor.WAIT : Cursor.DEFAULT);
        }
    }

    private boolean isConsoleAvailable(Console console) {
        return databaseManager == null || databaseManager.isConsoleAvailable(console);
    }

    private void refreshHistoryCache() {
        historyByUrl.clear();
        if (downloadHistoryManager == null) {
            return;
        }
        try {
            for (DownloadHistory entry : downloadHistoryManager.loadHistory()) {
                if (entry.getPkgUrl() != null) {
                    historyByUrl.put(entry.getPkgUrl(), entry);
                }
            }
        } catch (Exception e) {
            log.warn("Could not load the download history", e);
        }
    }

    private String historyStatus(String pkgUrl) {
        DownloadHistory entry = historyByUrl.get(pkgUrl);
        return entry == null ? null : entry.getStatus();
    }

    private void updateRegionCombo() {
        String allRegions = I18n.get("filter.allregions");
        String currentSelection = comboRegion.getValue();

        Set<String> regions = new TreeSet<>();
        for (Game game : masterData) {
            String r = game.getRegion();
            if (r != null && !r.isEmpty()) {
                regions.add(r);
            }
        }

        comboRegion.getItems().clear();
        comboRegion.getItems().add(allRegions);
        comboRegion.getItems().addAll(regions);

        if (currentSelection != null && comboRegion.getItems().contains(currentSelection)) {
            comboRegion.setValue(currentSelection);
        } else {
            comboRegion.getSelectionModel().selectFirst();
        }
    }

    private void handleGameSelection(Game game) {
        addToQueue(game);
    }

    @FXML
    private void handleAddToQueue() {
        Game game = tableGames.getSelectionModel().getSelectedItem();
        if (game == null) {
            addToQueue(null);
            return;
        }
        if (isQueued(game)) {
            removeFromQueue(game);
        } else {
            addToQueue(game);
        }
    }

    @FXML
    private void handleAddSelected() {
        addToQueue(tableGames.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void handleDownloadNow() {
        downloadNow(tableGames.getSelectionModel().getSelectedItem());
    }

    /**
     * Adds a game to the queue without a confirmation dialog. The action is
     * reversible from the queue until downloads start.
     */
    private boolean addToQueue(Game game) {
        if (game == null) {
            notificationPane.showNotification(I18n.get("notify.selectgame"),
                    NotificationPane.NotificationType.INFO);
            return false;
        }
        if (downloadQueue == null) {
            notificationPane.showNotification(I18n.get("notify.servicenotready"),
                    NotificationPane.NotificationType.ERROR);
            return false;
        }
        if (!ensureDownloadable(game)) {
            return false;
        }

        boolean alreadyInQueue = downloadQueue.stream()
                .anyMatch(queued -> Objects.equals(queued.getPkgUrl(), game.getPkgUrl()));
        if (alreadyInQueue) {
            notificationPane.showNotification(I18n.get("notify.alreadyinlist"),
                    NotificationPane.NotificationType.WARNING);
            return false;
        }

        downloadQueue.add(game);
        downloadQueueManager.saveQueue(new ArrayList<>(downloadQueue));
        notificationPane.showActionNotification(
                I18n.get("notify.addedtolist", game.getTitle()),
                NotificationPane.NotificationType.SUCCESS,
                I18n.get("button.undo"),
                () -> undoAddToQueue(game),
                6000);
        updateDetailStatus(game);
        updateAddToQueueButton(game);
        return true;
    }

    private boolean isQueued(Game game) {
        return downloadQueue != null && game != null
                && downloadQueue.stream()
                .anyMatch(queued -> Objects.equals(queued.getPkgUrl(), game.getPkgUrl()));
    }

    private void removeFromQueue(Game game) {
        if (downloadQueue == null || game == null) {
            return;
        }
        boolean removed = downloadQueue.removeIf(
                queued -> Objects.equals(queued.getPkgUrl(), game.getPkgUrl()));
        if (removed) {
            downloadQueueManager.saveQueue(new ArrayList<>(downloadQueue));
            updateDetailStatus(game);
            updateAddToQueueButton(game);
            notificationPane.showNotification(I18n.get("notify.undone"),
                    NotificationPane.NotificationType.INFO);
        }
    }

    private void undoAddToQueue(Game game) {
        removeFromQueue(game);
    }

    /**
     * Starts a single game immediately. If a session is already running, the
     * game is queued instead so the current downloads are not interrupted.
     */
    private void downloadNow(Game game) {
        if (game == null) {
            notificationPane.showNotification(I18n.get("notify.selectgame"),
                    NotificationPane.NotificationType.INFO);
            return;
        }
        if (downloadCoordinator == null) {
            notificationPane.showNotification(I18n.get("notify.servicenotready"),
                    NotificationPane.NotificationType.ERROR);
            return;
        }
        if (!ensureDownloadable(game)) {
            return;
        }

        if (downloadCoordinator.isDownloading()) {
            if (addToQueue(game)) {
                notificationPane.showNotification(I18n.get("notify.queuedwhilebusy"),
                        NotificationPane.NotificationType.INFO);
            }
            return;
        }

        downloadQueue.removeIf(queued -> Objects.equals(queued.getPkgUrl(), game.getPkgUrl()));
        downloadQueueManager.saveQueue(new ArrayList<>(downloadQueue));

        resetDownloadUi();
        globalProgress.startDownloads(1, game.getFileSize());
        if (!downloadCoordinator.start(List.of(game), Set.of())) {
            globalProgress.reset();
            notificationPane.showNotification(
                    I18n.get("notify.startfailed", downloadCoordinator.getLastStartError()),
                    NotificationPane.NotificationType.ERROR);
            return;
        }

        btnResumePause.setDisable(false);
        notificationPane.showNotification(I18n.get("notify.started", 1),
                NotificationPane.NotificationType.SUCCESS);
        updateDetailStatus(game);
    }

    /** Returns false and shows the reason when a game cannot be downloaded. */
    private boolean ensureDownloadable(Game game) {
        String pkgUrl = game.getPkgUrl();
        if (pkgUrl == null || pkgUrl.isBlank() || "MISSING".equals(pkgUrl)) {
            notificationPane.showNotification(I18n.get("notify.missing"),
                    NotificationPane.NotificationType.ERROR);
            return false;
        }
        if ("CART ONLY".equals(pkgUrl)) {
            notificationPane.showNotification(I18n.get("notify.cartonly"),
                    NotificationPane.NotificationType.INFO);
            return false;
        }
        if ("NOT REQUIRED".equals(pkgUrl)) {
            notificationPane.showNotification(I18n.get("notify.notrequired"),
                    NotificationPane.NotificationType.INFO);
            return false;
        }
        return true;
    }

    private void showGameDetail(Game game) {
        boolean selected = game != null;
        detailHint.setVisible(!selected);
        detailHint.setManaged(!selected);
        detailTitle.setText(selected ? game.getTitle() : I18n.get("detail.none.title"));
        detailMeta.setText(selected ? formatDetailMeta(game) : "");
        detailContentId.setText(selected && game.getContentId() != null
                ? game.getContentId() : I18n.get("misc.na"));
        detailStatus.setVisible(false);
        detailStatus.setManaged(false);

        boolean downloadable = selected && isDownloadable(game);
        btnDownloadNow.setDisable(!downloadable);
        btnAddToQueue.setDisable(!downloadable);
        updateAddToQueueButton(game);
        if (selected) {
            updateDetailStatus(game);
        }
    }

    private void updateAddToQueueButton(Game game) {
        if (btnAddToQueue == null) {
            return;
        }
        boolean downloadable = game != null && isDownloadable(game);
        btnAddToQueue.setDisable(!downloadable);
        btnAddToQueue.setText(I18n.get(
                downloadable && isQueued(game) ? "button.removefromqueue" : "button.addtoqueue"));
    }

    private String formatDetailMeta(Game game) {
        return consoleLabel(game.getConsole()) + " · "
                + (game.getRegion() == null ? "" : game.getRegion()) + " · "
                + formatSize(game.getFileSize());
    }

    private String consoleLabel(Console console) {
        if (console == null) {
            return "";
        }
        return switch (console) {
            case PSVITA -> I18n.get("console.psvita");
            case PSP -> I18n.get("console.psp");
            case PSX -> I18n.get("console.psx");
        };
    }

    private boolean isDownloadable(Game game) {
        String pkgUrl = game.getPkgUrl();
        return pkgUrl != null && !pkgUrl.isBlank()
                && !"MISSING".equals(pkgUrl)
                && !"CART ONLY".equals(pkgUrl)
                && !"NOT REQUIRED".equals(pkgUrl);
    }

    private void updateDetailStatus(Game game) {
        if (detailStatus == null || game == null) {
            return;
        }
        String status = historyStatus(game.getPkgUrl());
        if (status == null && downloadQueue != null
                && downloadQueue.stream().anyMatch(q -> Objects.equals(q.getPkgUrl(), game.getPkgUrl()))) {
            status = "queued";
        }
        if (status == null) {
            detailStatus.setVisible(false);
            detailStatus.setManaged(false);
            return;
        }

        String key;
        String style;
        switch (status) {
            case "completed" -> {
                key = "detail.status.completed";
                style = "badge-success";
            }
            case "failed" -> {
                key = "detail.status.failed";
                style = "badge-danger";
            }
            case "paused" -> {
                key = "detail.status.paused";
                style = "badge-warning";
            }
            case "queued" -> {
                key = "detail.status.queued";
                style = "badge-info";
            }
            default -> {
                return;
            }
        }
        detailStatus.getStyleClass().removeAll("badge-success", "badge-danger", "badge-warning", "badge-info");
        detailStatus.getStyleClass().add(style);
        detailStatus.setText(I18n.get(key));
        detailStatus.setVisible(true);
        detailStatus.setManaged(true);
    }

    private boolean confirm(String titleKey, String messageKey, Object... args) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18n.get(titleKey));
        alert.setHeaderText(null);
        alert.setContentText(I18n.get(messageKey, args));
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    @FXML
    private void handleOpenDownloadList() {
        if (queueDrawer == null) {
            return;
        }
        if (queueDrawer.isVisible()) {
            closeQueueDrawer();
        } else {
            openQueueDrawer();
        }
    }

    private void openQueueDrawer() {
        if (downloadQueue == null || queueDrawer == null) {
            return;
        }
        try {
            if (queueRoot == null) {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/DownloadListView.fxml"),
                        I18n.getResourceBundle());
                queueRoot = loader.load();
                queueController = loader.getController();
                queueController.setQueueData(downloadQueue, downloadQueueManager);
                queueController.setOnStartDownloads(this::startDownloadsFromQueue);
                queueController.setOnClose(this::closeQueueDrawer);
                VBox.setVgrow(queueRoot, javafx.scene.layout.Priority.ALWAYS);
                queueDrawer.getChildren().add(queueRoot);
            } else {
                queueController.refresh();
            }

            queueDrawer.setMaxHeight(Double.MAX_VALUE);
            queueDrawer.setManaged(true);
            queueDrawer.setVisible(true);
        } catch (IOException e) {
            log.error("Failed to open the download queue drawer", e);
            notificationPane.showNotification(I18n.get("notify.openfailed",
                    I18n.get("dialog.downloadqueue.title"), e.getMessage()),
                    NotificationPane.NotificationType.ERROR);
        }
    }

    private void closeQueueDrawer() {
        if (queueDrawer == null) {
            return;
        }
        queueDrawer.setVisible(false);
        queueDrawer.setManaged(false);
    }

    private boolean startDownloadsFromQueue() {
        if (downloadQueue == null || downloadQueue.isEmpty()) {
            notificationPane.showNotification(I18n.get("notify.queueempty"),
                    NotificationPane.NotificationType.INFO);
            return false;
        }
        if (downloadCoordinator.isDownloading()) {
            notificationPane.showNotification(I18n.get("notify.downloadsinprogress"),
                    NotificationPane.NotificationType.WARNING);
            return false;
        }

        List<Game> gamesToStart = new ArrayList<>(downloadQueue);
        long totalBytes = gamesToStart.stream().mapToLong(Game::getFileSize).sum();

        resetDownloadUi();
        globalProgress.startDownloads(gamesToStart.size(), totalBytes);

        if (!downloadCoordinator.start(gamesToStart, Set.of())) {
            globalProgress.reset();
            notificationPane.showNotification(
                    I18n.get("notify.startfailed", downloadCoordinator.getLastStartError()),
                    NotificationPane.NotificationType.ERROR);
            return false;
        }

        downloadQueue.clear();
        downloadQueueManager.clearQueue();
        btnResumePause.setDisable(false);
        notificationPane.showNotification(I18n.get("notify.started", gamesToStart.size()),
                NotificationPane.NotificationType.SUCCESS);
        return true;
    }

    @FXML
    private void handleRefresh() {
        Console selected = getSelectedConsole();
        if (selected == null) {
            notificationPane.showNotification(I18n.get("notify.selectconsole"),
                    NotificationPane.NotificationType.WARNING);
            return;
        }
        loadGames(selected);
    }

    @FXML
    private void handleOpenSettings() {
        openConfigWindow(false);
    }

    private void openConfigWindow(boolean firstRun) {
        if (configManager == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ConfigView.fxml"),
                    I18n.getResourceBundle());
            Parent root = loader.load();

            ConfigController controller = loader.getController();
            controller.setConfigManager(configManager);
            controller.setFirstRun(firstRun);
            controller.setOnSaveCallback(() -> {
                applyTheme();
                if (firstRun) {
                    start();
                } else {
                    handleConfigSaved();
                }
                notificationPane.showNotification(I18n.get("config.saved"),
                        NotificationPane.NotificationType.SUCCESS);
            });

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            if (rootStack.getScene() != null) {
                stage.initOwner(rootStack.getScene().getWindow());
            }
            stage.setTitle(I18n.get(firstRun ? "config.firstrun.title" : "dialog.settings.title"));
            Scene scene = new Scene(root);
            ThemeManager.apply(scene, configManager.isDarkModeEnabled());
            stage.setScene(scene);
            stage.showAndWait();

        } catch (IOException e) {
            log.error("Failed to open settings", e);
            notificationPane.showNotification(I18n.get("notify.openfailed",
                    I18n.get("dialog.settings.title"), e.getMessage()),
                    NotificationPane.NotificationType.ERROR);
        }
    }

    private void handleConfigSaved() {
        applyTheme();
        updateThemeButton();
        if (!started) {
            start();
            return;
        }
        databaseManager.checkAllConsolesAvailability().thenRun(() -> Platform.runLater(() -> {
            for (Console console : Console.values()) {
                updateConsoleAvailability(console, databaseManager.isConsoleAvailable(console));
            }
            Console selected = getSelectedConsole();
            if (selected != null && databaseManager.isConsoleAvailable(selected)) {
                loadGames(selected);
            }
            databaseManager.downloadAllDatabases();
        }));
    }

    private void applyTheme() {
        if (rootStack == null || rootStack.getScene() == null) {
            return;
        }
        ThemeManager.apply(rootStack.getScene(), configManager.isDarkModeEnabled());
    }

    private void setupDatabaseListener() {
        databaseManager.addListener(new DatabaseManager.DatabaseListener() {
            @Override
            public void onAvailabilityChanged(Console console, boolean available) {
                Platform.runLater(() -> updateConsoleAvailability(console, available));
            }

            @Override
            public void onDownloadComplete(Console console, boolean success) {
                Platform.runLater(() -> {
                    if (success) {
                        updateConsoleAvailability(console, true);
                        notificationPane.showNotification(
                                I18n.get("notify.database.downloaded", console.getDisplayName()),
                                NotificationPane.NotificationType.SUCCESS);
                        Console selected = getSelectedConsole();
                        if (selected == null) {
                            selectConsole(console);
                        } else if (selected == console) {
                            loadGames(console);
                        }
                    } else {
                        notificationPane.showNotification(
                                I18n.get("notify.database.failed", console.getDisplayName()),
                                NotificationPane.NotificationType.ERROR);
                    }
                });
            }

            @Override
            public void onDownloadProgress(Console console, String message) {
                log.debug("Database download progress for {}: {}", console, message);
                Platform.runLater(() ->
                        statusLabel.setText(I18n.get("status.database.downloading")));
            }
        });
    }

    private void refreshConsoleAvailability() {
        databaseManager.checkAllConsolesAvailability().thenRun(() -> Platform.runLater(() -> {
            Console firstAvailable = null;
            for (Console console : Console.values()) {
                boolean available = databaseManager.isConsoleAvailable(console);
                updateConsoleAvailability(console, available);
                if (available && firstAvailable == null) {
                    firstAvailable = console;
                }
            }

            databaseManager.downloadAllDatabases();

            Console selected = getSelectedConsole();
            if (selected != null) {
                loadGames(selected);
            } else if (firstAvailable != null) {
                selectConsole(firstAvailable);
            } else {
                statusLabel.setText(I18n.get("status.database.downloading"));
                notificationPane.showNotification(I18n.get("status.database.downloading"),
                        NotificationPane.NotificationType.INFO);
            }
        }));
    }

    private void updateConsoleAvailability(Console console, boolean available) {
        RadioButton button = getConsoleButton(console);
        if (button == null) return;

        button.setDisable(!available || loading);
        if (!available && button.isSelected()) {
            button.setSelected(false);
            masterData.clear();
        }
    }

    private RadioButton getConsoleButton(Console console) {
        return switch (console) {
            case PSP -> rbPsp;
            case PSVITA -> rbVita;
            case PSX -> rbPsx;
        };
    }

    private Console getSelectedConsole() {
        if (rbVita.isSelected()) return Console.PSVITA;
        if (rbPsp.isSelected()) return Console.PSP;
        if (rbPsx.isSelected()) return Console.PSX;
        return null;
    }

    private void selectConsole(Console console) {
        RadioButton button = getConsoleButton(console);
        if (button == null || button.isDisabled()) {
            return;
        }
        button.setSelected(true);
        loadGames(console);
    }

    private void resetDownloadUi() {
        downloadItems.clear();
        downloadOrder.clear();
        selectedDownloadUrls.clear();
        lastProgressUiMs.clear();
        lastSelectionAnchor = null;
        progressContainer.getChildren().removeIf(node -> node instanceof DownloadProgressItem);
        btnResumePause.setDisable(true);
        btnResumePause.setText(I18n.get("button.pause"));
    }

    @FXML
    private void handleResumePause() {
        Set<String> targets = targetDownloadUrls();
        if (targets.isEmpty()) {
            return;
        }
        if (downloadCoordinator.anyPaused(targets)) {
            for (String url : targets) {
                downloadCoordinator.resume(url);
            }
        } else {
            for (String url : targets) {
                downloadCoordinator.pause(url);
            }
        }
        refreshDownloadSelection();
        updatePauseButton();
    }

    @FXML
    private void handleRetryFailed() {
        int failedCount = failedHistoryCount();
        if (downloadCoordinator == null || failedCount == 0) {
            notificationPane.showNotification(I18n.get("notify.nofailed"),
                    NotificationPane.NotificationType.INFO);
            return;
        }
        if (retryingFailed) {
            return;
        }

        if (!confirm("dialog.retryfailed.title", "dialog.retryfailed.message", failedCount)) {
            return;
        }

        // Resolving the games touches the TSV databases, so keep it off the FX
        // thread. The history is updated by the coordinator on the worker.
        retryingFailed = true;
        btnRetry.setDisable(true);
        statusLabel.setText(I18n.get("status.retrying"));

        Task<List<Game>> retryTask = new Task<>() {
            @Override
            protected List<Game> call() {
                return downloadCoordinator.takeFailedGames();
            }
        };

        retryTask.setOnSucceeded(event -> {
            retryingFailed = false;
            addRetriedGames(retryTask.getValue());
            statusLabel.setText(I18n.get("status.ready"));
            refreshHistoryCache();
            tableGames.refresh();
            updateRetryButton();
        });

        retryTask.setOnFailed(event -> {
            retryingFailed = false;
            statusLabel.setText(I18n.get("status.ready"));
            refreshHistoryCache();
            updateRetryButton();
            Throwable error = retryTask.getException();
            notificationPane.showNotification(
                    I18n.get("notify.retryfailed.error",
                            error == null ? "" : error.getMessage()),
                    NotificationPane.NotificationType.ERROR);
        });

        Thread thread = new Thread(retryTask, "retry-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private void addRetriedGames(List<Game> failedGames) {
        int added = 0;
        for (Game game : failedGames) {
            boolean alreadyQueued = downloadQueue.stream()
                    .anyMatch(queued -> Objects.equals(queued.getPkgUrl(), game.getPkgUrl()));
            if (!alreadyQueued) {
                downloadQueue.add(game);
                added++;
            }
        }
        if (added > 0) {
            downloadQueueManager.saveQueue(new ArrayList<>(downloadQueue));
            notificationPane.showNotification(I18n.get("notify.failedadded", added),
                    NotificationPane.NotificationType.SUCCESS);
        } else {
            notificationPane.showNotification(I18n.get("notify.nofailedadded"),
                    NotificationPane.NotificationType.WARNING);
        }
    }

    private void updateRetryButton() {
        if (btnRetry != null) {
            btnRetry.setDisable(failedHistoryCount() == 0);
        }
    }

    private void updateQueueButton() {
        if (btnDownloadList == null) {
            return;
        }
        int count = downloadQueue == null ? 0 : downloadQueue.size();
        btnDownloadList.setText(I18n.get(
                count > 0 ? "button.queue.count" : "button.queue", count));
    }

    private void updateThemeButton() {
        if (btnTheme == null || configManager == null) {
            return;
        }
        btnTheme.setText(I18n.get(
                configManager.isDarkModeEnabled() ? "button.theme.light" : "button.theme.dark"));
    }

    @FXML
    private void handleToggleTheme() {
        if (configManager == null) {
            return;
        }
        boolean dark = !configManager.isDarkModeEnabled();
        configManager.setProperty("darkMode", Boolean.toString(dark));
        configManager.saveConfig();
        applyTheme();
        updateThemeButton();
    }

    /** Number of failed entries in the in-memory history cache (no disk IO on FX). */
    private int failedHistoryCount() {
        int count = 0;
        for (DownloadHistory entry : historyByUrl.values()) {
            if ("failed".equals(entry.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private Set<String> targetDownloadUrls() {
        return selectedDownloadUrls.isEmpty()
                ? new LinkedHashSet<>(downloadOrder)
                : new LinkedHashSet<>(selectedDownloadUrls);
    }

    private void updatePauseButton() {
        if (btnResumePause == null) {
            return;
        }
        Set<String> targets = targetDownloadUrls();
        btnResumePause.setDisable(downloadCoordinator == null || !downloadCoordinator.isDownloading());
        btnResumePause.setText(I18n.get(
                downloadCoordinator.anyPaused(targets) ? "button.resume" : "button.pause"));
    }

    private void addDownloadItem(DownloadTaskState task) {
        String url = task.getUrl();
        if (downloadItems.containsKey(url)) {
            return;
        }
        DownloadProgressItem item =
                new DownloadProgressItem(task.getGame().getTitle(), task.getTotalBytes());
        item.setUrl(url);
        item.setOnCancel(() -> requestCancel(Set.of(url), false));
        item.setOnMouseClicked(event -> handleDownloadClick(url, event));
        item.setOnContextMenuRequested(event -> {
            if (!selectedDownloadUrls.contains(url)) {
                selectOnlyDownload(url);
            }
            DownloadProgressItem source = downloadItems.get(url);
            if (source != null) {
                showDownloadContextMenu(source, event.getScreenX(), event.getScreenY());
            }
        });

        downloadItems.put(url, item);
        downloadOrder.add(url);
        progressContainer.getChildren().add(item);
        btnResumePause.setDisable(false);
        refreshDownloadSelection();
    }

    private void handleDownloadClick(String url, MouseEvent event) {
        if (event.getButton() == MouseButton.SECONDARY) {
            return;
        }
        int index = downloadOrder.indexOf(url);
        if (index < 0) {
            return;
        }

        if (event.isShiftDown() && lastSelectionAnchor != null) {
            int anchor = downloadOrder.indexOf(lastSelectionAnchor);
            if (anchor >= 0) {
                if (!event.isControlDown()) {
                    selectedDownloadUrls.clear();
                }
                int from = Math.min(anchor, index);
                int to = Math.max(anchor, index);
                for (int i = from; i <= to; i++) {
                    selectedDownloadUrls.add(downloadOrder.get(i));
                }
            }
        } else if (event.isControlDown() || event.isMetaDown()) {
            if (!selectedDownloadUrls.remove(url)) {
                selectedDownloadUrls.add(url);
            }
            lastSelectionAnchor = url;
        } else {
            selectedDownloadUrls.clear();
            selectedDownloadUrls.add(url);
            lastSelectionAnchor = url;
        }

        refreshDownloadSelection();
        updatePauseButton();
    }

    private void selectOnlyDownload(String url) {
        selectedDownloadUrls.clear();
        selectedDownloadUrls.add(url);
        lastSelectionAnchor = url;
        refreshDownloadSelection();
        updatePauseButton();
    }

    private void refreshDownloadSelection() {
        for (Map.Entry<String, DownloadProgressItem> entry : downloadItems.entrySet()) {
            entry.getValue().setSelected(selectedDownloadUrls.contains(entry.getKey()));
        }
    }

    private void showDownloadContextMenu(DownloadProgressItem source, double screenX, double screenY) {
        boolean hasSelection = !selectedDownloadUrls.isEmpty();
        Set<String> targets = targetDownloadUrls();
        ContextMenu menu = new ContextMenu();

        MenuItem pause = new MenuItem(I18n.get(hasSelection ? "menu.pauseselected" : "menu.pauseall"));
        pause.setOnAction(event -> {
            for (String url : targets) {
                downloadCoordinator.pause(url);
            }
            refreshDownloadSelection();
            updatePauseButton();
        });

        MenuItem resume = new MenuItem(I18n.get(hasSelection ? "menu.resumeselected" : "menu.resumeall"));
        resume.setOnAction(event -> {
            for (String url : targets) {
                downloadCoordinator.resume(url);
            }
            refreshDownloadSelection();
            updatePauseButton();
        });

        MenuItem cancel = new MenuItem(I18n.get(hasSelection ? "menu.cancelselected" : "menu.cancelall"));
        cancel.setOnAction(event -> requestCancel(targets, false));

        MenuItem cancelRemove = new MenuItem(I18n.get(
                hasSelection ? "menu.cancelremoveselected" : "menu.cancelremoveall"));
        cancelRemove.setOnAction(event -> requestCancel(targets, true));

        MenuItem removeFromSelection = new MenuItem(I18n.get("menu.removefromselection"));
        removeFromSelection.setOnAction(event -> {
            selectedDownloadUrls.remove(source.getUrl());
            refreshDownloadSelection();
            updatePauseButton();
        });

        MenuItem clearSelection = new MenuItem(I18n.get("menu.clearselection"));
        clearSelection.setDisable(!hasSelection);
        clearSelection.setOnAction(event -> {
            selectedDownloadUrls.clear();
            lastSelectionAnchor = null;
            refreshDownloadSelection();
            updatePauseButton();
        });

        menu.getItems().addAll(pause, resume, new SeparatorMenuItem(),
                cancel, cancelRemove, new SeparatorMenuItem(),
                removeFromSelection, clearSelection);
        menu.show(source, screenX, screenY);
    }

    private void requestCancel(Set<String> urls, boolean removeBars) {
        if (urls == null || urls.isEmpty()) {
            return;
        }
        String messageKey = removeBars
                ? "dialog.confirmcancel.messageremove"
                : "dialog.confirmcancel.message";
        if (!confirm("dialog.confirmcancel.title", messageKey, urls.size())) {
            return;
        }

        for (String url : new ArrayList<>(urls)) {
            downloadCoordinator.cancel(url, removeBars);
            if (removeBars) {
                removeDownloadItem(url);
            }
        }
        refreshDownloadSelection();
        updatePauseButton();
    }

    private void removeDownloadItem(String url) {
        DownloadProgressItem item = downloadItems.remove(url);
        if (item != null) {
            progressContainer.getChildren().remove(item);
        }
        downloadOrder.remove(url);
        selectedDownloadUrls.remove(url);
        lastProgressUiMs.remove(url);
        if (url.equals(lastSelectionAnchor)) {
            lastSelectionAnchor = null;
        }
    }

    /**
     * Offers to restore downloads that were in progress when the application
     * was closed. The coordinator resumes them and reapplies paused states.
     */
    private void restoreSavedDownloads() {
        if (restorePrompted || downloadStateManager == null) {
            return;
        }
        restorePrompted = true;

        List<DownloadState> states = downloadStateManager.loadStates();
        if (states.isEmpty()) {
            return;
        }

        String message = I18n.get("dialog.restoredownloads.message", states.size());
        if (!downloadQueue.isEmpty()) {
            message += "\n\n" + I18n.get("dialog.restoredownloads.pending", downloadQueue.size());
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18n.get("dialog.restoredownloads.title"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (alert.showAndWait().filter(ButtonType.OK::equals).isEmpty()) {
            downloadStateManager.clearStates();
            return;
        }

        List<Game> games = new ArrayList<>();
        Set<String> pausedUrls = new LinkedHashSet<>();
        for (DownloadState state : states) {
            if (state.getPkgUrl() == null || state.getConsole() == null) {
                continue;
            }
            try {
                Console console = Console.fromDisplayName(state.getConsole());
                Game game = new Game(state.getTitle(), state.getRegion(), console,
                        state.getPkgUrl(), state.getzRif(), state.getTotalBytes());
                games.add(game);
                if (state.isPaused()) {
                    pausedUrls.add(state.getPkgUrl());
                }
            } catch (IllegalArgumentException e) {
                log.warn("Ignoring saved download with unknown console: {}", state.getConsole());
            }
        }
        if (games.isEmpty()) {
            downloadStateManager.clearStates();
            return;
        }

        resetDownloadUi();
        globalProgress.startDownloads(games.size(),
                games.stream().mapToLong(Game::getFileSize).sum());
        if (!downloadCoordinator.start(games, pausedUrls)) {
            globalProgress.reset();
            notificationPane.showNotification(
                    I18n.get("notify.startfailed", downloadCoordinator.getLastStartError()),
                    NotificationPane.NotificationType.ERROR);
            return;
        }

        Set<String> restoredUrls = new LinkedHashSet<>();
        for (Game game : games) {
            restoredUrls.add(game.getPkgUrl());
        }
        downloadQueue.removeIf(game -> restoredUrls.contains(game.getPkgUrl()));
        downloadQueueManager.saveQueue(new ArrayList<>(downloadQueue));
    }

    private void installShortcuts() {
        Scene scene = rootStack.getScene();
        if (scene == null) {
            return;
        }

        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN),
                this::focusSearch);
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN),
                () -> handleGameSelection(tableGames.getSelectionModel().getSelectedItem()));
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN),
                this::handleOpenDownloadList);
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.SPACE),
                () -> {
                    if (scene.getFocusOwner() instanceof TextInputControl) {
                        return;
                    }
                    if (!btnResumePause.isDisable()) {
                        handleResumePause();
                    }
                });
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.ESCAPE),
                () -> {
                    if (queueDrawer != null && queueDrawer.isVisible()) {
                        closeQueueDrawer();
                    }
                });
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.DELETE),
                () -> {
                    if (scene.getFocusOwner() instanceof TextInputControl) {
                        return;
                    }
                    if (!selectedDownloadUrls.isEmpty()) {
                        requestCancel(new LinkedHashSet<>(selectedDownloadUrls), false);
                    }
                });
    }

    private void focusSearch() {
        txtSearch.requestFocus();
        txtSearch.selectAll();
    }

    /** Marshals coordinator events onto the FX thread. */
    private final class CoordinatorListener implements DownloadCoordinator.Listener {
        @Override
        public void onTaskStarted(DownloadTaskState task) {
            Platform.runLater(() -> addDownloadItem(task));
        }

        @Override
        public void onTaskProgress(DownloadTaskState task) {
            Platform.runLater(() -> {
                DownloadProgressItem item = downloadItems.get(task.getUrl());
                if (item == null) {
                    return;
                }
                long now = System.currentTimeMillis();
                long last = lastProgressUiMs.getOrDefault(task.getUrl(), 0L);
                if (!task.isPaused() && now - last < 120) {
                    return;
                }
                lastProgressUiMs.put(task.getUrl(), now);
                item.updateProgress(task.getDownloadedBytes(), task.getTotalBytes(),
                        task.getSpeedBytesPerSecond(), task.getEtaSeconds(),
                        task.getRetryCount(), task.isPaused());
                globalProgress.updateProgress(downloadCoordinator.getTotalDownloaded());
                updatePauseButton();
            });
        }

        @Override
        public void onTaskSegments(DownloadTaskState task, List<Segment> segments) {
            Platform.runLater(() -> {
                DownloadProgressItem item = downloadItems.get(task.getUrl());
                if (item != null) {
                    item.updateSegments(segments);
                }
            });
        }

        @Override
        public void onTaskCompleted(DownloadTaskState task, java.io.File file) {
            Platform.runLater(() -> {
                DownloadProgressItem item = downloadItems.get(task.getUrl());
                if (item != null) {
                    item.setCompleted();
                }
                globalProgress.gameCompleted();
                globalProgress.updateProgress(downloadCoordinator.getTotalDownloaded());
                refreshHistoryCache();
                tableGames.refresh();
                updateRetryButton();
                updatePauseButton();
                notificationPane.showNotification(
                        I18n.get("notify.downloadcompleted", task.getGame().getTitle()),
                        NotificationPane.NotificationType.SUCCESS);
            });
        }

        @Override
        public void onTaskFailed(DownloadTaskState task, Exception error) {
            Platform.runLater(() -> {
                DownloadProgressItem item = downloadItems.get(task.getUrl());
                if (item != null) {
                    item.setFailed(error == null ? null : error.getMessage());
                }
                refreshHistoryCache();
                tableGames.refresh();
                updateRetryButton();
                updatePauseButton();
                notificationPane.showNotification(
                        I18n.get("notify.downloadfailed", task.getGame().getTitle()),
                        NotificationPane.NotificationType.ERROR);
            });
        }

        @Override
        public void onTaskCancelled(DownloadTaskState task) {
            Platform.runLater(() -> {
                DownloadProgressItem item = downloadItems.get(task.getUrl());
                if (item != null) {
                    item.setCancelled();
                }
                selectedDownloadUrls.remove(task.getUrl());
                refreshDownloadSelection();
                updatePauseButton();
                notificationPane.showNotification(
                        I18n.get("notify.downloadcancelled", task.getGame().getTitle()),
                        NotificationPane.NotificationType.WARNING);
            });
        }

        @Override
        public void onExtractionFinished(DownloadTaskState task, boolean success) {
            Platform.runLater(() -> notificationPane.showNotification(
                    I18n.get(success ? "notify.extractioncompleted" : "notify.extractionfailed",
                            task.getGame().getTitle()),
                    success ? NotificationPane.NotificationType.SUCCESS
                            : NotificationPane.NotificationType.ERROR));
        }

        @Override
        public void onAllFinished() {
            Platform.runLater(() -> {
                globalProgress.reset();
                btnResumePause.setDisable(true);
                updatePauseButton();
                updateRetryButton();
                notificationPane.showNotification(I18n.get("notify.allcompleted"),
                        NotificationPane.NotificationType.SUCCESS);
            });
        }
    }

    /**
     * Persists the pending queue and stops background work owned by this
     * controller. Called from {@link com.squarepeace.nnppss.MainApp#stop()}.
     */
    public void shutdown() {
        if (queueController != null) {
            queueController.detach();
        }
        if (downloadQueue != null && !downloadQueue.isEmpty() && downloadQueueManager != null) {
            downloadQueueManager.saveQueue(new ArrayList<>(downloadQueue));
        }
        if (downloadCoordinator != null) {
            downloadCoordinator.shutdown();
        }
    }
}
