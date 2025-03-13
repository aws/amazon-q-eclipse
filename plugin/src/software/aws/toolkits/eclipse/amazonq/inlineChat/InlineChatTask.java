// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.inlineChat;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.texteditor.ITextEditor;

import software.aws.toolkits.eclipse.amazonq.chat.models.CursorState;

class InlineChatTask {

    // Task state variables
    private final ITextEditor editor;
    private final String originalCode;
    private final String tabId;
    private final AtomicReference<String> prompt = new AtomicReference<>(null);
    private final AtomicReference<CursorState> cursorState = new AtomicReference<>(null);
    private final AtomicReference<SessionState> taskState = new AtomicReference<>(null);
    private final AtomicBoolean hasActiveSelection = new AtomicBoolean(false);

    // Selection offset -> accounts for every character in the document, regardless of collapsing
    // Visual offset -> only cares about position on screen, not characters in doc
    private final int selectionOffset;
    private final int visualOffset;

    // Diff tracking variables
    private final AtomicReference<String> previousPartialResponse = new AtomicReference<>(null);
    private final AtomicInteger previousDisplayLength;

    // Telemetry variables
    private final AtomicInteger numDeletedLines;
    private final int numSelectedLines;
    private final AtomicInteger numAddedLines;
    private List<TextDiff> textDiffs;
    private final AtomicReference<UserDecision> userDecision = new AtomicReference<>(UserDecision.DISMISS);

    // Latency variables
    private final AtomicLong lastUpdateTime;
    private final AtomicLong requestTime;
    private final AtomicLong firstTokenTime;
    private final AtomicLong lastTokenTime;

    InlineChatTask(final ITextEditor editor, final String selectionText, final int visualOffset,
            final IRegion region, final int numSelectedLines) {

        boolean hasActiveSelection = !selectionText.isBlank();

        this.taskState.set(SessionState.ACTIVE);
        this.editor = editor;
        this.tabId = UUID.randomUUID().toString();
        this.hasActiveSelection.set(hasActiveSelection);

        this.visualOffset = visualOffset;
        this.selectionOffset = region.getOffset();
        this.originalCode = (hasActiveSelection) ? selectionText : "";
        this.previousDisplayLength = new AtomicInteger((hasActiveSelection) ? selectionText.length() : 0);

        this.lastUpdateTime = new AtomicLong(0);
        this.requestTime = new AtomicLong(0);
        this.firstTokenTime = new AtomicLong(-1);
        this.lastTokenTime = new AtomicLong(0);

        this.numDeletedLines = new AtomicInteger(0);
        this.numAddedLines = new AtomicInteger(0);
        this.numSelectedLines = (hasActiveSelection) ? numSelectedLines : 0;
    }

    boolean isActive() {
        return taskState.get() != SessionState.INACTIVE;
    }

    void setTaskState(final SessionState state) {
        taskState.set(state);
    }

    int getVisualOffset() {
        return this.visualOffset;
    }

    long getLastUpdateTime() {
        return lastUpdateTime.get();
    }

    void setLastUpdateTime(final long time) {
        lastUpdateTime.set(time);
    }

    String getPreviousPartialResponse() {
        return previousPartialResponse.get();
    }

    void setPreviousPartialResponse(final String response) {
        previousPartialResponse.set(response);
    }

    int getPreviousDisplayLength() {
        return previousDisplayLength.get();
    }

    void setPreviousDisplayLength(final int length) {
        previousDisplayLength.set(length);
    }

    boolean hasActiveSelection() {
        return hasActiveSelection.get();
    }

    ITextEditor getEditor() {
        return editor;
    }

    String getOriginalCode() {
        return originalCode;
    }

    String getTabId() {
        return tabId;
    }

    int getSelectionOffset() {
        return selectionOffset;
    }

    String getPrompt() {
        return prompt.get();
    }

    void setPrompt(final String prompt) {
        this.prompt.set(prompt);
    }

    CursorState getCursorState() {
        return cursorState.get();
    }

    void setCursorState(final CursorState state) {
        this.cursorState.set(state);
    }

    long getRequestTime() {
        return requestTime.get();
    }

    void setRequestTime(final long newValue) {
        requestTime.set(newValue);
    }

    long getFirstTokenTime() {
        return firstTokenTime.get();
    }

    void setFirstTokenTime(final long newValue) {
        firstTokenTime.set(newValue);
    }

    long getLastTokenTime() {
        return lastTokenTime.get();
    }

    void setLastTokenTime(final long newValue) {
        lastTokenTime.set(newValue);
    }

    void setUserDecision(final boolean accepted) {
        this.userDecision.set((accepted) ? UserDecision.ACCEPT : UserDecision.REJECT);
    }

    void setNumDeletedLines(final int lines) {
        this.numDeletedLines.set(lines);
    }

    void setNumAddedLines(final int lines) {
        this.numAddedLines.set(lines);
    }

    void setTextDiffs(final List<TextDiff> textDiffs) {
        this.textDiffs = textDiffs;
    }

    InlineChatResultParams buildResultObject() {
        var userDecision = this.userDecision.get();
        int inputLength = -1;
        double startLatency = -1;
        double endLatency = -1;

        if (userDecision != UserDecision.DISMISS) {
            inputLength = getPrompt().length();
            startLatency = getFirstTokenTime() - getRequestTime();
            endLatency = getLastTokenTime() - getRequestTime();
        }

        int numSuggestionAddChars = textDiffs.stream()
                .filter(diff -> !diff.isDeletion())
                .mapToInt(TextDiff::length)
                .sum();
        int numSuggestionDelChars = textDiffs.stream()
                .filter(TextDiff::isDeletion)
                .mapToInt(TextDiff::length)
                .sum();
        int numSuggestionDelLines = this.numDeletedLines.get();
        int numSuggestionAddLines = this.numAddedLines.get();

        return new InlineChatResultParams(
                inputLength,
                numSelectedLines,
                numSuggestionAddChars,
                numSuggestionAddLines,
                numSuggestionDelChars,
                numSuggestionDelLines,
                userDecision,
                startLatency,
                endLatency);
    }

}
