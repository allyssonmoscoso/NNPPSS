package com.squarepeace.nnppss.app;

/**
 * Build/version information embedded in the packaged JAR manifest.
 */
public final class BuildInfo {

    private BuildInfo() {
    }

    /**
     * Version from the JAR manifest, or an explicit override, or {@code dev}
     * when running from classes during development.
     */
    public static String currentVersion() {
        String version = BuildInfo.class.getPackage() == null
                ? null
                : BuildInfo.class.getPackage().getImplementationVersion();
        if (version == null || version.isBlank()) {
            version = System.getProperty("nnppss.version");
        }
        return version == null || version.isBlank() ? "dev" : version.trim();
    }

    public static String displayVersion() {
        String version = currentVersion();
        return "v" + version;
    }
}
