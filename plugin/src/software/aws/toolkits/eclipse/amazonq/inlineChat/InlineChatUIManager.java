// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.inlineChat;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import software.aws.toolkits.eclipse.amazonq.chat.models.CursorState;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.Constants;
import software.aws.toolkits.eclipse.amazonq.util.PluginPlatform;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;
import software.aws.toolkits.eclipse.amazonq.util.ToolkitNotification;

public final class InlineChatUIManager {

    // State variables
    private static InlineChatUIManager instance;
    private InlineChatTask task;

    // UI elements
    private PopupDialog inputBox;
    private ITextViewer viewer;
    private final int maxInputLength = 256;
    private PaintListener currentPaintListener;
    private final String inputPromptMessage = "Enter instructions for Amazon Q (Enter | Esc)";
    private final String generatingMessage = "Amazon Q is generating...";
    private final String decidingMessage = "Accept (Enter) | Reject (Esc)";
    private boolean isDarkTheme;
    private int latestOffset;

    private InlineChatUIManager() {
        // Prevent instantiation
    }

    public static InlineChatUIManager getInstance() {
        if (instance == null) {
            instance = new InlineChatUIManager();
        }
        return instance;
    }

    public void initNewTask(final InlineChatTask task, final boolean isDarkTheme) {
        this.task = task;
        this.viewer = task.getEditor().getAdapter(ITextViewer.class);
        this.isDarkTheme = isDarkTheme;
        this.latestOffset = 0;
    }

    public CompletableFuture<Void> showUserInputPrompt() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Display.getDefault().syncExec(() -> {
            if (inputBox != null) {
                inputBox.close();
            }

            if (viewer == null || viewer.getTextWidget() == null) {
                future.completeExceptionally(new IllegalStateException("Text widget not available"));
                return;
            }

            var widget = viewer.getTextWidget();
            inputBox = new PopupDialog(widget.getShell(), PopupDialog.INFOPOPUP_SHELLSTYLE, false, false, true, false, false, null, null) {
                private Point screenLocation;
                private Text inputField;

                @Override
                public int open() {
                    int result = super.open();
                    Display.getCurrent().asyncExec(() -> {
                        if (inputField != null && !inputField.isDisposed()) {
                            inputField.setFocus();
                        }
                    });
                    return result;
                }

                @Override
                protected Point getInitialLocation(final Point initialSize) {
                    if (screenLocation == null) {
                        try {
                            int visualOffset;
                            if (viewer instanceof ITextViewerExtension5) {
                                visualOffset = ((ITextViewerExtension5) viewer).modelOffset2WidgetOffset(task.getSelectionOffset());
                            } else if (viewer instanceof ProjectionViewer) {
                                visualOffset = ((ProjectionViewer) viewer).modelOffset2WidgetOffset(task.getSelectionOffset());
                            } else {
                                visualOffset = task.getSelectionOffset();
                            }
                            int indentedOffset = calculateIndentOffset(widget, visualOffset);
                            Point location = widget.getLocationAtOffset(indentedOffset);

                            // Move input bar up as to not block the selected code
                            location.y -= widget.getLineHeight() * 2.5;
                            screenLocation = Display.getCurrent().map(widget, null, location);
                        } catch (Exception e) {
                            Activator.getLogger().error("Exception positioning input prompt: " + e.getMessage(), e);
                            if (widget != null) {
                                Point location = widget.getLocationAtOffset(widget.getCaretOffset());
                                location.y -= widget.getLineHeight() * 2.5;
                                screenLocation = Display.getCurrent().map(widget, null, location);
                            }
                        }
                    }
                    return screenLocation;
                }

                @Override
                protected Control createDialogArea(final Composite parent) {
                    var composite = (Composite) super.createDialogArea(parent);
                    composite.setLayout(new GridLayout(1, false));

                    inputField = new Text(composite, SWT.SEARCH | SWT.BORDER | SWT.SINGLE);
                    if (PluginUtils.getPlatform() == PluginPlatform.WINDOWS) {
                        Display.getDefault().asyncExec(() -> {
                            inputField.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_GRAY));
                            inputField.setText(inputPromptMessage);
                        });

                        inputField.addKeyListener(new KeyAdapter() {
                            @Override
                            public void keyPressed(final KeyEvent e) {
                                // If this is the first character being typed
                                boolean backspace = (e.keyCode == SWT.DEL || e.keyCode == SWT.BS);
                                if (inputField.getText().equals(inputPromptMessage)) {
                                    if (!backspace) {
                                        inputField.setText("");
                                        inputField.setForeground(isDarkTheme
                                                ? Display.getDefault().getSystemColor(SWT.COLOR_WHITE)
                                                        : Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
                                    }
                                    e.doit = !backspace;
                                } else if (backspace && inputField.getText().length() <= 1) {
                                    inputField.setText(inputPromptMessage);
                                    inputField.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_GRAY));
                                }
                            }
                        });
                    } else {
                        inputField.setMessage(inputPromptMessage);
                    }

                    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
                    gridData.widthHint = 350;
                    inputField.setLayoutData(gridData);

                    // Enforce maximum character count that can be entered into the input
                    inputField.addVerifyListener(e -> {
                        String currentText = inputField.getText();
                        String newText = currentText.substring(0, e.start) + e.text + currentText.substring(e.end);
                        if (newText.length() > maxInputLength) {
                            e.doit = false; // Prevent the input
                        }
                    });

                    inputField.addKeyListener(new KeyAdapter() {
                        @Override
                        public void keyPressed(final KeyEvent e) {
                            if (e.character == SWT.CR || e.character == SWT.LF) {
                                // Gather inputs and send back to controller
                                var userInput = inputField.getText();
                                if (userInputIsValid(userInput)) {
                                    var cursorState = getSelectionRangeCursorState().get();
                                    task.setCursorState(cursorState);
                                    task.setPrompt(userInput);
                                    future.complete(null);
                                    inputBox.close();
                                }
                            }
                        }
                    });

                    // Close prompt if user scrolls away
                    widget.getDisplay().addFilter(SWT.MouseVerticalWheel, event -> {
                        close();
                        if (!future.isDone()) {
                            future.complete(null);
                        }
                    });

                    // Disposal before future completes indicates user hit ESC to cancel
                    getShell().addDisposeListener(e -> {
                        widget.getDisplay().removeFilter(SWT.MouseVerticalWheel, event -> {
                        });
                        if (!future.isDone()) {
                            future.complete(null);
                        }
                    });

                    composite.layout(true, true);
                    return composite;
                }
            };

            inputBox.setBlockOnOpen(true);
            inputBox.open();
        });
        return future;
    }

    private void showPrompt(final String promptText) {
        Display.getDefault().asyncExec(() -> {
            removeCurrentPaintListener();
            var widget = viewer.getTextWidget();
            try {
                if (viewer instanceof ITextViewerExtension5) {
                    latestOffset = ((ITextViewerExtension5) viewer).modelOffset2WidgetOffset(task.getSelectionOffset());
                } else if (viewer instanceof ProjectionViewer) {
                    latestOffset = ((ProjectionViewer) viewer).modelOffset2WidgetOffset(task.getSelectionOffset());
                } else {
                    latestOffset = task.getSelectionOffset();
                }
                currentPaintListener = createPaintListenerPrompt(widget, latestOffset, promptText, isDarkTheme);
                widget.addPaintListener(currentPaintListener);
                widget.redraw();
            } catch (Exception e) {
                Activator.getLogger().error("Failed to create paint listener: " + e.getMessage(), e);
            }
        });
    }

    public void updatePromptPosition(final SessionState state) {
        try {
            int offset = ((ITextViewerExtension5) viewer).modelOffset2WidgetOffset(task.getSelectionOffset());
            if (offset != latestOffset) {
                closePrompt();
                if (state == SessionState.GENERATING) {
                    transitionToGeneratingPrompt();
                } else {
                    transitionToDecidingPrompt();
                }
            }
        } catch (Exception e) {
            Activator.getLogger().error("Error updating prompt location", e);
            closePrompt();
        }

    }

    PaintListener createPaintListenerPrompt(final StyledText widget, final int offset, final String promptText, final boolean isDarkTheme) {
        return new PaintListener() {
            @Override
            public void paintControl(final PaintEvent event) {
                try {
                    int indentedOffset = calculateIndentOffset(widget, offset);
                    Point location = widget.getLocationAtOffset(indentedOffset);
                    Point textExtent = event.gc.textExtent(promptText);

                    // Check if selection is atop the editor
                    Rectangle clientArea = widget.getClientArea();
                    boolean hasSpaceAbove = (location.y - widget.getLineHeight() * 2) >= clientArea.y;

                    // If space above, draw above. Otherwise draw over the selected line
                    if (hasSpaceAbove) {
                        location.y -= widget.getLineHeight() * 2;
                    }
                    // If no space above, keep location.y as is

                    Color backgroundColor;
                    Color textColor;

                    // Toggle color based on editor theme
                    if (isDarkTheme) {
                        backgroundColor = new Color(Display.getCurrent(), 100, 100, 100);
                        textColor = new Color(Display.getCurrent(), 255, 255, 255);
                    } else {
                        backgroundColor = new Color(Display.getCurrent(), 230, 230, 230);
                        textColor = new Color(Display.getCurrent(), 0, 0, 0);
                    }

                    try {
                        // Draw background
                        event.gc.setBackground(backgroundColor);
                        event.gc.fillRectangle(location.x, location.y, textExtent.x + 10, textExtent.y + 10);

                        // Draw text
                        event.gc.setForeground(textColor);
                        event.gc.drawText(promptText, location.x + 5, location.y + 5, false);
                    } finally {
                        backgroundColor.dispose();
                        textColor.dispose();
                    }
                } catch (Exception e) {
                    Activator.getLogger().error("Exception rendering paint control: " + e.getMessage(), e);
                    if (widget != null) {
                        widget.removePaintListener(this);
                        widget.redraw();
                    }
                }
            }
        };

    }

    void transitionToGeneratingPrompt() {
        showPrompt(generatingMessage);
    }

    void transitionToDecidingPrompt() {
        showPrompt(decidingMessage);
    }

    void closePrompt() {
        Display.getDefault().syncExec(() -> {
            removeCurrentPaintListener();
        });
    }

    void endSession() {
        closePrompt();
        task = null;
    }

    private void removeCurrentPaintListener() {
        if (viewer == null) {
            return;
        }
        try {
            if (viewer.getTextWidget() != null && !viewer.getTextWidget().isDisposed() && currentPaintListener != null) {
                viewer.getTextWidget().removePaintListener(currentPaintListener);
                viewer.getTextWidget().redraw();
                currentPaintListener = null;
            }
        } catch (Exception e) {
            Activator.getLogger().error("Failed to remove paint listener: " + e.getMessage(), e);
        }
    }

    private int calculateIndentOffset(final StyledText widget, final int currentOffset) {
        int lineIndex = widget.getLineAtOffset(currentOffset);
        String line = widget.getLine(lineIndex);
        int lineOffset = widget.getOffsetAtLine(lineIndex);
        int linePosition = currentOffset - lineOffset;

        while (linePosition < line.length() && Character.isWhitespace(line.charAt(linePosition))) {
            linePosition++;
        }
        return lineOffset + linePosition;

    }

    private boolean userInputIsValid(final String input) {
        return input != null && input.length() >= 2 && input.length() < maxInputLength;
    }

    /**
     * Custom implementation of cursor state is needed to adjust selection range to include full lines,
     * as inline chat should not allow for partially highlighted lines to be processed. The cursor
     * position methods in QEclipseEditorUtils use the selection range from the editor.
     *
     * @see InlineChatSession#expandSelectionToFullLine
     */
    private Optional<CursorState> getSelectionRangeCursorState() {
        AtomicReference<Optional<Range>> range = new AtomicReference<Optional<Range>>();
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                range.set(getSelectionRange());
            }
        });

        return range.get().map(CursorState::new);
    }

    private Optional<Range> getSelectionRange() {
        var document = task.getEditor().getDocumentProvider().getDocument(task.getEditor().getEditorInput());
        try {
            int startOffset = task.getSelectionOffset();
            int startLine = document.getLineOfOffset(startOffset);
            int startColumn = startOffset - document.getLineOffset(startLine);

            int endOffset = startOffset + task.getOriginalCode().length();
            int endLine = document.getLineOfOffset(endOffset);
            int endColumn = endOffset - document.getLineOffset(endLine);

            var start = new Position(startLine, startColumn);
            var end = new Position(endLine, endColumn);
            return Optional.of(new Range(start, end));
        } catch (Exception e) {
            Activator.getLogger().error("Error occurred while attempting to determine selected text position in editor", e);
        }
        return Optional.empty();
    }

    void showErrorNotification() {
        showNotification(Constants.INLINE_CHAT_ERROR_NOTIFICATION_BODY);
    }

    void showCodeReferencesNotification() {
        showNotification(Constants.INLINE_CHAT_CODEREF_NOTIFICATION_BODY);
    }

    void showNoSuggestionsNotification() {
        showNotification(Constants.INLINE_CHAT_NO_SUGGESTIONS_BODY);
    }

    void showAuthExpiredNotification() {
        showNotification(Constants.INLINE_CHAT_EXPIRED_AUTH_BODY);
    }

    private void showNotification(final String notificationBody) {
        Display.getDefault().asyncExec(() -> {
            var notification = new ToolkitNotification(Display.getCurrent(),
                    Constants.INLINE_CHAT_NOTIFICATION_TITLE,
                    notificationBody);
            notification.open();
        });
    }
}

