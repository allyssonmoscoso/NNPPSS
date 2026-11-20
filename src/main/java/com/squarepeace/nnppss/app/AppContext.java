package com.squarepeace.nnppss.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.squarepeace.nnppss.service.ConfigManager;
import com.squarepeace.nnppss.service.DatabaseManager;
import com.squarepeace.nnppss.service.DownloadHistoryManager;
import com.squarepeace.nnppss.service.DownloadQueueManager;
import com.squarepeace.nnppss.service.DownloadService;
import com.squarepeace.nnppss.service.DownloadStateManager;
import com.squarepeace.nnppss.service.GameRepository;
import com.squarepeace.nnppss.service.PackageService;
import com.squarepeace.nnppss.service.SystemValidator;
import com.squarepeace.nnppss.util.I18n;

/**
 * Application-wide service container. It is created once in {@code MainApp}
 * and handed to the controllers, so that the UI does not need to know how the
 * services are constructed.
 */
public final class AppContext {
    private static final Logger log = LoggerFactory.getLogger(AppContext.class);

    private final ConfigManager configManager;
    private final GameRepository gameRepository;
    private final DownloadService downloadService;
    private final PackageService packageService;
    private final DownloadStateManager downloadStateManager;
    private final DownloadQueueManager downloadQueueManager;
    private final DownloadHistoryManager downloadHistoryManager;
    private final DatabaseManager databaseManager;
    private final SystemValidator systemValidator;

    private AppContext(ConfigManager configManager,
                       GameRepository gameRepository,
                       DownloadService downloadService,
                       PackageService packageService,
                       DownloadStateManager downloadStateManager,
                       DownloadQueueManager downloadQueueManager,
                       DownloadHistoryManager downloadHistoryManager,
                       DatabaseManager databaseManager,
                       SystemValidator systemValidator) {
        this.configManager = configManager;
        this.gameRepository = gameRepository;
        this.downloadService = downloadService;
        this.packageService = packageService;
        this.downloadStateManager = downloadStateManager;
        this.downloadQueueManager = downloadQueueManager;
        this.downloadHistoryManager = downloadHistoryManager;
        this.databaseManager = databaseManager;
        this.systemValidator = systemValidator;
    }

    /**
     * Builds every service and applies the configured locale before any FXML
     * bundle is loaded.
     */
    public static AppContext create() {
        ConfigManager configManager = new ConfigManager();
        I18n.setLocale(configManager.getLanguage());

        GameRepository gameRepository = new GameRepository();
        DownloadService downloadService = new DownloadService(configManager);
        PackageService packageService = new PackageService(configManager);
        DownloadStateManager downloadStateManager = new DownloadStateManager();
        DownloadQueueManager downloadQueueManager = new DownloadQueueManager();
        DownloadHistoryManager downloadHistoryManager = new DownloadHistoryManager();
        DatabaseManager databaseManager = new DatabaseManager(configManager, downloadService);
        SystemValidator systemValidator = new SystemValidator(configManager);

        log.info("Services initialized (locale={})", I18n.getCurrentLanguageCode());
        return new AppContext(configManager, gameRepository, downloadService, packageService,
                downloadStateManager, downloadQueueManager, downloadHistoryManager,
                databaseManager, systemValidator);
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public GameRepository getGameRepository() {
        return gameRepository;
    }

    public DownloadService getDownloadService() {
        return downloadService;
    }

    public PackageService getPackageService() {
        return packageService;
    }

    public DownloadStateManager getDownloadStateManager() {
        return downloadStateManager;
    }

    public DownloadQueueManager getDownloadQueueManager() {
        return downloadQueueManager;
    }

    public DownloadHistoryManager getDownloadHistoryManager() {
        return downloadHistoryManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public SystemValidator getSystemValidator() {
        return systemValidator;
    }

    /** Releases background resources owned by the services. */
    public void shutdown() {
        downloadService.shutdown();
        log.info("Application services shut down");
    }
}
