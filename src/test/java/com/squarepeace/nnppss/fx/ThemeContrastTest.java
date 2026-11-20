package com.squarepeace.nnppss.fx;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the semantic color tokens to WCAG AA for normal text. The test reads
 * the real stylesheets, so changing a token without checking contrast fails.
 */
class ThemeContrastTest {

    private static final Pattern TOKEN =
            Pattern.compile("(?m)^\\s*-(app-bg|surface|surface-alt|border-color|text-color|"
                    + "text-muted|primary|primary-hover|success|warning|danger|selected-row):\\s*"
                    + "(#[0-9a-fA-F]{6});");

    @Test
    void lightAndDarkTextTokensMeetWcagAa() throws IOException {
        Map<String, String> light = readTokens("/styles/base.css");
        light.put("white", "#ffffff");
        Map<String, String> dark = new HashMap<>(light);
        dark.putAll(readTokens("/styles/dark.css"));
        dark.put("white", "#ffffff");

        assertPair(light, "text-color", "surface", "light text / surface");
        assertPair(light, "text-muted", "surface", "light muted / surface");
        assertPair(light, "text-color", "surface-alt", "light text / alt surface");
        assertPair(light, "text-muted", "surface-alt", "light muted / alt surface");
        assertPair(light, "white", "primary", "white / primary");

        assertPair(dark, "text-color", "surface", "dark text / surface");
        assertPair(dark, "text-muted", "surface", "dark muted / surface");
        assertPair(dark, "text-color", "surface-alt", "dark text / alt surface");
        assertPair(dark, "text-muted", "surface-alt", "dark muted / alt surface");
        assertPair(dark, "primary", "surface", "dark primary / surface");
    }

    private void assertPair(Map<String, String> tokens, String foreground,
                            String background, String label) {
        String fg = tokens.get(foreground);
        String bg = tokens.get(background);
        assertTrue(fg != null && bg != null, "Missing token for " + label);
        double ratio = contrast(fg, bg);
        assertTrue(ratio >= 4.5,
                () -> label + " contrast is " + ratio + ":1 (needs 4.5:1)");
    }

    private Map<String, String> readTokens(String resource) throws IOException {
        Map<String, String> tokens = new HashMap<>();
        try (InputStream stream = getClass().getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IOException("Missing stylesheet " + resource);
            }
            String css = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            Matcher matcher = TOKEN.matcher(css);
            while (matcher.find()) {
                tokens.put(matcher.group(1), matcher.group(2));
            }
        }
        return tokens;
    }

    private double contrast(String first, String second) {
        double a = luminance(first);
        double b = luminance(second);
        double brighter = Math.max(a, b);
        double darker = Math.min(a, b);
        return (brighter + 0.05) / (darker + 0.05);
    }

    private double luminance(String hex) {
        int red = Integer.parseInt(hex.substring(1, 3), 16);
        int green = Integer.parseInt(hex.substring(3, 5), 16);
        int blue = Integer.parseInt(hex.substring(5, 7), 16);
        return 0.2126 * channel(red) + 0.7152 * channel(green) + 0.0722 * channel(blue);
    }

    private double channel(int value) {
        double channel = value / 255.0;
        return channel <= 0.03928
                ? channel / 12.92
                : Math.pow((channel + 0.055) / 1.055, 2.4);
    }
}
