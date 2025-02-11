package software.aws.toolkits.eclipse.amazonq.views;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import software.aws.toolkits.eclipse.amazonq.chat.models.ChatRequestParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatResult;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatPrompt;
import software.aws.toolkits.eclipse.amazonq.chat.models.CursorState;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;

import software.aws.toolkits.eclipse.amazonq.chat.ChatCommunicationManager;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils;
import software.aws.toolkits.eclipse.amazonq.views.model.Command;

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

public class QInlineChatSession implements KeyListener, ChatUiRequestListener {
	
	private static QInlineChatSession instance;
	
    // Session state enum
    private enum SessionState {
        INACTIVE,
        ACTIVE,
        GENERATING
    }
	
    private volatile SessionState currentState = SessionState.INACTIVE;
	private ITextEditor editor = null;
	private String previousPartialResponse = null;
	private String originalCode;
	private int originalSelectionStart;
	private PopupDialog popup;
	private int previousDisplayLength;
	private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private ScheduledFuture<?> pendingUpdate;
	private static final int BATCH_DELAY_MS = 100;
	private final String progressMessage = "Amazon Q is generating...";
	private long lastUpdateTime = 0;

    public QInlineChatSession() {
        chatCommunicationManager = ChatCommunicationManager.getInstance();
        // TODO: Update ChatCommunicationManager to track a list of listeners as opposed to tracking only one
        // For this startSession call to successful, the chat panel must be closed so that there is a single listener registered
        chatCommunicationManager.setChatUiRequestListener(this);
    }
    
    public static synchronized QInlineChatSession getInstance() {
    	if (instance == null) {
    		instance = new QInlineChatSession();
    	}
    	return instance;
    }
    
    // Session state management methods
    public synchronized boolean isSessionActive() {
    	return currentState != SessionState.INACTIVE;
    }
    
    private synchronized void setSessionState(SessionState newState) {
        this.currentState = newState;
    }
    
    // TODO: Implement an end session as well to track a single session is in progress and any resources tied up are disposed
    public final boolean startSession(final ITextEditor editor) {
    	
    	if (isSessionActive()) {
    		return false;
    	}
        IWorkbenchPage page = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow()
                .getActivePage();

        IEditorPart editorPart = page.getActiveEditor();

        if (!(editorPart instanceof ITextEditor)) {
            return false;
        }
        try {
        	setSessionState(SessionState.ACTIVE);
        	this.editor = (ITextEditor) editorPart;
        	
            Display.getDefault().asyncExec(() -> {
                
                var selection = (ITextSelection) editor.getSelectionProvider().getSelection();
                originalSelectionStart = selection.getOffset();
                Activator.getLogger().info(String.format("SELECTION OFFSET: %d", originalSelectionStart));
                originalCode = selection.getText();
                
                // Show user input dialog
                onUserInput(originalSelectionStart);
            });
            return true;
        } catch (Exception e) {
        	endSession();
        	return false;
        }
    }
    
    public synchronized void endSession() {
        if (!isSessionActive()) {
            return;
        }
        
        try {
            // Cleanup resources
            if (popup != null) {
                Display.getDefault().asyncExec(() -> {
                    if (popup != null) {
                        popup.close();
                        popup = null;
                    }
                });
            }
            
            if (executor != null && !executor.isShutdown()) {
                executor.shutdown();
            }
            
            if (pendingUpdate != null) {
                pendingUpdate.cancel(true);
            }
            
            // Clear state
            editor = null;
            previousPartialResponse = null;
            originalCode = null;
            previousDisplayLength = 0;
            
        } finally {
            setSessionState(SessionState.INACTIVE);
        }
    }
  
    public final void showUserDecisionRequiredPopup(int selectionOffset) {
        if (popup != null) {
            popup.close();
            if (popup.getShell() != null && !popup.getShell().isDisposed()) {
                popup.getShell().dispose();
            }
            popup = null;
        }
        IWorkbenchPage page = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow()
                .getActivePage();
        IEditorPart editorPart = page.getActiveEditor();
        
        if (!(editorPart instanceof ITextEditor)) {
            return;
        }

        ITextEditor editor = (ITextEditor) editorPart;
        var viewer = getActiveTextViewer(editor);
        var widget = viewer.getTextWidget();
        
        // Create key handler for accept/reject actions
        KeyAdapter keyHandler = new KeyAdapter() {
	        @Override
	        public void keyPressed(KeyEvent e) {
	            if (e.keyCode == SWT.TAB) {  // Accept changes
	                handleAcceptInlineChat(editor);
	                cleanup();
	            } else if (e.keyCode == SWT.ESC) {  // Reject changes
	                handleDeclineInlineChat(editor);
	                cleanup();
	            }
	        } 
	        
	        private void cleanup() {
	            // Remove the key listener and close the popup
	            widget.removeKeyListener(this);
	            popup.close();
	        }
        };
        popup = new PopupDialog(widget.getShell(), PopupDialog.HOVER_SHELLSTYLE, false, false, true, false, false,
                null, null) {
            private Point screenLocation; 
            
            @Override
            protected Point getInitialLocation(final Point initialSize) {
                if (screenLocation == null) {
                    Point location = widget.getLocationAtOffset(selectionOffset);
                    location.y -= widget.getLineHeight() * 1.1;
                    screenLocation = Display.getCurrent().map(widget, null, location);
                }
                return screenLocation;
            }

            @Override
            protected Control createDialogArea(final Composite parent) {
                Composite composite = (Composite) super.createDialogArea(parent);
                composite.setLayout(new GridLayout(1, false));
                
                Label infoLabel = new Label(composite, SWT.NONE);
                infoLabel.setLayoutData(new GridData(GridData.FILL_BOTH));
          
                String tipToDisplay = "Accept: Tab, Reject: Esc";
                infoLabel.setText(tipToDisplay);

                return composite;
            }
        };

        widget.addKeyListener(keyHandler);
        popup.open();
    }
    
    public final void showGeneratingPopup(int selectionOffset) {
        if (popup != null) {
            popup.close();
            if (popup.getShell() != null && !popup.getShell().isDisposed()) {
                popup.getShell().dispose();
            }
            popup = null;
        }
        
        IWorkbenchPage page = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow()
                .getActivePage();
        IEditorPart editorPart = page.getActiveEditor();
        
        if (!(editorPart instanceof ITextEditor)) {
            return;
        }

        ITextEditor editor = (ITextEditor) editorPart;
        var viewer = getActiveTextViewer(editor);
        var widget = viewer.getTextWidget();
        
        popup = new PopupDialog(widget.getShell(), PopupDialog.HOVER_SHELLSTYLE, false, false, true, false, false,
                null, null) {
            private Point screenLocation;
            
            @Override
            protected Point getInitialLocation(final Point initialSize) {
                if (screenLocation == null) {
                    Point location = widget.getLocationAtOffset(selectionOffset);
                    location.y -= widget.getLineHeight() * 1.1;
                    screenLocation = Display.getCurrent().map(widget, null, location);
                }
                return screenLocation;
            }

            @Override
            protected Control createDialogArea(final Composite parent) {
                Composite composite = (Composite) super.createDialogArea(parent);
                composite.setLayout(new GridLayout(1, false));
                
                Label infoLabel = new Label(composite, SWT.NONE);
                infoLabel.setLayoutData(new GridData(GridData.FILL_BOTH));
                infoLabel.setText(progressMessage);
                
                return composite;
            }
        };
        
        popup.open();
    }

    private void handleAcceptInlineChat(ITextEditor editor) {
    	handleUserDecision(editor, true);
    }

    private final void handleDeclineInlineChat(ITextEditor editor) {
    	handleUserDecision(editor, false);
    }
    
    private final void handleUserDecision(ITextEditor editor, boolean acceptedChanges) {
    	IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        IAnnotationModel annotationModel = editor.getDocumentProvider()
                .getAnnotationModel(editor.getEditorInput());
        
        var codeToInsert = (acceptedChanges) ? previousPartialResponse : originalCode;
        String errorMsg = (acceptedChanges) ? "accepting changes" : "restoring original code";
        
        try {
            // Restore original code
            document.replace(originalSelectionStart, previousDisplayLength, codeToInsert);
            
            // Clear all diff annotations
            clearDiffAnnotations(annotationModel);
        } catch (BadLocationException ex) {
            Activator.getLogger().error("Error while " + errorMsg, ex);
        }	      
    }

    public final void beforeRemoval() {
        if (popup != null) {
            popup.close();
        }
    }
    
    
    public static ITextViewer asTextViewer(final IEditorPart editorPart) {
        return editorPart != null ? editorPart.getAdapter(ITextViewer.class) : null;
    }

    public static ITextViewer getActiveTextViewer(final ITextEditor editor) {
        return asTextViewer(editor);
    }

    private void clearDiffAnnotations(IAnnotationModel annotationModel) {
        var annotations = annotationModel.getAnnotationIterator();
        while (annotations.hasNext()) {
            var annotation = annotations.next();
            String type = annotation.getType();
            if (type.startsWith("diffAnnotation.")) {
                annotationModel.removeAnnotation(annotation);
            }
        }
    }
    
    //method to display input box -- not currently wired up
    public final void onUserInput(int selectionOffset) {
        if (popup != null) {
            popup.close();
        }
        IWorkbenchPage page = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow()
                .getActivePage();
        IEditorPart editorPart = page.getActiveEditor();
        
        if (!(editorPart instanceof ITextEditor)) {
            return;
        }

        ITextEditor editor = (ITextEditor) editorPart;
        var widget = getActiveTextViewer(editor).getTextWidget();
        popup = new PopupDialog(widget.getShell(), PopupDialog.INFOPOPUP_SHELLSTYLE, false, false, true, false, false,
                null, null) {
            private Point screenLocation;
            private Text inputField;

            @Override
            protected Point getInitialLocation(final Point initialSize) {
                if (screenLocation == null) {
                    Point location = widget.getLocationAtOffset(selectionOffset);
                    location.y -= widget.getLineHeight() * 1.1;
                    screenLocation = Display.getCurrent().map(widget, null, location);
                }
                return screenLocation;
            }

            @Override
            protected Control createDialogArea(final Composite parent) {
                var composite = (Composite) super.createDialogArea(parent);
                composite.setLayout(new GridLayout(1, false));

                var titleLabel = new Label(composite, SWT.NONE);
                titleLabel.setText("Enter instructions for Amazon Q");
                titleLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

                inputField = new Text(composite, SWT.BORDER | SWT.MULTI | SWT.WRAP);
                GridData gridData = new GridData(GridData.FILL_BOTH);
                gridData.heightHint = 80;
                gridData.widthHint = 80; 
                inputField.setLayoutData(gridData);

                var instructionsLabel = new Label(composite, SWT.NONE);
                instructionsLabel.setText("Press Enter to confirm, Esc to cancel");
                instructionsLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

                inputField.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyPressed(KeyEvent e) {
                        // on enter, accept the input added
                        if (e.character == SWT.CR || e.character == SWT.LF) {

                            var userInput = inputField.getText();
                            close();
                            var cursorState = getSelectionRangeCursorState().get();
                            var prompt = new ChatPrompt(userInput, userInput, "");

                            var filePath = "C:\\Users\\somerandomusername\\Desktop\\lsp.txt";
                            // use a random uuid for tabId instead
                            var id = UUID.randomUUID().toString();
                            params = new ChatRequestParams(id, prompt, new TextDocumentIdentifier(filePath), Arrays.asList(cursorState));
                                 
                            chatCommunicationManager.sendMessageToChatServer(Command.CHAT_SEND_PROMPT, params);
                            // TODO: instead show the progress indicator that Q is thinking untill the full response comes
                            showGeneratingPopup(selectionOffset);
                         
                        } else if (e.character == SWT.ESC) {
                            close();
                        }
                    }
                });

                inputField.setFocus();
                return composite;
            }

            public String getInputText() {
                return inputField != null ? inputField.getText() : "";
            }
        };

        popup.setBlockOnOpen(true);
        popup.open();
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
    private  ChatCommunicationManager chatCommunicationManager;
    private ChatRequestParams params;

    @Override
    public void keyPressed(KeyEvent e) {

        if (e.keyCode == SWT.ESC && popup != null) {
            popup.close();
        }
    }

    @Override
    public void keyReleased(KeyEvent arg0) {
        // TODO Auto-generated method stub
        
    }



//    @Override
//    public void onSendToChatUi(String message) {
//    	Activator.getLogger().info("CHAT RESPONSE MESSAGE: " + message);
//        if (!message.equals(previousPartialResponse)) {
//            Display.getDefault().asyncExec(() -> {
//                // Always insert at the originalSelectionStart position
//                insertWithInlineDiffUtils(originalCode, message, originalSelectionStart);
//                previousPartialResponse = message;
//            });
//        }
//    }
    @Override
    public void onSendToChatUi(String message) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            var rootNode = mapper.readTree(message);
            var paramsNode = rootNode.get("params");
            var isPartialResult = rootNode.get("isPartialResult").asBoolean();
            
            var chatResult = mapper.treeToValue(paramsNode, ChatResult.class);

        	Activator.getLogger().info("CHAT RESPONSE MESSAGE: " + message);
        	
            if (isPartialResult) {
                // Handle partial results with batching
                if (!chatResult.body().equals(previousPartialResponse)) {
                    if (pendingUpdate != null) {
                        pendingUpdate.cancel(false);
                    }
                    
                    // Calculate remaining time since last UI update
                    long timeSinceUpdate = System.currentTimeMillis() - lastUpdateTime;
                    long delayToUse = Math.max(0, BATCH_DELAY_MS - timeSinceUpdate);
                    
                    //set delay to guarantee 100ms delay between updates
                    pendingUpdate = executor.schedule(() -> {
                        updateUI(chatResult, isPartialResult);
                    }, delayToUse, TimeUnit.MILLISECONDS);
                }
            } else {
                // Final result - always update UI state regardless of content
                updateUI(chatResult, isPartialResult);
                lastUpdateTime = System.currentTimeMillis();
            }
          
        } catch (JsonProcessingException e) {
            Activator.getLogger().error("Failed to parse ChatResult", e);
        }
    }
    
    private void updateUI(ChatResult chatResult, boolean isPartialResult) {
        Display.getDefault().asyncExec(() -> {
            insertWithInlineDiffUtils(originalCode, chatResult.body(), originalSelectionStart);
            previousPartialResponse = chatResult.body();

            if (!isPartialResult) {
                showUserDecisionRequiredPopup(originalSelectionStart);
            }
        });
    }
   
    
    
    /* I believe in the case that Q is only adding comments before
     * use this method to insert in place of cursor
    */
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


    private void insertWithInlineDiffUtils(String originalCode, String newCode, int offset) {
        IWorkbenchPage page = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow()
                .getActivePage();
        IEditorPart editorPart = page.getActiveEditor();
        
        if (!(editorPart instanceof ITextEditor)) {
            return;
        }

        ITextEditor editor = (ITextEditor) editorPart;
        IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        
        // Annotation model provides highlighting for the diff additions/deletions
        IAnnotationModel annotationModel = editor.getDocumentProvider()
                .getAnnotationModel(editor.getEditorInput());
        
        try {
        	// Clear existing diff annotations prior to starting new diff
        	clearDiffAnnotations(annotationModel);
        	
        	// Determine length to clear in editor window
        	int replaceLen = (previousPartialResponse == null)
					? originalCode.length()
					: previousDisplayLength;
        	
        	// Restore document to original state
        	doc.replace(offset, replaceLen, originalCode);

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
            
            // Clear existing annotations in the affected range
            clearAnnotationsInRange(annotationModel, offset, offset + originalCode.length());
            
            // Apply new diff text
            doc.replace(offset, originalCode.length(), finalText);
            
            // Store rendered text length for proper clearing next iteration
            previousDisplayLength = finalText.length();
            
            // Add new annotations for this diff
            for (Position position : deletedPositions) {
                Annotation annotation = new Annotation("diffAnnotation.deleted", false, "Deleted Code");
                annotationModel.addAnnotation(annotation, position);
            }
            
            for (Position position : addedPositions) {
                Annotation annotation = new Annotation("diffAnnotation.added", false, "Added Code");
                annotationModel.addAnnotation(annotation, position);
            }
            
        } catch (BadLocationException e) {
            Activator.getLogger().error("Failed to insert inline diff", e);
        }
    }
    
    private void clearAnnotationsInRange(IAnnotationModel model, int start, int end) {
        Iterator<Annotation> iterator = model.getAnnotationIterator();
        while (iterator.hasNext()) {
            Annotation annotation = iterator.next();
            Position position = model.getPosition(annotation);
            if (position != null && 
                position.offset >= start && 
                position.offset + position.length <= end) {
                model.removeAnnotation(annotation);
            }
        }
    }


    

/////////
}
