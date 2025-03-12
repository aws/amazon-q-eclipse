package software.aws.toolkits.eclipse.amazonq.inlineChat;

import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.texteditor.ITextEditor;

import software.aws.toolkits.eclipse.amazonq.chat.models.CursorState;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

class InlineChatTask {
    private final ITextEditor editor;
    private final String originalCode;
    private final String tabId;
    private final int selectionOffset;
    private final int caretOffset;

    // Use atomics for thread-safe mutable states
    private final AtomicReference<String> prompt = new AtomicReference<>(null);
    private final AtomicReference<CursorState> cursorState = new AtomicReference<>(null);
    private final AtomicReference<String> previousPartialResponse = new AtomicReference<>(null);
    private final AtomicReference<SessionState> taskState = new AtomicReference<>(null);
    private final AtomicBoolean hasActiveSelection = new AtomicBoolean(false);
    private final AtomicInteger previousDisplayLength;
    private final AtomicReference<ScheduledFuture<?>> pendingUpdate = new AtomicReference<>();
    private final AtomicLong lastUpdateTime;

    InlineChatTask(final ITextEditor editor, final String selectionText, final int caretOffset, final IRegion region) {
        boolean hasActiveSelection = !selectionText.isBlank();

        this.editor = editor;
        this.caretOffset = caretOffset;
        this.originalCode = (hasActiveSelection) ? selectionText : "";
        this.selectionOffset = region.getOffset();
        this.taskState.set(SessionState.ACTIVE);
        this.hasActiveSelection.set(hasActiveSelection);
        this.tabId = UUID.randomUUID().toString();
        this.previousDisplayLength = new AtomicInteger((hasActiveSelection) ? selectionText.length() : 0);
        this.lastUpdateTime = new AtomicLong(System.currentTimeMillis());
    }

    ScheduledFuture<?> getPendingUpdate() {
        return pendingUpdate.get();
    }

    boolean isActive() {
        return taskState.get() != SessionState.INACTIVE;
    }

    void setTaskState(final SessionState state) {
        taskState.set(state);
    }

    int getCaretOffset() {
        return this.caretOffset;
    }

    boolean cancelPendingUpdate() {
        ScheduledFuture<?> update = pendingUpdate.get();
        if (update != null) {
            try {
                return update.cancel(false);
            } catch (Exception e) {
                Activator.getLogger().error("Failed to cancel update: " + e.getMessage(), e);
            }
        }
        return false;
    }

    void setPendingUpdate(final ScheduledFuture<?> update) {
        ScheduledFuture<?> oldUpdate = pendingUpdate.getAndSet(update);
        if (oldUpdate != null) {
            oldUpdate.cancel(false);
        }
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
}
