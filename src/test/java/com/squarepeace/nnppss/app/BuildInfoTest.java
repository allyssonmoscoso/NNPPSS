package com.squarepeace.nnppss.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildInfoTest {

    @Test
    void versionIsAlwaysDisplayable() {
        String version = BuildInfo.currentVersion();
        assertFalse(version.isBlank());
        assertTrue(BuildInfo.displayVersion().startsWith("v"));
    }
}
