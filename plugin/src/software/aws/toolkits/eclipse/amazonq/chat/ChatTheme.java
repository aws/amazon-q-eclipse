// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat;

import java.util.HashMap;
import java.util.Map;

import software.aws.toolkits.eclipse.amazonq.chat.models.QChatCssVariable;
import software.aws.toolkits.eclipse.amazonq.util.ThemeDetector;

public final class ChatTheme {
    private ThemeDetector themeDetector;

    public ChatTheme() {
        this.themeDetector = new ThemeDetector();
    }

    public String getThemeVariables() {
        Map<QChatCssVariable, String> themeMap = themeDetector.isDarkTheme() ? getDarkThemeMap() : getLightThemeMap();
        return getCss(themeMap);
    }

    private Map<QChatCssVariable, String> getDarkThemeMap() {
        Map<QChatCssVariable, String> themeMap = new HashMap<>();

        String defaultTextColor = rgb(238, 238, 238);
        String cardBackgroundColor = rgb(55, 55, 55);

        // Text
        themeMap.put(QChatCssVariable.TextColorDefault, defaultTextColor);
        themeMap.put(QChatCssVariable.TextColorStrong, rgb(255, 255, 255));
        themeMap.put(QChatCssVariable.TextColorWeak, rgba(205, 205, 205, 0.5));
        themeMap.put(QChatCssVariable.TextColorLink, rgb(102, 168, 245));
        themeMap.put(QChatCssVariable.TextColorInput, defaultTextColor);
        themeMap.put(QChatCssVariable.TextColorAlternate, defaultTextColor);

        // Layout
        themeMap.put(QChatCssVariable.Background, rgb(47, 47, 47));
        themeMap.put(QChatCssVariable.TabActive, rgb(51, 51, 51));
        themeMap.put(QChatCssVariable.BorderDefault, rgb(76, 76, 76));
        themeMap.put(QChatCssVariable.ColorToggle, rgb(30, 30, 30));

        // Code Syntax
        themeMap.put(QChatCssVariable.SyntaxBackground, rgb(30, 31, 34));
        themeMap.put(QChatCssVariable.SyntaxVariable, rgb(102, 225, 248));
        themeMap.put(QChatCssVariable.SyntaxKeyword, rgb(204, 108, 29));
        themeMap.put(QChatCssVariable.SyntaxClassName, rgb(18, 144, 195));
        themeMap.put(QChatCssVariable.SyntaxFunction, rgb(187, 100, 29));
        themeMap.put(QChatCssVariable.SyntaxOperator, rgb(217, 111, 187));
        themeMap.put(QChatCssVariable.SyntaxAttributeValue, rgb(66, 141, 190));
        themeMap.put(QChatCssVariable.SyntaxAttribute, rgb(179, 108, 50));
        themeMap.put(QChatCssVariable.SyntaxProperty, rgb(57, 171, 184));
        themeMap.put(QChatCssVariable.SyntaxComment, rgb(130, 130, 130));
        themeMap.put(QChatCssVariable.SyntaxCode, defaultTextColor);
        themeMap.put(QChatCssVariable.SyntaxString, rgb(23, 198, 163));

        // Status
        themeMap.put(QChatCssVariable.StatusInfo, rgb(55, 148, 255));
        themeMap.put(QChatCssVariable.StatusSuccess, rgb(135, 217, 108));
        themeMap.put(QChatCssVariable.StatusWarning, rgb(255, 204, 102));
        themeMap.put(QChatCssVariable.StatusError, rgb(255, 102, 102));

        // Buttons
        themeMap.put(QChatCssVariable.ButtonBackground, rgb(14, 99, 156));
        themeMap.put(QChatCssVariable.ButtonForeground, rgb(255, 255, 255));

        // Alternates
        themeMap.put(QChatCssVariable.AlternateBackground, rgb(95, 106, 121));
        themeMap.put(QChatCssVariable.AlternateForeground, rgb(255, 255, 255));

        // Card
        themeMap.put(QChatCssVariable.CardBackground, cardBackgroundColor);
        themeMap.put(QChatCssVariable.CardBackgroundAlternate, rgb(31, 31, 34));

        themeMap.put(QChatCssVariable.LineHeight, "1.25em");

        // Input
        themeMap.put(QChatCssVariable.InputBackground, rgb(60, 60, 60));
        themeMap.put(QChatCssVariable.InputBorder, rgb(76, 76, 76));
        themeMap.put(QChatCssVariable.InputBorderFocused, rgb(14, 99, 156));

        return themeMap;
    }

    private Map<QChatCssVariable, String> getLightThemeMap() {
        Map<QChatCssVariable, String> themeMap = new HashMap<>();

        String defaultTextColor = rgb(10, 10, 10);
        String cardBackgroundColor = rgb(255, 255, 255);

        // Text
        themeMap.put(QChatCssVariable.TextColorDefault, defaultTextColor);
        themeMap.put(QChatCssVariable.TextColorStrong, rgb(0, 0, 0));
        themeMap.put(QChatCssVariable.TextColorWeak, rgba(45, 45, 45, 0.5));
        themeMap.put(QChatCssVariable.TextColorLink, rgb(59, 34, 246));
        themeMap.put(QChatCssVariable.TextColorInput, defaultTextColor);
        themeMap.put(QChatCssVariable.TextColorAlternate, defaultTextColor);

        // Layout
        themeMap.put(QChatCssVariable.Background, rgb(243, 243, 243));
        themeMap.put(QChatCssVariable.TabActive, rgb(250, 250, 250));
        themeMap.put(QChatCssVariable.BorderDefault, rgb(230, 230, 230));
        themeMap.put(QChatCssVariable.ColorToggle, rgb(220, 220, 220));

        // Code Syntax
        themeMap.put(QChatCssVariable.SyntaxBackground, rgb(255, 255, 255));
        themeMap.put(QChatCssVariable.SyntaxVariable, rgb(37, 37, 201));
        themeMap.put(QChatCssVariable.SyntaxFunction, rgb(86, 178, 80));
        themeMap.put(QChatCssVariable.SyntaxClassName, rgb(18, 144, 195));
        themeMap.put(QChatCssVariable.SyntaxOperator, rgb(217, 111, 187));
        themeMap.put(QChatCssVariable.SyntaxAttributeValue, rgb(66, 141, 190));
        themeMap.put(QChatCssVariable.SyntaxAttribute, rgb(179, 108, 50));
        themeMap.put(QChatCssVariable.SyntaxProperty, rgb(57, 171, 184));
        themeMap.put(QChatCssVariable.SyntaxComment, rgb(130, 130, 130));
        themeMap.put(QChatCssVariable.SyntaxCode, defaultTextColor);
        themeMap.put(QChatCssVariable.SyntaxString, rgb(70, 33, 255));

        // Status
        themeMap.put(QChatCssVariable.StatusInfo, rgb(55, 148, 255));
        themeMap.put(QChatCssVariable.StatusSuccess, rgb(135, 217, 108));
        themeMap.put(QChatCssVariable.StatusWarning, rgb(255, 204, 102));
        themeMap.put(QChatCssVariable.StatusError, rgb(255, 102, 102));

        // Buttons
        themeMap.put(QChatCssVariable.ButtonBackground, rgb(51, 118, 205));
        themeMap.put(QChatCssVariable.ButtonForeground, rgb(255, 255, 255));

        // Alternates
        themeMap.put(QChatCssVariable.AlternateBackground, rgb(95, 106, 121));
        themeMap.put(QChatCssVariable.AlternateForeground, rgb(0, 0, 0));

        // Card
        themeMap.put(QChatCssVariable.CardBackground, cardBackgroundColor);
        themeMap.put(QChatCssVariable.CardBackgroundAlternate, rgb(255, 255, 255));

        themeMap.put(QChatCssVariable.LineHeight, "1.25em");

        // Input
        themeMap.put(QChatCssVariable.InputBackground, rgb(255, 255, 255));
        themeMap.put(QChatCssVariable.InputBorder, rgb(230, 230, 230));
        themeMap.put(QChatCssVariable.InputBorderFocused, rgb(51, 118, 205));

        return themeMap;
    }

    private String getCss(final Map<QChatCssVariable, String> themeMap) {
        StringBuilder variables = new StringBuilder();

        for (var entry : themeMap.entrySet()) {
            if (entry.getValue().isBlank()) {
                continue;
            }

            variables.append(String.format("%s:%s !important;",
                    entry.getKey().getValue(),
                    entry.getValue()));
        }

        return String.format(":root{%s}", variables.toString());
    }

    private String rgb(final Integer r, final Integer g, final Integer b) {
        return String.format("rgb(%s,%s,%s)", r, g, b);
    }

    private String rgba(final Integer r, final Integer g, final Integer b, final Double a) {
        return String.format("rgba(%s,%s,%s,%s)", r, g, b, a);
    }
}
