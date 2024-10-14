// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;


import software.aws.toolkits.eclipse.amazonq.chat.models.AmazonQTheme;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;

public final class ThemeExtractor {

    private ColorRegistry colorRegistry;

    public ThemeExtractor() {
        IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
        ITheme theme = themeManager.getCurrentTheme();
        colorRegistry = theme.getColorRegistry();
    }

    private Color getColor(final String key) {
    	if (key.isBlank()) {
    		return null;
    	}
    	
    	try {
    		RGB rgb = colorRegistry.getRGB(key);
    		return new Color(rgb.red, rgb.green, rgb.blue);
    	} catch (Exception e) {
    		PluginLogger.info("Failed to retrieve color for key " + key);
    	}
    	return null;
    }
    
    private Color getColor(final int key) {
    	try {
    		Display display = Display.getCurrent();
    		org.eclipse.swt.graphics.Color swtColor = display.getSystemColor(key);
    		return new Color(swtColor.getRed(), swtColor.getGreen(), swtColor.getBlue(), swtColor.getAlpha());
    	} catch (Exception e) {
    		PluginLogger.info("Failed to retrieve color for key " + key);
    	}
    	return null;
    }
    

   public AmazonQTheme getAmazonQTheme() {
        Boolean darkMode = false;
        Font font = null;
        Font editorFont = null;
        
        Color defaultText = getColor(SWT.COLOR_WIDGET_FOREGROUND);
        Color inactiveText = getColor(SWT.COLOR_WIDGET_DISABLED_FOREGROUND);
        Color linkText = getColor(SWT.COLOR_LINK_FOREGROUND);

        Color background = getColor(SWT.COLOR_WIDGET_BACKGROUND);
        Color cardBackground = getColor(SWT.COLOR_LIST_BACKGROUND);
        Color border = getColor(SWT.COLOR_WIDGET_DISABLED_FOREGROUND);
        Color activeTab = getColor(SWT.COLOR_WIDGET_BACKGROUND);

        Color checkboxBackground = getColor(SWT.COLOR_WIDGET_BACKGROUND);
        Color checkboxForeground = getColor(SWT.COLOR_WIDGET_FOREGROUND);

        Color textFieldBackground = getColor(SWT.COLOR_WIDGET_BACKGROUND);
        Color textFieldForeground = getColor(SWT.COLOR_WIDGET_FOREGROUND);

        Color buttonBackground = getColor(SWT.COLOR_TITLE_BACKGROUND);
        Color buttonForeground = getColor(SWT.COLOR_TITLE_FOREGROUND);
        Color secondaryButtonBackground = getColor(SWT.COLOR_WIDGET_BACKGROUND);
        Color secondaryButtonForeground = getColor(SWT.COLOR_WIDGET_FOREGROUND);

        Color info = getColor(SWT.COLOR_INFO_FOREGROUND);
        Color success = getColor(SWT.COLOR_GREEN);
        Color warning = getColor(SWT.COLOR_YELLOW);
        Color error = getColor(SWT.COLOR_RED);

        Color editorBackground = getColor("org.eclipse.ui.editors.backgroundColor");
        Color editorForeground = getColor("org.eclipse.ui.editors.foregroundColor");
        Color editorVariable = getColor("org.eclipse.jdt.ui.localVariableHighlighting");
        Color editorOperator = getColor("org.eclipse.jdt.ui.java_operator");
        Color editorFunction = getColor("org.eclipse.jdt.ui.methodHighlighting");
        Color editorComment = getColor("org.eclipse.jdt.ui.java_single_line_comment");
        Color editorKeyword = getColor("org.eclipse.jdt.ui.java_keyword");
        Color editorString = getColor("org.eclipse.jdt.ui.java_string");
        Color editorProperty = getColor("org.eclipse.jdt.ui.parameterVariableHighlighting");

        return new AmazonQTheme(
                darkMode,
                font,
                defaultText,
                inactiveText,
                linkText,
                background,
                border,
                activeTab,
                checkboxBackground,
                checkboxForeground,
                textFieldBackground,
                textFieldForeground,
                buttonForeground,
                buttonBackground,
                secondaryButtonForeground,
                secondaryButtonBackground,
                info,
                success,
                warning,
                error,
                cardBackground,
                editorFont,
                editorBackground,
                editorForeground,
                editorVariable,
                editorOperator,
                editorFunction,
                editorComment,
                editorKeyword,
                editorString,
                editorProperty
        );
   }
}
