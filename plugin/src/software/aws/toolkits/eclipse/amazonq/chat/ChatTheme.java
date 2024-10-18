// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.browser.Browser;

import software.aws.toolkits.eclipse.amazonq.chat.models.MynahCssVariable;
import software.aws.toolkits.eclipse.amazonq.util.ThemeDetector;

public final class ChatTheme {
    private static final String CHAT_THEME_STYLE_TITLE = "CHAT_THEME_STYLE";

    private  ThemeDetector themeDetector;

    public ChatTheme() {
        this.themeDetector = new ThemeDetector();
    }

    public void injectTheme(final Browser browser) {
        String css = "";

        if (themeDetector.isDarkTheme()) {
            css = getCssForDarkTheme();
        } else {
            css = getCssForLightTheme();
        }

        String removeExistingThemeScript = String.format("""
                    var sheets = document.styleSheets;\
                    for (var i=0; i<sheets.length; i++){\
                        var sheet = sheets[i];\
                        if (sheet.title === "%s") {\
                            for (var j=0; j<sheet.rules.length; j++){\
                                sheet.deleteRule(j);\
                            }\
                        }\
                    }\
                """, CHAT_THEME_STYLE_TITLE);

        String addThemeScript = String.format("""
                    var style = document.createElement('style');\
                    style.type = "text/css";\
                    style.title = "%s";\
                    document.head.appendChild(style);\
                    style.sheet.insertRule("%s", style.sheet.cssRules.length);\
                """, CHAT_THEME_STYLE_TITLE, css);

        browser.evaluate(removeExistingThemeScript);
        browser.evaluate(addThemeScript);
    }

    private String getCssForDarkTheme() {
        Map<MynahCssVariable, String> themeMap = new HashMap<>();

        String defaultTextColor = rgb(238, 238, 238);
        String cardBackgroundColor = rgb(55, 55, 55);

        // Text
        themeMap.put(MynahCssVariable.TextColorDefault, defaultTextColor);
        themeMap.put(MynahCssVariable.TextColorStrong, rgb(255, 255, 255));
        themeMap.put(MynahCssVariable.TextColorWeak, rgba(205, 205, 205, 0.5));
        themeMap.put(MynahCssVariable.TextColorLink, rgb(102, 168, 245));
        themeMap.put(MynahCssVariable.TextColorInput, defaultTextColor);

        // Layout
        themeMap.put(MynahCssVariable.Background, rgb(47, 47, 47));
        themeMap.put(MynahCssVariable.TabActive, cardBackgroundColor);
        themeMap.put(MynahCssVariable.BorderDefault, rgb(76, 76, 76));
        themeMap.put(MynahCssVariable.ColorToggle, rgb(30, 30, 30));

        // Code Syntax
        themeMap.put(MynahCssVariable.SyntaxBackground, rgb(29, 30, 34));
        themeMap.put(MynahCssVariable.SyntaxVariable, rgb(247, 247, 80));
        themeMap.put(MynahCssVariable.SyntaxFunction, rgb(86, 178, 80));
        themeMap.put(MynahCssVariable.SyntaxOperator, rgb(217, 111, 187));
        themeMap.put(MynahCssVariable.SyntaxAttributeValue, rgb(66, 141, 190));
        themeMap.put(MynahCssVariable.SyntaxAttribute, rgb(179, 108, 50));
        themeMap.put(MynahCssVariable.SyntaxProperty, rgb(57, 171, 184));
        themeMap.put(MynahCssVariable.SyntaxComment, rgb(130, 130, 130));
        themeMap.put(MynahCssVariable.SyntaxCode, defaultTextColor);

        // Status
        themeMap.put(MynahCssVariable.StatusInfo, rgb(55, 148, 255));
        themeMap.put(MynahCssVariable.StatusSuccess, rgb(135, 217, 108));
        themeMap.put(MynahCssVariable.StatusWarning, rgb(255, 204, 102));
        themeMap.put(MynahCssVariable.StatusError, rgb(255, 102, 102));

        // Buttons
        themeMap.put(MynahCssVariable.ButtonBackground, rgb(51, 118, 205));
        themeMap.put(MynahCssVariable.ButtonForeground, rgb(255, 255, 255));

        // Alternates
        themeMap.put(MynahCssVariable.AlternateBackground, rgb(95, 106, 121));
        themeMap.put(MynahCssVariable.AlternateForeground, rgb(255, 255, 255));

        // Card
        themeMap.put(MynahCssVariable.CardBackground, cardBackgroundColor);

        return getCss(themeMap);
    }

    private String getCssForLightTheme() {
        Map<MynahCssVariable, String> themeMap = new HashMap<>();

        String defaultTextColor = rgb(10, 10, 10);
        String cardBackgroundColor = rgb(255, 255, 255);

        // Text
        themeMap.put(MynahCssVariable.TextColorDefault, defaultTextColor);
        themeMap.put(MynahCssVariable.TextColorStrong, rgb(0, 0, 0));
        themeMap.put(MynahCssVariable.TextColorWeak, rgba(45, 45, 45, 0.5));
        themeMap.put(MynahCssVariable.TextColorLink, rgb(59, 34, 246));
        themeMap.put(MynahCssVariable.TextColorInput, defaultTextColor);

        // Layout
        themeMap.put(MynahCssVariable.Background, rgb(250, 250, 250));
        themeMap.put(MynahCssVariable.TabActive, cardBackgroundColor);
        themeMap.put(MynahCssVariable.BorderDefault, rgb(230, 230, 230));
        themeMap.put(MynahCssVariable.ColorToggle, rgb(220, 220, 220));

        // Code Syntax
        themeMap.put(MynahCssVariable.SyntaxBackground, rgb(220, 232, 250));
        themeMap.put(MynahCssVariable.SyntaxVariable, rgb(247, 247, 80));
        themeMap.put(MynahCssVariable.SyntaxFunction, rgb(86, 178, 80));
        themeMap.put(MynahCssVariable.SyntaxOperator, rgb(217, 111, 187));
        themeMap.put(MynahCssVariable.SyntaxAttributeValue, rgb(66, 141, 190));
        themeMap.put(MynahCssVariable.SyntaxAttribute, rgb(179, 108, 50));
        themeMap.put(MynahCssVariable.SyntaxProperty, rgb(57, 171, 184));
        themeMap.put(MynahCssVariable.SyntaxComment, rgb(130, 130, 130));
        themeMap.put(MynahCssVariable.SyntaxCode, defaultTextColor);

        // Status
        themeMap.put(MynahCssVariable.StatusInfo, rgb(55, 148, 255));
        themeMap.put(MynahCssVariable.StatusSuccess, rgb(135, 217, 108));
        themeMap.put(MynahCssVariable.StatusWarning, rgb(255, 204, 102));
        themeMap.put(MynahCssVariable.StatusError, rgb(255, 102, 102));

        // Buttons
        themeMap.put(MynahCssVariable.ButtonBackground, rgb(51, 118, 205));
        themeMap.put(MynahCssVariable.ButtonForeground, rgb(255, 255, 255));

        // Alternates
        themeMap.put(MynahCssVariable.AlternateBackground, rgb(95, 106, 121));
        themeMap.put(MynahCssVariable.AlternateForeground, rgb(0, 0, 0));

        // Card
        themeMap.put(MynahCssVariable.CardBackground, cardBackgroundColor);

        return getCss(themeMap);
    }

    private  String getCss(final Map<MynahCssVariable, String> themeMap) {
        StringBuilder variables = new StringBuilder();

        for (var entry : themeMap.entrySet()) {
            if (entry.getValue().isBlank()) {
                continue;
            }

            variables.append(String.format("%s:%s;",
                    entry.getKey().getValue(),
                    entry.getValue()));
        }

        return String.format(":root{%s}", variables.toString());
    }

    private  String rgb(final Integer r, final Integer g, final Integer b) {
        return String.format("rgb(%s,%s,%s)", r, g, b);
    }

    private  String rgba(final Integer r, final Integer g, final Integer b, final Double a) {
        return String.format("rgb(%s,%s,%s,%s)", r, g, b, a);
    }

}
