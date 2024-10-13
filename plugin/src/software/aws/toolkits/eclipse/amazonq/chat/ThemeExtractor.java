// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;


import software.aws.toolkits.eclipse.amazonq.chat.models.AmazonQTheme;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;

public class ThemeExtractor {
	
	private ColorRegistry colorRegistry;
	
	public ThemeExtractor() {
		IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
		ITheme theme = themeManager.getCurrentTheme();
		colorRegistry = theme.getColorRegistry();
	}

    private Color getColor(String key) {
        RGB rgb = colorRegistry.getRGB(key);
        return new Color(rgb.red, rgb.green, rgb.blue);
    }

   public static AmazonQTheme getAmazonQTheme() {
        Boolean darkMode = false;
	    Font font = null;
        Font editorFont = null;

	    Color defaultText = getColor("");
	    Color inactiveText = getColor("");
	    Color linkText = getColor("");

	    Color background = getColor("");
	    Color border = getColor("");
	    Color activeTab = getColor("");

	    Color checkboxBackground = getColor("");
	    Color checkboxForeground = getColor("");

	    Color textFieldBackground = getColor("");
	    Color textFieldForeground = getColor("");

	    Color buttonForeground = getColor("");
	    Color buttonBackground = getColor("");
	    Color secondaryButtonForeground= getColor("");
	    Color secondaryButtonBackground= getColor("");

	    Color info= getColor("");
	    Color success= getColor("");
	    Color warning= getColor("");
	    Color error= getColor("");

	    Color cardBackground= getColor("");
	    
	    Color editorBackground= getColor("");
	    Color editorForeground= getColor("");
	    Color editorVariable= getColor("");
	    Color editorOperator= getColor("");
	    Color editorFunction= getColor("");
	    Color editorComment= getColor("");
	    Color editorKeyword= getColor("");
	    Color editorString= getColor("");
	    Color editorProperty= getColor("");
	    
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
