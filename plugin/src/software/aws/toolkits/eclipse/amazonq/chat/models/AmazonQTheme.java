// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat.models;

import java.awt.Color;
import java.awt.Font;

public record AmazonQTheme(
    Boolean darkMode,
    Font font,

    Color defaultText,
    Color inactiveText,
    Color linkText,

    Color background,
    Color border,
    Color activeTab,

    Color checkboxBackground,
    Color checkboxForeground,

    Color textFieldBackground,
    Color textFieldForeground,

    Color buttonForeground,
    Color buttonBackground,
    Color secondaryButtonForeground,
    Color secondaryButtonBackground,

    Color info,
    Color success,
    Color warning,
    Color error,

    Color cardBackground,

    Font editorFont,
    Color editorBackground,
    Color editorForeground,
    Color editorVariable,
    Color editorOperator,
    Color editorFunction,
    Color editorComment,
    Color editorKeyword,
    Color editorString,
    Color editorProperty
) { };