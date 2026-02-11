package com.squarepeace.nnppss;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.squarepeace.nnppss.service.ConfigManager;
import com.squarepeace.nnppss.service.DatabaseManager;
import com.squarepeace.nnppss.service.DownloadService;
import com.squarepeace.nnppss.service.DownloadStateManager;
import com.squarepeace.nnppss.service.DownloadQueueManager;
import com.squarepeace.nnppss.service.GameRepository;
import com.squarepeace.nnppss.service.PackageService;
import com.squarepeace.nnppss.fx.controllers.MainController;

import java.io.IOException;

public class MainApp extends Application {
    private static final Logger log = LoggerFactory.getLogger(MainApp.class);

    private ConfigManager configManager;
    private GameRepository gameRepository;
    private DownloadService downloadService;
    private PackageService packageService;
    private DownloadStateManager downloadStateManager;
    private DownloadQueueManager downloadQueueManager;
    private DatabaseManager databaseManager;

    @Override
    public void init() throws Exception {
        log.info("Initializing services...");
        configManager = new ConfigManager();
        gameRepository = new GameRepository();
        downloadService = new DownloadService(configManager);
        packageService = new PackageService(configManager);
        downloadStateManager = new DownloadStateManager();
        downloadQueueManager = new DownloadQueueManager();
        databaseManager = new DatabaseManager(configManager, downloadService);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        log.info("Starting JavaFX UI...");
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
        Parent root = loader.load();
        
        MainController controller = loader.getController();
        controller.setServices(configManager, gameRepository, downloadService, 
                               packageService, downloadStateManager, databaseManager,
                               downloadQueueManager);
        
        Scene scene = new Scene(root);
        // scene.getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());
        
        primaryStage.setTitle("NNPPSS");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
