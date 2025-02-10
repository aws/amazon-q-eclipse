package software.aws.toolkits.eclipse.amazonq.views;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
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
import software.aws.toolkits.eclipse.amazonq.chat.models.InsertToCursorPositionParams;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.compare.contentmergeviewer.ITokenComparator;
import org.eclipse.compare.internal.DocLineComparator;

import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ITextEditor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.Patch;

import software.aws.toolkits.eclipse.amazonq.chat.ChatCommunicationManager;
import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionItem;
import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionReference;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils;
import software.aws.toolkits.eclipse.amazonq.util.QInvocationSession;
import software.aws.toolkits.eclipse.amazonq.views.model.Command;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.compare.rangedifferencer.IRangeComparator;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.rangedifferencer.RangeDifferencer;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.rangedifferencer.RangeDifferencer;
import org.eclipse.compare.internal.DocLineComparator;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.HistogramDiff;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.jgit.diff.MyersDiff;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;

import java.nio.charset.StandardCharsets;

public class QInlineChatSession implements KeyListener, ChatUiRequestListener {
	
	private String previousPartialResponse= "";
	private String originalCode;
	 private int originalSelectionStart;

    public QInlineChatSession() {
        chatCommunicationManager = ChatCommunicationManager.getInstance();
        // TODO: Update ChatCommunicationManager to track a list of listeners as opposed to tracking only one
        // For this startSession call to successful, the chat panel must be closed so that there is a single listener registered
        chatCommunicationManager.setChatUiRequestListener(this);
    }
    

    // Sample selected code part of request
    private String getCode() {
        String code = """
                    public int addTwoNumbers(int a , int b)
                    {
                        return a + b;
                    }
                """;

        return code;
    }

    // Sample code response to be tested with diff tools
    private String getUpdate() {
        String code = """
                    /**
                     * Adds two integer numbers together
                     * @param a First integer number
                     * @param b Second integer number
                     * @return Sum of the two numbers
                     */
                    public int addTwoNumbers(int a , int b)
                    {
                        return a + b;
                    }
                """;
        return code;
    }
    
    
    /**
     * Attempts to show inline diff of the og code vs new code by comparing texts in each line and 
     * using StyledText to show them with appropriate colors 
     * Do not prefer this approach because diffing is a very manual process
     * @param editor
     * @param originalCode
     * @param newCode
     * @param offset
     */
    public void showInlineDiffManual(ITextEditor editor, String originalCode, String newCode, int offset) {
        var control = editor.getAdapter(Control.class);
        if (control instanceof StyledText) {
            StyledText styledText = (StyledText) control;
            
            // Style range for marking/highlighting
            StyleRange[] styles = new StyleRange[1];
            styles[0] = new StyleRange();
            styles[0].background = new Color(Display.getCurrent(), 127, 255, 127); // green
            
            // Get the text and find differences per line
            String currentText = styledText.getText();
            String[] originalLines = originalCode.split("\n");
            String[] currentLines = currentText.split("\n");
            
            for (String line : currentLines) {
                boolean isNewLine = true;
                for (String origLine : originalLines) {
                    if (line.trim().equals(origLine.trim())) {
                        isNewLine = false;
                        break;
                    }
                }
                
                if (isNewLine) {
                    // Mark the modified/different line
                    StyleRange style = new StyleRange();
                    style.start = offset;
                    style.length = line.length();
                    style.background = new Color(Display.getCurrent(), 127, 255, 127);
                    styledText.setStyleRange(style);
                }
                // add +1 to account for new lines
                offset += line.length() + 1;
            }
        }
       
        // another alternative way is to use sourceViewer to get the document and apply the text presentation for visualizing
        // this didn't work because sourceviewer was null, might need some fixes to make this approach work
        // but this one too compares lines which is tedious
        /*
        IDocument document = editor.getDocumentProvider()
                .getDocument(editor.getEditorInput());
        
        TextPresentation presentation = new TextPresentation();
        
        try {
            // Split both texts into lines
            String[] originalLines = originalCode.split("\n");
            String[] newLines = newCode.split("\n");
            
            int currentOffset = 0;
            
            // Compare line by line
            for (String newLine : newLines) {
                String trimmedNewLine = newLine.trim();
                boolean isNewLine = true;
                
                // Check if line exists in original
                for (String originalLine : originalLines) {
                    if (originalLine.trim().equals(trimmedNewLine)) {
                        isNewLine = false;
                        break;
                    }
                }
                
                if (isNewLine) {

                    StyleRange styleRange = new StyleRange();
                    styleRange.start = currentOffset;
                    styleRange.length = newLine.length();
                    styleRange.background = new Color(Display.getCurrent(), 127, 255, 127);
                    presentation.addStyleRange(styleRange);
                }
                
                currentOffset += newLine.length() + 1; 
            }
            

            sourceViewer.changeTextPresentation(presentation, true);
            
        } catch (Exception e) {
            // Handle exception
        }
        */
    }
    
    // Attempts to use org.eclipse.compare provided classes to produce a diff, this however resulted in marking the whole 
    // generated code response as newly added as opposed to marking just the actual new section for eg. comments for a function
    
    public void showInlineDiffWithCompare(String originalCode, String newCode, int offset) {
        Display.getDefault().asyncExec(() -> {
            // Get active editor
            IWorkbenchPage page = PlatformUI.getWorkbench()
                    .getActiveWorkbenchWindow()
                    .getActivePage();
            IEditorPart editorPart = page.getActiveEditor();
            
            if (editorPart instanceof ITextEditor) {
                ITextEditor editor = (ITextEditor) editorPart;
                IDocument document = editor.getDocumentProvider()
                        .getDocument(editor.getEditorInput());

                ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
                int selectionStart = selection.getOffset();

                DocLineComparator original = new DocLineComparator(new Document(originalCode), null, false);
                DocLineComparator revised = new DocLineComparator(new Document(newCode), null, false);

                RangeDifference[] differences = RangeDifferencer.findRanges(original, revised);

                // get annotations for identified differences
                IAnnotationModel annotationModel = editor.getDocumentProvider()
                        .getAnnotationModel(editor.getEditorInput());

                // clear any existing diff annotations if present
                clearDiffAnnotations(annotationModel);

                // Add new annotations for each difference
                for (RangeDifference diff : differences) {
                    try {
                        if (diff.kind() != RangeDifference.NOCHANGE) {
                            // the startLine and ednLine makes a difference when determining lines to be considered for applying annotations
                            int startLine = document.getLineOfOffset(offset) + diff.leftStart();
                            int endLine = document.getLineOfOffset(offset) + diff.rightEnd();
                            int startOffset = document.getLineOffset(startLine);
                            int endOffset;
                            if (endLine >= document.getNumberOfLines()) {
                                endOffset = document.getLength();
                            } else {
                                endOffset = document.getLineOffset(endLine) + document.getLineLength(endLine);
                            }

     
                            // We will want to use annotations to indicate the marking of different kinds of diffs
                            Annotation annotation = new Annotation("diffAnnotation", false, 
                                diff.kind() == RangeDifference.CHANGE ? "Changed" : "Added");
                            Position position = new Position(startOffset, endOffset - startOffset);
                            annotationModel.addAnnotation(annotation, position);
                        }
                    } catch (BadLocationException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
    
    // Uses jgit to compute the difference in og vs new code
     // This also marked the full new code as modified instead of marking just the newly added lines in the response
    public void showInlineDiffWithJgit(String originalCode, String newCode, int originalOffset) {
        Display.getDefault().asyncExec(() -> {
            try {
                IWorkbenchPage page = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow()
                        .getActivePage();
                IEditorPart editorPart = page.getActiveEditor();
                
                if (!(editorPart instanceof ITextEditor)) {
                    return;
                }

                ITextEditor editor = (ITextEditor) editorPart;
                IDocument document = editor.getDocumentProvider()
                        .getDocument(editor.getEditorInput());
                IAnnotationModel annotationModel = editor.getDocumentProvider()
                        .getAnnotationModel(editor.getEditorInput());

                clearDiffAnnotations(annotationModel);

                RawText oldText = new RawText(originalCode.getBytes(StandardCharsets.UTF_8));
                RawText newText = new RawText(newCode.getBytes(StandardCharsets.UTF_8));

                EditList edits = new EditList();
                edits.addAll(new HistogramDiff().diff(RawTextComparator.DEFAULT, oldText, newText));

                // this on testing produced a single edit for the whole code block with replace edit type
                // we expected this to have some deletes and some adds
                // might need more testing
                for (Edit edit : edits) {
                    try {
                        int startLine = document.getLineOfOffset(originalOffset);
                        int startOffset, endOffset;
                        String message;
                        String annotationType;

                        switch (edit.getType()) {
                            case INSERT:
                                startLine += edit.getBeginB();
                                startOffset = document.getLineOffset(startLine);
                                endOffset = document.getLineOffset(startLine + (edit.getEndB() - edit.getBeginB()));
                                annotationType = "diffAnnotation.added";
                                break;

                            case DELETE:
                                startLine += edit.getBeginA();
                                startOffset = document.getLineOffset(startLine);
                                endOffset = document.getLineOffset(startLine + (edit.getEndA() - edit.getBeginA()));
                                annotationType ="diffAnnotation.deleted";
                                break;

                            case REPLACE:
                                startLine += edit.getBeginA();
                                startOffset = document.getLineOffset(startLine);
                                int endLine = startLine + Math.max(edit.getEndA() - edit.getBeginA(), 
                                                                 edit.getEndB() - edit.getBeginB());
                                endOffset = endLine >= document.getNumberOfLines() ? 
                                          document.getLength() : 
                                          document.getLineOffset(endLine) + document.getLineLength(endLine);
                                annotationType = "diffAnnotation.modified";
                                break;

                            default:
                                continue;
                        }

                        if (endOffset < document.getLength()) {
                            endOffset += document.getLineLength(document.getLineOfOffset(endOffset));
                        }

                        var annotation = new Annotation(annotationType, false, getAnnotationMessage(edit.getType()));
                        var position = new Position(startOffset, endOffset - startOffset);
                        annotationModel.addAnnotation(annotation, position);

                    } catch (BadLocationException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    private String getAnnotationMessage(Edit.Type type) {
        switch (type) {
            case INSERT:
                return "Added code";
            case DELETE:
                return "Deleted code";
            case REPLACE:
                return "Modified code";
            default:
                return "Changed code";
        }
    }
  
    private PopupDialog popup;
    public final void showUserDecisionRequiredPopup(int selectionOffset) {
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
        getActiveTextViewer(editor).getTextWidget().addKeyListener(this);
        popup.open();
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

    // Attempts to use eclipse.compare APIs tokencomparator for diffing
    public void showInlineDiffWithCompare2(String originalCode, String newCode, int originalOffset) {
        Display.getDefault().asyncExec(() -> {
            try {
                IWorkbenchPage page = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow()
                        .getActivePage();
                IEditorPart editorPart = page.getActiveEditor();
                
                if (!(editorPart instanceof ITextEditor)) {
                    return;
                }

                ITextEditor editor = (ITextEditor) editorPart;
                IDocument document = editor.getDocumentProvider()
                        .getDocument(editor.getEditorInput());
                IAnnotationModel annotationModel = editor.getDocumentProvider()
                        .getAnnotationModel(editor.getEditorInput());

                clearDiffAnnotations(annotationModel);

                // Create token comparators for the original and new code
                TokenComparator original = new TokenComparator(originalCode);
                TokenComparator latest = new TokenComparator(newCode);

                // Get the differences using RangeDifferencer
                RangeDifference[] differences = RangeDifferencer.findDifferences(original, latest);

                int currentOffset = originalOffset;
                
                for (RangeDifference diff : differences) {
                    try {
                        if (diff.kind() == RangeDifference.CHANGE) {
                          
                            int startOffset = getOffsetForLine(document, currentOffset, diff.rightStart());
                            int endOffset = getOffsetForLine(document, currentOffset, diff.rightEnd());
                            
                            if (diff.leftLength() == 0) {
                                addAnnotation(annotationModel, startOffset, endOffset, 
                                    "diffAnnotation.added");
                            } else if (diff.rightLength() == 0) {
                                addAnnotation(annotationModel, startOffset, startOffset, 
                                    "diffAnnotation.deleted");
                            } else {
                                addAnnotation(annotationModel, startOffset, endOffset, 
                                    "diffAnnotation.modified");
                            }
                        }
                    } catch (BadLocationException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static class TokenComparator implements IRangeComparator {
        private final String[] lines;

        public TokenComparator(String text) {
            this.lines = text.split("\n", -1);
        }

        @Override
        public int getRangeCount() {
            return lines.length;
        }

        @Override
        public boolean rangesEqual(int thisIndex, IRangeComparator other, int otherIndex) {
            TokenComparator tc = (TokenComparator) other;
            return lines[thisIndex].equals(tc.lines[otherIndex]);
        }

        @Override
        public boolean skipRangeComparison(int length, int maxLength, IRangeComparator other) {
            return false;
        }

        public String getLine(int index) {
            return lines[index];
        }
    }

    private int getOffsetForLine(IDocument document, int baseOffset, int lineNumber) 
            throws BadLocationException {
        int currentLine = document.getLineOfOffset(baseOffset);
        return document.getLineOffset(currentLine + lineNumber);
    }

    private void addAnnotation(IAnnotationModel model, int start, int end, String type) {
        String message;
        switch (type) {
            case "diffAnnotation.added":
                message = "Added code";
                break;
            case "diffAnnotation.deleted":
                message = "Deleted code";
                break;
            case "diffAnnotation.modified":
                message = "Modified code";
                break;
            default:
                message = "Changed code";
        }
        
        Annotation annotation = new Annotation(type, false, message);
        Position position = new Position(start, end - start);
        model.addAnnotation(annotation, position);
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
   
    // TODO: Implement an end session as well to track a single session is in progress and any resources tied up are disposed
    public final void startSession() {
        IWorkbenchPage page = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow()
                .getActivePage();

        IEditorPart editorPart = page.getActiveEditor();
        if (editorPart instanceof ITextEditor) {
            final ITextEditor editor = (ITextEditor) editorPart;

            Display.getDefault().asyncExec(() -> {
              
                var selection = (ITextSelection) editor.getSelectionProvider().getSelection();
                originalSelectionStart = selection.getOffset();
                Activator.getLogger().info(String.format("SELECTION OFFSET: %d", originalSelectionStart));
           
                // Show user input dialog
//                onUserInput(selectionStart);
                
                
                /* PLAN OF ACTION
                 * 1. test without input box
                 * 2. purely want to insert some code to highlighted portion on CMD + C
                 * 3. diff should check the code to see if it matches whats already there
                 * 4. next step would be having diff react properly to partial results
                 * */
               
                // Sample code to play with for inserting a sample response
                originalCode = selection.getText();                
                var newCode = """
                   public int add(int a , int b)
                   {
                	   int num = a + b;
                       return num;
                   }
                    """;
                var newCode2 = """
                    public int addTwoNumbers(int a , int b)
                    {
                        int num = a + b;
                        return num;
                    }
                     """;                
                var newCode3 = """
                        public int addTwoNumbers(int a , int b)
                        {
                            System.out.println(String.format("RESULT: %d", a+b));
                            return a + b;
                        }
                         """;
                onSendToChatUi(newCode);
                Display.getDefault().timerExec(500, () -> {
                    // Second call after 250ms
                    onSendToChatUi(newCode2);
                    
                    Display.getDefault().timerExec(500, () -> {
                        // Third call after another 250ms
                        onSendToChatUi(newCode3);
                    });
                });
               
           
//                showInlineDiff(originalCode, newCode);
            });
        }
    }
    
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
                            params = new ChatRequestParams("abcd", prompt, new TextDocumentIdentifier(filePath), Arrays.asList(cursorState));
                                 
                            chatCommunicationManager.sendMessageToChatServer(Command.CHAT_SEND_PROMPT, params);
                            // TODO: instead show the progress indicator that Q is thinking untill the full response comes
                            showUserDecisionRequiredPopup(selectionOffset);
                         
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



    @Override
    public void onSendToChatUi(String latestResponse) {
        Activator.getLogger().info("SEND TO CHAT UI INVOKED!!!!");
        
        if (!latestResponse.equals(previousPartialResponse)) {
            Display.getDefault().asyncExec(() -> {
                // Always insert at the originalSelectionStart position
                insertWithInlineDiffUtils(originalCode, latestResponse, originalSelectionStart);
            });
            previousPartialResponse = latestResponse;
        }
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
        IAnnotationModel annotationModel = editor.getDocumentProvider()
                .getAnnotationModel(editor.getEditorInput());
        
        try {
        	// Clear existing diff annotations prior to starting new diff
        	clearDiffAnnotations(annotationModel);
        	
        	// Restore document to original state to prevent overlapping diff code
        	int replaceLen = originalCode.length();
        	doc.replace(offset, replaceLen, originalCode);
        	
        	// Split original and new code into lines for diff comparison
            String[] originalLines = originalCode.split("\n");
            String[] newLines = newCode.split("\n");
            
            // Diff generation
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
            doc.replace(offset, replaceLen, finalText);
            
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
