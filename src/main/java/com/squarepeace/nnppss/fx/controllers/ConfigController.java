package com.squarepeace.nnppss.fx.controllers;

import com.squarepeace.nnppss.app.AppPaths;
import com.squarepeace.nnppss.service.ConfigManager;
import com.squarepeace.nnppss.service.Pkg2zipResolver;
import com.squarepeace.nnppss.util.I18n;

import javafx.concurrent.Task;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ConfigController {

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private TextField txtPsvitaUrl;
    @FXML private TextField txtPspUrl;
    @FXML private TextField txtPsxUrl;
    @FXML private Button btnTestPsvita;
    @FXML private Button btnTestPsp;
    @FXML private Button btnTestPsx;
    @FXML private Label testPsvitaLabel;
    @FXML private Label testPspLabel;
    @FXML private Label testPsxLabel;
    @FXML private Spinner<Integer> spinnerSimultaneous;
    @FXML private Spinner<Integer> spinnerSpeedLimit;
    @FXML private ComboBox<String> comboLanguage;
    @FXML private CheckBox checkAutoCleanup;
    @FXML private CheckBox checkDarkMode;
    @FXML private Label validationLabel;
    @FXML private Button btnClose;
    @FXML private Button btnBack;
    @FXML private Button btnNext;
    @FXML private Button btnSave;
    @FXML private Button btnClear;
    @FXML private Label stepLabel;
    @FXML private VBox librariesSection;
    @FXML private VBox downloadsSection;
    @FXML private VBox appearanceSection;
    @FXML private VBox storageSection;
    @FXML private TextField txtDownloadsDir;
    @FXML private TextField txtExtractionDir;
    @FXML private Button btnChooseDownloads;
    @FXML private Button btnChooseExtraction;
    @FXML private Button btnOpenDownloads;
    @FXML private Button btnOpenExtraction;
    @FXML private Button btnResetStorage;
    @FXML private Button btnDetectPkg2zip;
    @FXML private Button btnChoosePkg2zip;
    @FXML private Label storageHint;
    @FXML private Label pkg2zipStatusLabel;
    @FXML private Label pkg2zipPathLabel;

    private ConfigManager configManager;
    private Runnable onSaveCallback;
    private boolean firstRun;
    private int wizardStep = 1;
    private Pkg2zipResolver pkg2zipResolver;
    private Path pendingPkg2zipPath;
    private boolean pkg2zipAutomatic = true;

    public void setConfigManager(ConfigManager configManager) {
        this.configManager = configManager;
        this.pkg2zipResolver = new Pkg2zipResolver(configManager);
        this.pendingPkg2zipPath = configManager.getPkg2zipPath();
        this.pkg2zipAutomatic = pendingPkg2zipPath == null;
        loadValues();
        loadStorageValues();
        updatePkg2zipStatus();
    }

    private void loadStorageValues() {
        if (configManager == null) {
            return;
        }
        txtDownloadsDir.setText(configManager.getDownloadsDirectory().toString());
        txtExtractionDir.setText(configManager.getExtractionDirectory().toString());
        updateStorageInfo();
    }

    private void updateStorageInfo() {
        if (storageHint == null) {
            return;
        }
        Path downloads = pathOrNull(txtDownloadsDir.getText());
        Path extraction = pathOrNull(txtExtractionDir.getText());
        String free = downloads == null ? "" : formatBytes(downloads.toFile().getUsableSpace());
        String message = I18n.get("config.storage.free", free);
        if (downloads != null && extraction != null
                && downloads.toAbsolutePath().normalize()
                .equals(extraction.toAbsolutePath().normalize())) {
            message += "  ·  " + I18n.get("config.label.samefolder");
        }
        storageHint.setText(message);
    }

    private void updatePkg2zipStatus() {
        if (pkg2zipStatusLabel == null || pkg2zipResolver == null) {
            return;
        }
        Pkg2zipResolver.Resolution resolution = resolvePkg2zip();
        String key;
        String style;
        switch (resolution.source()) {
            case CONFIGURED -> {
                key = "config.pkg2zip.configured";
                style = "pkg2zip-ok";
            }
            case BUNDLED -> {
                key = "config.pkg2zip.bundled";
                style = "pkg2zip-ok";
            }
            case PATH -> {
                key = "config.pkg2zip.path";
                style = "pkg2zip-ok";
            }
            case INVALID -> {
                key = "config.pkg2zip.invalid";
                style = "pkg2zip-error";
            }
            case NOT_FOUND -> {
                key = "config.pkg2zip.notfound";
                style = "pkg2zip-warn";
            }
            default -> {
                key = "config.pkg2zip.notfound";
                style = "pkg2zip-warn";
            }
        }
        pkg2zipStatusLabel.getStyleClass().setAll("pkg2zip-status", style);
        pkg2zipStatusLabel.setText(I18n.get(key));
        String path = resolution.executable() == null ? "" : resolution.executable().toString();
        pkg2zipPathLabel.setText(path);
        pkg2zipPathLabel.setTooltip(path.isBlank() ? null : new Tooltip(path));
    }

    private Pkg2zipResolver.Resolution resolvePkg2zip() {
        return pkg2zipResolver.resolve(pkg2zipAutomatic ? null : pendingPkg2zipPath);
    }

    private boolean hasUsablePkg2zip() {
        Pkg2zipResolver.Source source = resolvePkg2zip().source();
        return source != Pkg2zipResolver.Source.NOT_FOUND
                && source != Pkg2zipResolver.Source.INVALID;
    }

    @FXML
    private void handleDetectPkg2zip() {
        pendingPkg2zipPath = null;
        pkg2zipAutomatic = true;
        updatePkg2zipStatus();
    }

    @FXML
    private void handleChoosePkg2zip() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(I18n.get("config.pkg2zip.choosetitle"));
        Path current = pendingPkg2zipPath == null ? null : pendingPkg2zipPath.getParent();
        if (current != null && Files.isDirectory(current)) {
            chooser.setInitialDirectory(current.toFile());
        }
        File selected = chooser.showDialog(ownerWindow());
        if (selected == null) {
            return;
        }
        Path executable = pkg2zipResolver.findInDirectory(selected.toPath());
        if (executable == null) {
            showError(I18n.get("config.pkg2zip.notfoundindir", selected));
            return;
        }
        pendingPkg2zipPath = executable;
        pkg2zipAutomatic = false;
        hideValidation();
        updatePkg2zipStatus();
    }

    @FXML
    private void handleChooseDownloads() {
        chooseDirectory(txtDownloadsDir, "config.label.downloadsdir");
    }

    @FXML
    private void handleChooseExtraction() {
        chooseDirectory(txtExtractionDir, "config.label.extractiondir");
    }

    private void chooseDirectory(TextField field, String titleKey) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(I18n.get(titleKey));
        Path current = pathOrNull(field.getText());
        if (current != null && Files.isDirectory(current)) {
            chooser.setInitialDirectory(current.toFile());
        }
        File selected = chooser.showDialog(ownerWindow());
        if (selected != null) {
            field.setText(selected.getAbsolutePath());
            updateStorageInfo();
        }
    }

    @FXML
    private void handleOpenDownloads() {
        openDirectory(txtDownloadsDir);
    }

    @FXML
    private void handleOpenExtraction() {
        openDirectory(txtExtractionDir);
    }

    private void openDirectory(TextField field) {
        Path path = pathOrNull(field.getText());
        if (path == null || !Files.isDirectory(path)) {
            showError(I18n.get("config.error.pathnotdirectory"));
            return;
        }
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            List<String> command;
            if (os.contains("win")) {
                command = List.of("explorer.exe", path.toString());
            } else if (os.contains("mac")) {
                command = List.of("open", path.toString());
            } else {
                command = List.of("xdg-open", path.toString());
            }
            new ProcessBuilder(command).start();
        } catch (IOException e) {
            showError(I18n.get("config.error.openfolder", e.getMessage()));
        }
    }

    @FXML
    private void handleResetStorage() {
        txtDownloadsDir.setText(AppPaths.dataDir().resolve("packages").toString());
        txtExtractionDir.setText(AppPaths.dataDir().resolve("extracted").toString());
        updateStorageInfo();
        hideValidation();
    }

    private Path pathOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Paths.get(value.trim());
        } catch (InvalidPathException e) {
            return null;
        }
    }

    private Path validateDirectory(TextField field) {
        Path path = pathOrNull(field.getText());
        if (path == null) {
            showError(I18n.get("config.error.pathempty"));
            return null;
        }
        path = path.toAbsolutePath().normalize();
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            showError(I18n.get("config.error.pathcreate", path, e.getMessage()));
            return null;
        }
        if (!Files.isDirectory(path) || !Files.isWritable(path)) {
            showError(I18n.get("config.error.pathnotwritable", path));
            return null;
        }
        return path;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        String[] units = {"KB", "MB", "GB", "TB"};
        double value = bytes;
        int unit = -1;
        do {
            value /= 1024.0;
            unit++;
        } while (value >= 1024 && unit < units.length - 1);
        return String.format("%.1f %s", value, units[unit]);
    }

    private Stage ownerWindow() {
        if (txtPsvitaUrl != null && txtPsvitaUrl.getScene() != null
                && txtPsvitaUrl.getScene().getWindow() instanceof Stage stage) {
            return stage;
        }
        return null;
    }

    public void setFirstRun(boolean firstRun) {
        this.firstRun = firstRun;
        this.wizardStep = 1;
        if (titleLabel != null) {
            titleLabel.setText(I18n.get(firstRun ? "config.firstrun.title" : "config.title"));
            subtitleLabel.setText(I18n.get(
                    firstRun ? "config.firstrun.subtitle" : "config.subtitle"));
        }
        updateWizard();
    }

    private void updateWizard() {
        if (librariesSection == null) {
            return;
        }

        boolean wizard = firstRun;
        stepLabel.setVisible(wizard);
        stepLabel.setManaged(wizard);
        stepLabel.setText(wizard ? I18n.get("config.step", wizardStep) : "");

        if (!wizard) {
            setSectionVisible(librariesSection, true);
            setSectionVisible(storageSection, true);
            setSectionVisible(downloadsSection, true);
            setSectionVisible(appearanceSection, true);
            setButtonVisible(btnBack, false);
            setButtonVisible(btnNext, false);
            setButtonVisible(btnSave, true);
            setButtonVisible(btnClear, true);
            btnClose.setText(I18n.get("button.close"));
            btnSave.setDefaultButton(true);
            btnNext.setDefaultButton(false);
            return;
        }

        boolean librariesStep = wizardStep == 1;
        setSectionVisible(librariesSection, librariesStep);
        setSectionVisible(storageSection, true);
        setSectionVisible(downloadsSection, !librariesStep);
        setSectionVisible(appearanceSection, !librariesStep);
        setButtonVisible(btnBack, !librariesStep);
        setButtonVisible(btnNext, librariesStep);
        setButtonVisible(btnSave, !librariesStep);
        setButtonVisible(btnClear, librariesStep);
        btnClose.setText(I18n.get("button.configurelater"));
        btnNext.setDefaultButton(librariesStep);
        btnSave.setDefaultButton(!librariesStep);
        hideValidation();
    }

    private void setSectionVisible(VBox section, boolean visible) {
        if (section != null) {
            section.setVisible(visible);
            section.setManaged(visible);
        }
    }

    private void setButtonVisible(Button button, boolean visible) {
        if (button != null) {
            button.setVisible(visible);
            button.setManaged(visible);
        }
    }

    @FXML
    private void handleNext() {
        hideValidation();
        if (isBlank(txtPsvitaUrl.getText())
                && isBlank(txtPspUrl.getText())
                && isBlank(txtPsxUrl.getText())) {
            showError(I18n.get("config.error.nourl"));
            return;
        }
        if (!hasUsablePkg2zip()) {
            showError(I18n.get("config.error.pkg2ziprequired"));
            return;
        }
        wizardStep = 2;
        updateWizard();
    }

    @FXML
    private void handleBack() {
        wizardStep = 1;
        hideValidation();
        updateWizard();
    }

    public void setOnSaveCallback(Runnable onSaveCallback) {
        this.onSaveCallback = onSaveCallback;
    }

    @FXML
    public void initialize() {
        spinnerSimultaneous.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 4, 1));
        spinnerSpeedLimit.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100000, 0, 50));

        comboLanguage.getItems().addAll("English", "Español");

        spinnerSpeedLimit.setTooltip(new Tooltip(I18n.get("config.tooltip.speedlimit")));
        checkAutoCleanup.setTooltip(new Tooltip(I18n.get("config.tooltip.autocleanup")));
        checkDarkMode.setTooltip(new Tooltip(I18n.get("config.tooltip.darkmode")));

        Platform.runLater(txtPsvitaUrl::requestFocus);
    }

    private void loadValues() {
        if (configManager == null) {
            return;
        }

        String psvitaUrl = configManager.getPsVitaUrl();
        String pspUrl = configManager.getPspUrl();
        String psxUrl = configManager.getPsxUrl();
        String simultaneousDownloads = configManager.getProperty("simultaneousDownloads");
        int downloadSpeedLimit = configManager.getDownloadSpeedLimit();

        if (psvitaUrl != null) txtPsvitaUrl.setText(psvitaUrl);
        if (pspUrl != null) txtPspUrl.setText(pspUrl);
        if (psxUrl != null) txtPsxUrl.setText(psxUrl);

        if (simultaneousDownloads != null && simultaneousDownloads.matches("[0-9]+")) {
            int value = Integer.parseInt(simultaneousDownloads);
            spinnerSimultaneous.getValueFactory().setValue(Math.max(1, Math.min(4, value)));
        }

        spinnerSpeedLimit.getValueFactory().setValue(downloadSpeedLimit);

        checkAutoCleanup.setSelected(configManager.isAutoCleanupEnabled());
        checkDarkMode.setSelected(configManager.isDarkModeEnabled());

        if ("es".equals(configManager.getLanguage())) {
            comboLanguage.getSelectionModel().select("Español");
        } else {
            comboLanguage.getSelectionModel().select("English");
        }
    }

    @FXML
    private void handleTestPsvita() {
        testUrl(txtPsvitaUrl, btnTestPsvita, testPsvitaLabel);
    }

    @FXML
    private void handleTestPsp() {
        testUrl(txtPspUrl, btnTestPsp, testPspLabel);
    }

    @FXML
    private void handleTestPsx() {
        testUrl(txtPsxUrl, btnTestPsx, testPsxLabel);
    }

    /**
     * Checks a URL on a daemon thread; the dialog stays responsive.
     */
    private void testUrl(TextField field, Button button, Label status) {
        String urlText = field.getText() == null ? "" : field.getText().trim();
        if (urlText.isEmpty()) {
            setTestStatus(status, "config.test.empty", "test-fail");
            return;
        }

        button.setDisable(true);
        setTestStatus(status, "config.test.testing", "test-pending");

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                HttpURLConnection connection =
                        (HttpURLConnection) new URL(urlText).openConnection();
                connection.setRequestMethod("HEAD");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setInstanceFollowRedirects(true);
                int code = connection.getResponseCode();
                connection.disconnect();
                return code >= 200 && code < 400;
            }
        };

        task.setOnSucceeded(event -> {
            button.setDisable(false);
            boolean ok = Boolean.TRUE.equals(task.getValue());
            setTestStatus(status, ok ? "config.test.ok" : "config.test.fail",
                    ok ? "test-ok" : "test-fail");
        });
        task.setOnFailed(event -> {
            button.setDisable(false);
            setTestStatus(status, "config.test.fail", "test-fail");
        });

        Thread thread = new Thread(task, "url-test");
        thread.setDaemon(true);
        thread.start();
    }

    private void setTestStatus(Label label, String key, String styleClass) {
        if (label == null) {
            return;
        }
        label.getStyleClass().setAll("test-status", styleClass);
        label.setText(I18n.get(key));
    }

    @FXML
    private void handleSave() {
        if (configManager == null) {
            return;
        }
        hideValidation();

        int simultaneous = spinnerSimultaneous.getValue();
        if (simultaneous < 1 || simultaneous > 4) {
            showError(I18n.get("config.error.simultaneous"));
            return;
        }
        int speedLimit = spinnerSpeedLimit.getValue();
        if (speedLimit < 0) {
            showError(I18n.get("config.error.speedlimit"));
            return;
        }
        if (firstRun
                && isBlank(txtPsvitaUrl.getText())
                && isBlank(txtPspUrl.getText())
                && isBlank(txtPsxUrl.getText())) {
            showError(I18n.get("config.error.nourl"));
            return;
        }
        if (firstRun && !hasUsablePkg2zip()) {
            showError(I18n.get("config.error.pkg2ziprequired"));
            return;
        }

        Path downloads = validateDirectory(txtDownloadsDir);
        if (downloads == null) {
            return;
        }
        Path extraction = validateDirectory(txtExtractionDir);
        if (extraction == null) {
            return;
        }

        configManager.setDownloadsDirectory(downloads);
        configManager.setExtractionDirectory(extraction);
        if (pkg2zipAutomatic) {
            configManager.setPkg2zipPath(null);
        } else {
            configManager.setPkg2zipPath(pendingPkg2zipPath);
        }

        configManager.setProperty("psvita.url", txtPsvitaUrl.getText().trim());
        configManager.setProperty("psp.url", txtPspUrl.getText().trim());
        configManager.setProperty("psx.url", txtPsxUrl.getText().trim());
        configManager.setProperty("simultaneousDownloads", String.valueOf(simultaneous));
        configManager.setProperty("downloadSpeedLimit", String.valueOf(speedLimit));
        configManager.setProperty("autoCleanupPkg", String.valueOf(checkAutoCleanup.isSelected()));
        configManager.setProperty("darkMode", String.valueOf(checkDarkMode.isSelected()));

        String selectedLang = comboLanguage.getValue();
        String newLanguage = "Español".equals(selectedLang) ? "es" : "en";
        configManager.setProperty("language", newLanguage);

        configManager.saveConfig();

        if (onSaveCallback != null) {
            onSaveCallback.run();
        }

        closeWindow();
    }

    private void showError(String message) {
        validationLabel.setText(message);
        validationLabel.setManaged(true);
        validationLabel.setVisible(true);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void hideValidation() {
        validationLabel.setVisible(false);
        validationLabel.setManaged(false);
        validationLabel.setText("");
    }

    @FXML
    private void handleClear() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(I18n.get("dialog.confirmclearfields.title"));
        confirm.setHeaderText(null);
        confirm.setContentText(I18n.get("dialog.confirmclearfields.message"));
        if (confirm.showAndWait().filter(ButtonType.OK::equals).isEmpty()) {
            return;
        }
        txtPsvitaUrl.clear();
        txtPspUrl.clear();
        txtPsxUrl.clear();
        clearTestStatus(testPsvitaLabel);
        clearTestStatus(testPspLabel);
        clearTestStatus(testPsxLabel);
        hideValidation();
    }

    private void clearTestStatus(Label label) {
        if (label != null) {
            label.setText("");
            label.getStyleClass().setAll("test-status");
        }
    }

    @FXML
    private void handleClose() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) txtPsvitaUrl.getScene().getWindow();
        stage.close();
    }
}
