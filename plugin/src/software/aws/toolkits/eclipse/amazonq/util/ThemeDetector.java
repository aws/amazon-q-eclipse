// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import java.util.Optional;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.swt.widgets.Display;

public final class ThemeDetector {
    private static final String THEME_STORE_LOCATION_FOR_ECLIPSE = "org.eclipse.e4.ui.css.swt.theme";
    private static final String THEME_KEY_FOR_ECLIPSE = "themeid";

    public boolean isDarkTheme() {
        Optional<Boolean> isDarkThemeFromEclipsePreferences = isDarkThemeFromEclipsePreferences();

        if (isDarkThemeFromEclipsePreferences.isPresent()) {
            return isDarkThemeFromEclipsePreferences.get();
        }

        return Display.isSystemDarkTheme();
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

}
