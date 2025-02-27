package software.aws.toolkits.eclipse.amazonq.inlineChat;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.lsp4j.Range;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import software.aws.toolkits.eclipse.amazonq.chat.models.CursorState;
import software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils;

public class InlineChatUIManager {
    private PopupDialog inputBox;
    private Shell promptShell;
    private final ITextViewer viewer;
    private final int MAX_INPUT_LENGTH = 128;
    private final InlineChatTask task;
    private final String GENERATING_MESSAGE = "Amazon Q is generating...";
    private final String DECIDING_MESSAGE = "Accept (Tab) | Reject (Esc)";

    public InlineChatUIManager(final InlineChatTask task) {
        this.task = task;
        this.viewer = task.getEditor().getAdapter(ITextViewer.class);
    }

    public CompletableFuture<InlineChatTask> showUserInputPrompt() {
        CompletableFuture<InlineChatTask> future = new CompletableFuture<>();
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
                        // Get the vertical position
                        Point location = widget.getLocationAtOffset(task.getOffset());
                        location.y -= widget.getLineHeight() * 1.1;

                        // Get editor bounds
                        Rectangle editorBounds = widget.getBounds();

                        // Center the popup horizontally within the editor
                        location.x = (editorBounds.width / 2) - (initialSize.x / 2);

                        // Convert the final position to screen coordinates
                        screenLocation = Display.getCurrent().map(widget, null, location);
                    }
                    return screenLocation;
                }

                @Override
                protected Control createDialogArea(final Composite parent) {
                    var composite = (Composite) super.createDialogArea(parent);
                    composite.setLayout(new GridLayout(1, false));

                    var titleLabel = new Label(composite, SWT.CENTER);
                    titleLabel.setText("Enter instructions for Amazon Q");
                    GridData titleGridData = new GridData(GridData.FILL_HORIZONTAL);
                    titleGridData.horizontalAlignment = GridData.CENTER;
                    titleLabel.setLayoutData(titleGridData);

                    inputField = new Text(composite, SWT.BORDER | SWT.SINGLE);
                    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
                    gridData.widthHint = 350;
                    gridData.heightHint = 20;
                    inputField.setLayoutData(gridData);

                    // Enforce maximum character count that can be entered into the input
                    inputField.addVerifyListener(e -> {
                        String currentText = inputField.getText();
                        String newText = currentText.substring(0, e.start) + e.text + currentText.substring(e.end);
                        if (newText.length() > MAX_INPUT_LENGTH) {
                            e.doit = false; // Prevent the input
                        }
                    });

                    var instructionsLabel = new Label(composite, SWT.CENTER);
                    instructionsLabel.setText("Press Enter to confirm, Esc to cancel");
                    GridData instructionsGridData = new GridData(GridData.FILL_HORIZONTAL);
                    instructionsGridData.horizontalAlignment = GridData.CENTER;
                    instructionsLabel.setLayoutData(instructionsGridData);

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
                                    future.complete(task);
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

            promptShell = new Shell(Display.getDefault().getActiveShell(), SWT.TOOL | SWT.NO_TRIM);
            promptShell.setLayout(new GridLayout(1, false));

            Label promptLabel = new Label(promptShell, SWT.NONE);
            promptLabel.setText(promptText);
            promptLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
            promptLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_INFO_FOREGROUND));

            promptShell.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
            promptShell.pack();

            // Position the shell
            Point location = widget.getLocationAtOffset(task.getOffset());
            location.y -= widget.getLineHeight() * 2;
            Point screenLocation = Display.getCurrent().map(widget, null, location);
            promptShell.setLocation(screenLocation);

            promptShell.setVisible(true);
        });
    }

    void transitionToGeneratingPrompt() {
        showPrompt(GENERATING_MESSAGE);
    }

    void transitionToDecidingPrompt() {
        showPrompt(DECIDING_MESSAGE);
    }

    void closePrompt() {
        if (promptShell != null && !promptShell.isDisposed()) {
            Display.getDefault().syncExec(() -> {
                promptShell.dispose();
                promptShell = null;
            });
        }
    }

    void cleanupState() {
        closePrompt();
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

}

