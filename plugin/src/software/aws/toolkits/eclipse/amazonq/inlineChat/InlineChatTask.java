package software.aws.toolkits.eclipse.amazonq.inlineChat;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.texteditor.ITextEditor;

import software.aws.toolkits.eclipse.amazonq.chat.models.CursorState;

class InlineChatTask {
    private final ITextEditor editor;
    private final String originalCode;
    private final String tabId;

    // Selection offset -> accounts for every character in the document, regardless of collapsing
    // Visual offset -> only cares about position on screen, not characters in doc
    private final int selectionOffset;
    private final int visualOffset;

    // Use atomics for thread-safe mutable states
    private final AtomicReference<String> prompt = new AtomicReference<>(null);
    private final AtomicReference<CursorState> cursorState = new AtomicReference<>(null);
    private final AtomicReference<String> previousPartialResponse = new AtomicReference<>(null);
    private final AtomicReference<SessionState> taskState = new AtomicReference<>(null);
    private final AtomicBoolean hasActiveSelection = new AtomicBoolean(false);
    private final AtomicInteger previousDisplayLength;

    // Latency variables
    private final AtomicLong lastUpdateTime;
    private final AtomicLong requestTime;
    private final AtomicLong firstTokenTime;
    private final AtomicLong lastTokenTime;

    InlineChatTask(final ITextEditor editor, final String selectionText, final int visualOffset, final IRegion region) {
        boolean hasActiveSelection = !selectionText.isBlank();

        this.editor = editor;
        this.visualOffset = visualOffset;
        this.originalCode = (hasActiveSelection) ? selectionText : "";
        this.selectionOffset = region.getOffset();
        this.taskState.set(SessionState.ACTIVE);
        this.hasActiveSelection.set(hasActiveSelection);
        this.tabId = UUID.randomUUID().toString();
        this.previousDisplayLength = new AtomicInteger((hasActiveSelection) ? selectionText.length() : 0);
        this.lastUpdateTime = new AtomicLong(0);
        this.requestTime = new AtomicLong(0);
        this.firstTokenTime = new AtomicLong(-1);
        this.lastTokenTime = new AtomicLong(0);
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

}
