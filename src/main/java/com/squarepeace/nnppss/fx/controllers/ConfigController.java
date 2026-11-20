package com.squarepeace.nnppss.fx.controllers;

import com.squarepeace.nnppss.service.ConfigManager;
import com.squarepeace.nnppss.util.I18n;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class ConfigController {

    @FXML private TextField txtPsvitaUrl;
    @FXML private TextField txtPspUrl;
    @FXML private TextField txtPsxUrl;
    @FXML private Spinner<Integer> spinnerSimultaneous;
    @FXML private Spinner<Integer> spinnerSpeedLimit;
    @FXML private ComboBox<String> comboLanguage;
    @FXML private CheckBox checkAutoCleanup;
    @FXML private CheckBox checkDarkMode;

    private ConfigManager configManager;
    private Runnable onSaveCallback;

    public void setConfigManager(ConfigManager configManager) {
        this.configManager = configManager;
        loadValues();
    }
    
    public void setOnSaveCallback(Runnable onSaveCallback) {
        this.onSaveCallback = onSaveCallback;
    }

    @FXML
    public void initialize() {
        // Initialize spinners
        spinnerSimultaneous.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 4, 1));
        spinnerSpeedLimit.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100000, 0, 50));

        // Initialize combo box
        comboLanguage.getItems().addAll("English", "Español");

        spinnerSpeedLimit.setTooltip(new Tooltip(I18n.get("config.tooltip.speedlimit")));
        checkAutoCleanup.setTooltip(new Tooltip(I18n.get("config.tooltip.autocleanup")));
        checkDarkMode.setTooltip(new Tooltip(I18n.get("config.tooltip.darkmode")));
    }

    private void loadValues() {
        if (configManager == null) return;

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
    private void handleSave() {
        if (configManager == null) return;

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

        String previousLanguage = configManager.getLanguage();

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

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18n.get("config.title"));
        alert.setHeaderText(null);
        alert.setContentText(I18n.get("config.saved"));
        alert.showAndWait();

        if (!previousLanguage.equals(newLanguage)) {
            Alert languageInfo = new Alert(Alert.AlertType.INFORMATION);
            languageInfo.setTitle(I18n.get("config.language.title"));
            languageInfo.setHeaderText(null);
            languageInfo.setContentText(I18n.get("config.language.message"));
            languageInfo.showAndWait();
        }

        if (onSaveCallback != null) {
            onSaveCallback.run();
        }

        closeWindow();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(I18n.get("config.error.title"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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
