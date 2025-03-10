package software.aws.toolkits.eclipse.amazonq.inlineChat;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

import software.aws.toolkits.eclipse.amazonq.chat.models.CursorState;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.LanguageUtil;

class InlineChatTask {

    private final ITextSelection selection;
    private final ITextEditor editor;
    private final String originalCode;
    private final String tabId;
    private final int offset;

    // Use atomics for thread-safe mutable states
    private final AtomicReference<String> prompt = new AtomicReference<>(null);
    private final AtomicReference<CursorState> cursorState = new AtomicReference<>(null);
    private final AtomicReference<String> previousPartialResponse = new AtomicReference<>(null);
    private final AtomicReference<SessionState> taskState = new AtomicReference<>(null);
    private final AtomicReference<UserDecision> userDecision = new AtomicReference<>(UserDecision.DISMISS);
    private final AtomicBoolean hasActiveSelection = new AtomicBoolean(false);
    private final AtomicInteger previousDisplayLength;
    private final AtomicInteger numDeletedLines;
    private final AtomicInteger numAddedLines;
    private final AtomicReference<ScheduledFuture<?>> pendingUpdate = new AtomicReference<>();
    private final AtomicLong lastUpdateTime;
    private List<TextDiff> textDiffs;

    InlineChatTask(final ITextEditor editor, final ITextSelection selection) {
        String originalCode = selection.getText();
        boolean hasActiveSelection = !originalCode.isBlank();

        this.selection = selection;
        this.editor = editor;
        this.originalCode = (hasActiveSelection) ? originalCode : "";
        this.offset = selection.getOffset();
        this.taskState.set(SessionState.ACTIVE);
        this.hasActiveSelection.set(hasActiveSelection);
        this.tabId = UUID.randomUUID().toString();
        this.previousDisplayLength = new AtomicInteger((hasActiveSelection) ? originalCode.length() : 0);
        this.lastUpdateTime = new AtomicLong(System.currentTimeMillis());
        this.numDeletedLines = new AtomicInteger(0);
        this.numAddedLines = new AtomicInteger(0);
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

    ITextSelection getSelection() {
        return selection;
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

    int getOffset() {
        return offset;
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

    void setNumDeletedLines(final int lines) {
        this.numDeletedLines.set(lines);
    }

    void setNumAddedLines(final int lines) {
        this.numAddedLines.set(lines);
    }

    void setTextDiffs(final List<TextDiff> textDiffs) {
        this.textDiffs = textDiffs;
    }

    void setUserDecision(final boolean accepted) {
        this.userDecision.set((accepted) ? UserDecision.ACCEPT : UserDecision.REJECT);
    }

    //TODO: requestID and handling when they dismiss this very early
    // e.g. before submitting prompt, textDiffs DNE, etc.
    InlineChatSessionResultParams buildResultObject() {
        var userDecision = this.userDecision.get();
        var language = LanguageUtil.extractLanguageFromOpenFile();
        int inputLength = -1;
        double startLatency = -1;
        double endLatency = -1;

        if (userDecision != UserDecision.DISMISS) {
            inputLength = getPrompt().length();
            startLatency = 0.0;
            endLatency = 1.0;
        }

        int numSelectedLines = (hasActiveSelection()) ? selection.getEndLine() - selection.getStartLine() + 1 : 0;
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

        return new InlineChatSessionResultParams(
                language,
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
