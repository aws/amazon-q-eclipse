// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.ITextEditor;

import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionItem;
import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionParams;
import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionTriggerKind;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.views.model.InlineSuggestionCodeReference;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.function.Consumer;

import static software.aws.toolkits.eclipse.amazonq.util.QConstants.Q_INLINE_HINT_TEXT_STYLE;
import static software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils.getActiveTextViewer;
import static software.aws.toolkits.eclipse.amazonq.util.SuggestionTextUtil.replaceSpacesWithTabs;

public final class QInvocationSession extends QResource {

    // Static variable to hold the single instance
    private static QInvocationSession instance;

    private volatile QInvocationSessionState state = QInvocationSessionState.INACTIVE;
    private CaretMovementReason caretMovementReason = CaretMovementReason.UNEXAMINED;
    private boolean suggestionAccepted = false;

    private QSuggestionsContext suggestionsContext = null;

    private ITextEditor editor = null;
    private ITextViewer viewer = null;
    private Font inlineTextFont = null;
    private Font inlineTextFontBold = null;
    private int invocationOffset = -1;
    private int tabSize;
    private QInlineRendererListener paintListener = null;
    private CaretListener caretListener = null;
    private QInlineInputListener inputListener = null;
    private QInlineTerminationListener terminationListener = null;
    private int[] headOffsetAtLine = new int[500];
    private boolean isTabOnly = false;
    private Consumer<Integer> unsetVerticalIndent;
    private ConcurrentHashMap<UUID, Future<?>> unresolvedTasks = new ConcurrentHashMap<>();
    private Runnable changeStatusToQuerying;
    private Runnable changeStatusToIdle;
    private Runnable changeStatusToPreviewing;

    // Private constructor to prevent instantiation
    private QInvocationSession() {
        // Initialization code here
    }

    // Method to get the single instance
    public static synchronized QInvocationSession getInstance() {
        if (instance == null) {
            instance = new QInvocationSession();
        }
        return instance;
    }

    // TODO: separation of concerns between session attributes, session management,
    // and remote invocation logic
    // Method to start the session
    public synchronized boolean start(final ITextEditor editor) throws ExecutionException {
        if (!isActive()) {
            if (!Activator.getLoginService().getAuthState().isLoggedIn()) {
                Activator.getLogger().warn("Attempted to start inline session while logged out.");
                this.end();
                return false;
            }
            Activator.getLogger().info("Starting inline session");
            transitionToInvokingState();

            // Start session logic here
            this.editor = editor;
            viewer = getActiveTextViewer(editor);
            if (viewer == null) {
                // cannot continue the invocation
                throw new IllegalStateException("no viewer available");
            }

            var widget = viewer.getTextWidget();
            terminationListener = QEclipseEditorUtils.getInlineTerminationListener();
            widget.addFocusListener(terminationListener);

            suggestionsContext = new QSuggestionsContext();
            inlineTextFont = QEclipseEditorUtils.getInlineTextFont(widget, Q_INLINE_HINT_TEXT_STYLE);
            inlineTextFontBold = QEclipseEditorUtils.getInlineCloseBracketFontBold(widget);
            invocationOffset = widget.getCaretOffset();

            return true;
        } else {
            return false;
        }
    }

    private void attachListeners() {
        var widget = this.viewer.getTextWidget();

        paintListener = new QInlineRendererListener();
        widget.addPaintListener(paintListener);

        inputListener = QEclipseEditorUtils.getInlineInputListener(widget);
        widget.addVerifyKeyListener(inputListener);
        widget.addMouseListener(inputListener);

        caretListener = new QInlineCaretListener(widget);
        widget.addCaretListener(caretListener);
    }

    public void invoke(final int invocationOffset, final int inputLength) {
        var session = QInvocationSession.getInstance();

        try {
            int adjustedInvocationOffset = QEclipseEditorUtils.getOffsetInFullyExpandedDocument(viewer,
                    invocationOffset) + inputLength;
            var params = InlineCompletionUtils.cwParamsFromContext(session.getEditor(), session.getViewer(),
                    adjustedInvocationOffset, InlineCompletionTriggerKind.Automatic);
            queryAsync(params, invocationOffset + inputLength);
        } catch (BadLocationException e) {
            Activator.getLogger().error("Unable to compute inline completion request from document", e);
        }
    }

    public void invoke() {
        var session = QInvocationSession.getInstance();

        try {
            int adjustedInvocationOffset = QEclipseEditorUtils.getOffsetInFullyExpandedDocument(viewer,
                    session.getInvocationOffset());
            var params = InlineCompletionUtils.cwParamsFromContext(session.getEditor(), session.getViewer(),
                    adjustedInvocationOffset, InlineCompletionTriggerKind.Invoke);
            queryAsync(params, session.getInvocationOffset());
        } catch (BadLocationException e) {
            Activator.getLogger().error("Unable to compute inline completion request from document", e);
        }
    }

    private synchronized void queryAsync(final InlineCompletionParams params, final int invocationOffset) {
        var uuid = UUID.randomUUID();
        long invocationTimeInMs = System.currentTimeMillis();
        Activator.getLogger().info(uuid + " queried made at " + invocationOffset);
        var future = ThreadingUtils.executeAsyncTaskAndReturnFuture(() -> {
            try {
                var session = QInvocationSession.getInstance();

                List<InlineCompletionItem> newSuggestions = Activator.getLspProvider().getAmazonQServer().get()
                        .inlineCompletionWithReferences(params)
                        .thenApply(result -> result.getItems().parallelStream().map(item -> {
                            if (isTabOnly) {
                                String sanitizedText = replaceSpacesWithTabs(item.getInsertText(), tabSize);
                                item.setInsertText(sanitizedText);
                            }
                            return item;
                        }).collect(Collectors.toList())).get();
                unresolvedTasks.remove(uuid);

                Display.getDefault().asyncExec(() -> {
                    long curTimeInMs = System.currentTimeMillis();
                    long timeUsedInMs = curTimeInMs - invocationTimeInMs;
                    if (newSuggestions == null || newSuggestions.isEmpty()) {
                        if (!session.isPreviewingSuggestions()) {
                            end();
                        }
                        Activator.getLogger()
                                .info(uuid + " returned with no result. Time used: " + timeUsedInMs + " ms");
                        if (params.getContext().getTriggerKind() == InlineCompletionTriggerKind.Invoke) {
                            Display display = Display.getDefault();
                            String message = "Q returned no suggestions";
                            QEclipseEditorUtils.showToast(message, display, 2000);
                        }
                        return;
                    } else {
                        Activator.getLogger().info(uuid + " returned with " + newSuggestions.size()
                                + " results. Time used: " + timeUsedInMs + " ms");
                    }

                    // If the caret positions has moved on from the invocation offset, we need to
                    // see if there exists in the suggestions fetched
                    // one more suggestions that qualify for what has been typed since the
                    // invocation.
                    // Note that we should not remove the ones that have been disqualified by the
                    // content typed since the user might still want to explore them.
                    int currentIdxInSuggestion = 0;
                    boolean hasAMatch = false;
                    var viewer = session.getViewer();
                    if (viewer == null || viewer.getTextWidget() == null || viewer.getTextWidget().getCaretOffset() < invocationOffset) {
                        end();
                        return;
                    }

                    if (viewer != null && viewer.getTextWidget() != null && viewer.getTextWidget().getCaretOffset() > invocationOffset) {
                        var widget = viewer.getTextWidget();
                        int currentOffset = widget.getCaretOffset();
                        for (int i = 0; i < newSuggestions.size(); i++) {
                            String prefix = widget.getTextRange(invocationOffset, currentOffset - invocationOffset);
                            if (newSuggestions.get(i).getInsertText().startsWith(prefix)) {
                                currentIdxInSuggestion = i;
                                hasAMatch = true;
                                break;
                            }
                        }
                        if (invocationOffset != currentOffset && !hasAMatch) {
                            end();
                            return;
                        }
                    }

                    session.invocationOffset = invocationOffset;

                    suggestionsContext.getDetails()
                            .addAll(newSuggestions.stream().map(QSuggestionContext::new).collect(Collectors.toList()));

                    suggestionsContext.setCurrentIndex(currentIdxInSuggestion);

                    session.transitionToPreviewingState();
                    attachListeners();
                    session.primeListeners();
                    session.getViewer().getTextWidget().redraw();
                });
            } catch (InterruptedException e) {
                Activator.getLogger().error("Inline completion interrupted", e);
            } catch (ExecutionException e) {
                Activator.getLogger().error("Error executing inline completion", e);
            }
        });
        unresolvedTasks.put(uuid, future);
    }

    // Method to end the session
    public void end() {
        if (isActive() && unresolvedTasks.isEmpty()) {
            if (state == QInvocationSessionState.SUGGESTION_PREVIEWING) {
                int lastKnownLine = getLastKnownLine();
                unsetVerticalIndent(lastKnownLine + 1);
            }
            if (changeStatusToIdle != null) {
                changeStatusToIdle.run();
            }
            dispose();
            state = QInvocationSessionState.INACTIVE;
            Activator.getLogger().info("Session ended");
        }
    }

    public void endImmediately() {
        if (isActive()) {
            if (state == QInvocationSessionState.SUGGESTION_PREVIEWING) {
                int lastKnownLine = getLastKnownLine();
                unsetVerticalIndent(lastKnownLine + 1);
            }
            if (changeStatusToIdle != null) {
                changeStatusToIdle.run();
            }
            dispose();
            state = QInvocationSessionState.INACTIVE;
            Activator.getLogger().info("Session ended forcifully");
        }
    }

    // Method to check if session is active
    public boolean isActive() {
        return state != QInvocationSessionState.INACTIVE;
    }

    public boolean isPreviewingSuggestions() {
        return state == QInvocationSessionState.SUGGESTION_PREVIEWING;
    }

    public boolean isDecisionMade() {
        return state == QInvocationSessionState.DECISION_MADE;
    }

    public synchronized void transitionToPreviewingState() {
        assert state == QInvocationSessionState.INVOKING;
        state = QInvocationSessionState.SUGGESTION_PREVIEWING;
        if (changeStatusToPreviewing != null) {
            changeStatusToPreviewing.run();
        }
    }

    public void transitionToInvokingState() {
        assert state == QInvocationSessionState.INACTIVE;
        state = QInvocationSessionState.INVOKING;
        if (changeStatusToQuerying != null) {
            changeStatusToQuerying.run();
        }
    }

    public void transitionToDecisionMade() {
        var widget = viewer.getTextWidget();
        var caretLine = widget.getLineAtOffset(widget.getCaretOffset());
        transitionToDecisionMade(caretLine + 1);
    }

    public void transitionToDecisionMade(final int line) {
        if (state != QInvocationSessionState.SUGGESTION_PREVIEWING) {
            return;
        }
        state = QInvocationSessionState.DECISION_MADE;
        unsetVerticalIndent(line);
    }

    public void setSuggestionAccepted(final boolean suggestionAccepted) {
        this.suggestionAccepted = suggestionAccepted;
    }

    public boolean getSuggestionAccepted() {
        return suggestionAccepted;
    }

    public void setCaretMovementReason(final CaretMovementReason reason) {
        this.caretMovementReason = reason;
    }

    public void setHeadOffsetAtLine(final int lineNum, final int offSet) throws IllegalArgumentException {
        if (lineNum >= headOffsetAtLine.length || lineNum < 0) {
            throw new IllegalArgumentException("Problematic index given");
        }
        headOffsetAtLine[lineNum] = offSet;
    }

    public Font getInlineTextFont() {
        return inlineTextFont;
    }

    public int getInvocationOffset() {
        return invocationOffset;
    }

    public ITextEditor getEditor() {
        return editor;
    }

    public ITextViewer getViewer() {
        return viewer;
    }

    public synchronized QInvocationSessionState getState() {
        return state;
    }

    public CaretMovementReason getCaretMovementReason() {
        return caretMovementReason;
    }

    public int getHeadOffsetAtLine(final int lineNum) throws IllegalArgumentException {
        if (lineNum >= headOffsetAtLine.length || lineNum < 0) {
            throw new IllegalArgumentException("Problematic index given");
        }
        return headOffsetAtLine[lineNum];
    }

    public InlineCompletionItem getCurrentSuggestion() {
        if (suggestionsContext == null) {
            Activator.getLogger().warn("QSuggestion context is null");
            return null;
        }
        var details = suggestionsContext.getDetails();
        var index = suggestionsContext.getCurrentIndex();
        if (details.isEmpty() && index != -1 || !details.isEmpty() && (index < 0 || index >= details.size())) {
            Activator.getLogger().warn("QSuggestion context index is incorrect");
            return null;
        }
        var detail = details.get(index);
        if (detail.getState() == QSuggestionState.DISCARD) {
            throw new IllegalStateException("QSuggestion showing discarded suggestions");
        }

        return details.get(index).getInlineCompletionItem();
    }

    public int getNumberOfSuggestions() {
        return suggestionsContext.getNumberOfSuggestions();
    }

    public int getCurrentSuggestionNumber() {
        return suggestionsContext.getCurrentIndex() + 1;
    }

    public void decrementCurrentSuggestionIndex() {
        if (suggestionsContext != null) {
            suggestionsContext.decrementIndex();
            primeListeners();
            getViewer().getTextWidget().redraw();
        }
    }

    public void incrementCurentSuggestionIndex() {
        if (suggestionsContext != null) {
            suggestionsContext.incrementIndex();
            primeListeners();
            getViewer().getTextWidget().redraw();
        }
    }

    public boolean hasBeenTypedahead() {
        return getInvocationOffset() != getViewer().getTextWidget().getCaretOffset();
    }

    public void executeCallbackForCodeReference() {
        var selectedSuggestion = getCurrentSuggestion();
        var widget = viewer.getTextWidget();
        int startLine = widget.getLineAtOffset(invocationOffset);

        var references = selectedSuggestion.getReferences();
        var suggestionText = selectedSuggestion.getInsertText();
        var filename = instance.getEditor().getTitle();
        InlineSuggestionCodeReference codeReference = new InlineSuggestionCodeReference(references, suggestionText, filename, startLine);

        Activator.getCodeReferenceLoggingService().log(codeReference);
    }

    public void setVerticalIndent(final int line, final int height) {
        var widget = viewer.getTextWidget();
        widget.setLineVerticalIndent(line, height);
        unsetVerticalIndent = (caretLine) -> {
            widget.setLineVerticalIndent(caretLine, 0);
        };
    }

    public void unsetVerticalIndent(final int caretLine) {
        if (unsetVerticalIndent != null) {
            unsetVerticalIndent.accept(caretLine);
            unsetVerticalIndent = null;
        }
    }

    public List<IQInlineSuggestionSegment> getSegments() {
        return inputListener.getSegments();
    }

    public int getNumSuggestionLines() {
        return inputListener.getNumSuggestionLines();
    }

    public int getOutstandingPadding() {
        return inputListener.getOutstandingPadding();
    }

    public void primeListeners() {
        inputListener.onNewSuggestion();
        paintListener.onNewSuggestion();
    }

    public int getLastKnownLine() {
        return ((QInlineCaretListener) caretListener).getLastKnownLine();
    }

    public void awaitAllUnresolvedTasks() throws ExecutionException {
        List<Future<?>> tasks = unresolvedTasks.values().stream().toList();
        for (Future<?> future : tasks) {
            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                // Propagate the execution exception
                throw e;
            }
        }
    }

    public void assignQueryingCallback(final Runnable runnable) {
        changeStatusToQuerying = runnable;
    }

    public void assignIdlingCallback(final Runnable runnable) {
        changeStatusToIdle = runnable;
    }

    public void assignPreviewingCallback(final Runnable runnable) {
        changeStatusToPreviewing = runnable;
    }

    public Font getBoldInlineFont() {
        return inlineTextFontBold;
    }

    // Additional methods for the session can be added here
    @Override
    public void dispose() {
        var widget = viewer.getTextWidget();

        suggestionsContext = null;
        if (inlineTextFont != null) {
            inlineTextFont.dispose();
        }
        if (inlineTextFontBold != null) {
            inlineTextFontBold.dispose();
        }
        inlineTextFont = null;
        inlineTextFontBold = null;
        caretMovementReason = CaretMovementReason.UNEXAMINED;
        unresolvedTasks.forEach((uuid, task) -> {
            boolean cancelled = task.cancel(true);
            if (cancelled) {
                Activator.getLogger().info(uuid + " cancelled.");
            } else {
                Activator.getLogger().error(uuid + " failed to cancel.");
            }
        });
        unresolvedTasks.clear();
        if (inputListener != null) {
            inputListener.beforeRemoval();
            widget.removeVerifyKeyListener(inputListener);
            widget.removeMouseListener(inputListener);
        }
        if (terminationListener != null) {
            widget.removeFocusListener(terminationListener);
        }
        QInvocationSession.getInstance().getViewer().getTextWidget().redraw();
        if (paintListener != null) {
            paintListener.beforeRemoval();
            widget.removePaintListener(paintListener);
        }
        if (caretListener != null) {
            widget.removeCaretListener(caretListener);
        }
        paintListener = null;
        caretListener = null;
        inputListener = null;
        terminationListener = null;
        invocationOffset = -1;
        editor = null;
        viewer = null;
        suggestionAccepted = false;
    }
}
