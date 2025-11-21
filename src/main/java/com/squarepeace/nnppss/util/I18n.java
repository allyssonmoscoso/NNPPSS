package com.squarepeace.nnppss.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internationalization utility class for managing localized messages.
 * Supports English (en) and Spanish (es) languages.
 */
public class I18n {
    private static final Logger log = LoggerFactory.getLogger(I18n.class);
    private static final String BUNDLE_NAME = "messages";
    
    private static Locale currentLocale = Locale.getDefault();
    private static ResourceBundle resourceBundle = loadBundle(currentLocale);
    
    /**
     * Load resource bundle for the given locale
     */
    private static ResourceBundle loadBundle(Locale locale) {
        try {
            return ResourceBundle.getBundle(BUNDLE_NAME, locale);
        } catch (MissingResourceException e) {
            log.warn("Could not load resource bundle for locale: {}. Falling back to default.", locale);
            return ResourceBundle.getBundle(BUNDLE_NAME, Locale.ENGLISH);
        }
    }
    
    /**
     * Get localized message for the given key
     * @param key The message key
     * @return Localized message, or the key itself if not found
     */
    public static String get(String key) {
        try {
            return resourceBundle.getString(key);
        } catch (MissingResourceException e) {
            log.warn("Missing translation for key: {}", key);
            return key;
        }
    }
    
    /**
     * Get localized message with parameter substitution
     * @param key The message key
     * @param params Parameters to substitute in the message
     * @return Formatted localized message
     */
    public static String get(String key, Object... params) {
        try {
            String pattern = resourceBundle.getString(key);
            return MessageFormat.format(pattern, params);
        } catch (MissingResourceException e) {
            log.warn("Missing translation for key: {}", key);
            return key;
        }
    }
    
    /**
     * Set the current locale and reload resource bundle
     * @param locale The locale to use (e.g., Locale.ENGLISH, new Locale("es"))
     */
    public static void setLocale(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        currentLocale = locale;
        resourceBundle = loadBundle(currentLocale);
        log.info("Locale changed to: {}", currentLocale);
    }
    
    /**
     * Set locale by language code
     * @param languageCode Language code ("en" for English, "es" for Spanish)
     */
    public static void setLocale(String languageCode) {
        if (languageCode == null || languageCode.isEmpty()) {
            languageCode = "en";
        }
        setLocale(new Locale(languageCode));
    }
    
    /**
     * Get the current locale
     * @return Current locale
     */
    public static Locale getCurrentLocale() {
        return currentLocale;
    }
    
    /**
     * Get the current language code
     * @return Language code (e.g., "en", "es")
     */
    public static String getCurrentLanguageCode() {
        return currentLocale.getLanguage();
    }
    
    /**
     * Check if a key exists in the resource bundle
     * @param key The message key
     * @return true if key exists, false otherwise
     */
    public static boolean hasKey(String key) {
        try {
            resourceBundle.getString(key);
            return true;
        } catch (MissingResourceException e) {
            return false;
        }
    }
}
