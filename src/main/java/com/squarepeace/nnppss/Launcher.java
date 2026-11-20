package com.squarepeace.nnppss;

/**
 * Non-Application launcher used by jpackage. When JavaFX is on the classpath
 * (instead of the module path), the Java launcher refuses to start a main class
 * that extends {@link javafx.application.Application}; this thin wrapper avoids
 * that check.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        MainApp.main(args);
    }
}
