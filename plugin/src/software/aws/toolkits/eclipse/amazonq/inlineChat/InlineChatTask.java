package software.aws.toolkits.eclipse.amazonq.inlineChat;

import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.ui.texteditor.ITextEditor;

import software.aws.toolkits.eclipse.amazonq.chat.models.CursorState;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public class InlineChatTask {
    private final ITextEditor editor;
    private final String originalCode;
    private final String tabId;
    private final int offset;

    // Use atomics for thread-safe mutable states
    private final AtomicReference<String> prompt = new AtomicReference<>(null);
    private final AtomicReference<CursorState> cursorState = new AtomicReference<>(null);
    private final AtomicReference<String> previousPartialResponse = new AtomicReference<>(null);
    private final AtomicReference<SessionState> taskState = new AtomicReference<>(null);
    private final AtomicBoolean hasActiveSelection = new AtomicBoolean(false);
    private final AtomicInteger previousDisplayLength;
    private final AtomicReference<ScheduledFuture<?>> pendingUpdate = new AtomicReference<>();
    private final AtomicLong lastUpdateTime;

    public InlineChatTask(final ITextEditor editor, final String originalCode, final int offset) {
        var hasActiveSelection = !originalCode.isBlank();
        this.editor = editor;
        this.originalCode = (hasActiveSelection) ? originalCode : "";
        this.offset = offset;
        this.taskState.set(SessionState.ACTIVE);
        this.hasActiveSelection.set(hasActiveSelection);
        this.tabId = UUID.randomUUID().toString();
        this.previousDisplayLength = new AtomicInteger((hasActiveSelection) ? originalCode.length() : 0);
        this.lastUpdateTime = new AtomicLong(System.currentTimeMillis());
    }

    public ScheduledFuture<?> getPendingUpdate() {
        return pendingUpdate.get();
    }

    public SessionState getTaskState() {
        return taskState.get();
    }

    public boolean isActive() {
        return taskState.get() != SessionState.INACTIVE;
    }

    public void setTaskState(final SessionState state) {
        taskState.set(state);
    }

    public boolean cancelPendingUpdate() {
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

    public void setPendingUpdate(final ScheduledFuture<?> update) {
        ScheduledFuture<?> oldUpdate = pendingUpdate.getAndSet(update);
        if (oldUpdate != null) {
            oldUpdate.cancel(false);
        }
    }

    public long getLastUpdateTime() {
        return lastUpdateTime.get();
    }

    public void setLastUpdateTime(final long time) {
        lastUpdateTime.set(time);
    }

    public String getPreviousPartialResponse() {
        return previousPartialResponse.get();
    }

    public void setPreviousPartialResponse(final String response) {
        previousPartialResponse.set(response);
    }

    public int getPreviousDisplayLength() {
        return previousDisplayLength.get();
    }

    public void setPreviousDisplayLength(final int length) {
        previousDisplayLength.set(length);
    }

    public boolean hasActiveSelection() {
        return hasActiveSelection.get();
    }

    public ITextEditor getEditor() {
        return editor;
    }

    public String getOriginalCode() {
        return originalCode;
    }

    public String getTabId() {
        return tabId;
    }

    public int getOffset() {
        return offset;
    }

    public String getPrompt() {
        return prompt.get();
    }

    public void setPrompt(final String prompt) {
        this.prompt.set(prompt);
    }

    public CursorState getCursorState() {
        return cursorState.get();
    }

    public void setCursorState(final CursorState state) {
        this.cursorState.set(state);
    }
}
