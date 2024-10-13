// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat;

import java.util.HashMap;
import java.util.Map;

import software.aws.toolkits.eclipse.amazonq.chat.models.AmazonQTheme;
import software.aws.toolkits.eclipse.amazonq.chat.models.MynahCssVariable;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;

import java.awt.Color;
import java.awt.Font;

public class ChatTheme {

    private final Map<String, String> themeMap = new HashMap<>();

    public ChatTheme(final AmazonQTheme theme) {
        addEntry(MynahCssVariable.FontSize.getValue(), theme.font());
        addEntry(MynahCssVariable.FontFamily.getValue(), toCssFontFamily(theme.font()));

        addEntry(MynahCssVariable.TextColorDefault.getValue(), theme.defaultText());
        addEntry(MynahCssVariable.TextColorStrong.getValue(), theme.textFieldForeground());
        addEntry(MynahCssVariable.TextColorInput.getValue(), theme.textFieldForeground());
        addEntry(MynahCssVariable.TextColorLink.getValue(), theme.linkText());
        addEntry(MynahCssVariable.TextColorWeak.getValue(), theme.inactiveText());

        addEntry(MynahCssVariable.Background.getValue(), theme.background());
        addEntry(MynahCssVariable.BackgroundAlt.getValue(), theme.background());
        addEntry(MynahCssVariable.CardBackground.getValue(), theme.cardBackground());
        addEntry(MynahCssVariable.BorderDefault.getValue(), theme.border());
        addEntry(MynahCssVariable.TabActive.getValue(), theme.activeTab());

        addEntry(MynahCssVariable.InputBackground.getValue(), theme.textFieldBackground());

        addEntry(MynahCssVariable.ButtonBackground.getValue(), theme.buttonBackground());
        addEntry(MynahCssVariable.ButtonForeground.getValue(), theme.buttonForeground());
        addEntry(MynahCssVariable.SecondaryButtonBackground.getValue(), theme.secondaryButtonBackground());
        addEntry(MynahCssVariable.SecondaryButtonForeground.getValue(), theme.secondaryButtonForeground());

        addEntry(MynahCssVariable.StatusInfo.getValue(), theme.info());
        addEntry(MynahCssVariable.StatusSuccess.getValue(), theme.success());
        addEntry(MynahCssVariable.StatusWarning.getValue(), theme.warning());
        addEntry(MynahCssVariable.StatusError.getValue(), theme.error());

        addEntry(MynahCssVariable.ColorDeep.getValue(), theme.checkboxBackground());
        addEntry(MynahCssVariable.ColorDeepReverse.getValue(), theme.checkboxForeground());

        addEntry(MynahCssVariable.SyntaxCodeFontFamily.getValue(), toCssFontFamily(theme.editorFont(), "monospace"));
        addEntry(MynahCssVariable.SyntaxCodeFontSize.getValue(), theme.editorFont());
        addEntry(MynahCssVariable.SyntaxBackground.getValue(), theme.editorBackground());
        addEntry(MynahCssVariable.SyntaxVariable.getValue(), theme.editorVariable());
        addEntry(MynahCssVariable.SyntaxOperator.getValue(), theme.editorOperator());
        addEntry(MynahCssVariable.SyntaxFunction.getValue(), theme.editorFunction());
        addEntry(MynahCssVariable.SyntaxComment.getValue(), theme.editorComment());
        addEntry(MynahCssVariable.SyntaxAttributeValue.getValue(), theme.editorKeyword());
        addEntry(MynahCssVariable.SyntaxAttribute.getValue(), theme.editorString());
        addEntry(MynahCssVariable.SyntaxProperty.getValue(), theme.editorProperty());
        addEntry(MynahCssVariable.SyntaxCode.getValue(), theme.editorForeground());

        addEntry(MynahCssVariable.MainBackground.getValue(), theme.buttonBackground());
        addEntry(MynahCssVariable.MainForeground.getValue(), theme.buttonForeground());
    }

    /*
     * Returns a string of the root tag nesting the mynah-ui css variables
     */
    public final String getCss() {
        StringBuilder variables = new StringBuilder();

        for (var entry : themeMap.entrySet()) {

            if (entry.getValue().isBlank()) {
                continue;
            }

            variables.append(String.format("%s:%s;",
                entry.getKey(),
                entry.getValue()
            ));
        }
        
        PluginLogger.info(variables.toString());

        return String.format(":root{%s}", variables.toString());
    }

    private String toCssColor(final Color color) {
        return String.format("rgba(%s,%s,%s,%s)",
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                color.getAlpha()
        );
    }

    private String toCssSize(final Font font) {
        return String.format("%spx", font.getSize());
    };

    private String toCssFontFamily(final Font font) {
        String fallback = "system-ui";
        return toCssFontFamily(font, fallback);
    }

    private String toCssFontFamily(final Font font, final String fallback) {
    	if (font == null) {
    		return "";
    	}
    	
        String fontFamily = font.getFamily();

        if (fontFamily.isBlank()) {
            return String.format("font-family:%s", fallback);
        }

        return String.format("font-family:%s", fontFamily);
    }

    private void addEntry(final String key, final Color color) {
    	if (color == null) {
    		PluginLogger.info("Failed to add theme entry: No color for key " + key);
    		return;
    	}
    	
        themeMap.put(key, toCssColor(color));
    }

    private void addEntry(final String key, final Font font) {
    	if (font == null) {
    		PluginLogger.info("Failed to add theme entry: No font for key " + key);
    		return;
    	}
    	
        themeMap.put(key, toCssSize(font));
    }

    private void addEntry(final String key, final String value) {
    	if (value.isBlank()) {
    		PluginLogger.info("Failed to add theme entry: No value provided for key " + key);
    		return;
    	}
    	
        themeMap.put(key, value);

    }
}
