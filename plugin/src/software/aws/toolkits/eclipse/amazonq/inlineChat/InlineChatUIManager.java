package software.aws.toolkits.eclipse.amazonq.inlineChat;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.text.ITextViewer;
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
import software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils;
import software.aws.toolkits.eclipse.amazonq.util.ToolkitNotification;

public class InlineChatUIManager {

    // State variables
    private static InlineChatUIManager instance;
    private InlineChatTask task;

    // UI elements
    private PopupDialog inputBox;
    private ITextViewer viewer;
    private final int MAX_INPUT_LENGTH = 128;
    private PaintListener currentPaintListener;
    private final String INPUT_PROMPT_MESSAGE = "Enter instructions for Amazon Q (Enter | Esc)";
    private final String GENERATING_MESSAGE = "Amazon Q is generating...";
    private final String DECIDING_MESSAGE = "Accept (Enter) | Reject (Esc)";
    private boolean isDarkTheme;

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
                            int indentedOffset = calculateIndentOffset(widget, task.getCaretOffset());
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

                    inputField = new Text(composite, SWT.BORDER | SWT.SINGLE);
                    inputField.setMessage(INPUT_PROMPT_MESSAGE);
                    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
                    gridData.widthHint = 350;
                    inputField.setLayoutData(gridData);

                    // Enforce maximum character count that can be entered into the input
                    inputField.addVerifyListener(e -> {
                        String currentText = inputField.getText();
                        String newText = currentText.substring(0, e.start) + e.text + currentText.substring(e.end);
                        if (newText.length() > MAX_INPUT_LENGTH) {
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
                    // Disposal before future completes indicates user hit ESC to cancel
                    getShell().addListener(SWT.Dispose, e -> {
                        if (!future.isDone()) {
                            future.complete(null);
                        }
                    });
                    return composite;
                }
            };

            inputBox.setBlockOnOpen(true);
            inputBox.open();
        });
        return future;
    }

    private void showPrompt(final String promptText) {
        closePrompt();
        Display.getDefault().asyncExec(() -> {
            var widget = viewer.getTextWidget();
            try {
                currentPaintListener = createPaintListenerPrompt(widget, task.getCaretOffset(), promptText, isDarkTheme);
                widget.addPaintListener(currentPaintListener);
                widget.redraw();
            } catch (Exception e) {
                Activator.getLogger().error("Failed to create paint listener: " + e.getMessage(), e);
            }
        });
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
        showPrompt(GENERATING_MESSAGE);
    }

    void transitionToDecidingPrompt() {
        showPrompt(DECIDING_MESSAGE);
    }

    void closePrompt() {
        removeCurrentPaintListener();
    }

    private void removeCurrentPaintListener() {
        try {
            if (viewer != null && !viewer.getTextWidget().isDisposed() && currentPaintListener != null) {
                Display.getDefault().syncExec(() -> {
                    viewer.getTextWidget().removePaintListener(currentPaintListener);
                    viewer.getTextWidget().redraw();
                    currentPaintListener = null;
                });
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
        return input != null && input.length() >= 2 && input.length() < MAX_INPUT_LENGTH;
    }

    Optional<CursorState> getSelectionRangeCursorState() {
        AtomicReference<Optional<Range>> range = new AtomicReference<Optional<Range>>();
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                range.set(QEclipseEditorUtils.getActiveSelectionRange());
            }
        });

        return range.get().map(CursorState::new);
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

    private void showNotification(final String notificationBody) {
        Display.getDefault().asyncExec(() -> {
            var notification = new ToolkitNotification(Display.getCurrent(),
                    Constants.INLINE_CHAT_NOTIFICATION_TITLE,
                    notificationBody);
            notification.open();
        });
    }

    // Method not in use, leaving in case reintroduced in future
    void restoreSelection() {
        if (task.getEditor() != null && task.hasActiveSelection()) {
            task.getEditor().getSelectionProvider().setSelection(task.getSelection());
        }
    }

}

