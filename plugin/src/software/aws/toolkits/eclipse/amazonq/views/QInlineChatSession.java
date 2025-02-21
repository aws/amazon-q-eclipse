package software.aws.toolkits.eclipse.amazonq.views;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.text.undo.DocumentUndoManagerRegistry;
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

//import org.eclipse.swt.events.FocusAdapter;
//import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.text.BadLocationException;
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
    private StyledText widget = null;
    private IDocumentUndoManager undoManager;
    private IDocument document;
    private boolean isCompoundChange = false;
	private final int MAX_INPUT_LENGTH = 128;

	// Annotation coloring variables
    private ThemeDetector themeDetector;
	private String ANNOTATION_ADDED = "diffAnnotation.added";
	private String ANNOTATION_DELETED = "diffAnnotation.deleted";

	// Diff generation and rendering variables
	private String previousPartialResponse = null;
	private String originalCode;
	private int originalSelectionStart;
	private int previousDisplayLength;

	// Batching variables
	private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private ScheduledFuture<?> pendingUpdate;
	private static final int BATCH_DELAY_MS = 200;
	private long lastUpdateTime;

	// User input and prompt variables
	private PopupDialog inputBox;
	private Shell promptShell;
	private final String GENERATING_POPUP_MSG = "Amazon Q is generating...";
	private final String USER_DECISION_POPUP_MSG = "Accept: Tab, Reject: Esc";
	
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
    private synchronized void setSessionState(final SessionState newState) {
        this.currentState = newState;
    }
    
    public void initUndoManager() {
        try {
            undoManager.disconnect(document);
        } catch (Exception e) {
            // undoManager wasn't connected
        }
        undoManager.connect(document);
        undoManager.beginCompoundChange();
        isCompoundChange = true;
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
            
        	setSessionState(SessionState.ACTIVE);
        	this.editor = (ITextEditor) editor;
            this.viewer = getActiveTextViewer(editor);
            if (viewer == null) {
                Activator.getLogger().info("Viewer is null!!!!");
            }
            this.widget = viewer.getTextWidget();
            if (this.widget == null) {
                Activator.getLogger().info("Widget is null!!!!");
            }
            this.document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
            this.undoManager = DocumentUndoManagerRegistry.getDocumentUndoManager(document);
        	this.lastUpdateTime = System.currentTimeMillis();
        	
        	initUndoManager();
        	
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
        	endSession();
        	return false;
        }
    }
    public void showUserInputBox(final int selectionOffset) {
        if (inputBox != null) {
            inputBox.close();
        }

        inputBox = new PopupDialog(this.widget.getShell(), PopupDialog.INFOPOPUP_SHELLSTYLE, false, false, true, false, false,
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
                if (currentState != SessionState.GENERATING) {
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
                            var userInput = inputField.getText();
                            if (userInputIsValid(userInput)) {
                                setSessionState(SessionState.GENERATING);
                                close();
                                var cursorState = getSelectionRangeCursorState().get();
                                var prompt = new ChatPrompt(userInput, userInput, "");

                                var filePath = "C:\\Users\\somerandomusername\\Desktop\\lsp.txt";
                                var id = UUID.randomUUID().toString();
                                params = new ChatRequestParams(id, prompt, new TextDocumentIdentifier(filePath), Arrays.asList(cursorState));

                                chatCommunicationManager.sendMessageToChatServer(Command.CHAT_SEND_PROMPT, params);
                                // TODO: instead show the progress indicator that Q is thinking untill the full response comes
                                showPrompt(GENERATING_POPUP_MSG, selectionOffset);
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

            Activator.getLogger().info("CHAT RESPONSE MESSAGE: " + message);

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
            endSessionImmediately();
        }
    }
    
    private void updateUI(final ChatResult chatResult, final boolean isPartialResult) {
        Display.getDefault().asyncExec(() -> {
            insertWithInlineDiffUtils(originalCode, chatResult.body(), originalSelectionStart);

            if (!isPartialResult) {
                showPrompt(USER_DECISION_POPUP_MSG, originalSelectionStart);
            }
        });
    }
    
    private void insertWithInlineDiffUtils(final String originalCode, final String newCode, final int offset) {

        // Annotation model provides highlighting for the diff additions/deletions
        IAnnotationModel annotationModel = editor.getDocumentProvider()
                .getAnnotationModel(editor.getEditorInput());

        try {
            // Clear existing diff annotations prior to starting new diff
            clearDiffAnnotations(annotationModel);

            // Determine length to clear in editor window
            final int replaceLen = (previousPartialResponse == null)
                    ? originalCode.length()
                    : previousDisplayLength;

            // Restore document to original state
//            document.replace(offset, replaceLen, originalCode);

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
            document.replace(offset, replaceLen, finalText);

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

        } catch (BadLocationException e) {
            Activator.getLogger().error("Failed to insert inline diff", e);
            endSessionImmediately();
        } finally {
            previousPartialResponse = newCode;
        }
    }
    
    private void showPrompt(String promptText, int selectionOffset) {
        closePrompt();
        Display.getDefault().syncExec(() -> {
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
        closePrompt();
        final IAnnotationModel annotationModel = editor.getDocumentProvider()
                .getAnnotationModel(editor.getEditorInput());
        try {
            
            document.replace(originalSelectionStart, previousDisplayLength, previousPartialResponse);
            clearDiffAnnotations(annotationModel);
            
            undoManager.endCompoundChange();
        } catch (final Exception e) {
            Activator.getLogger().error("Error while accepting changes", e);
            throw e;
        } finally {
            endSession();
        }
    }
    public void handleDeclined() throws Exception {
        closePrompt();
        if (undoManager.undoable()) {
            undoManager.undo();
        }
        endSession();
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
        try {
            if (this.widget != null && !this.widget.isDisposed()) {
                this.widget = null;
            }
            
            if (this.viewer != null) {
                this.viewer = null;
            }
            if (isCompoundChange) {
                undoManager.endCompoundChange();
                undoManager.disconnect(document);
                isCompoundChange = false;
            }
            
            if (contextService != null && contextActivation != null) {
                contextService.deactivateContext(contextActivation);
                contextActivation = null;
            }
            
            // Clear session state
            this.originalCode = null;
            previousPartialResponse = null;
            this.editor = null;
            this.document = null;
            this.undoManager = null;
            this.lastUpdateTime = 0;
            this.previousDisplayLength = 0;
            this.originalSelectionStart = 0;

            if (executor != null && !executor.isShutdown()) {
                executor.shutdown();
            }

            if (pendingUpdate != null) {
                pendingUpdate.cancel(true);
            }
        } catch (Exception e) {
            Activator.getLogger().error("Error while ending session: " + e.getMessage(), e);
        } finally {
            setSessionState(SessionState.INACTIVE);
        }
    }
    public synchronized void endSessionImmediately() {
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
    	return input != null && input.length() > 5 && input.length() < MAX_INPUT_LENGTH;
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
}
