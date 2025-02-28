package software.aws.toolkits.eclipse.amazonq.inlineChat;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.text.undo.DocumentUndoEvent;
import org.eclipse.text.undo.DocumentUndoManagerRegistry;
import org.eclipse.text.undo.IDocumentUndoListener;
import org.eclipse.text.undo.IDocumentUndoManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.texteditor.ITextEditor;

import com.fasterxml.jackson.databind.ObjectMapper;

import software.aws.toolkits.eclipse.amazonq.chat.ChatCommunicationManager;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatPrompt;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatRequestParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatResult;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.ObjectMapperFactory;
import software.aws.toolkits.eclipse.amazonq.views.ChatUiRequestListener;
import software.aws.toolkits.eclipse.amazonq.views.model.Command;

public class InlineChatSession implements ChatUiRequestListener {

    // Session state variables
    private static InlineChatSession instance;
    private SessionState currentState = SessionState.INACTIVE;
    private final Object stateLock = new Object();
    private InlineChatTask task;

    // Dependencies
    private final InlineChatUIManager uiManager;
    private final InlineChatDiffManager diffManager;
    private ChatRequestParams params;
    private final ChatCommunicationManager chatCommunicationManager;

    // Document-update batching variables
    private IDocumentUndoManager undoManager;
    private IDocumentUndoListener undoListener;
    private IDocument document;
    private boolean isCompoundChange;

    // Context handler variables
    private final IContextService contextService;
    private IContextActivation contextActivation;
    private final String CONTEXT_ID = "org.eclipse.ui.inlineChatContext";

    private InlineChatSession() {
        chatCommunicationManager = ChatCommunicationManager.getInstance();
        chatCommunicationManager.setInlineChatRequestListener(this);
        uiManager = InlineChatUIManager.getInstance();
        diffManager = InlineChatDiffManager.getInstance();
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

            // Get the context service and activate inline chat context used for button
            contextActivation = contextService.activateContext(CONTEXT_ID);

            // Set up undoManager to batch document edits together
            this.document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
            this.undoManager = DocumentUndoManagerRegistry.getDocumentUndoManager(this.document);
            initUndoManager(this.document);

            // Create InlineChatTask to unify context between managers
            Display.getDefault().syncExec(() -> {
                final var selection = (ITextSelection) editor.getSelectionProvider().getSelection();
                task = new InlineChatTask(editor, selection.getText(), selection.getOffset());
            });

            // Set up necessary managers with the context they need
            this.uiManager.initNewTask(task);
            this.diffManager.initNewTask(task);
            chatCommunicationManager.updateInlineChatTabId(task.getTabId());

            CompletableFuture.runAsync(() -> {
                try {
                    start();
                } catch (Exception e) {
                    Display.getDefault().asyncExec(() -> {
                        endSession();
                    });
                }
            });
            return true;
        } catch (Exception e) {
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
            endSession();
            return null;
        });
    }

    // Chat server response handler
    @Override
    public void onSendToChatUi(final String message) {
        try {
            // Deserialize object
            ObjectMapper mapper = ObjectMapperFactory.getInstance();
            var rootNode = mapper.readTree(message);
            var paramsNode = rootNode.get("params");

            // Check and pass through error message if server returns exception
            if (rootNode.has("commandName") && "errorMessage".equals(rootNode.get("commandName").asText())) {
                String errorMessage = (paramsNode != null && paramsNode.has("message")) ? paramsNode.get("message").asText() : "Unknown error occurred";
                throw new RuntimeException("Error returned by server: " + errorMessage);
            }

            var isPartialResult = rootNode.get("isPartialResult").asBoolean();
            var chatResult = mapper.treeToValue(paramsNode, ChatResult.class);

            // Render diffs and move to deciding once we receive final result
            diffManager.processDiff(chatResult, isPartialResult).thenRun(() -> {
                if (!isPartialResult) {
                    setState(SessionState.DECIDING);
                    uiManager.transitionToDecidingPrompt();
                }
            }).exceptionally(throwable -> {
                Activator.getLogger().error("Failed to process diff", throwable);
                restoreAndEndSession();
                return null;
            });

        } catch (Exception e) {
            restoreAndEndSession();
        }
    }

    // Registered to accept and decline handler in plugin.xml
    public void handleDecision(final boolean userAcceptedChanges) throws Exception {
        uiManager.closePrompt();
        diffManager.handleDecision(userAcceptedChanges).thenRun(() -> {
            undoManager.endCompoundChange();
            endSession();
        }).exceptionally(throwable -> {
            Activator.getLogger().error("Failed to handle decision", throwable);
            restoreAndEndSession();
            return null;
        });
    }

    private void sendInlineChatRequest() {
        var prompt = task.getPrompt();
        var chatPrompt = new ChatPrompt(prompt, prompt, "");
        params = new ChatRequestParams(task.getTabId(), chatPrompt, null, Arrays.asList(task.getCursorState()));
        chatCommunicationManager.sendMessageToChatServer(Command.CHAT_SEND_PROMPT, params);
    }

    private void endSession() {
        cleanupDocumentState(false);
        cleanupContext();
        diffManager.cleanupState();
        uiManager.cleanupState();
        task.cleanup();
        cleanupSessionState();

        setState(SessionState.INACTIVE);
        Activator.getLogger().info("SESSION ENDED!");
    }

    private synchronized void restoreAndEndSession() {
        try {
            restoreState();
        } catch (Exception e) {
            Activator.getLogger().error("Failed to restore state: " + e.getMessage(), e);
        } finally {
            endSession();
        }
    }

    private void restoreState() throws Exception {
        Display.getDefault().asyncExec(() -> {
            try {
                cleanupDocumentState(true);

                // Clear any remaining annotations
                diffManager.restoreState();

                endSession();
            } catch (Exception e) {
                Activator.getLogger().error("Error restoring editor state: " + e.getMessage(), e);
                // In case of failure during restore, at least try to clean up the session
                endSession();
            }
        });
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
                    if (event.getEventType() == 17 && isSessionActive()) {
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
        this.task = null;
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

}
