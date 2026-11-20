package com.squarepeace.nnppss.fx;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.squarepeace.nnppss.app.AppContext;
import com.squarepeace.nnppss.app.AppPaths;
import com.squarepeace.nnppss.app.ThemeManager;
import com.squarepeace.nnppss.model.Console;
import com.squarepeace.nnppss.model.Game;
import com.squarepeace.nnppss.service.DownloadQueueManager;
import com.squarepeace.nnppss.fx.controllers.ConfigController;
import com.squarepeace.nnppss.fx.controllers.DownloadListController;
import com.squarepeace.nnppss.fx.controllers.MainController;
import com.squarepeace.nnppss.util.I18n;

import javafx.collections.FXCollections;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Loads every FXML view on the JavaFX thread. This catches broken fx:id
 * references, missing resource-bundle keys and controller wiring errors
 * without opening a window.
 */
class FxmlSmokeTest {

    @TempDir
    static Path dataDir;

    @BeforeAll
    static void startToolkit() throws InterruptedException {
        System.setProperty(AppPaths.DATA_DIR_PROPERTY, dataDir.toString());
        I18n.setLocale("en");

        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
        } catch (IllegalStateException alreadyRunning) {
            started.countDown();
        }
        assertTrue(started.await(30, TimeUnit.SECONDS), "JavaFX toolkit did not start");
        Platform.setImplicitExit(false);
    }

    @AfterAll
    static void stopToolkit() {
        Platform.exit();
        System.clearProperty(AppPaths.DATA_DIR_PROPERTY);
    }

    @Test
    void loadsEveryView() throws Exception {
        assertView("/fxml/MainView.fxml", MainController.class);
        assertView("/fxml/ConfigView.fxml", ConfigController.class);
        assertView("/fxml/DownloadListView.fxml", DownloadListController.class);
        assertNotNull(getClass().getResource("/styles/base.css"), "base.css is missing");
        assertNotNull(getClass().getResource("/styles/dark.css"), "dark.css is missing");
    }

    @Test
    void downloadListViewAcceptsQueueData() throws Exception {
        LoadedView view = load("/fxml/DownloadListView.fxml");
        DownloadListController controller = (DownloadListController) view.controller();

        var queue = FXCollections.observableArrayList(new Game("Alpha", "USA",
                Console.PSVITA, "https://example.test/alpha.pkg", "zrif", 1024L));
        controller.setQueueData(queue, new DownloadQueueManager());

        assertEquals(1, queue.size());
        assertNotNull(view.root());
    }

    @Test
    void opensDownloadListDrawerWithCurrentControllerSignatures() throws Exception {
        AppContext context = AppContext.create();
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        AtomicReference<Throwable> error = new AtomicReference<>();
        AtomicReference<Stage> ownerRef = new AtomicReference<>();
        AtomicBoolean drawerVisible = new AtomicBoolean(false);
        try {
            LoadedView view = load("/fxml/MainView.fxml");
            MainController controller = (MainController) view.controller();
            controller.setContext(context);

            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> error.set(throwable));
            CountDownLatch closed = new CountDownLatch(1);
            Platform.runLater(() -> {
                Stage owner = new Stage();
                ownerRef.set(owner);
                try {
                    owner.setScene(new Scene(view.root()));
                    owner.show();

                    Button downloadList = (Button) view.root().lookup("#btnDownloadList");
                    assertNotNull(downloadList, "download list button is missing");
                    downloadList.fire();

                    var drawer = view.root().lookup("#queueDrawer");
                    drawerVisible.set(drawer != null && drawer.isVisible());
                } finally {
                    closed.countDown();
                    owner.close();
                }
            });

            assertTrue(closed.await(20, TimeUnit.SECONDS), "download list drawer did not open");
            if (error.get() != null) {
                error.get().printStackTrace();
            }
            assertNull(error.get(), () -> "Exception opening the download list: " + error.get());
            assertTrue(drawerVisible.get(), "queue drawer was not visible");
            controller.shutdown();
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previous);
            Platform.runLater(() -> {
                Stage owner = ownerRef.get();
                if (owner != null) {
                    owner.close();
                }
            });
            context.shutdown();
        }
    }

    @Test
    void controllerDetectsMissingConfigurationAndThemeCanBeToggled() throws Exception {        AppContext context = AppContext.create();
        try {
            LoadedView view = load("/fxml/MainView.fxml");
            MainController controller = (MainController) view.controller();
            controller.setContext(context);

            assertTrue(controller.needsConfiguration(),
                    "fresh configuration must require the initial wizard");

            AtomicBoolean themeOk = new AtomicBoolean(false);
            CountDownLatch applied = new CountDownLatch(1);
            Platform.runLater(() -> {
                try {
                    Scene scene = new Scene(view.root());
                    ThemeManager.apply(scene, true);
                    boolean darkApplied = view.root().getStyleClass().contains("dark");
                    ThemeManager.apply(scene, false);
                    boolean darkRemoved = !view.root().getStyleClass().contains("dark");
                    themeOk.set(darkApplied && darkRemoved);
                } finally {
                    applied.countDown();
                }
            });
            assertTrue(applied.await(30, TimeUnit.SECONDS), "Timed out applying the theme");
            assertTrue(themeOk.get(), "dark style class was not toggled");

            controller.shutdown();
        } finally {
            context.shutdown();
        }
    }

    private void assertView(String resource, Class<?> expectedController) throws Exception {
        LoadedView view = load(resource);

        assertNotNull(view.root(), "Root node is null for " + resource);
        assertTrue(expectedController.isInstance(view.controller()),
                "Unexpected controller for " + resource + ": " + view.controller());
    }

    private LoadedView load(String resource) throws Exception {
        AtomicReference<Parent> root = new AtomicReference<>();
        AtomicReference<Object> controller = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch loaded = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(FxmlSmokeTest.class.getResource(resource),
                        I18n.getResourceBundle());
                root.set(loader.load());
                controller.set(loader.getController());
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                loaded.countDown();
            }
        });

        assertTrue(loaded.await(30, TimeUnit.SECONDS), "Timed out loading " + resource);
        if (failure.get() != null) {
            fail("Could not load " + resource, failure.get());
        }
        assertNotNull(root.get(), "Root node is null for " + resource);
        assertFalse(controller.get() == null, "Controller is null for " + resource);
        return new LoadedView(root.get(), controller.get());
    }

    private record LoadedView(Parent root, Object controller) {
    }
}
