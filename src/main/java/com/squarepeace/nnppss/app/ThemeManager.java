package com.squarepeace.nnppss.app;

import javafx.scene.Scene;

/**
 * Installs the stylesheets and toggles the dark theme without recreating the
 * scene.
 */
public final class ThemeManager {
    public static final String BASE_STYLESHEET = "/styles/base.css";
    public static final String DARK_STYLESHEET = "/styles/dark.css";
    private static final String DARK_STYLE_CLASS = "dark";

    private ThemeManager() {
    }

    /** Adds the base and dark stylesheets (idempotent). */
    public static void install(Scene scene) {
        addStylesheet(scene, BASE_STYLESHEET);
        addStylesheet(scene, DARK_STYLESHEET);
    }

    /** Applies or removes the dark theme. */
    public static void apply(Scene scene, boolean dark) {
        if (scene == null || scene.getRoot() == null) {
            return;
        }
        install(scene);
        var classes = scene.getRoot().getStyleClass();
        if (dark) {
            if (!classes.contains(DARK_STYLE_CLASS)) {
                classes.add(DARK_STYLE_CLASS);
            }
        } else {
            classes.remove(DARK_STYLE_CLASS);
        }
    }

    private static void addStylesheet(Scene scene, String resource) {
        var url = ThemeManager.class.getResource(resource);
        if (url == null) {
            return;
        }
        String external = url.toExternalForm();
        if (!scene.getStylesheets().contains(external)) {
            scene.getStylesheets().add(external);
        }
    }
}
