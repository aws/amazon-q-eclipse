// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionItem;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;

import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.function.Consumer;

import static software.aws.toolkits.eclipse.amazonq.util.QConstants.Q_INLINE_HINT_TEXT_STYLE;
import static software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils.getActiveTextViewer;
import static software.aws.toolkits.eclipse.amazonq.util.SuggestionTextUtil.replaceSpacesWithTabs;

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
    private int tabSize;
    private long invocationTimeInMs = -1L;
    private QInlineRendererListener paintListener = null;
    private CaretListener caretListener = null;
    private QInlineInputListener inputListener = null;
    private Stack<String> closingBrackets = new Stack<>();
    private int[] headOffsetAtLine = new int[500];
    private boolean hasBeenTypedahead = false;
    private boolean isTabOnly = false;
    private CodeReferenceAcceptanceCallback codeReferenceAcceptanceCallback = null;
    private Consumer<Integer> unsetVerticalIndent;

    // Private constructor to prevent instantiation
    private QInvocationSession() {
        // Initialization code here
    }

    // Method to get the single instance
    public static synchronized QInvocationSession getInstance() {
        if (instance == null) {
            instance = new QInvocationSession();
            IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode("org.eclipse.jdt.ui");
            boolean isBracesSetToAutoClose = preferences.getBoolean("closeBraces", true);
            boolean isBracketsSetToAutoClose = preferences.getBoolean("closeBrackets", true);
            boolean isStringSetToAutoClose = preferences.getBoolean("closeStrings", true);

            // We'll also need tab sizes since suggestions do not take that into account
            // and is only given in spaces
            IEclipsePreferences tabPref = InstanceScope.INSTANCE.getNode("org.eclipse.jdt.core");
            instance.tabSize = tabPref.getInt("org.eclipse.jdt.core.formatter.tabulation.size", 4);
            instance.isTabOnly = tabPref.getBoolean("use_tabs_only_for_leading_indentations", true);

            PlatformUI.getWorkbench().addWorkbenchListener(new IWorkbenchListener() {
                @Override
                public boolean preShutdown(final IWorkbench workbench, final boolean forced) {
                    preferences.putBoolean("closeBraces", isBracesSetToAutoClose);
                    preferences.putBoolean("closeBrackets", isBracketsSetToAutoClose);
                    preferences.putBoolean("closeStrings", isStringSetToAutoClose);
                    return true;
                }

                @Override
                public void postShutdown(final IWorkbench workbench) {
                    return;
                }
            });
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

            inputListener = new QInlineInputListener(widget);
            widget.addVerifyListener(inputListener);
            widget.addVerifyKeyListener(inputListener);
            widget.addMouseListener(inputListener);

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
                    if (!DefaultLoginService.getInstance().getLoginDetails().get().getIsLoggedIn()) {
                        this.end();
                        return;
                    } else {
                        DefaultLoginService.getInstance().updateToken();
                    }

                    List<InlineCompletionItem> newSuggestions = LspProvider.getAmazonQServer().get()
                            .inlineCompletionWithReferences(params)
                            .thenApply(result -> result.getItems().parallelStream().map(item -> {
                                if (isTabOnly) {
                                    String sanitizedText = replaceSpacesWithTabs(item.getInsertText(), tabSize);
                                    System.out.println("Sanitized text: " + sanitizedText.replace("\n", "\\n").replace("\t", "\\t"));
                                    item.setInsertText(sanitizedText);
                                }
                                return item;
                            }).collect(Collectors.toList())).get();

                    Display.getDefault().asyncExec(() -> {
                        if (newSuggestions == null || newSuggestions.isEmpty() || session
                                .getInvocationOffset() != session.getViewer().getTextWidget().getCaretOffset()) {
                            end();
                            return;
                        }

                        suggestionsContext.getDetails().addAll(
                                newSuggestions.stream().map(QSuggestionContext::new).collect(Collectors.toList()));

                        suggestionsContext.setCurrentIndex(0);
                        session.primeListeners();

                        // TODO: remove print
                        // Update the UI with the results
                        System.out.println("Suggestions: " + newSuggestions.stream()
                                .map(suggestion -> suggestion.getInsertText()).collect(Collectors.toList()));
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
            dispose();
            state = QInvocationSessionState.INACTIVE;
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

    public QInvocationSessionState getState() {
        return state;
    }

    public CaretMovementReason getCaretMovementReason() {
        return caretMovementReason;
    }

    public Stack<String> getClosingBrackets() {
        return closingBrackets;
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

    public void setHasBeenTypedahead(final boolean hasBeenTypedahead) {
        this.hasBeenTypedahead = hasBeenTypedahead;
    }

    public boolean hasBeenTypedahead() {
        return hasBeenTypedahead;
    }

    public void registerCallbackForCodeReference(
            final CodeReferenceAcceptanceCallback codeReferenceAcceptanceCallback) {
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

    public void primeListeners() {
        inputListener.onNewSuggestion();
    }

    public int getLastKnownLine() {
        return ((QInlineCaretListener) caretListener).getLastKnownLine();
    }

    // Additional methods for the session can be added here
    @Override
    public void dispose() {
        var widget = viewer.getTextWidget();

        suggestionsContext = null;
        inlineTextFont.dispose();
        inlineTextFont = null;
        closingBrackets = null;
        caretMovementReason = CaretMovementReason.UNEXAMINED;
        hasBeenTypedahead = false;
        inputListener.beforeRemoval();
        QInvocationSession.getInstance().getViewer().getTextWidget().redraw();
        widget.removePaintListener(paintListener);
        widget.removeCaretListener(caretListener);
        widget.removeVerifyListener(inputListener);
        widget.removeVerifyKeyListener(inputListener);
        widget.removeMouseListener(inputListener);
        paintListener = null;
        caretListener = null;
        inputListener = null;
        invocationOffset = -1;
        invocationTimeInMs = -1L;
        editor = null;
        viewer = null;
    }
}
