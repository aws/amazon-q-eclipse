// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

// See examples in https://github.com/aws/mynah-ui/blob/main/docs/STYLING.md
public enum QChatCssVariable {
    // Text
    TextColorDefault("--mynah-color-text-default"),
    TextColorStrong("--mynah-color-text-strong"),
    TextColorWeak("--mynah-color-text-weak"),
    TextColorLink("--mynah-color-text-link"),
    TextColorInput("--mynah-color-text-input"),
    TextColorAlternate("--mynah-color-text-alternate"),

    // Layout
    Background("--mynah-color-bg"),
    TabActive("--mynah-color-tab-active"),
    BorderDefault("--mynah-color-border-default"),
    ColorToggle("--mynah-color-toggle"),

    // Code syntax
    SyntaxBackground("--mynah-color-syntax-bg"),
    SyntaxVariable("--mynah-color-syntax-variable"),
    SyntaxFunction("--mynah-color-syntax-function"),
    SyntaxOperator("--mynah-color-syntax-operator"),
    SyntaxAttributeValue("--mynah-color-syntax-attr-value"),
    SyntaxAttribute("--mynah-color-syntax-attr"),
    SyntaxProperty("--mynah-color-syntax-property"),
    SyntaxComment("--mynah-color-syntax-comment"),
    SyntaxCode("--mynah-color-syntax-code"),

    // Status
    StatusInfo("--mynah-color-status-info"),
    StatusSuccess("--mynah-color-status-success"),
    StatusWarning("--mynah-color-status-warning"),
    StatusError("--mynah-color-status-error"),

    // Buttons
    ButtonBackground("--mynah-color-button"),
    ButtonForeground("--mynah-color-button-reverse"),

    // Alternates
    AlternateBackground("--mynah-color-alternate"),
    AlternateForeground("--mynah-color-alternate-reverse"),

    // Card
    CardBackground("--mynah-card-bg"),
    CardBackgroundAlternate("--mynah-card-bg-alternate"),

    // Line height
    LineHeight("--mynah-line-height"),

    // Input
    InputBackground("--mynah-input-bg"),

    // Borders
    InputBorder("--mynah-color-text-input-border"),
    InputBorderFocused("--mynah-color-text-input-border-focused");

    private String value;

    QChatCssVariable(final String name) {
        this.value = name;
    }

    public String getValue() {
        return this.value;
    }
}
