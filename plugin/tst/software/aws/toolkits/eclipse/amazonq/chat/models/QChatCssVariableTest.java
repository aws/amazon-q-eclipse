// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class QChatCssVariableTest {

    @Test
    void testEnumValues() {
        assertEquals(28, QChatCssVariable.values().length);
    }

    @Test
    void testTextColorValues() {
        assertEquals("--mynah-color-text-default", QChatCssVariable.TextColorDefault.getValue());
        assertEquals("--mynah-color-text-strong", QChatCssVariable.TextColorStrong.getValue());
        assertEquals("--mynah-color-text-weak", QChatCssVariable.TextColorWeak.getValue());
        assertEquals("--mynah-color-text-link", QChatCssVariable.TextColorLink.getValue());
        assertEquals("--mynah-color-text-input", QChatCssVariable.TextColorInput.getValue());
    }

    @Test
    void testLayoutValues() {
        assertEquals("--mynah-color-bg", QChatCssVariable.Background.getValue());
        assertEquals("--mynah-color-tab-active", QChatCssVariable.TabActive.getValue());
        assertEquals("--mynah-color-border-default", QChatCssVariable.BorderDefault.getValue());
        assertEquals("--mynah-color-toggle", QChatCssVariable.ColorToggle.getValue());
    }

    @Test
    void testSyntaxValues() {
        assertEquals("--mynah-color-syntax-bg", QChatCssVariable.SyntaxBackground.getValue());
        assertEquals("--mynah-color-syntax-variable", QChatCssVariable.SyntaxVariable.getValue());
        assertEquals("--mynah-color-syntax-function", QChatCssVariable.SyntaxFunction.getValue());
        assertEquals("--mynah-color-syntax-operator", QChatCssVariable.SyntaxOperator.getValue());
        assertEquals("--mynah-color-syntax-attr-value", QChatCssVariable.SyntaxAttributeValue.getValue());
        assertEquals("--mynah-color-syntax-attr", QChatCssVariable.SyntaxAttribute.getValue());
        assertEquals("--mynah-color-syntax-property", QChatCssVariable.SyntaxProperty.getValue());
        assertEquals("--mynah-color-syntax-comment", QChatCssVariable.SyntaxComment.getValue());
        assertEquals("--mynah-color-syntax-code", QChatCssVariable.SyntaxCode.getValue());
    }

    @Test
    void testStatusValues() {
        assertEquals("--mynah-color-status-info", QChatCssVariable.StatusInfo.getValue());
        assertEquals("--mynah-color-status-success", QChatCssVariable.StatusSuccess.getValue());
        assertEquals("--mynah-color-status-warning", QChatCssVariable.StatusWarning.getValue());
        assertEquals("--mynah-color-status-error", QChatCssVariable.StatusError.getValue());
    }

    @Test
    void testButtonValues() {
        assertEquals("--mynah-color-button", QChatCssVariable.ButtonBackground.getValue());
        assertEquals("--mynah-color-button-reverse", QChatCssVariable.ButtonForeground.getValue());
    }

    @Test
    void testAlternateValues() {
        assertEquals("--mynah-color-alternate", QChatCssVariable.AlternateBackground.getValue());
        assertEquals("--mynah-color-alternate-reverse", QChatCssVariable.AlternateForeground.getValue());
    }

    @Test
    void testCardValues() {
        assertEquals("--mynah-card-bg", QChatCssVariable.CardBackground.getValue());
    }

    @Test
    void testLineHeighValues() {
        assertEquals("--mynah-line-height", QChatCssVariable.LineHeight.getValue());
    }

    @Test
    void testInvalidEnum() {
        assertThrows(IllegalArgumentException.class, () -> QChatCssVariable.valueOf("NonExistentVariable"));
    }

    @Test
    void testNullValue() {
        for (QChatCssVariable variable : QChatCssVariable.values()) {
            assertNotNull(variable.getValue());
        }
    }

    @Test
    void testCssVariableFormat() {
        for (QChatCssVariable variable : QChatCssVariable.values()) {
            assertTrue(variable.getValue().startsWith("--mynah-"));
        }
    }

}
