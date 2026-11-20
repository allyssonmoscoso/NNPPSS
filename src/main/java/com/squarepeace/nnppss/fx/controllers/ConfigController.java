package com.squarepeace.nnppss.fx.controllers;

import com.squarepeace.nnppss.service.ConfigManager;
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
import javafx.stage.Stage;

import java.net.HttpURLConnection;
import java.net.URL;

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

    private ConfigManager configManager;
    private Runnable onSaveCallback;
    private boolean firstRun;
    private int wizardStep = 1;

    public void setConfigManager(ConfigManager configManager) {
        this.configManager = configManager;
        loadValues();
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
