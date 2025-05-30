// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.inlineChat;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.swt.custom.VerifyKeyListener;
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
import software.aws.toolkits.eclipse.amazonq.chat.ChatMessage;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatPrompt;
import software.aws.toolkits.eclipse.amazonq.chat.models.InlineChatRequestParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.InlineChatResult;
import software.aws.toolkits.eclipse.amazonq.editor.InMemoryInput;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.preferences.AmazonQPreferencePage;
import software.aws.toolkits.eclipse.amazonq.telemetry.CodeWhispererTelemetryProvider;
import software.aws.toolkits.eclipse.amazonq.util.Constants;
import software.aws.toolkits.eclipse.amazonq.util.LanguageUtil;
import software.aws.toolkits.eclipse.amazonq.util.ObjectMapperFactory;
import software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils;
import software.aws.toolkits.eclipse.amazonq.util.ThemeDetector;
import software.aws.toolkits.eclipse.amazonq.views.ChatUiRequestListener;

public final class InlineChatSession extends FoldingListener implements ChatUiRequestListener, IPartListener2 {

    // Session state variables
    private static InlineChatSession instance;
    private SessionState currentState = SessionState.INACTIVE;
    private final Object stateLock = new Object();
    private InlineChatTask task;
    private boolean referencesEnabled;
    private IWorkbenchPage workbenchPage;
    private ProjectionAnnotationModel projectionModel;

    // Dependencies
    private final InlineChatUIManager uiManager;
    private final InlineChatDiffManager diffManager;
    private InlineChatRequestParams params;
    private final ChatCommunicationManager chatCommunicationManager;
    private final ThemeDetector themeDetector;

    // Document-update batching variables
    private IDocumentUndoManager undoManager;
    private IDocumentUndoListener undoListener;
    private IDocument document;
    private boolean isCompoundChange;
    private VerifyKeyListener verifyKeyListener;

    // Context handler variables
    private final IContextService contextService;
    private IContextActivation contextActivation;
    private final int aboutToUndo = 17; // 17 maps to this event type

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
        if (editor == null || !(editor instanceof ITextEditor) || (editor.getEditorInput() instanceof InMemoryInput)) {
            return false;
        }
        try {
            InlineChatEditorListener.getInstance().closePrompt();

            this.document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
            if (this.document == null) {
                return false;
            }
            setState(SessionState.ACTIVE);
            // Get the context service and activate inline chat context used for button
            contextActivation = contextService.activateContext(Constants.INLINE_CHAT_CONTEXT_ID);

            workbenchPage = editor.getSite().getPage();
            workbenchPage.addPartListener(this);

            // Set up undoManager to batch document edits together
            this.undoManager = DocumentUndoManagerRegistry.getDocumentUndoManager(this.document);
            initUndoManager(this.document);

            Display.getDefault().asyncExec(() -> {
                projectionModel = attachFoldingListener(editor);
            });

            // Check if user has code references enabled
            var currentLoginType = Activator.getLoginService().getAuthState().loginType();
            this.referencesEnabled = Activator.getDefault().getPreferenceStore().getBoolean(AmazonQPreferencePage.CODE_REFERENCE_OPT_IN)
                    && currentLoginType.equals(LoginType.BUILDER_ID);

            createInlineChatTask(editor);
            var isDarkTheme = themeDetector.isDarkTheme();
            // Set up necessary managers with the context they need
            this.uiManager.initNewTask(task, isDarkTheme);
            this.diffManager.initNewTask(task, isDarkTheme);

            CompletableFuture.runAsync(() -> start());
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
                blockUserInput(true);
            } else {
                endSession();
                Activator.getLogger().info("Inline chat not submitted. Ending session.");
            }
        }).exceptionally(throwable -> {
            Activator.getLogger().error("Failed to open user input prompt: " + throwable.getMessage());
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
            var chatResult = mapper.treeToValue(paramsNode, InlineChatResult.class);

            if (chatResult != null && chatResult.messageId() != null) {
                task.setRequestId(chatResult.messageId());
            }

            if (!verifyChatResultParams(chatResult)) {
                restoreAndEndSession();
                return;
            }

            // Render diffs and move to deciding once we receive final result
            diffManager.processDiff(chatResult, isPartialResult).thenRun(() -> {
                if (!isPartialResult) {
                    setState(SessionState.DECIDING);
                    uiManager.transitionToDecidingPrompt();
                    blockUserInput(false);
                }
            }).exceptionally(throwable -> {
                Activator.getLogger().error("Failed to process diff: " + throwable.getMessage());
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
            Activator.getLogger().error("Failed to handle decision: " + throwable.getMessage());
            uiManager.showErrorNotification();
            restoreAndEndSession();
            return null;
        });
    }

    private void sendInlineChatRequest() {
        try {
            var prompt = task.getPrompt();
            var chatPrompt = new ChatPrompt(prompt, prompt, "", Collections.emptyList());
            params = new InlineChatRequestParams(chatPrompt, null, Arrays.asList(task.getCursorState()));
            chatCommunicationManager.sendInlineChatMessageToChatServer(new ChatMessage(params));

            Optional<String> fileUri = QEclipseEditorUtils.getOpenFileUri();
            if (fileUri.isPresent()) {
                String language = LanguageUtil.extractLanguageFromFileUri(fileUri.get());
                task.setLanguage(language);
            }

            task.setRequestTime(System.currentTimeMillis());
        } catch (Exception e) {
            Activator.getLogger().error("Failed to send message to chat server: " + e.getMessage());
            endSession();
        }
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
                removeFoldingListener(projectionModel);
                uiThreadFuture.complete(null);
            } catch (Exception e) {
                Activator.getLogger().error("Error in UI cleanup: " + e.getMessage());
                uiThreadFuture.completeExceptionally(e);
            }
        });

        uiThreadFuture.whenComplete((result, ex) -> {
            try {
                var inlineChatSessionResult = task.buildResultObject();
                emitInlineChatEventMetric(inlineChatSessionResult);
            } catch (Exception e) {
                Activator.getLogger().error("FAILURE ON EMISSION: " + e.getMessage());
            }
            uiManager.endSession();
            diffManager.endSession();
            cleanupSessionState();
            setState(SessionState.INACTIVE);
            Activator.getLogger().info("Inline chat session ended.");
        }).thenRun(() -> {
            task = null;
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
                Activator.getLogger().error("Error restoring editor state: " + e.getMessage());
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
                    if (event.getEventType() == aboutToUndo && isSessionActive()) {
                        if (isGenerating() || isDeciding()) {
                            if (isDeciding()) {
                                task.setUserDecision(false);
                            }
                            uiManager.closePrompt();
                            endSession();
                        }
                    }
                }
            };
            undoManager.addDocumentUndoListener(undoListener);
        }
    }

    private boolean verifyChatResultParams(final InlineChatResult chatResult) {
        if (chatResult == null) {
            return false;
        }

        var options = (chatResult.followUp() != null)
            ? chatResult.followUp().options()
            : null;
        String type = (options != null)
            ? options[0].type()
            : null;

        // End session if user auth status needs refreshing
        if (type == "full-auth" || type == "re-auth") {
            uiManager.showAuthExpiredNotification();
            return false;
        }

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

    private void setState(final SessionState newState) {
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
        if (verifyKeyListener != null) {
            try {
                var viewer = task.getEditor().getAdapter(ITextViewer.class);
                ((ITextViewerExtension) viewer).removeVerifyKeyListener(verifyKeyListener);
                verifyKeyListener = null;
            } catch (Exception e) {
                Activator.getLogger().error("Failed to remove verify key listener: " + e.getMessage());
            }
        }
    }

    private void cleanupContext() {
        try {
            if (contextService != null && contextActivation != null) {
                contextService.deactivateContext(contextActivation);
                contextActivation = null;
            }
        } catch (Exception e) {
            Activator.getLogger().error("Error cleaning up context: " + e.getMessage());
        }
    }

    private void cleanupWorkbench() {
        try {
            if (workbenchPage != null) {
                workbenchPage.removePartListener(this);
                workbenchPage = null;
            }
        } catch (Exception e) {
            Activator.getLogger().error("Failed to clean up part listener: " + e.getMessage());
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
            Activator.getLogger().error("Error cleaning up document state: " + e.getMessage());
        }
    }

    // Create InlineChatTask to unify context between managers
    private void createInlineChatTask(final ITextEditor editor) {
        Display.getDefault().syncExec(() -> {
            /* Ensure visual offset begins at start of selection and
             * that selection always includes full line */
            final var selection = (ITextSelection) editor.getSelectionProvider().getSelection();
            if (selection == null) {
                uiManager.showErrorNotification();
                endSession();
                return;
            }
            int selectedLines = selection.getEndLine() - selection.getStartLine() + 1;
            try {
                final var region = expandSelectionToFullLines(document, selection);
                final String selectionText = document.get(region.getOffset(), region.getLength());
                task = new InlineChatTask(editor, selectionText, region, selectedLines);
            } catch (Exception e) {
                Activator.getLogger().error("Failed to expand selection region: " + e.getMessage());
                var region = new Region(selection.getOffset(), selection.getLength());
                task = new InlineChatTask(editor, selection.getText(), region, selectedLines);
            }
        });
    }

    private void blockUserInput(final boolean blockInput) {
        Display.getDefault().asyncExec(() -> {
            ITextEditor editor = task.getEditor();
            ITextViewer viewer = editor.getAdapter(ITextViewer.class);
            if (viewer != null) {
                if (blockInput) {
                    verifyKeyListener = event -> event.doit = false;
                    ((ITextViewerExtension) viewer).prependVerifyKeyListener(verifyKeyListener);
                } else {
                    ((ITextViewerExtension) viewer).removeVerifyKeyListener(verifyKeyListener);
                    verifyKeyListener = null;
                }
            }
        });
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
            Activator.getLogger().error("Could not calculate line information: " + e.getMessage());
            return new Region(selection.getOffset(), selection.getLength());
        }
    }

    private void emitInlineChatEventMetric(final InlineChatResultParams params) {
        CodeWhispererTelemetryProvider.emitInlineChatEventMetric(params);
    }

    // End session when editor is closed
    @Override
    public void partClosed(final IWorkbenchPartReference partRef) {
        if (isSessionActive() && partRef.getPart(false) == task.getEditor()) {
            Activator.getLogger().info("Editor closed. Ending inline chat session.");
            endSession();
        }
    }

    // Ensure UI prompts update position when selection offset changes
    @Override
    public void modelChanged(final IAnnotationModel model) {
        if (model instanceof ProjectionAnnotationModel) {
            if (isGenerating() || isDeciding()) {
                InlineChatUIManager.getInstance().updatePromptPosition(getCurrentState());
            }
        }
    }

}
