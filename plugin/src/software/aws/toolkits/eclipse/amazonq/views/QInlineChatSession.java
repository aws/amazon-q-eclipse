package software.aws.toolkits.eclipse.amazonq.views;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.text.undo.DocumentUndoEvent;
import org.eclipse.text.undo.DocumentUndoManagerRegistry;
import org.eclipse.text.undo.IDocumentUndoListener;
import org.eclipse.text.undo.IDocumentUndoManager;

import software.aws.toolkits.eclipse.amazonq.chat.models.ChatRequestParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatResult;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatPrompt;
import software.aws.toolkits.eclipse.amazonq.chat.models.CursorState;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.texteditor.ITextEditor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;

import software.aws.toolkits.eclipse.amazonq.chat.ChatCommunicationManager;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils;
import software.aws.toolkits.eclipse.amazonq.util.ThemeDetector;
import software.aws.toolkits.eclipse.amazonq.views.model.Command;

import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;

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
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;

public final class QInlineChatSession implements KeyListener, ChatUiRequestListener {

    // Session state enum
    private enum SessionState {
        INACTIVE,
        ACTIVE,
        GENERATING,
        DECIDING
    }

    // Session state variables
	private static QInlineChatSession instance;
	private ChatCommunicationManager chatCommunicationManager;
	private ChatRequestParams params;
	private volatile SessionState currentState = SessionState.INACTIVE;
	private ITextEditor editor = null;
    private ITextViewer viewer = null;
    private IDocumentUndoManager undoManager;
    private IDocumentUndoListener undoListener;
    private IDocument document;
    private boolean isCompoundChange = false;
	private final int MAX_INPUT_LENGTH = 128;

	// Annotation coloring variables
    private ThemeDetector themeDetector;
	private String ANNOTATION_ADDED;
	private String ANNOTATION_DELETED;

	// Diff generation and rendering variables
	private String previousPartialResponse = null;
	private String originalCode;
	private int originalSelectionStart;
	private int previousDisplayLength;

	// Batching variables
	private ScheduledExecutorService executor;
	private ScheduledFuture<?> pendingUpdate;
	private static final int BATCH_DELAY_MS = 200;
	private long lastUpdateTime;

	// User input and prompt variables
	private PopupDialog inputBox;
	private Shell promptShell;
	private final String GENERATING_POPUP_MSG = "Amazon Q is generating...";
	private final String USER_DECISION_POPUP_MSG = "Accept (Tab) | Reject (Esc)";
	
	// Context handler variables
    private IContextService contextService;
    private IContextActivation contextActivation;
    private final String CONTEXT_ID = "org.eclipse.ui.inlineChatContext";

    public QInlineChatSession() {
        chatCommunicationManager = ChatCommunicationManager.getInstance();
        // TODO: Update ChatCommunicationManager to track a list of listeners as opposed to tracking only one
        // For this startSession call to successful, the chat panel must be closed so that there is a single listener registered
        chatCommunicationManager.setChatUiRequestListener(this);
        themeDetector = new ThemeDetector();
    }

    public static synchronized QInlineChatSession getInstance() {
    	if (instance == null) {
    		instance = new QInlineChatSession();
    	}
    	return instance;
    }

    public synchronized boolean isSessionActive() {
    	return currentState != SessionState.INACTIVE;
    }
    public synchronized boolean isDeciding() {
        return currentState == SessionState.DECIDING;
    }
    public synchronized boolean isGenerating() {
        return currentState == SessionState.GENERATING;
    }
    private synchronized void setSessionState(final SessionState newState) {
        this.currentState = newState;
    }
    private synchronized SessionState getSessionState() {
        return currentState;
    }
    
    public void initUndoManager(IDocument document) {
        try {
            undoManager.disconnect(document);
        } catch (Exception e) {
            // undoManager wasn't connected
        }
        undoManager.connect(document);
        undoManager.beginCompoundChange();
        isCompoundChange = true;
        setupUndoDetection(document);
    }

    public boolean startSession(final ITextEditor editor) {

    	if (isSessionActive()) {
    		return false;
    	}
        if (editor == null || !(editor instanceof ITextEditor)) {
            return false;
        }

        try {
            
            // Get the context service and activate inline chat context
            contextService = PlatformUI.getWorkbench()
                    .getService(IContextService.class);
            contextActivation = contextService.activateContext(CONTEXT_ID);
            
            // Set session variables
        	setSessionState(SessionState.ACTIVE);
        	this.editor = (ITextEditor) editor;
            this.viewer = getActiveTextViewer(editor);
            this.document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
            this.undoManager = DocumentUndoManagerRegistry.getDocumentUndoManager(document);
        	this.lastUpdateTime = System.currentTimeMillis();
        	this.executor = Executors.newSingleThreadScheduledExecutor();
        	
        	initUndoManager(this.document);

        	this.ANNOTATION_ADDED = "diffAnnotation.added";
        	this.ANNOTATION_DELETED = "diffAnnotation.deleted";

        	// Change diff colors to dark mode settings if necessary
        	if (themeDetector.isDarkTheme()) {
        	    this.ANNOTATION_ADDED += ".dark";
        	    this.ANNOTATION_DELETED += ".dark";
        	}

            Display.getDefault().asyncExec(() -> {
                final var selection = (ITextSelection) editor.getSelectionProvider().getSelection();
                originalSelectionStart = selection.getOffset();
                Activator.getLogger().info(String.format("SELECTION OFFSET: %d", originalSelectionStart));
                originalCode = selection.getText();

                // Show user input dialog
                showUserInputBox(originalSelectionStart);
            });
            return true;
        } catch (Exception e) {
            Activator.getLogger().error("SESSION ENDING AT START: " + e.getMessage(), e);
        	endSession();
        	return false;
        }
    }
    public void showUserInputBox(final int selectionOffset) {
        if (!isSessionActive()) {
            return;
        }
        if (inputBox != null) {
            inputBox.close();
        }
        var widget = viewer.getTextWidget();

        inputBox = new PopupDialog(widget.getShell(), PopupDialog.INFOPOPUP_SHELLSTYLE, false, false, true, false, false,
                null, null) {
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
            public boolean close() {
                Activator.getLogger().info("Closing input box!");
                if (getSessionState() != SessionState.GENERATING) {
                    inputBox = null;
                    endSession();
                }
                return super.close();
            }


            @Override
            protected Point getInitialLocation(final Point initialSize) {
                if (screenLocation == null) {
                    // Get the vertical position
                    Point location = widget.getLocationAtOffset(selectionOffset);
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
                        // on enter, accept the input added
                        if (e.character == SWT.CR || e.character == SWT.LF) {
                            try {
                                var userInput = inputField.getText();
                                if (userInputIsValid(userInput)) {
                                    setSessionState(SessionState.GENERATING);
                                    var cursorState = getSelectionRangeCursorState().get();
                                    var prompt = new ChatPrompt(userInput, userInput, "");

                                    var filePath = "C:\\Users\\somerandomusername\\Desktop\\lsp.txt";
                                    var id = UUID.randomUUID().toString();
                                    params = new ChatRequestParams(id, prompt, new TextDocumentIdentifier(filePath), Arrays.asList(cursorState));

                                    chatCommunicationManager.sendMessageToChatServer(Command.CHAT_SEND_PROMPT, params);
                                    // TODO: instead show the progress indicator that Q is thinking untill the full response comes
                                    showPrompt(GENERATING_POPUP_MSG, selectionOffset);
                                }
                            } catch (Exception ex) {
                                Activator.getLogger().error("Error while transitioning to generating state: " + ex.getMessage(), ex);
                                endSession();
                            } finally {
                                close();
                            }
                        }
                    }
                });
                return composite;
            }
        };

        inputBox.setBlockOnOpen(true);
        inputBox.open();
    }
    
    @Override
    public void onSendToChatUi(final String message) {
        if (!isSessionActive()) {
            return;
        }
        try {
            
            // Deserialize object
            ObjectMapper mapper = new ObjectMapper();
            var rootNode = mapper.readTree(message);
            var paramsNode = rootNode.get("params");
            
            // Check and pass through error message if server returns exception
            if (rootNode.has("commandName") && "errorMessage".equals(rootNode.get("commandName").asText())) {
                String errorMessage = (paramsNode != null && paramsNode.has("message"))
                        ? paramsNode.get("message").asText()
                        : "Unknown error occurred";
                throw new RuntimeException("Error returned by server: " + errorMessage);
            }
            
            var isPartialResult = rootNode.get("isPartialResult").asBoolean();
            var chatResult = mapper.treeToValue(paramsNode, ChatResult.class);

            if (isPartialResult) {
                // Only process if content has changed
                if (!chatResult.body().equals(previousPartialResponse)) {
                    if (pendingUpdate != null) {
                        pendingUpdate.cancel(false);
                    }

                    // Calculate remaining time since last UI update
                    long currentTime = System.currentTimeMillis();
                    long timeSinceUpdate = currentTime - lastUpdateTime;

                    if (timeSinceUpdate >= BATCH_DELAY_MS) {
                        Activator.getLogger().info("Immediate update: " + timeSinceUpdate + "ms since last update");
                        // Push update immediately if enough time has passed
                        updateUI(chatResult, isPartialResult);
                        lastUpdateTime = currentTime;
                    } else {
                        // Calculate remaining batch delay and schedule update
                        long delayToUse = BATCH_DELAY_MS - timeSinceUpdate;
                        Activator.getLogger().info("Scheduled update: waiting " + delayToUse + "ms");
                        pendingUpdate = executor.schedule(() -> {
                            Activator.getLogger().info("Executing scheduled update after " + (System.currentTimeMillis() - lastUpdateTime) + "ms delay");
                            updateUI(chatResult, isPartialResult);
                            lastUpdateTime = System.currentTimeMillis();
                        }, delayToUse, TimeUnit.MILLISECONDS);
                    }

                }
            } else {
                // Final result - always update UI state regardless of content
                updateUI(chatResult, isPartialResult);
                lastUpdateTime = System.currentTimeMillis();
            }
        } catch (Exception e) {
            Activator.getLogger().error("Failed to handle ChatResult", e);
            restoreAndEndSession();
        }
    }
    
    private void updateUI(final ChatResult chatResult, final boolean isPartialResult) {
        Display.getDefault().syncExec(() -> {
            try {
                var newCode = unescapeChatResult(chatResult.body());
                boolean success = generateAndInsertDiff(originalCode, newCode, originalSelectionStart);
                if (success && !isPartialResult) {
                    setSessionState(SessionState.DECIDING);
                    showPrompt(USER_DECISION_POPUP_MSG, originalSelectionStart);
                }
            } catch (Exception e) {
                restoreAndEndSession();
            }
        });
    }
    
    private boolean generateAndInsertDiff(final String originalCode, final String newCode, final int offset) throws Exception{
        if (!isSessionActive()) {
            return false;
        }

        // Annotation model provides highlighting for the diff additions/deletions
        IAnnotationModel annotationModel = editor.getDocumentProvider()
                .getAnnotationModel(editor.getEditorInput());
        var document = editor.getDocumentProvider().getDocument(editor.getEditorInput());

        try {
            // Clear existing diff annotations prior to starting new diff
            clearDiffAnnotations(annotationModel);

            // Determine length to clear in editor window
            final int replaceLen = (previousPartialResponse == null)
                    ? originalCode.length()
                    : previousDisplayLength;

            // Restore document to original state
            document.replace(offset, replaceLen, originalCode);

            // Split original and new code into lines for diff comparison
            String[] originalLines = originalCode.lines().toArray(String[]::new);
            String[] newLines = newCode.lines().toArray(String[]::new);

            // Diff generation --> returns Patch object which contains deltas for each line
            Patch<String> patch = DiffUtils.diff(Arrays.asList(originalLines), Arrays.asList(newLines));

            StringBuilder resultText = new StringBuilder();
            List<Position> deletedPositions = new ArrayList<>();
            List<Position> addedPositions = new ArrayList<>();
            int currentPos = 0;
            int currentLine = 0;

            for (AbstractDelta<String> delta : patch.getDeltas()) {
                // Continuously copy unchanged lines until we hit a diff
                while (currentLine < delta.getSource().getPosition()) {
                    resultText.append(originalLines[currentLine]).append("\n");
                    currentPos += originalLines[currentLine].length() + 1;
                    currentLine++;
                }

                List<String> originalChangedLines = delta.getSource().getLines();
                List<String> newChangedLines = delta.getTarget().getLines();

                // Handle deleted lines and mark position
                for (String line : originalChangedLines) {
                    resultText.append(line).append("\n");
                    Position position = new Position(offset + currentPos, line.length());
                    deletedPositions.add(position);
                    currentPos += line.length() + 1;
                }

                // Handle added lines and mark position
                for (String line : newChangedLines) {
                    resultText.append(line).append("\n");
                    Position position = new Position(offset + currentPos, line.length());
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
            for (Position position : deletedPositions) {
                Annotation annotation = new Annotation(ANNOTATION_DELETED, false, "Deleted Code");
                annotationModel.addAnnotation(annotation, position);
            }

            for (Position position : addedPositions) {
                Annotation annotation = new Annotation(ANNOTATION_ADDED, false, "Added Code");
                annotationModel.addAnnotation(annotation, position);
            }
            return true;
        } catch (Exception e) {
            Activator.getLogger().error("Failed to insert inline diff", e);
            throw e;
        } finally {
            previousPartialResponse = newCode;
        }
    }
    
    private void showPrompt(String promptText, int selectionOffset) {
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
            Point location = widget.getLocationAtOffset(selectionOffset);
            location.y -= widget.getLineHeight() * 2;
            Point screenLocation = Display.getCurrent().map(widget, null, location);
            promptShell.setLocation(screenLocation);
            
            promptShell.setVisible(true);
        });
    }
    public void handleAccepted() throws Exception {
        Display.getDefault().asyncExec(() -> {
            final IAnnotationModel annotationModel = editor.getDocumentProvider()
                    .getAnnotationModel(editor.getEditorInput());
            var document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
            try {
                closePrompt();
                document.replace(originalSelectionStart, previousDisplayLength, previousPartialResponse);
                clearDiffAnnotations(annotationModel);
                
                undoManager.endCompoundChange();
                endSession();
            } catch (final Exception e) {
                Activator.getLogger().error("Accepting inline chat results failed with: " + e.getMessage(), e);
                restoreAndEndSession();
            }
        });
    }
    public void handleDeclined() throws Exception {
        Display.getDefault().asyncExec(() -> {
            try {
                closePrompt();
                cleanupDocumentState(true);
                endSession();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        });
    }
    private void closePrompt() {
        if (promptShell != null && !promptShell.isDisposed()) {
            Display.getDefault().syncExec(() -> {
                promptShell.dispose();
                promptShell = null;
            });
        }
    }

    public synchronized void endSession() {
        if (!isSessionActive()) {
            return;
        }
        // Clean up batching components
        cancelBatchingOperations();
        
        // Clean up undoManager
        cleanupDocumentState(false);
        
        // Clean up context service
        cleanupContext();
        
        // Restore all session state variables to default values
        cleanupSessionState();
        
        // Set session to inactive
        setSessionState(SessionState.INACTIVE);
        
        Activator.getLogger().info("SESSION ENDED!");
    }
    
    public synchronized void restoreAndEndSession() {
        try {
            handleDeclined();
        } catch (Exception e) {
            Activator.getLogger().error("Failed to end session: " + e.getMessage(), e);
        } finally {
            endSession();
        }
    }

    public void beforeRemoval() {
        if (inputBox != null) {
            inputBox.close();
        }
    }

    public static ITextViewer asTextViewer(final IEditorPart editorPart) {
        return editorPart != null ? editorPart.getAdapter(ITextViewer.class) : null;
    }

    public static ITextViewer getActiveTextViewer(final ITextEditor editor) {
        return asTextViewer(editor);
    }

    private void clearDiffAnnotations(final IAnnotationModel annotationModel) {
        var annotations = annotationModel.getAnnotationIterator();
        while (annotations.hasNext()) {
            var annotation = annotations.next();
            String type = annotation.getType();
            if (type.startsWith("diffAnnotation.")) {
                annotationModel.removeAnnotation(annotation);
            }
        }
    }
    private void clearAnnotationsInRange(final IAnnotationModel model, final int start, final int end) {
        Iterator<Annotation> iterator = model.getAnnotationIterator();
        while (iterator.hasNext()) {
            Annotation annotation = iterator.next();
            Position position = model.getPosition(annotation);
            if (position != null
                && position.offset >= start
                && position.offset + position.length <= end) {
                model.removeAnnotation(annotation);
            }
        }
    }
    
    private boolean userInputIsValid(String input) {
    	return input != null && input.length() >= 2 && input.length() < MAX_INPUT_LENGTH;
    }

    protected Optional<CursorState> getSelectionRangeCursorState() {
        AtomicReference<Optional<Range>> range = new AtomicReference<Optional<Range>>();
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
        if (e.keyCode == SWT.ESC && inputBox != null) {
            inputBox.close();
        }
    }

    @Override
    public void keyReleased(final KeyEvent arg0) {
        // TODO Auto-generated method stub

    }

    private Optional<CursorState> insertAtCursor(final String code) {
        AtomicReference<Optional<Range>> range = new AtomicReference<Optional<Range>>();
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                range.set(QEclipseEditorUtils.insertAtCursor(code));
            }
        });
        return range.get().map(CursorState::new);
    }
    private void cancelBatchingOperations() {
        try {
            if (pendingUpdate != null) {
                pendingUpdate.cancel(true);
            }
            if (executor != null && !executor.isShutdown()) {
                executor.shutdownNow();
            }
        } catch (Exception e) {
            Activator.getLogger().error("Error cancelling async operations: " + e.getMessage(), e);
        }
    }

    private void cleanupDocumentState(boolean shouldRestoreState) {
        try {
            if (isCompoundChange) {
                if (undoManager != null) {
                    if (undoListener != null) {
                        undoManager.removeDocumentUndoListener(undoListener);
                    }
                    undoManager.endCompoundChange();
                    if (shouldRestoreState) {
                        undoManager.undo();
                    }
                    undoManager.disconnect(document);
                }
                isCompoundChange = false;
            }
        } catch (Exception e) {
            Activator.getLogger().error("Error cleaning up document state: " + e.getMessage(), e);
        }
    }
    
    // Ensure that undo operation ends session correctly 
    private void setupUndoDetection(IDocument document) {
        if (undoManager != null) {
            undoListener = new IDocumentUndoListener() {
                @Override
                public void documentUndoNotification(DocumentUndoEvent event) {
                    if (event.getEventType() == 17 && isSessionActive()) {
                        Activator.getLogger().info("Undo request being processed!");
                        if (isGenerating() || isDeciding()) {
                            closePrompt();
                            endSession();
                        }
                    }
                }
            };
            undoManager.addDocumentUndoListener(undoListener);
        }
    }

    private void cleanupContext() {
        try {
            if (contextService != null && contextActivation != null) {
                contextService.deactivateContext(contextActivation);
                contextActivation = null;
            }
        } catch (Exception e) {
            Activator.getLogger().error("Error cleaning up context: " + e.getMessage(), e);
        }
    }
    
    private void cleanupSessionState() {
        this.originalCode = null;
        this.previousPartialResponse = null;
        this.editor = null;
        this.inputBox = null;
        this.viewer = null;
        this.document = null;
        this.undoManager = null;
        this.undoListener = null;
        this.lastUpdateTime = 0;
        this.previousDisplayLength = 0;
        this.originalSelectionStart = 0;
    }

    private String unescapeChatResult(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        
        return s.replace("&quot;", "\"")
               .replace("&#39;", "'")
               .replace("&lt;", "<")
               .replace("=&lt;", "=<")
               .replace("&lt;=", "<=")
               .replace("&gt;", ">")
               .replace("=&gt;", "=>")
               .replace("&gt;=", ">=")
               .replace("&nbsp;", " ")
               .replace("&lsquo;", "'")
               .replace("&rsquo;", "'")
               .replace("&amp;", "&");
    }

}
