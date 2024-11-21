// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class GenericCommandVerbTest {

    @Test
    void testGetValue() {
        assertEquals("Explain", GenericCommandVerb.Explain.getValue());
        assertEquals("Refactor", GenericCommandVerb.Refactor.getValue());
        assertEquals("Fix", GenericCommandVerb.Fix.getValue());
        assertEquals("Optimize", GenericCommandVerb.Optimize.getValue());
    }

    @Test
    void testToString() {
        assertEquals("Explain", GenericCommandVerb.Explain.toString());
        assertEquals("Refactor", GenericCommandVerb.Refactor.toString());
        assertEquals("Fix", GenericCommandVerb.Fix.toString());
        assertEquals("Optimize", GenericCommandVerb.Optimize.toString());
    }

    @Test
    void testToStringEqualsGetValue() {
        for (ChatUIInboundCommandName command : ChatUIInboundCommandName.values()) {
            assertEquals(command.getValue(), command.toString());
        }
    }

    @Test
    void testInvalidEnum() {
        assertThrows(IllegalArgumentException.class, () -> ChatUIInboundCommandName.valueOf("NonExistentCommand"));
    }

}
