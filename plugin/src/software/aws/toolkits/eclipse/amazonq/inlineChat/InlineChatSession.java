package software.aws.toolkits.eclipse.amazonq.inlineChat;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.swt.widgets.Display;
import org.eclipse.text.undo.DocumentUndoEvent;
import org.eclipse.text.undo.DocumentUndoManagerRegistry;
import org.eclipse.text.undo.IDocumentUndoListener;
import org.eclipse.text.undo.IDocumentUndoManager;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.texteditor.ITextEditor;

import com.fasterxml.jackson.databind.ObjectMapper;

import software.aws.toolkits.eclipse.amazonq.chat.ChatCommunicationManager;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatPrompt;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatRequestParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatResult;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.preferences.AmazonQPreferencePage;
import software.aws.toolkits.eclipse.amazonq.util.Constants;
import software.aws.toolkits.eclipse.amazonq.util.ObjectMapperFactory;
import software.aws.toolkits.eclipse.amazonq.util.ThemeDetector;
import software.aws.toolkits.eclipse.amazonq.views.ChatUiRequestListener;

public class InlineChatSession implements ChatUiRequestListener, IPartListener2 {

    // Session state variables
    private static InlineChatSession instance;
    private SessionState currentState = SessionState.INACTIVE;
    private final Object stateLock = new Object();
    private InlineChatTask task;
    boolean referencesEnabled;
    private IWorkbenchPage workbenchPage;

    // Dependencies
    private final InlineChatUIManager uiManager;
    private final InlineChatDiffManager diffManager;
    private ChatRequestParams params;
    private final ChatCommunicationManager chatCommunicationManager;
    private final ThemeDetector themeDetector;

    // Document-update batching variables
    private IDocumentUndoManager undoManager;
    private IDocumentUndoListener undoListener;
    private IDocument document;
    private boolean isCompoundChange;

    // Context handler variables
    private final IContextService contextService;
    private IContextActivation contextActivation;
    private final int ABOUT_TO_UNDO = 17; // 17 maps to this event type

    private InlineChatSession() {
        chatCommunicationManager = ChatCommunicationManager.getInstance();
        chatCommunicationManager.setInlineChatRequestListener(this);
        uiManager = InlineChatUIManager.getInstance();
        diffManager = InlineChatDiffManager.getInstance();
        themeDetector = new ThemeDetector();
        contextService = PlatformUI.getWorkbench().getService(IContextService.class);
    }

    public static synchronized InlineChatSession getInstance() {
        if (instance == null) {
            instance = new InlineChatSession();
        }
        return instance;
    }

    public boolean startSession(final ITextEditor editor) {
        if (isSessionActive()) {
            return false;
        }
        if (editor == null || !(editor instanceof ITextEditor)) {
            return false;
        }
        try {
            setState(SessionState.ACTIVE);

            InlineChatEditorListener.getInstance().closePrompt();

            workbenchPage = editor.getSite().getPage();
            workbenchPage.addPartListener(this);

            // Get the context service and activate inline chat context used for button
            contextActivation = contextService.activateContext(Constants.INLINE_CHAT_CONTEXT_ID);

            // Set up undoManager to batch document edits together
            this.document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
            this.undoManager = DocumentUndoManagerRegistry.getDocumentUndoManager(this.document);
            initUndoManager(this.document);

            // Check if user has code references enabled
            var currentLoginType = Activator.getLoginService().getAuthState().loginType();
            this.referencesEnabled = Activator.getDefault().getPreferenceStore().getBoolean(AmazonQPreferencePage.CODE_REFERENCE_OPT_IN)
                    && currentLoginType.equals(LoginType.BUILDER_ID);

            var viewer = editor.getAdapter(ITextViewer.class);
            if (viewer == null) {
                return false;
            }
            var widget = viewer.getTextWidget();
            if (widget == null) {
                return false;
            }

            // Create InlineChatTask to unify context between managers
            Display.getDefault().syncExec(() -> {

                /* Ensure visual offset begins at start of selection and
                 * that selection always includes full line */
                final var selection = (ITextSelection) editor.getSelectionProvider().getSelection();
                int selectedLines = selection.getEndLine() - selection.getStartLine() + 1;
                var selectionRange = widget.getSelectionRange();
                int visualOffset = (selectionRange != null) ? selectionRange.x : widget.getCaretOffset();
                try {
                    final var region = expandSelectionToFullLines(document, selection);
                    final String selectionText = document.get(region.getOffset(), region.getLength());
                    task = new InlineChatTask(editor, selectionText, visualOffset, region, selectedLines);
                } catch (Exception e) {
                    Activator.getLogger().error("Failed to expand selection region: " + e.getMessage(), e);
                    var region = new Region(selection.getOffset(), selection.getLength());
                    task = new InlineChatTask(editor, selection.getText(), visualOffset, region, selectedLines);
                }
            });

            var isDarkTheme = themeDetector.isDarkTheme();
            // Set up necessary managers with the context they need
            this.uiManager.initNewTask(task, isDarkTheme);
            this.diffManager.initNewTask(task, isDarkTheme);
            chatCommunicationManager.updateInlineChatTabId(task.getTabId());

            CompletableFuture.runAsync(() -> {
                try {
                    start();
                } catch (Exception e) {
                    Display.getDefault().asyncExec(() -> {
                        uiManager.showErrorNotification();
                        endSession();
                    });
                }
            });
            return true;
        } catch (Exception e) {
            uiManager.showErrorNotification();
            endSession();
            return false;
        }
    }

    // Initiate process by opening user prompt and sending result to chat server
    private void start() {
        uiManager.showUserInputPrompt().thenRun(() -> {
            if (task.getPrompt() != null) {
                sendInlineChatRequest();
                uiManager.transitionToGeneratingPrompt();
                setState(SessionState.GENERATING);
            } else {
                endSession();
                Activator.getLogger().info("TASK NOT SUBMITTED");
            }
        }).exceptionally(throwable -> {
            Activator.getLogger().error("Failed to open user input prompt", throwable);
            uiManager.showErrorNotification();
            endSession();
            return null;
        });
    }

    // Chat server response handler
    @Override
    public void onSendToChatUi(final String message) {
        if (!isSessionActive()) {
            return;
        }

        try {
            // Deserialize object
            ObjectMapper mapper = ObjectMapperFactory.getInstance();
            var rootNode = mapper.readTree(message);
            if (rootNode.has("command") && "errorMessage".equals(rootNode.get("command").asText())) {
                uiManager.showErrorNotification();
                restoreAndEndSession();
                return;
            }

            var paramsNode = rootNode.get("params");
            var isPartialResult = rootNode.get("isPartialResult").asBoolean();
            var chatResult = mapper.treeToValue(paramsNode, ChatResult.class);

            Activator.getLogger().info("RESPONSE: " + message);

            if (!verifyChatResultParams(chatResult)) {
                restoreAndEndSession();
                return;
            }

            // Render diffs and move to deciding once we receive final result
            diffManager.processDiff(chatResult, isPartialResult).thenRun(() -> {
                if (!isPartialResult) {
                    setState(SessionState.DECIDING);
                    uiManager.transitionToDecidingPrompt();
                }
            }).exceptionally(throwable -> {
                Activator.getLogger().error("Failed to process diff", throwable);
                uiManager.showErrorNotification();
                restoreAndEndSession();
                return null;
            });

        } catch (Exception e) {
            uiManager.showErrorNotification();
            restoreAndEndSession();
        }
    }

    // Registered to accept and decline handler in plugin.xml
    public void handleDecision(final boolean userAcceptedChanges) throws Exception {
        uiManager.closePrompt();
        diffManager.handleDecision(userAcceptedChanges).thenRun(() -> {
            undoManager.endCompoundChange();
            task.setUserDecision(userAcceptedChanges);
            endSession();
        }).exceptionally(throwable -> {
            Activator.getLogger().error("Failed to handle decision", throwable);
            uiManager.showErrorNotification();
            restoreAndEndSession();
            return null;
        });
    }

    private void sendInlineChatRequest() {
        var prompt = task.getPrompt();
        var chatPrompt = new ChatPrompt(prompt, prompt, "");
        params = new ChatRequestParams(task.getTabId(), chatPrompt, null, Arrays.asList(task.getCursorState()));
        chatCommunicationManager.sendInlineChatMessageToChatServer(params);

        task.setRequestTime(System.currentTimeMillis());
        task.setLastUpdateTime(System.currentTimeMillis());
        Activator.getLogger().info("Sending inline chat request!");
    }

    private synchronized void endSession() {
        if (!isSessionActive()) {
            return;
        }
        CompletableFuture<Void> uiThreadFuture = new CompletableFuture<>();
        cleanupContext();

        Display.getDefault().asyncExec(() -> {
            try {
                cleanupWorkbench();
                cleanupDocumentState(false);
                diffManager.cleanupState();
                uiThreadFuture.complete(null);
            } catch (Exception e) {
                Activator.getLogger().error("Error in UI cleanup: " + e.getMessage(), e);
                uiThreadFuture.completeExceptionally(e);
            }
        });

        uiThreadFuture.whenComplete((result, ex) -> {
            uiManager.closePrompt();
            cleanupSessionState();
            setState(SessionState.INACTIVE);
            Activator.getLogger().info("SESSION ENDED!");
        });
    }

    private synchronized void restoreAndEndSession() {
        if (!isSessionActive()) {
            return;
        }
        restoreState().whenComplete((res, ex) -> endSession());
    }

    private CompletableFuture<Void> restoreState() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Display.getDefault().asyncExec(() -> {
            try {
                // If previous response exists --> we know we've made document changes
                cleanupDocumentState(task.getPreviousPartialResponse() != null);
                // Clear any remaining annotations
                diffManager.restoreState();
                future.complete(null);
            } catch (Exception e) {
                Activator.getLogger().error("Error restoring editor state: " + e.getMessage(), e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private void initUndoManager(final IDocument document) {
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

    // Ensure that undo operation ends session correctly
    private void setupUndoDetection(final IDocument document) {
        if (undoManager != null) {
            undoListener = new IDocumentUndoListener() {
                @Override
                public void documentUndoNotification(final DocumentUndoEvent event) {
                    if (event.getEventType() == ABOUT_TO_UNDO && isSessionActive()) {
                        Activator.getLogger().info("Undo request being processed!");
                        if (isGenerating() || isDeciding()) {
                            uiManager.closePrompt();
                            endSession();
                        }
                    }
                }
            };
            undoManager.addDocumentUndoListener(undoListener);
        }
    }

    private boolean verifyChatResultParams(final ChatResult chatResult) {

        // End session if server responds with no suggestions
        if (chatResult.body() == null || chatResult.body().isBlank()) {
            uiManager.showNoSuggestionsNotification();
            return false;
        }

        // End session if response has code refs and user has setting disabled
        if (chatResult.codeReference() != null && chatResult.codeReference().length > 0) {
            if (!this.referencesEnabled) {
                uiManager.showCodeReferencesNotification();
                return false;
            }
        }
        return true;
    }

    public boolean isSessionActive() {
        synchronized (stateLock) {
            return currentState != SessionState.INACTIVE;
        }
    }

    public boolean isGenerating() {
        synchronized (stateLock) {
            return currentState == SessionState.GENERATING;
        }
    }

    public boolean isDeciding() {
        synchronized (stateLock) {
            return currentState == SessionState.DECIDING;
        }
    }

    void setState(final SessionState newState) {
        synchronized (stateLock) {
            this.currentState = newState;
        }
        if (task != null) {
            task.setTaskState(newState);
        }
    }

    public SessionState getCurrentState() {
        synchronized (stateLock) {
            return currentState;
        }
    }

    private void cleanupSessionState() {
        this.document = null;
        this.undoManager = null;
        this.undoListener = null;
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

    private void cleanupWorkbench() {
        try {
            if (workbenchPage != null) {
                workbenchPage.removePartListener(this);
                workbenchPage = null;
            }
        } catch (Exception e) {
            Activator.getLogger().error("Failed to clean up part listener: " + e.getMessage(), e);
        }
    }

    private void cleanupDocumentState(final boolean shouldRestoreState) {
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

    // Expand selection to include full line if user partially selects start or end line
    private IRegion expandSelectionToFullLines(final IDocument document, final ITextSelection selection) throws Exception {
        try {
            if (selection.getText().isBlank()) {
                return new Region(selection.getOffset(), 0);
            }
            var startRegion = document.getLineInformation(selection.getStartLine());
            var endRegion = document.getLineInformation(selection.getEndLine());
            int selectionLength = (endRegion.getOffset() + endRegion.getLength()) - startRegion.getOffset();

            return new Region(startRegion.getOffset(), selectionLength);
        } catch (Exception e) {
            Activator.getLogger().info("Could not calculate line information: " + e.getMessage(), e);
            return new Region(selection.getOffset(), selection.getLength());
        }
    }

    @Override
    public void partClosed(final IWorkbenchPartReference partRef) {
        if (isSessionActive() && partRef.getPart(false) == task.getEditor()) {
            Activator.getLogger().info("Editor deactivated");
            endSession();
        }
    }

}
