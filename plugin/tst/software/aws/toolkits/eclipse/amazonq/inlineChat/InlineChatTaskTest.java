// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.inlineChat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.texteditor.ITextEditor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import software.aws.toolkits.telemetry.TelemetryDefinitions.UserDecision;

public final class InlineChatTaskTest {
    private InlineChatTask task;
    private ITextEditor mockEditor;
    private IRegion mockRegion;
    private final String selection = "one\ntwo\nthree\n";

    @BeforeEach
    void setUp() {
        mockEditor = mock(ITextEditor.class);
        mockRegion = mock(IRegion.class);
        task = new InlineChatTask(mockEditor, selection, mockRegion, 3);
    }

    @Test
    public void testBuildResultObjectWhenDismiss() {
        // if user decision isn't updated --> dismiss is assumed
        List<TextDiff> diffs = new ArrayList<>();
        task.setTextDiffs(diffs);

        InlineChatResultParams result = task.buildResultObject();
        assertEquals(-1, result.inputLength());
        assertEquals(-1, result.startLatency());
        assertEquals(-1, result.endLatency());
        assertEquals(UserDecision.DISMISS, result.userDecision());
    }

    // Flow is the same regardless of ACCEPT/REJECT
    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    public void testBuildResultObjectWhenCompleted(final boolean userAccepted) {
        task.setUserDecision(userAccepted);
        task.setPrompt("test prompt");
        task.setRequestTime(1000L);
        task.setFirstTokenTime(1100L);
        task.setLastTokenTime(1200L);
        task.setLanguage("java");

        TextDiff addDiff = new TextDiff(0, 10, false);
        TextDiff delDiff = new TextDiff(20, 20, true);
        List<TextDiff> diffs = Arrays.asList(addDiff, delDiff);
        task.setTextDiffs(diffs);
        task.setNumAddedLines(2);
        task.setNumDeletedLines(1);

        InlineChatResultParams result = task.buildResultObject();

        var decision = (userAccepted) ? UserDecision.ACCEPT : UserDecision.REJECT;
        assertEquals("java", result.language());
        assertEquals(11, result.inputLength());
        assertEquals(3, result.numSelectedLines());
        assertEquals(10, result.numSuggestionAddChars());
        assertEquals(2, result.numSuggestionAddLines());
        assertEquals(20, result.numSuggestionDelChars());
        assertEquals(1, result.numSuggestionDelLines());
        assertEquals(decision, result.userDecision());
        assertEquals(100.0, result.startLatency()); // First token time - request time
        assertEquals(200.0, result.endLatency()); // Last token time - request time
    }
}
