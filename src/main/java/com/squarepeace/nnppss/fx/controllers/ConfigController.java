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
        spinnerSimultaneous.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1));
        spinnerSpeedLimit.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100000, 0, 50));
        
        // Initialize combo box
        comboLanguage.getItems().addAll("English", "Español");
    }

    private void loadValues() {
        if (configManager == null) return;

        String psvitaUrl = configManager.getPsVitaUrl();
        String pspUrl = configManager.getPspUrl();
        String psxUrl = configManager.getPsxUrl();
        String simultaneousDownloads = configManager.getProperty("simultaneousDownloads");
        int downloadSpeedLimit = configManager.getDownloadSpeedLimit();
        String autoCleanup = configManager.getProperty("autoCleanupPkg");
        String darkMode = configManager.getProperty("darkMode");
        String language = configManager.getProperty("language");

        if (psvitaUrl != null) txtPsvitaUrl.setText(psvitaUrl);
        if (pspUrl != null) txtPspUrl.setText(pspUrl);
        if (psxUrl != null) txtPsxUrl.setText(psxUrl);

        if (simultaneousDownloads != null && simultaneousDownloads.matches("[0-9]+")) {
            spinnerSimultaneous.getValueFactory().setValue(Integer.parseInt(simultaneousDownloads));
        }

        spinnerSpeedLimit.getValueFactory().setValue(downloadSpeedLimit);

        checkAutoCleanup.setSelected(autoCleanup != null && Boolean.parseBoolean(autoCleanup));
        checkDarkMode.setSelected(darkMode != null && Boolean.parseBoolean(darkMode));

        if (language != null && language.equals("es")) {
            comboLanguage.getSelectionModel().select("Español");
        } else {
            comboLanguage.getSelectionModel().select("English");
        }
    }

    @FXML
    private void handleSave() {
        if (configManager == null) return;

        configManager.setProperty("psvita.url", txtPsvitaUrl.getText());
        configManager.setProperty("psp.url", txtPspUrl.getText());
        configManager.setProperty("psx.url", txtPsxUrl.getText());
        configManager.setProperty("simultaneousDownloads", String.valueOf(spinnerSimultaneous.getValue()));
        configManager.setProperty("downloadSpeedLimit", String.valueOf(spinnerSpeedLimit.getValue()));
        configManager.setProperty("autoCleanupPkg", String.valueOf(checkAutoCleanup.isSelected()));
        configManager.setProperty("darkMode", String.valueOf(checkDarkMode.isSelected()));

        String selectedLang = comboLanguage.getValue();
        configManager.setProperty("language", "Español".equals(selectedLang) ? "es" : "en");

        configManager.saveConfig();
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Configuration Saved");
        alert.setHeaderText(null);
        alert.setContentText("Configuration has been saved successfully.");
        alert.showAndWait();

        if (onSaveCallback != null) {
            onSaveCallback.run();
        }
        
        closeWindow();
    }

    @FXML
    private void handleClear() {
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
