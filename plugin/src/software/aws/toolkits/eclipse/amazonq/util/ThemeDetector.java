// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.util;

import java.util.Optional;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.swt.widgets.Display;

public class ThemeDetector {
    private final String THEME_STORE_LOCATION_FOR_ECLIPSE = "org.eclipse.e4.ui.css.swt.theme";
    private final String THEME_KEY_FOR_ECLIPSE = "themeid";
    private final String THEME_STORE_LOCATION_FOR_AMAZON_Q = "amazon-q-eclipse.ui";
    private final String THEME_KEY_FOR_AMAZON_Q = "themeid";
    private final String DARK_MODE_THEME_ID_FOR_AMAZON_Q = "dark";
    private final String LIGHT_MDOE_THEME_ID_FOR_AMAZON_Q = "light";

    public boolean isDarkTheme() {
        Optional<Boolean> isDarkThemeFromAmazonQPreferences = isDarkThemeFromAmazonQPreferences();
        Optional<Boolean> isDarkThemeFromEclipsePreferences = isDarkThemeFromEclipsePreferences();

        if (isDarkThemeFromAmazonQPreferences.isPresent()) {
            return isDarkThemeFromAmazonQPreferences.get();
        }

        if (isDarkThemeFromEclipsePreferences.isPresent()) {
            return isDarkThemeFromEclipsePreferences.get();
        }

        return Display.isSystemDarkTheme();
    }

    public void setDarkModePreference() {
        setThemePreference(DARK_MODE_THEME_ID_FOR_AMAZON_Q);
    }

    public void setLightModePreference() {
        setThemePreference(LIGHT_MDOE_THEME_ID_FOR_AMAZON_Q);
    }

    private final void setThemePreference(String theme) {
        IEclipsePreferences themePreferences = InstanceScope.INSTANCE.getNode(THEME_STORE_LOCATION_FOR_AMAZON_Q);
        themePreferences.put(THEME_KEY_FOR_AMAZON_Q, theme);

        try {
            themePreferences.flush();
        } catch (Exception e) {
            PluginLogger.error("Error occurred while setting Amazon Q theme preference", e);
        }
    }

    private final Optional<Boolean> isDarkThemeFromAmazonQPreferences() {
        IEclipsePreferences themePreferences = InstanceScope.INSTANCE.getNode(THEME_STORE_LOCATION_FOR_AMAZON_Q);
        String theme = themePreferences.get(THEME_KEY_FOR_AMAZON_Q, "");

        if (theme.isBlank()) {
            return Optional.empty();
        }

        Boolean isDarkTheme = theme.equals(DARK_MODE_THEME_ID_FOR_AMAZON_Q);
        return Optional.ofNullable(isDarkTheme);
    }

    private final Optional<Boolean> isDarkThemeFromEclipsePreferences() {
        IEclipsePreferences themePreferences = InstanceScope.INSTANCE.getNode(THEME_STORE_LOCATION_FOR_ECLIPSE);
        String theme = themePreferences.get(THEME_KEY_FOR_ECLIPSE, "");

        if (theme.isBlank()) {
            return Optional.empty();
        }

        Boolean isDarkTheme = theme.contains("dark");
        return Optional.ofNullable(isDarkTheme);
    }

}
