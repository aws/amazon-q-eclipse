// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.texteditor.ITextEditor;

import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionItem;
import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionParams;
import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionStates;
import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionTriggerKind;
import software.aws.toolkits.eclipse.amazonq.lsp.model.LogInlineCompletionSessionResultsParams;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.views.model.InlineSuggestionCodeReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.function.Consumer;

import static software.aws.toolkits.eclipse.amazonq.util.QConstants.Q_INLINE_HINT_TEXT_STYLE;
import static software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils.getActiveTextViewer;
import static software.aws.toolkits.eclipse.amazonq.util.SuggestionTextUtil.replaceSpacesWithTabs;

public final class QInvocationSession extends QResource {

    // Static variable to hold the single instance
    private static QInvocationSession instance;

    private static final ExecutorService SESSION_PROCESSOR = Executors.newSingleThreadExecutor();
    private static volatile UUID pendingRequestId = null;
    private static final long REQUEST_TIMEOUT_SECONDS = 5;

    private volatile QInvocationSessionState state = QInvocationSessionState.INACTIVE;
    private CaretMovementReason caretMovementReason = CaretMovementReason.UNEXAMINED;
    private boolean suggestionAccepted = false;
    private final boolean isMacOS;

    private QSuggestionsContext suggestionsContext = null;
    private final ConcurrentHashMap<String, InlineCompletionStates> suggestionCompletionResults = new ConcurrentHashMap<String, InlineCompletionStates>();
    private IContextService contextService;
    private IContextActivation contextActivation;

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
    private final boolean isTabOnly = false;
    private Consumer<Integer> unsetVerticalIndent;
    private Runnable changeStatusToQuerying;
    private Runnable changeStatusToIdle;
    private Runnable changeStatusToPreviewing;
    private boolean hasSeenFirstSuggestion = false;
    private long firstSuggestionDisplayLatency;
    private final StopWatch suggestionDisplaySessionStopWatch = new StopWatch();
    private Optional<Integer> initialTypeaheadLength = Optional.empty();

    // Private constructor to prevent instantiation
    private QInvocationSession() {
        // Initialization code here
        isMacOS = System.getProperty("os.name").toLowerCase().contains("mac");
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
            contextService = PlatformUI.getWorkbench()
                    .getService(IContextService.class);
            if (contextService.getActiveContextIds().contains(Constants.INLINE_CHAT_CONTEXT_ID)) {
                Activator.getLogger().warn("Attempted to start inline session while inline chat is processing.");
                this.end();
                return false;
            }
            Activator.getLogger().info("Starting inline session");
            transitionToInvokingState();
            contextActivation = contextService.activateContext(Constants.INLINE_SUGGESTIONS_CONTEXT_ID);

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
        UUID requestId = UUID.randomUUID();
        pendingRequestId = requestId;

        SESSION_PROCESSOR.submit(() -> {
            try {
                if (requestId.equals(pendingRequestId)) {
                    CompletableFuture<Void> task = CompletableFuture.runAsync(() ->
                        processRequest(params, invocationOffset, requestId));
                    try {
                        task.get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    } catch (TimeoutException e) {
                        Activator.getLogger().warn(requestId + " inline completion request task timed out after "
                                + REQUEST_TIMEOUT_SECONDS + " seconds");
                    }
                } else {
                    Activator.getLogger().info(requestId + " skipped as newer inline completion request was queued");
                }
            } catch (InterruptedException e) {
                Activator.getLogger().info(requestId + " inline completion request interrupted");
            } catch (ExecutionException e) {
                Activator.getLogger().error("Error executing inline completion request " + requestId, e);
            }
        });
    }

    private void processRequest(final InlineCompletionParams params, final int invocationOffset, final UUID requestId) {
        try {
            Activator.getLogger().info(requestId + " inline query made at offset " + invocationOffset);

            List<InlineCompletionItem> newSuggestions = new ArrayList<>();
            String sessionId;
            long requestInvocation = System.currentTimeMillis();

            // Execute the LSP query
            var response = Activator.getLspProvider().getAmazonQServer().get()
                    .inlineCompletionWithReferences(params).get();

            sessionId = response.getSessionId();
            var suggestions = response.getItems().parallelStream()
                .map(item -> {
                    if (isTabOnly) {
                        String sanitizedText = replaceSpacesWithTabs(item.getInsertText(), tabSize);
                        item.setInsertText(sanitizedText);
                    }
                    return item;
                })
                .collect(Collectors.toList());
            newSuggestions.addAll(suggestions);

            Display.getDefault().syncExec(() -> {
                if (requestId.equals(pendingRequestId)) {
                    handleQueryResults(newSuggestions, sessionId, requestInvocation,
                                     new RequestContext(requestId, invocationOffset, params));
                } else {
                    Activator.getLogger().info(requestId + " skipped rendering as newer inline completion request queued");
                }
            });

        } catch (InterruptedException e) {
            Activator.getLogger().info(requestId + " inline completion request interrupted");
        } catch (Exception e) {
            Activator.getLogger().error("Error processing inline completion request " + requestId, e);
        }
    }

    private void handleQueryResults(final List<InlineCompletionItem> newSuggestions,
                              final String sessionId,
                              final long requestInvocation,
                              final RequestContext task) {
        if (newSuggestions == null || newSuggestions.isEmpty() || sessionId == null || sessionId.isEmpty()) {
            if (!isPreviewingSuggestions()) {
                end();
            }
            Activator.getLogger().info(task.getUuid() + " returned with no result.");
            if (task.getParams().getContext().getTriggerKind() == InlineCompletionTriggerKind.Invoke) {
                Display display = Display.getDefault();
                String message = "Q returned no suggestions";
                QEclipseEditorUtils.showToast(message, display, 2000);
            }
            return;
        }

        // Check if current caret position matches the task's invocation offset
        var viewer = getViewer();
        if (viewer == null || viewer.getTextWidget() == null) {
            end();
            return;
        }

        int currentCaretOffset = viewer.getTextWidget().getCaretOffset();
        if (currentCaretOffset != task.getInvocationOffset()) {
            Activator.getLogger().info(task.getUuid()
                + " skipped rendering as caret position changed from "
                + task.getInvocationOffset() + " to " + currentCaretOffset);
            return;
        }

        Activator.getLogger().info(task.getUuid() + " returned with " + newSuggestions.size() + " results.");

        suggestionsContext.setSessionId(sessionId);
        suggestionsContext.setRequestedAtEpoch(requestInvocation);
        suggestionsContext.getDetails()
                .addAll(newSuggestions.stream().map(QSuggestionContext::new).collect(Collectors.toList()));

        initializeSuggestionCompletionResults();

        // Handle typeahead if it exists
        int currentIdxInSuggestion = 0;
        boolean hasAMatch = false;

        if (currentCaretOffset > task.getInvocationOffset()) {
            var widget = viewer.getTextWidget();
            String prefix = widget.getTextRange(task.getInvocationOffset(),
                                              currentCaretOffset - task.getInvocationOffset());
            // Computes the typed prefix and typeahead length from when user invocation happened to
            // before suggestions are first shown in UI
            initialTypeaheadLength = Optional.of(prefix.length());

            for (int i = 0; i < newSuggestions.size(); i++) {
                if (newSuggestions.get(i).getInsertText().startsWith(prefix)) {
                    currentIdxInSuggestion = i;
                    hasAMatch = true;
                    break;
                }
            }
            // indicates that typeahead prefix does not match any suggestions
            if (!hasAMatch) {
                // all suggestions filtered out, mark them as discarded
                updateCompletionStates(new ArrayList<String>());
                end();
                return;
            }

            // if typeahead exists, mark all suggestions except for current suggestion index with match as discarded
            var currentSuggestion = suggestionsContext.getDetails().get(currentIdxInSuggestion);
            var filteredSuggestions = List.of(currentSuggestion.getInlineCompletionItem().getItemId());
            updateCompletionStates(filteredSuggestions);
        }

        invocationOffset = task.getInvocationOffset();
        suggestionsContext.setCurrentIndex(currentIdxInSuggestion);
        transitionToPreviewingState();
        attachListeners();
        primeListeners();
        getViewer().getTextWidget().redraw();
    }

    /*
     *  Updates completion state of each suggestion in the `suggestionCompletionResult` map, given the updated filtered suggestion list
     * @param filteredSuggestions
     */
    public void updateCompletionStates(final List<String> filteredSuggestions) {
        // Mark all suggestions as unseen and discarded
        suggestionCompletionResults.values().forEach(item -> {
            item.setSeen(false);
            item.setDiscarded(true);
        });

        // Reset discarded state for filtered suggestions
        filteredSuggestions.forEach(itemId -> {
            var item = suggestionCompletionResults.get(itemId);
            item.setDiscarded(false);
        });
    }

    /*
     * Mark all suggestions as unseen and not discarded
     */
    public void resetCompletionStates() {
        suggestionCompletionResults.values().forEach(item -> {
            item.setSeen(false);
            item.setDiscarded(false);
        });
    }

    private void initializeSuggestionCompletionResults() {
        suggestionsContext.getDetails().forEach(suggestion -> suggestionCompletionResults
                .put(suggestion.getInlineCompletionItem().getItemId(), new InlineCompletionStates()));
    }

    public void setAccepted(final String suggestionId) {
        var item =  Optional.ofNullable(suggestionCompletionResults.get(suggestionId));
        item.ifPresent(result -> result.setAccepted(true));
    }

    /*
     * When a suggestion is seen, marks it and starts display timer
     */
    public void markSuggestionAsSeen() {
        var index = suggestionsContext.getCurrentIndex();
        var suggestion = suggestionsContext.getDetails().get(index);
        var item = Optional
                .ofNullable(suggestionCompletionResults.get(suggestion.getInlineCompletionItem().getItemId()));
        item.ifPresent(result -> {
            result.setSeen(true);
            // if this was the first suggestion displayed, start suggestion session display timer and record first suggestion's display latency
            if (!hasSeenFirstSuggestion) {
                suggestionDisplaySessionStopWatch.start();
                firstSuggestionDisplayLatency = System.currentTimeMillis() - suggestionsContext.getRequestedAtEpoch();
                hasSeenFirstSuggestion = true;
            }
        });
    }

    // Method to end the session
    public void end() {
        if (isActive()) {
            if (state == QInvocationSessionState.SUGGESTION_PREVIEWING) {
                int lastKnownLine = getLastKnownLine();
                unsetVerticalIndent(lastKnownLine + 1);
            }
            if (changeStatusToIdle != null) {
                changeStatusToIdle.run();
            }
            // Deactivate context
            if (contextService != null && contextActivation != null) {
                contextService.deactivateContext(contextActivation);
                contextActivation = null;
            }
            dispose();
            state = QInvocationSessionState.INACTIVE;
            Activator.getLogger().info("Session ended");
        }
    }


    /*
     *  Usually called when user attempts to cancel the suggestion session such as by a diverging typeahead or hitting escape
     */
    public void endImmediately() {
        if (isActive()) {
            if (state == QInvocationSessionState.SUGGESTION_PREVIEWING) {
                int lastKnownLine = getLastKnownLine();
                unsetVerticalIndent(lastKnownLine + 1);
            }
            if (changeStatusToIdle != null) {
                changeStatusToIdle.run();
            }
            // Deactivate context
            if (contextService != null && contextActivation != null) {
                contextService.deactivateContext(contextActivation);
                contextActivation = null;
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

    private synchronized void transitionToPreviewingState() {
        assert state == QInvocationSessionState.INVOKING;
        state = QInvocationSessionState.SUGGESTION_PREVIEWING;
        if (changeStatusToPreviewing != null) {
            changeStatusToPreviewing.run();
        }
    }

    private void transitionToInvokingState() {
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
        if (suggestionsContext != null && suggestionsContext.getNumberOfSuggestions() > 1) {
            suggestionsContext.decrementIndex();
            primeListeners();
            getViewer().getTextWidget().redraw();
        }
    }

    public void incrementCurentSuggestionIndex() {
        if (suggestionsContext != null && suggestionsContext.getNumberOfSuggestions() > 1) {
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

    void setVerticalIndent(final int line, final int height) {
        var widget = viewer.getTextWidget();
        widget.setLineVerticalIndent(line, height);
        unsetVerticalIndent = (caretLine) -> {
            widget.setLineVerticalIndent(caretLine, 0);
        };
    }

    void unsetVerticalIndent(final int caretLine) {
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

    private void primeListeners() {
        inputListener.onNewSuggestion();
        paintListener.onNewSuggestion();
        markSuggestionAsSeen();
    }

    public int getLastKnownLine() {
        return ((QInlineCaretListener) caretListener).getLastKnownLine();
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

    public boolean isMacOS() {
        return isMacOS;
    }

    /*
     * Sends inline completion results for current suggestion session over to lsp for use with telemetry
     */
    private void sendSessionCompletionResult() {
        try {
            if (StringUtils.isEmpty(suggestionsContext.getSessionId()) || suggestionsContext.getDetails() == null
                    || suggestionsContext.getDetails().isEmpty()) {
                return;
            }
            if (suggestionDisplaySessionStopWatch.isStarted()) {
                suggestionDisplaySessionStopWatch.stop();
            }

            final ConcurrentHashMap<String, InlineCompletionStates> completionStatesCopy = new ConcurrentHashMap<String, InlineCompletionStates>();
            // Explicitly copy the map contents
            for (Map.Entry<String, InlineCompletionStates> entry : suggestionCompletionResults.entrySet()) {
                completionStatesCopy.put(String.valueOf(entry.getKey()), entry.getValue());
            }

            var result = new LogInlineCompletionSessionResultsParams(suggestionsContext.getSessionId(),
                    completionStatesCopy);
            if (firstSuggestionDisplayLatency > 0L) {
                result.setFirstCompletionDisplayLatency(firstSuggestionDisplayLatency);
            }

            var sessionTime = suggestionDisplaySessionStopWatch.getTime(TimeUnit.MILLISECONDS);
            if (sessionTime > 0L) {
                result.setTotalSessionDisplayTime(sessionTime);
            }

            if (initialTypeaheadLength.isPresent()) {
                result.setTypeaheadLength(initialTypeaheadLength.get());
            }
            sendCompletionSessionResult(result);
        } catch (Exception e) {
            Activator.getLogger()
            .error("Error occurred when sending suggestion completion results to Amazon Q language server", e);
        }
    }

    private void sendCompletionSessionResult(final LogInlineCompletionSessionResultsParams result)
            throws InterruptedException, ExecutionException {
        ThreadingUtils.executeAsyncTask(() -> {
            Activator.getLspProvider().getAmazonQServer().thenAccept(lsp -> lsp.logInlineCompletionSessionResult(result));
        });
    }

    private void resetSessionResultParams() {
        hasSeenFirstSuggestion = false;
        firstSuggestionDisplayLatency = 0L;
        suggestionDisplaySessionStopWatch.reset();
        suggestionCompletionResults.clear();
        initialTypeaheadLength = Optional.empty();
    }

    @Override
    public void dispose() {
        var widget = viewer.getTextWidget();
        sendSessionCompletionResult();
        suggestionsContext = null;
        resetSessionResultParams();
        if (inlineTextFont != null) {
            inlineTextFont.dispose();
        }
        if (inlineTextFontBold != null) {
            inlineTextFontBold.dispose();
        }
        inlineTextFont = null;
        inlineTextFontBold = null;
        caretMovementReason = CaretMovementReason.UNEXAMINED;
        if (inputListener != null) {
            inputListener.beforeRemoval();
            widget.removeVerifyKeyListener(inputListener);
            widget.removeMouseListener(inputListener);
        }
        if (terminationListener != null) {
            widget.removeFocusListener(terminationListener);
        }
        if (paintListener != null) {
            paintListener.beforeRemoval();
            widget.removePaintListener(paintListener);
        }
        if (caretListener != null) {
            widget.removeCaretListener(caretListener);
        }
        Display.getDefault().asyncExec(() -> {
            if (!widget.isDisposed()) {
                widget.redraw();
                widget.update();
            }
        });
        paintListener = null;
        caretListener = null;
        inputListener = null;
        terminationListener = null;
        invocationOffset = -1;
        editor = null;
        viewer = null;
        suggestionAccepted = false;
    }

    private static final class RequestContext {
        private final UUID uuid;
        private final int invocationOffset;
        private final InlineCompletionParams params;

        RequestContext(final UUID uuid, final int invocationOffset, final InlineCompletionParams params) {
            this.uuid = uuid;
            this.invocationOffset = invocationOffset;
            this.params = params;
        }

        public UUID getUuid() {
            return uuid;
        }

        public int getInvocationOffset() {
            return invocationOffset;
        }

        public InlineCompletionParams getParams() {
            return params;
        }
    }
}
