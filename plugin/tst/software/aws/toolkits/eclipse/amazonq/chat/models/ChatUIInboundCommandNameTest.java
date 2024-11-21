// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class ChatUIInboundCommandNameTest {

    @Test
    void testGetValue() {
        assertEquals("aws/chat/sendChatPrompt", ChatUIInboundCommandName.ChatPrompt.getValue());
        assertEquals("sendToPrompt", ChatUIInboundCommandName.SendToPrompt.getValue());
        assertEquals("errorMessage", ChatUIInboundCommandName.ErrorMessage.getValue());
        assertEquals("insertToCursorPosition", ChatUIInboundCommandName.InsertToCursorPosition.getValue());
        assertEquals("authFollowUpClicked", ChatUIInboundCommandName.AuthFollowUpClicked.getValue());
        assertEquals("genericCommand", ChatUIInboundCommandName.GenericCommand.getValue());
    }

    @Test
    void testToString() {
        for (ChatUIInboundCommandName command : ChatUIInboundCommandName.values()) {
            assertEquals(command.getValue(), command.toString());
        }
    }

    @Test
    void testInvalidEnum() {
        assertThrows(IllegalArgumentException.class, () -> ChatUIInboundCommandName.valueOf("NonExistentCommand"));
    }

}
