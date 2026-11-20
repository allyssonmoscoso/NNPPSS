package com.squarepeace.nnppss.util;

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class I18nTest {

    @Test
    void englishAndSpanishBundlesHaveTheSameKeys() {
        ResourceBundle english = ResourceBundle.getBundle("messages", Locale.ENGLISH);
        ResourceBundle spanish = ResourceBundle.getBundle("messages", new Locale("es"));

        assertFalse(english.keySet().isEmpty());
        assertEquals(english.keySet(), spanish.keySet());
    }

    @Test
    void localeSwitchChangesTranslatedValues() {
        I18n.setLocale("en");
        String english = I18n.get("button.save");

        I18n.setLocale("es");
        String spanish = I18n.get("button.save");

        assertEquals("es", I18n.getCurrentLanguageCode());
        assertNotEquals(english, spanish);

        I18n.setLocale("en");
    }

    @Test
    void missingKeyFallsBackToTheKeyItself() {
        assertEquals("missing.key", I18n.get("missing.key"));
    }
}
