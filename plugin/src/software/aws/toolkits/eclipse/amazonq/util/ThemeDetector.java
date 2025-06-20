// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import java.util.Optional;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.ITheme;

public final class ThemeDetector {
    private static final String THEME_STORE_LOCATION_FOR_ECLIPSE = "org.eclipse.e4.ui.css.swt.theme";
    private static final String THEME_KEY_FOR_ECLIPSE = "themeid";
    private static final String ACTIVE_TAB_BG_KEY = "org.eclipse.ui.workbench.ACTIVE_TAB_BG_START";

    public boolean isDarkTheme() {
        Optional<Boolean> isDarkThemeFromEclipsePreferences = isDarkThemeFromEclipsePreferences();

        if (isDarkThemeFromEclipsePreferences.isPresent()) {
            return isDarkThemeFromEclipsePreferences.get();
        }
        try {
            return themeUsingDarkColors();
        } catch (Exception e) {
            return Display.isSystemDarkTheme();
        }
    }

    private Optional<Boolean> isDarkThemeFromEclipsePreferences() {
        IEclipsePreferences themePreferences = InstanceScope.INSTANCE.getNode(THEME_STORE_LOCATION_FOR_ECLIPSE);
        String theme = themePreferences.get(THEME_KEY_FOR_ECLIPSE, "");

        if (theme.isBlank()) {
            return Optional.empty();
        }

        Boolean isDarkTheme = theme.contains("dark");
        return Optional.ofNullable(isDarkTheme);
    }

    private boolean themeUsingDarkColors() throws Exception {
        ITheme currentTheme = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
        Color backgroundColor = currentTheme.getColorRegistry().get(ACTIVE_TAB_BG_KEY);
        // Check if the background color is dark by examining its RGB values
        if (backgroundColor != null) {
            int brightness = (backgroundColor.getRed() + backgroundColor.getGreen() + backgroundColor.getBlue()) / 3;
            return brightness < 128; // If average RGB value is less than 128, we consider it dark
        }
        return false;
    }

}
