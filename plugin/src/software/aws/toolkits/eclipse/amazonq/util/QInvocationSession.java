// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.ITextEditor;
import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionItem;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;

import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static software.aws.toolkits.eclipse.amazonq.util.QConstants.Q_INLINE_HINT_TEXT_STYLE;
import static software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils.getActiveTextViewer;

public final class QInvocationSession extends QResource {

    // Static variable to hold the single instance
    private static QInvocationSession instance;

    private QInvocationSessionState state = QInvocationSessionState.INACTIVE;
    private CaretMovementReason caretMovementReason = CaretMovementReason.UNEXAMINED;

    private QSuggestionsContext suggestionsContext = null;

    private ITextEditor editor = null;
    private ITextViewer viewer = null;
    private Font inlineTextFont = null;
    private int invocationOffset = -1;
    private long invocationTimeInMs = -1L;
    private QInlineRendererListener paintListener = null;
    private CaretListener caretListener = null;
    private VerifyKeyListener verifyKeyListener = null;
    private int leadingWhitespaceSkipped = 0;
    private Stack<Character> closingBrackets = new Stack<>();
    private boolean isLastKeyNewLine = false;
    private int[] headOffsetAtLine = new int[500];
    private boolean hasBeenTypedahead = false;
    private CodeReferenceAcceptanceCallback codeReferenceAcceptanceCallback = null;
    private Runnable unsetVerticalIndent;

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
    public synchronized boolean start(final ITextEditor editor) {
        if (!isActive()) {
            state = QInvocationSessionState.INVOKING;

            // Start session logic here
            this.editor = editor;
            viewer = getActiveTextViewer(editor);
            if (viewer == null) {
                // cannot continue the invocation
                throw new IllegalStateException("no viewer available");
            }

            var widget = viewer.getTextWidget();

            suggestionsContext = new QSuggestionsContext();
            inlineTextFont = getInlineTextFont(widget);
            invocationOffset = widget.getCaretOffset();
            invocationTimeInMs = System.currentTimeMillis();
            System.out.println("Session started.");

            var listeners = widget.getTypedListeners(SWT.Paint, QInlineRendererListener.class)
                    .collect(Collectors.toList());
            System.out.println("Current listeners for " + widget);
            listeners.forEach(System.out::println);
            if (listeners.isEmpty()) {
                paintListener = new QInlineRendererListener();
                widget.addPaintListener(paintListener);
            }

            verifyKeyListener = new QInlineVerifyKeyListener(widget);
            widget.addVerifyKeyListener(verifyKeyListener);

            caretListener = new QInlineCaretListener(widget);
            widget.addCaretListener(caretListener);

            return true;
        } else {
            System.out.println("Session is already active.");
            return false;
        }
    }

    public void invoke() {
        var session = QInvocationSession.getInstance();

        try {
            var params = InlineCompletionUtils.cwParamsFromContext(session.getEditor(), session.getViewer(),
                    session.getInvocationOffset());

            ThreadingUtils.executeAsyncTask(() -> {
                try {
                    if (!AuthUtils.isLoggedIn().get()) {
                        this.end();
                        return;
                    } else {
                        AuthUtils.updateToken().get();
                    }

                    List<InlineCompletionItem> newSuggestions = LspProvider.getAmazonQServer().get()
                            .inlineCompletionWithReferences(params).thenApply(result -> result.getItems()).get();

                    Display.getDefault().asyncExec(() -> {
                        if (newSuggestions == null || newSuggestions.isEmpty()) {
                            end();
                            return;
                        }

                        suggestionsContext.getDetails().addAll(
                                newSuggestions.stream().map(QSuggestionContext::new).collect(Collectors.toList()));

                        suggestionsContext.setCurrentIndex(0);

                        // TODO: remove print
                        // Update the UI with the results
                        System.out.println("Suggestions: " + newSuggestions);
                        System.out.println("Total suggestion number: " + newSuggestions.size());

                        transitionToPreviewingState();
                        session.getViewer().getTextWidget().redraw();
                    });
                } catch (InterruptedException e) {
                    System.out.println("Query InterruptedException: " + e.getMessage());
                    PluginLogger.error("Inline completion interrupted", e);
                } catch (ExecutionException e) {
                    System.out.println("Query ExecutionException: " + e.getMessage());
                    PluginLogger.error("Error executing inline completion", e);
                }
            });
        } catch (BadLocationException e) {
            System.out.println("BadLocationException: " + e.getMessage());
            PluginLogger.error("Unable to compute inline completion request from document", e);
        }
    }

    private Font getInlineTextFont(final StyledText widget) {
        FontData[] fontData = widget.getFont().getFontData();
        for (FontData fontDatum : fontData) {
            fontDatum.setStyle(Q_INLINE_HINT_TEXT_STYLE);
        }
        return new Font(widget.getDisplay(), fontData);
    }

    // Method to end the session
    public synchronized void end() {
        // Get the current thread's stack trace
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

        // Log the stack trace
        System.out.println("Stack trace:");
        for (StackTraceElement element : stackTraceElements) {
            System.out.println(element);
        }
        if (isActive()) {
            state = QInvocationSessionState.INACTIVE;
            dispose();
            // End session logic here
            System.out.println("Session ended.");
        } else {
            System.out.println("Session is not active.");
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

    public void transitionToPreviewingState() {
        assert state == QInvocationSessionState.INVOKING;
        state = QInvocationSessionState.SUGGESTION_PREVIEWING;
    }

    public void transitionToInvokingState() {
        assert state == QInvocationSessionState.INACTIVE;
        state = QInvocationSessionState.INVOKING;
    }

    public void transitionToInactiveState() {
        state = QInvocationSessionState.INACTIVE;
    }

    public void transitionToDecisionMade() {
        if (state != QInvocationSessionState.SUGGESTION_PREVIEWING) {
            return;
        }
        state = QInvocationSessionState.DECISION_MADE;

        unsetVerticalIndent();
    }

    public void setCaretMovementReason(final CaretMovementReason reason) {
        this.caretMovementReason = reason;
    }

    public void setLeadingWhitespaceSkipped(final int numSkipped) {
        this.leadingWhitespaceSkipped = numSkipped;
    }

    public void setIsLastKeyNewLine(final boolean isLastKeyNewLine) {
        this.isLastKeyNewLine = isLastKeyNewLine;
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

    public QInvocationSessionState getState() {
        return state;
    }

    public CaretMovementReason getCaretMovementReason() {
        return caretMovementReason;
    }

    public Stack<Character> getClosingBrackets() {
        return closingBrackets;
    }

    public int getLeadingWhitespaceSkipped() {
        return leadingWhitespaceSkipped;
    }

    public boolean isLastKeyNewLine() {
        return isLastKeyNewLine;
    }

    public int getHeadOffsetAtLine(final int lineNum) throws IllegalArgumentException {
        if (lineNum >= headOffsetAtLine.length || lineNum < 0) {
            throw new IllegalArgumentException("Problematic index given");
        }
        return headOffsetAtLine[lineNum];
    }

    public InlineCompletionItem getCurrentSuggestion() {
        if (suggestionsContext == null) {
            PluginLogger.warn("QSuggestion context is null");
            return null;
        }
        var details = suggestionsContext.getDetails();
        var index = suggestionsContext.getCurrentIndex();
        if (details.isEmpty() && index != -1 || !details.isEmpty() && (index < 0 || index >= details.size())) {
            PluginLogger.warn("QSuggestion context index is incorrect");
            return null;
        }
        var detail = details.get(index);
        if (detail.getState() == QSuggestionState.DISCARD) {
            throw new IllegalStateException("QSuggestion showing discarded suggestions");
        }

        return details.get(index).getInlineCompletionItem();
    }

    public void decrementCurrentSuggestionIndex() {
        if (suggestionsContext != null) {
            suggestionsContext.decrementIndex();
            getViewer().getTextWidget().redraw();
        }
    }

    public void incrementCurentSuggestionIndex() {
        if (suggestionsContext != null) {
            suggestionsContext.incrementIndex();
            getViewer().getTextWidget().redraw();
        }
    }

    public void setHasBeenTypedahead(final boolean hasBeenTypedahead) {
        this.hasBeenTypedahead = hasBeenTypedahead;
    }

    public boolean hasBeenTypedahead() {
        return hasBeenTypedahead;
    }

    public void registerCallbackForCodeReference(final CodeReferenceAcceptanceCallback codeReferenceAcceptanceCallback) {
        this.codeReferenceAcceptanceCallback = codeReferenceAcceptanceCallback;
    }

    public void executeCallbackForCodeReference() {
        if (codeReferenceAcceptanceCallback != null) {
            var selectedSuggestion = getCurrentSuggestion();
            var widget = viewer.getTextWidget();
            int startLine = widget.getLineAtOffset(invocationOffset);
            codeReferenceAcceptanceCallback.onCallback(selectedSuggestion, startLine);
        }
    }
  
    public void setVerticalIndent(int line, int height) {
        var widget = viewer.getTextWidget();
        widget.setLineVerticalIndent(line, height);
        unsetVerticalIndent = () -> {
            var caretLine = widget.getLineAtOffset(widget.getCaretOffset());
            widget.setLineVerticalIndent(caretLine + 1, 0);
        };
    }

    public void unsetVerticalIndent() {
        if (unsetVerticalIndent != null) {
            unsetVerticalIndent.run();
            unsetVerticalIndent = null;
        }
    }

    // Additional methods for the session can be added here
    @Override
    public void dispose() {
        var widget = viewer.getTextWidget();

        suggestionsContext = null;
        inlineTextFont.dispose();
        inlineTextFont = null;
        closingBrackets = null;
        leadingWhitespaceSkipped = 0;
        isLastKeyNewLine = false;
        caretMovementReason = CaretMovementReason.UNEXAMINED;
        hasBeenTypedahead = false;
        QInvocationSession.getInstance().getViewer().getTextWidget().redraw();
        widget.removePaintListener(paintListener);
        widget.removeCaretListener(caretListener);
        widget.removeVerifyKeyListener(verifyKeyListener);
        paintListener = null;
        caretListener = null;
        verifyKeyListener = null;
        invocationOffset = -1;
        invocationTimeInMs = -1L;
        editor = null;
        viewer = null;
    }
}
