// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class TriggerTypeTest {

    @Test
    void testEnumValues() {
        assertEquals(3, TriggerType.values().length);
    }

    @Test
    void testGetValue() {
        assertEquals("hotkeys", TriggerType.Hotkeys.getValue());
        assertEquals("click", TriggerType.Click.getValue());
        assertEquals("contextMenu", TriggerType.ContextMenu.getValue());
    }

    @Test
    void testToString() {
        for (TriggerType command : TriggerType.values()) {
            assertEquals(command.getValue(), command.toString());
        }
    }

    @Test
    void testInvalidEnum() {
        assertThrows(IllegalArgumentException.class, () -> TriggerType.valueOf("InvalidTrigger"));
    }

    @Test
    void testNullValue() {
        for (TriggerType type : TriggerType.values()) {
            assertNotNull(type.getValue());
        }
    }

}
