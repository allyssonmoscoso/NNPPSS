package com.squarepeace.nnppss;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.squarepeace.nnppss.app.AppContext;
import com.squarepeace.nnppss.app.AppPaths;
import com.squarepeace.nnppss.app.ThemeManager;
import com.squarepeace.nnppss.fx.controllers.MainController;
import com.squarepeace.nnppss.util.I18n;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MainApp extends Application {
    static {
        // Must run before the first logger call so that logback can resolve
        // ${nnppss.dataDir} in logback.xml.
        System.setProperty(AppPaths.DATA_DIR_PROPERTY, AppPaths.dataDir().toString());
        AppPaths.ensureDirectory("logs");
    }

    private static final Logger log = LoggerFactory.getLogger(MainApp.class);

    private AppContext context;
    private MainController mainController;

    @Override
    public void init() throws Exception {
        List<String> migrated = AppPaths.migrateLegacyData();
        log.info("Starting NNPPSS. Data directory: {}", AppPaths.dataDir());
        if (!migrated.isEmpty()) {
            log.info("Migrated legacy data: {}", migrated);
        }

        Thread.setDefaultUncaughtExceptionHandler((thread, error) ->
                log.error("Unhandled exception in thread {}", thread.getName(), error));

        log.info("Initializing services...");
        context = AppContext.create();
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        log.info("Starting JavaFX UI...");

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"),
                I18n.getResourceBundle());
        Parent root = loader.load();

        mainController = loader.getController();
        mainController.setContext(context);

        Scene scene = new Scene(root, 1180, 760);
        boolean darkMode = context.getConfigManager().isDarkModeEnabled();
        ThemeManager.apply(scene, darkMode);

        primaryStage.setTitle("NNPPSS");
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(720);
        primaryStage.setScene(scene);
        addIcon(primaryStage);
        primaryStage.show();
        log.info("JavaFX UI ready");

        Platform.runLater(() -> {
            if (mainController.needsConfiguration()) {
                mainController.showInitialConfiguration();
            } else {
                mainController.start();
            }
        });
    }

    @Override
    public void stop() {
        log.info("Shutting down NNPPSS...");
        if (mainController != null) {
            mainController.shutdown();
        }
        if (context != null) {
            context.shutdown();
        }
        log.info("Shutdown complete");
    }

    private void addIcon(Stage stage) {
        try (InputStream icon = getClass().getResourceAsStream("/n.png")) {
            if (icon != null) {
                stage.getIcons().add(new Image(icon));
            }
        } catch (IOException e) {
            log.warn("Could not load the application icon", e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
