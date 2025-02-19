package software.aws.toolkits.eclipse.amazonq.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
//import org.eclipse.swt.events.FocusAdapter;
//import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
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
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.widgets.Display;
import org.eclipse.text.undo.IDocumentUndoManager;
import org.eclipse.text.undo.DocumentUndoManagerRegistry;
import org.eclipse.jface.text.IDocument;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;

import software.aws.toolkits.eclipse.amazonq.chat.ChatCommunicationManager;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatPrompt;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatRequestParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatResult;
import software.aws.toolkits.eclipse.amazonq.chat.models.CursorState;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils;
import software.aws.toolkits.eclipse.amazonq.views.model.Command;

public class QInlineChatSession2 implements KeyListener, ChatUiRequestListener {
    
    // Session state enum
    private enum SessionState {
        INACTIVE, ACTIVE, GENERATING
    }

    // Session state variables
    private static QInlineChatSession2 instance;
    private volatile SessionState currentState = SessionState.INACTIVE;
    private ITextEditor editor = null;
    private ITextViewer viewer = null;
    private StyledText widget = null;
    private final int MAX_INPUT_LENGTH = 512;
    private final String GENERATING_POPUP_MSG = "Amazon Q is generating...";
    private final String USER_DECISION_POPUP_MSG = "Accept: Tab, Reject: Esc";
    
    private PopupDialog popup;
    private final ChatCommunicationManager chatCommunicationManager;
    private ChatRequestParams params;
    
    private long lastUpdateTime;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> pendingUpdate;
    private static final int BATCH_DELAY_MS = 200;
    
    private String previousPartialResponse = null;
    private String originalCode;
    private int originalSelectionStart;
    private int previousDisplayLength;
    private boolean isFirstPartialResult = true;
    
    private IDocumentUndoManager undoManager;
    private IDocument document;
    private boolean isCompoundChange = false;
    
    public static synchronized QInlineChatSession2 getInstance() {
        if (instance == null) {
            instance = new QInlineChatSession2();
        }
        return instance;
    }
    
    public QInlineChatSession2() {
        chatCommunicationManager = ChatCommunicationManager.getInstance();
        chatCommunicationManager.setChatUiRequestListener(this);
    }

    public synchronized boolean isSessionActive() {
        return currentState != SessionState.INACTIVE;
    }

    private synchronized void setSessionState(final SessionState newState) {
        Activator.getLogger().info("UPDATING SESSION STATE: " + newState);
        this.currentState = newState;
    }
    
    public boolean startSession(final ITextEditor editor) {

        if (isSessionActive()) {
            return false;
        }
        if (editor == null || !(editor instanceof ITextEditor)) {
            return false;
        }

        try {
            setSessionState(SessionState.ACTIVE);
            this.editor = (ITextEditor) editor;
            this.viewer = getActiveTextViewer(editor);
            this.widget = viewer.getTextWidget();
            this.document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
            this.undoManager = DocumentUndoManagerRegistry.getDocumentUndoManager(document);
            this.lastUpdateTime = System.currentTimeMillis();

            Display.getDefault().asyncExec(() -> {

                final var selection = (ITextSelection) editor.getSelectionProvider().getSelection();
                originalSelectionStart = selection.getOffset();
                Activator.getLogger().info(String.format("SELECTION OFFSET: %d", originalSelectionStart));
                originalCode = selection.getText();

                // Show user input dialog
                onUserInput(originalSelectionStart);
            });
            return true;
        } catch (final Exception e) {
            endSession();
            return false;
        }
    }
    
    public void onUserInput(final int selectionOffset) {
        if (popup != null) {
            popup.close();
        }

        popup = new PopupDialog(widget.getShell(), PopupDialog.INFOPOPUP_SHELLSTYLE, false, false, true, false, false,
                null, null) {
            private Point screenLocation;
            private Text inputField;

            @Override
            public boolean close() {
                Activator.getLogger().info("Closing input box!");
                if (currentState != SessionState.GENERATING) {
                    popup = null;
                    endSession();
                }
                return super.close();
            }

            @Override
            protected Point getInitialLocation(final Point initialSize) {
                if (screenLocation == null) {
                    final Point location = widget.getLocationAtOffset(selectionOffset);
                    location.y -= widget.getLineHeight() * 1.1;
                    screenLocation = Display.getCurrent().map(widget, null, location);
                }
                return screenLocation;
            }

            @Override
            protected Control createDialogArea(final Composite parent) {
                final var composite = (Composite) super.createDialogArea(parent);
                composite.setLayout(new GridLayout(1, false));

                final var titleLabel = new Label(composite, SWT.NONE);
                titleLabel.setText("Enter instructions for Amazon Q");
                titleLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

                inputField = new Text(composite, SWT.BORDER | SWT.MULTI | SWT.WRAP);
                final GridData gridData = new GridData(GridData.FILL_BOTH);
                gridData.heightHint = 80;
                gridData.widthHint = 80;
                inputField.setLayoutData(gridData);

                final var instructionsLabel = new Label(composite, SWT.NONE);
                instructionsLabel.setText("Press Enter to confirm, Esc to cancel");
                instructionsLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

                inputField.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyPressed(final KeyEvent e) {
                        // on enter, accept the input added
                        if (e.character == SWT.CR || e.character == SWT.LF) {
                            final var userInput = inputField.getText();
                            if (userInputIsValid(userInput)) {
                                setSessionState(SessionState.GENERATING);
                                close();
                                final var cursorState = getSelectionRangeCursorState().get();
                                final var prompt = new ChatPrompt(userInput, userInput, "");

                                final var filePath = "C:\\Users\\somerandomusername\\Desktop\\lsp.txt";
                                final var id = UUID.randomUUID().toString();
                                params = new ChatRequestParams(id, prompt, new TextDocumentIdentifier(filePath),
                                        Arrays.asList(cursorState));

                                chatCommunicationManager.sendMessageToChatServer(Command.CHAT_SEND_PROMPT, params);
                            }
                        }
                    }
                });

                inputField.setFocus();
                return composite;
            }
        };

        popup.setBlockOnOpen(true);
        popup.open();
    }
    
    private boolean userInputIsValid(final String input) {
        return input != null && input.length() > 5 && input.length() < MAX_INPUT_LENGTH;
    }
    
    //state.gen
    //
    
    @Override
    public void onSendToChatUi(final String message) {
        try {
            // Deserialize object
            final ObjectMapper mapper = new ObjectMapper();
            final var rootNode = mapper.readTree(message);
            final var paramsNode = rootNode.get("params");
            
            // Isolate chat result info
            final var isPartialResult = rootNode.get("isPartialResult").asBoolean();
            final var chatResult = mapper.treeToValue(paramsNode, ChatResult.class);
            
            if (isFirstPartialResult && !isCompoundChange) {
                undoManager.beginCompoundChange();
                isCompoundChange = true;
            }
            
            // Apply the changes
            try {
                undoManager.connect(document);
                
                // Your existing update logic here
                updateUI(chatResult, isPartialResult);
                
                if (!isPartialResult && isCompoundChange) {
                    // End of streaming message
                    undoManager.endCompoundChange();
                    isCompoundChange = false;
                }
            } finally {
                undoManager.disconnect(document);
            }
            
        } catch (Exception e) {
            if (isCompoundChange) {
                undoManager.endCompoundChange();
                isCompoundChange = false;
            }
            Activator.getLogger().error("Failed to handle ChatResult", e);
            endSession();
        }
    }

    private void updateUI(final ChatResult chatResult, boolean isPartialResult) {
        Display.getDefault().asyncExec(() -> {
            insertWithInlineDiffUtils(originalCode, chatResult.body(), originalSelectionStart);
            previousPartialResponse = chatResult.body();
            
            if (!isPartialResult) {
                final KeyAdapter keyHandler = createUserDecisionKeyHandler(widget);
                showPopup(originalSelectionStart, keyHandler, USER_DECISION_POPUP_MSG);
            }
        });
    }
    
    private void insertWithInlineDiffUtils(final String originalCode, final String newCode, final int offset) {

        // Annotation model provides highlighting for the diff additions/deletions
        final IAnnotationModel annotationModel = editor.getDocumentProvider()
                .getAnnotationModel(editor.getEditorInput());

        try {
            // Clear existing diff annotations prior to starting new diff
            clearDiffAnnotations(annotationModel);

            // Determine length to clear in editor window
            final int replaceLen = (previousPartialResponse == null) ? originalCode.length() : previousDisplayLength;

            // Restore document to original state
            document.replace(offset, replaceLen, originalCode);

            // Split original and new code into lines for diff comparison
            final String[] originalLines = originalCode.lines().toArray(String[]::new);
            final String[] newLines = newCode.lines().toArray(String[]::new);

            // Diff generation --> returns Patch object which contains deltas for each line
            final Patch<String> patch = DiffUtils.diff(Arrays.asList(originalLines), Arrays.asList(newLines));

            final StringBuilder resultText = new StringBuilder();
            final List<Position> deletedPositions = new ArrayList<>();
            final List<Position> addedPositions = new ArrayList<>();
            int currentPos = 0;
            int currentLine = 0;

            for (final AbstractDelta<String> delta : patch.getDeltas()) {
                // Continuously copy unchanged lines until we hit a diff
                while (currentLine < delta.getSource().getPosition()) {
                    resultText.append(originalLines[currentLine]).append("\n");
                    currentPos += originalLines[currentLine].length() + 1;
                    currentLine++;
                }

                final List<String> originalChangedLines = delta.getSource().getLines();
                final List<String> newChangedLines = delta.getTarget().getLines();

                // Handle deleted lines and mark position
                for (final String line : originalChangedLines) {
                    resultText.append(line).append("\n");
                    final Position position = new Position(offset + currentPos, line.length());
                    deletedPositions.add(position);
                    currentPos += line.length() + 1;
                }

                // Handle added lines and mark position
                for (final String line : newChangedLines) {
                    resultText.append(line).append("\n");
                    final Position position = new Position(offset + currentPos, line.length());
                    addedPositions.add(position);
                    currentPos += line.length() + 1;
                }

                currentLine = delta.getSource().getPosition() + delta.getSource().size();
            }
            // Loop through remaining unchanged lines
            while (currentLine < originalLines.length) {
                resultText.append(originalLines[currentLine]).append("\n");
                currentPos += originalLines[currentLine].length() + 1;
                currentLine++;
            }

            final String finalText = resultText.toString();

            // Clear existing annotations in the affected rangee
            clearAnnotationsInRange(annotationModel, offset, offset + originalCode.length());

            // Apply new diff text
            document.replace(offset, originalCode.length(), finalText);

            // Store rendered text length for proper clearing next iteration
            previousDisplayLength = finalText.length();

            // Add new annotations for this diff
            for (final Position position : deletedPositions) {
                final Annotation annotation = new Annotation("diffAnnotation.deleted", false, "Deleted Code");
                annotationModel.addAnnotation(annotation, position);
            }

            for (final Position position : addedPositions) {
                final Annotation annotation = new Annotation("diffAnnotation.added", false, "Added Code");
                annotationModel.addAnnotation(annotation, position);
            }

        } catch (final BadLocationException e) {
            Activator.getLogger().error("Failed to insert inline diff", e);
        }
    }
    
    public void showPopup(final int selectionOffset, final KeyAdapter keyHandler, final String message) {
        if (popup != null) {
            popup.close();
            if (popup.getShell() != null && !popup.getShell().isDisposed()) {
                popup.getShell().dispose();
            }
            popup = null;
        }

        popup = new PopupDialog(widget.getShell(), PopupDialog.HOVER_SHELLSTYLE, true, false, false, false, false, null,
                null) {
            private Point screenLocation;

            @Override
            protected Point getInitialLocation(final Point initialSize) {
                if (screenLocation == null) {
                    final Point location = widget.getLocationAtOffset(selectionOffset);
                    location.y -= widget.getLineHeight() * 1.1;
                    screenLocation = Display.getCurrent().map(widget, null, location);
                }
                return screenLocation;
            }

            @Override
            protected Control createDialogArea(final Composite parent) {
                final Composite composite = (Composite) super.createDialogArea(parent);
                composite.setLayout(new GridLayout(1, false));

                final Label infoLabel = new Label(composite, SWT.NONE);
                infoLabel.setLayoutData(new GridData(GridData.FILL_BOTH));
                infoLabel.setText(message);

                return composite;
            }

            @Override
            protected void configureShell(final Shell shell) {
                super.configureShell(shell);

                // Handle both shell deactivation and window switching
                shell.addListener(SWT.Deactivate, event -> {
                    Display.getCurrent().asyncExec(() -> {
                        // Check if our window is still the active one
                        if (!shell.isDisposed() && (PlatformUI.getWorkbench().getActiveWorkbenchWindow() == null ||
                        // Check if the active editor has changed
                                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                                        .getActiveEditor() != editor)) {
                            close();
                        }
                    });
                });
                // Ensure popup is owned by the editor window
                shell.setParent(widget.getShell());
            }
        };

        if (keyHandler != null) {
            widget.addKeyListener(keyHandler);
        }

        popup.open();
    }
    
    KeyAdapter createUserDecisionKeyHandler(final StyledText widget) {
        return new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                if (e.keyCode == SWT.TAB || e.keyCode == SWT.ESC) { // Accept changes
                    boolean accepted = (e.keyCode == SWT.TAB);
                    onUserDecision(accepted);
                } else {
                    endSession();
                }
                cleanupKeyHandler();
            }

            private void cleanupKeyHandler() {
                widget.removeKeyListener(this);
            }
        };
    }
    
    private void onUserDecision(boolean accepted) {
        try {
            if (accepted) {
                handleAccepted();
                undoManager.endCompoundChange();
            } else {
                if (undoManager.undoable()) {
                    undoManager.undo();
                }
                isCompoundChange = false;
            }
        } catch (Exception e) {
            Activator.getLogger().error("Failed to handle user decision", e);
        } finally {
            endSession();
        }
    }
    
    private void handleAccepted() {
        final IAnnotationModel annotationModel = editor.getDocumentProvider()
                .getAnnotationModel(editor.getEditorInput());
        try {
            document.replace(originalSelectionStart, previousDisplayLength, previousPartialResponse);

            // Clear all diff annotations
            clearDiffAnnotations(annotationModel);
        } catch (final Exception e) {
            Activator.getLogger().error("Error while accepting changes", e);
        } finally {
            endSession();
        }
    }
    
    public synchronized void endSession() {
        try {
            // Reset all session-specific variables           
            if (this.widget != null && !this.widget.isDisposed()) {
                this.widget = null;
            }
            this.originalCode = null;
            this.originalSelectionStart = 0;
            this.viewer = null;
            this.editor = null;
            this.document = null;
            this.undoManager = null;
            this.isCompoundChange = false;
            this.lastUpdateTime = 0;
            
        } catch (Exception e) {
            // Log any errors that occur during cleanup
            Activator.getLogger().error("Error while ending session: " + e.getMessage(), e);
        } finally {
            setSessionState(SessionState.INACTIVE);
        }
    }
    
    private void clearDiffAnnotations(final IAnnotationModel annotationModel) {
        final var annotations = annotationModel.getAnnotationIterator();
        while (annotations.hasNext()) {
            final var annotation = annotations.next();
            final String type = annotation.getType();
            if (type.startsWith("diffAnnotation.")) {
                annotationModel.removeAnnotation(annotation);
            }
        }
    }
    
    private void clearAnnotationsInRange(final IAnnotationModel model, final int start, final int end) {
        final Iterator<Annotation> iterator = model.getAnnotationIterator();
        while (iterator.hasNext()) {
            final Annotation annotation = iterator.next();
            final Position position = model.getPosition(annotation);
            if (position != null && position.offset >= start && position.offset + position.length <= end) {
                model.removeAnnotation(annotation);
            }
        }
    }
    
    public static ITextViewer asTextViewer(final IEditorPart editorPart) {
        return editorPart != null ? editorPart.getAdapter(ITextViewer.class) : null;
    }

    public static ITextViewer getActiveTextViewer(final ITextEditor editor) {
        return asTextViewer(editor);
    }
    
    protected Optional<CursorState> getSelectionRangeCursorState() {
        final AtomicReference<Optional<Range>> range = new AtomicReference<>();
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                range.set(QEclipseEditorUtils.getActiveSelectionRange());
            }
        });

        return range.get().map(CursorState::new);
    }
    
    @Override
    public void keyPressed(final KeyEvent e) {
        if (e.keyCode == SWT.ESC && popup != null) {
            popup.close();
        }
    }

    @Override
    public void keyReleased(final KeyEvent arg0) {
        // TODO Auto-generated method stub

    }


}
