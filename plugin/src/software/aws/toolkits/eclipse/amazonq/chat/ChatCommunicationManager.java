// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.lsp4j.ProgressParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.swt.widgets.Display;

import com.google.gson.JsonObject;

import software.aws.toolkits.eclipse.amazonq.broker.api.EventObserver;
import software.aws.toolkits.eclipse.amazonq.chat.models.ButtonClickResult;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommand;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommandName;
import software.aws.toolkits.eclipse.amazonq.chat.models.CursorState;
import software.aws.toolkits.eclipse.amazonq.chat.models.EncryptedChatParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.EncryptedQuickActionParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ErrorParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ReferenceTrackerInformation;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.encryption.DefaultLspEncryptionManager;
import software.aws.toolkits.eclipse.amazonq.lsp.encryption.LspEncryptionManager;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.JsonHandler;
import software.aws.toolkits.eclipse.amazonq.util.ObjectMapperFactory;
import software.aws.toolkits.eclipse.amazonq.util.ProgressNotificationUtils;
import software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils;
import software.aws.toolkits.eclipse.amazonq.util.ThreadingUtils;
import software.aws.toolkits.eclipse.amazonq.views.ChatUiRequestListener;
import software.aws.toolkits.eclipse.amazonq.views.model.ChatCodeReference;
import software.aws.toolkits.eclipse.amazonq.views.model.Command;

/**
 * ChatCommunicationManager is a central component of the Amazon Q Eclipse
 * Plugin that acts as a bridge between the plugin's UI and the LSP server. It
 * is also responsible for managing communication between the plugin and the
 * webview used for displaying chat conversations. It is implemented as a
 * singleton to centralize control of all communication in the plugin.
 */
public final class ChatCommunicationManager implements EventObserver<ChatUIInboundCommand> {
    private static volatile ChatCommunicationManager instance;

    private final JsonHandler jsonHandler;
    private final ChatPartialResultMap chatPartialResultMap;
    private final LspEncryptionManager lspEncryptionManager;

    private final BlockingQueue<ChatUIInboundCommand> commandQueue;

    private final Map<String, Long> lastProcessedTimeMap = new ConcurrentHashMap<>();

    private static final int MINIMUM_PARTIAL_RESPONSE_LENGTH = 50;
    private static final int MIN_DELAY_BETWEEN_PARTIALS = 500;
    private static final int MAX_DELAY_BETWEEN_PARTIALS = 2500;
    private static final int CHAR_COUNT_FOR_MAX_DELAY = 5000;

    private final ConcurrentHashMap<String, Object> partialResultLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> finalResultProcessed = new ConcurrentHashMap<>();

    private CompletableFuture<ChatUiRequestListener> chatUiRequestListenerFuture;
    private CompletableFuture<ChatUiRequestListener> inlineChatListenerFuture;
    private Map<String, CompletableFuture<String>> inflightRequestByTabId = new ConcurrentHashMap<String, CompletableFuture<String>>();

    private volatile boolean isChatReady = false;
    private volatile boolean isQueueProcessorRunning = false;
    private volatile Thread queueProcessorThread;

    private final String inlineChatTabId = "123456789";

    private ChatCommunicationManager(final Builder builder) {
        this.jsonHandler = builder.jsonHandler != null ? builder.jsonHandler : new JsonHandler();
        this.chatPartialResultMap = builder.chatPartialResultMap != null ? builder.chatPartialResultMap
                : new ChatPartialResultMap();
        this.lspEncryptionManager = builder.lspEncryptionManager != null ? builder.lspEncryptionManager
                : DefaultLspEncryptionManager.getInstance();
        chatUiRequestListenerFuture = new CompletableFuture<>();
        inlineChatListenerFuture = new CompletableFuture<>();
        commandQueue = new LinkedBlockingQueue<>();
        Activator.getEventBroker().subscribe(ChatUIInboundCommand.class, this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ChatCommunicationManager getInstance() {
        if (instance == null) {
            synchronized (ChatCommunicationManager.class) {
                if (instance == null) {
                    instance = ChatCommunicationManager.builder().build();
                }
            }
        }
        return instance;
    }

    @SuppressWarnings("MethodLength")
    public void sendMessageToChatServer(final Command command, final ChatMessage message) {
        Activator.getLspProvider().getAmazonQServer().thenAcceptAsync(amazonQLspServer -> {
            try {
                if (isQueueProcessorRunning && (queueProcessorThread != null && !queueProcessorThread.isAlive())) {
                    isQueueProcessorRunning = false;
                    startCommandQueueProcessor();
                }

                switch (command) {
                    case CHAT_SEND_PROMPT:
                        message.addValueForKey("context", message.getValueForKey("prompt.context"));
                        addEditorState(message, true);
                        sendEncryptedChatMessage(message.getValueAsString("tabId"), token -> {
                            String encryptedMessage = lspEncryptionManager.encrypt(message.getData());
                                EncryptedChatParams encryptedChatRequestParams = new EncryptedChatParams(encryptedMessage,
                                        token);
                            String tabId = message.getValueAsString("tabId");
                            var response = amazonQLspServer.sendChatPrompt(encryptedChatRequestParams);
                            inflightRequestByTabId.put(tabId, response);
                            return handleChatResponse(tabId, response);
                        });
                        break;
                    case CHAT_PROMPT_OPTION_CHANGE:
                        amazonQLspServer.promptInputOptionChange(message.getData());
                        break;
                    case CHAT_QUICK_ACTION:
                        sendEncryptedChatMessage(message.getValueAsString("tabId"), token -> {
                            String encryptedMessage = lspEncryptionManager.encrypt(message.getData());
                            EncryptedQuickActionParams encryptedQuickActionParams = new EncryptedQuickActionParams(
                                    encryptedMessage, token);
                            String tabId = message.getValueAsString("tabId");
                            var response = amazonQLspServer.sendQuickAction(encryptedQuickActionParams);
                            return handleChatResponse(tabId, response);
                        });
                        break;
                    case CHAT_READY:
                        isChatReady = true;
                        amazonQLspServer.chatReady();
                        break;
                    case CHAT_TAB_ADD:
                        amazonQLspServer.tabAdd(message.getData());
                        break;
                    case CHAT_TAB_REMOVE:
                        lastProcessedTimeMap.remove(message.getValueAsString("tabId"));
                        amazonQLspServer.tabRemove(message.getData());
                        break;
                    case CHAT_TAB_CHANGE:
                        amazonQLspServer.tabChange(message.getData());
                        break;
                    case FILE_CLICK:
                        if (validateFileInWorkspaceRoot(message.getValueAsString("fullPath"))) {
                            amazonQLspServer.fileClick(message.getData());
                        }
                        break;
                    case CHAT_INFO_LINK_CLICK:
                        amazonQLspServer.infoLinkClick(message.getData());
                        break;
                    case CHAT_LINK_CLICK:
                        amazonQLspServer.linkClick(message.getData());
                        break;
                    case CHAT_SOURCE_LINK_CLICK:
                        amazonQLspServer.sourceLinkClick(message.getData());
                        break;
                    case CHAT_FOLLOW_UP_CLICK:
                        amazonQLspServer.followUpClick(message.getData());
                        break;
                    case CHAT_END_CHAT:
                        amazonQLspServer.endChat(message.getData());
                        break;
                    case CHAT_INSERT_TO_CURSOR_POSITION:
                        amazonQLspServer.sendTelemetryEvent(message.getData());
                        break;
                    case CHAT_FEEDBACK:
                        amazonQLspServer.sendFeedback(message.getData());
                        break;
                    case STOP_CHAT_RESPONSE:
                        cancelInflightRequests(message.getValueAsString("tabId"));
                        break;
                    case TELEMETRY_EVENT:
                        amazonQLspServer.sendTelemetryEvent(message.getData());
                        break;
                    case LIST_CONVERSATIONS:
                        try {
                            Object response = amazonQLspServer.listConversations(message.getData()).get();
                            var listConversationsCommand = ChatUIInboundCommand.createCommand("aws/chat/listConversations",
                                    response);
                            Activator.getEventBroker().post(ChatUIInboundCommand.class, listConversationsCommand);
                        } catch (Exception e) {
                            Activator.getLogger().error("Error processing listConversations: " + e);
                        }
                        break;
                    case CONVERSATION_CLICK:
                        try {
                            Object response = amazonQLspServer.conversationClick(message.getData()).get();
                            var conversationClickCommand = ChatUIInboundCommand.createCommand("aws/chat/conversationClick",
                                    response);
                            Activator.getEventBroker().post(ChatUIInboundCommand.class, conversationClickCommand);
                        } catch (Exception e) {
                            Activator.getLogger().error("Error processing conversationClick: " + e);
                        }
                        break;
                    case CREATE_PROMPT:
                        amazonQLspServer.createPrompt(message.getData());
                        break;
                    case TAB_BAR_ACTION:
                        try {
                            Object response = amazonQLspServer.tabBarAction(message.getData()).get();
                            var tabBarActionsCommand = ChatUIInboundCommand.createCommand("aws/chat/tabBarAction",
                                    response);
                            Activator.getEventBroker().post(ChatUIInboundCommand.class, tabBarActionsCommand);
                        } catch (Exception e) {
                            Activator.getLogger().error("Error processing tabBarActions: " + e);
                        }
                        break;
                    case BUTTON_CLICK:
                        String tabId = message.getValueAsString("tabId");
                        ButtonClickResult response = amazonQLspServer.buttonClick(message.getData()).get();
                        if (!response.success()) {
                            sendErrorToUi(tabId, new Throwable(response.failureReason()));
                        }
                        break;
                    case LIST_MCP_SERVERS:
                        try {
                            Object mcpServersResponse = amazonQLspServer.listMcpServers(message.getData()).get();
                            var listMcpServersCommand = ChatUIInboundCommand.createCommand("aws/chat/listMcpServers",
                                    mcpServersResponse);
                            Activator.getEventBroker().post(ChatUIInboundCommand.class, listMcpServersCommand);
                        } catch (Exception e) {
                            Activator.getLogger().error("Error processing listMcpServers: " + e);
                        }
                        break;
                    case MCP_SERVER_CLICK:
                        try {
                            Object mcpServerClickResponse = amazonQLspServer.mcpServerClick(message.getData()).get();
                            var mcpServerClickCommand = ChatUIInboundCommand.createCommand("aws/chat/mcpServerClick",
                                    mcpServerClickResponse);
                            Activator.getEventBroker().post(ChatUIInboundCommand.class, mcpServerClickCommand);
                        } catch (Exception e) {
                            Activator.getLogger().error("Error processing mcpServerClick: " + e);
                        }
                        break;
                    default:
                        throw new AmazonQPluginException("Unexpected command received from Chat UI: " + command.toString());
                }
            } catch (Exception e) {
                throw new AmazonQPluginException("Error occurred when sending message to server", e);
            }
        }, ThreadingUtils.getWorkerPool()).exceptionally(throwable -> {
            Activator.getLogger().error("Failed to process message: " + throwable.getMessage());
            return null;
        });
    }

    public void sendInlineChatMessageToChatServer(final ChatMessage chatMessage) {
        Activator.getLspProvider().getAmazonQServer().thenAcceptAsync(amazonQLspServer -> {
            try {
                addEditorState(chatMessage, false);
                sendEncryptedChatMessage(inlineChatTabId, token -> {
                    String encryptedMessage = lspEncryptionManager.encrypt(chatMessage.getData());

                    EncryptedChatParams encryptedChatRequestParams = new EncryptedChatParams(encryptedMessage, token);
                    return amazonQLspServer.sendInlineChatPrompt(encryptedChatRequestParams);
                });
            } catch (Exception e) {
                throw new AmazonQPluginException("Error occurred when sending message to server", e);
            }
        });
    }

    private CompletableFuture<String> handleChatResponse(final String tabId, final CompletableFuture<String> response) {
        return response.whenComplete((result, exception) -> {
            inflightRequestByTabId.remove(tabId);
        });
    }

    protected Optional<String> getOpenFileUri() {
        AtomicReference<Optional<String>> fileUri = new AtomicReference<Optional<String>>();
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                fileUri.set(QEclipseEditorUtils.getOpenFileUri());
            }
        });
        return fileUri.get();
    }

    public void cancelInflightRequests(final String tabId) {
        var inflightRequest = inflightRequestByTabId.getOrDefault(tabId, null);
        if (inflightRequest != null) {
            inflightRequest.cancel(true);
            inflightRequestByTabId.remove(tabId);
        }
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

    private boolean validateFileInWorkspaceRoot(final String fullPath) {
        if (fullPath == null) {
            return true;
        }

        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IPath path = new Path(fullPath);

        try {
            IProject[] projects = root.getProjects();
            boolean isInProjectRoot = false;

            for (IProject project : projects) {
                if (project.isOpen()) {
                    IPath projectPath = project.getLocation();

                    if (projectPath.isPrefixOf(path)) {
                        isInProjectRoot = true;
                        break;
                    }
                }
            }

            return isInProjectRoot;
        } catch (Exception e) {
            Activator.getLogger().error("Error checking project paths", e);
        }

        return false;
    }

    private ChatMessage addEditorState(final ChatMessage chatRequestParams, final boolean addCursorState) {
        // only include files that are accessible via lsp which have absolute paths
        getOpenFileUri().ifPresent(filePathUri -> {
            chatRequestParams.addValueForKey("textDocument", new TextDocumentIdentifier(filePathUri));
            if (addCursorState) {
                getSelectionRangeCursorState().ifPresent(
                        cursorState -> chatRequestParams.addValueForKey("cursorState", Arrays.asList(cursorState)));
            }
        });
        return chatRequestParams;
    }

    private CompletableFuture<Object> sendEncryptedChatMessage(final String tabId,
            final Function<String, CompletableFuture<String>> action) {
        String partialResultToken = addPartialChatMessage(tabId);
        registerPartialResultToken(partialResultToken);

        return action.apply(partialResultToken).handle((encryptedChatResult, exception) -> {
            if (exception != null) {
                // handle cancellations
                if (exception instanceof CancellationException
                        || exception.getCause() instanceof CancellationException) {
                    ChatAsyncResultManager manager = ChatAsyncResultManager.getInstance();
                    try {
                        manager.createRequestId(partialResultToken);
                        manager.getResult(partialResultToken);
                        handleCancellation(tabId);
                    } catch (Exception e) {
                        Activator.getLogger().error("An error occurred while processing cancellation: " + exception.getMessage());
                    } finally {
                        manager.removeRequestId(partialResultToken);
                        partialResultLocks.remove(partialResultToken);
                        finalResultProcessed.remove(partialResultToken);
                        lastProcessedTimeMap.remove(tabId);
                    }
                    return null;
                }

                // handle non-cancellation errors
                Activator.getLogger()
                        .error("An error occurred while processing chat request: " + exception.getMessage());
                sendErrorToUi(tabId, exception);
                removePartialChatMessage(partialResultToken);
                partialResultLocks.remove(partialResultToken);
                finalResultProcessed.remove(partialResultToken);
                lastProcessedTimeMap.remove(tabId);
                return null;
            }

            // process successful responses
            removePartialChatMessage(partialResultToken);
            try {
                finalResultProcessed.put(partialResultToken, true);
                String serializedData = lspEncryptionManager.decrypt(encryptedChatResult);
                Map<String, Object> result = jsonHandler.deserialize(serializedData, Map.class);

                if (result.containsKey("codeReference")) {
                    ReferenceTrackerInformation[] codeReferences = ObjectMapperFactory.getInstance()
                            .convertValue(result.get("codeReference"), ReferenceTrackerInformation[].class);
                    if (codeReferences != null && codeReferences.length >= 1) {
                        Activator.getCodeReferenceLoggingService()
                                .log(new ChatCodeReference(codeReferences));
                    }
                }

                String command = inlineChatTabId.equals(tabId)
                        ? ChatUIInboundCommandName.InlineChatPrompt.getValue()
                        : ChatUIInboundCommandName.ChatPrompt.getValue();

                sendMessageToChatUI(new ChatUIInboundCommand(command, tabId, result, false, null));
                return result;
            } catch (Exception e) {
                Activator.getLogger()
                        .error("An error occurred while processing chat response: " + e.getMessage());
                sendErrorToUi(tabId, e);
                partialResultLocks.remove(partialResultToken);
                finalResultProcessed.remove(partialResultToken);
                return null;
            }
        });
    }

    void registerPartialResultToken(final String partialResultToken) {
        Object lock = new Object();
        partialResultLocks.put(partialResultToken, lock);
        finalResultProcessed.put(partialResultToken, false);
    }

    // Workaround to properly report cancellation event to chatUI
    private CompletableFuture<Void> handleCancellation(final String tabId) {
        Activator.getLogger().info("Chat request was cancelled for tab: " + tabId);
        lastProcessedTimeMap.remove(tabId);

        var errorParams = new ErrorParams(tabId, null, "", "");
        ChatUIInboundCommand inbound = new ChatUIInboundCommand(
                ChatUIInboundCommandName.ErrorMessage.getValue(), tabId, errorParams, false, null);
        sendMessageToChatUI(inbound);
        return CompletableFuture.completedFuture(null);
    }

    private void sendErrorToUi(final String tabId, final Throwable exception) {
        String errorTitle = "An error occurred while processing your request.";
        String errorMessage = extractErrorMessage(exception);
        ErrorParams errorParams = new ErrorParams(tabId, null, errorMessage, errorTitle);
        ChatUIInboundCommand chatUIInboundCommand = new ChatUIInboundCommand(
                ChatUIInboundCommandName.ErrorMessage.getValue(), tabId, errorParams, false, null);
        sendMessageToChatUI(chatUIInboundCommand);
    }

    private String extractErrorMessage(final Throwable exception) {
        if (exception instanceof ResponseErrorException) {
            ResponseError responseError = ((ResponseErrorException) exception).getResponseError();
            if (responseError != null && responseError.getData() instanceof JsonObject) {
                JsonObject responseData = (JsonObject) responseError.getData();
                if (responseData.has("type") && "answer".equals(responseData.get("type").getAsString())
                    && responseData.has("body")) {
                    String body = responseData.get("body").getAsString();
                    // Convert literal \n characters to proper line breaks for QModelResponse errors with Request IDs
                    // Language server sends these errors with \n\n patterns (e.g., "Error message \n\nRequest ID: 123")
                    body = body.replace("\\n", System.lineSeparator());
                    return String.format("Details: %s", body);
                } else {
                    return String.format("Details: %s", responseError.getMessage());
                }
            } else if (responseError != null) {
                return String.format("Details: %s", responseError.getMessage());
            } else {
                return String.format("Details: %s", exception.getMessage());
            }
        } else if (exception.getCause() instanceof ResponseErrorException) {
            ResponseError responseError = ((ResponseErrorException) exception.getCause()).getResponseError();
            if (responseError != null) {
                return String.format("Details: %s", responseError.getMessage());
            } else {
                return String.format("Details: %s", exception.getMessage());
            }
        } else {
            return String.format("Details: %s", exception.getMessage());
        }
    }


    public void setChatUiRequestListener(final ChatUiRequestListener listener) {
        if (listener != null) {
            chatUiRequestListenerFuture.complete(listener);
        }
    }

    public void setInlineChatRequestListener(final ChatUiRequestListener listener) {
        if (listener != null) {
            inlineChatListenerFuture.complete(listener);
        }
    }

    public void removeListener(final ChatUiRequestListener listener) {
        if (chatUiRequestListenerFuture.isDone() && listener == chatUiRequestListenerFuture.join()) {
            chatUiRequestListenerFuture = new CompletableFuture<>();
        } else if (inlineChatListenerFuture.isDone() && listener == inlineChatListenerFuture.join()) {
            inlineChatListenerFuture = new CompletableFuture<>();
        }
    }

    public void activate() {
        startCommandQueueProcessor();
    }

    /*
     * Handles chat progress notifications from the Amazon Q LSP server. - Process
     * partial results for Chat messages if provided token is maintained by
     * ChatCommunicationManager - Other notifications are ignored at this time. -
     * Sends a partial chat prompt message to the webview.
     */
    public void handlePartialResultProgressNotification(final ProgressParams params) {
        String token = ProgressNotificationUtils.getToken(params);
        String tabId = getPartialChatMessage(token);

        if (tabId == null || tabId.isEmpty()) {
            return;
        }

        if (params.getValue().isLeft() || Objects.isNull(params.getValue().getRight())) {
            throw new AmazonQPluginException(
                    "Error handling partial result notification: expected value of type Object");
        }

        String encryptedPartialChatResult = ProgressNotificationUtils.getObject(params, String.class);
        String serializedData = lspEncryptionManager.decrypt(encryptedPartialChatResult);
        Map<String, Object> partialChatResult = jsonHandler.deserialize(serializedData, Map.class);

        if (partialChatResult == null) {
            return;
        }

        String command = inlineChatTabId.equals(tabId)
                ? ChatUIInboundCommandName.InlineChatPrompt.getValue()
                : ChatUIInboundCommandName.ChatPrompt.getValue();

        // special case: check for stop message before acquiring lock
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> additionalMessages = (List<Map<String, Object>>) partialChatResult.get("additionalMessages");
        if (additionalMessages != null) {
            for (Map<String, Object> message : additionalMessages) {
                String messageId = (String) message.get("messageId");
                if (messageId != null && messageId.startsWith("stopped")) {
                    // process stop messages immediately
                    sendMessageToChatUI(new ChatUIInboundCommand(command, tabId, partialChatResult, true, null));
                    finalResultProcessed.put(token, true);
                    ChatAsyncResultManager.getInstance().setResult(token, partialChatResult);
                    return;
                }
            }
        }

        // normal partial processing
        Object lock = partialResultLocks.get(token);
        if (lock == null) {
            return;
        }

        synchronized (lock) {
            if (partialResultLocks.get(token) == null || Boolean.TRUE.equals(finalResultProcessed.get(token))) {
                return;
            }

            Object body = partialChatResult.get("body");
            boolean hasAdditionalMessages = (additionalMessages != null && !additionalMessages.isEmpty());
            long currentTime = System.currentTimeMillis();

            // rate limit by discarding messages that have arrived too soon since the last was fired
            if (!hasAdditionalMessages && body instanceof String) {
                Long lastProcessedTime = lastProcessedTimeMap.get(tabId);
                if (lastProcessedTime != null) {
                    int currentDelay = calculateDelay((String) body);
                    if ((currentTime - lastProcessedTime) < currentDelay) {
                        return;
                    }
                }
            }

            boolean insufficientContent = (body == null
                    || (body instanceof String && ((String) body).length() < MINIMUM_PARTIAL_RESPONSE_LENGTH));
            if (insufficientContent && !hasAdditionalMessages) {
                return;
            }

            // send partial response to UI if not cancelled in the interim
            if (Boolean.FALSE.equals(finalResultProcessed.get(token))) {
                sendMessageToChatUI(new ChatUIInboundCommand(command, tabId, partialChatResult, true, null));
                lastProcessedTimeMap.put(tabId, currentTime);
            }
        }
    }

    private int calculateDelay(final String bodyString) {
        if (bodyString == null || bodyString.isEmpty()) {
            return MIN_DELAY_BETWEEN_PARTIALS;
        }
        int length = bodyString.length();
        double ratio = Math.min(1.0, (double) length / CHAR_COUNT_FOR_MAX_DELAY);
        int delay = (int) (MIN_DELAY_BETWEEN_PARTIALS + (MAX_DELAY_BETWEEN_PARTIALS - MIN_DELAY_BETWEEN_PARTIALS) * ratio);
        return delay;
    }

    @Override
    public void onEvent(final ChatUIInboundCommand command) {
        commandQueue.add(command);
    }

    /*
     * Sends message to Chat UI to show in webview
     */
    private void sendMessageToChatUI(final ChatUIInboundCommand command) {
        String message = jsonHandler.serialize(command);
        String inlineChatCommand = ChatUIInboundCommandName.InlineChatPrompt.getValue();
        if (inlineChatCommand.equals(command.command())) {
            inlineChatListenerFuture.thenApply(listener -> {
                listener.onSendToChatUi(message);
                return listener;
            });
        } else {
            chatUiRequestListenerFuture.thenApply(listener -> {
                listener.onSendToChatUi(message);
                return listener;
            });
        }
    }

    /*
     * Gets the partial chat message represented by the tabId using the provided
     * token.
     */
    private String getPartialChatMessage(final String partialResultToken) {
        return chatPartialResultMap.getValue(partialResultToken);
    }

    /*
     * Adds an entry to the partialResultToken to ChatMessage's tabId map.
     */
    private String addPartialChatMessage(final String tabId) {
        String partialResultToken = UUID.randomUUID().toString();
        chatPartialResultMap.setEntry(partialResultToken, tabId);
        return partialResultToken;
    }

    /*
     * Removes an entry from the partialResultToken to ChatMessage's tabId map.
     */
    private void removePartialChatMessage(final String partialResultToken) {
        String tabId = chatPartialResultMap.getValue(partialResultToken);
        chatPartialResultMap.removeEntry(partialResultToken);
        if (tabId != null) {
            lastProcessedTimeMap.remove(tabId);
        }
    }

    private void startCommandQueueProcessor() {
        if (isQueueProcessorRunning) {
            return;
        }
        isQueueProcessorRunning = true;
        ThreadingUtils.executeAsyncTask(() -> {
            queueProcessorThread = Thread.currentThread();
            while (isQueueProcessorRunning && !Thread.currentThread().isInterrupted()) {
                try {
                    if (!isChatReady) {
                        Thread.sleep(100);
                        continue;
                    }
                    ChatUIInboundCommand command = commandQueue.take();
                    sendMessageToChatUI(command);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    isQueueProcessorRunning = false;
                } catch (Exception e) {
                    Activator.getLogger().error("Error processing command from queue", e);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        isQueueProcessorRunning = false;
                    }
                }
            }
            isQueueProcessorRunning = false;
        });
    }

    public static final class Builder {

        private JsonHandler jsonHandler;
        private ChatPartialResultMap chatPartialResultMap;
        private LspEncryptionManager lspEncryptionManager;

        public Builder withJsonHandler(final JsonHandler jsonHandler) {
            this.jsonHandler = jsonHandler;
            return this;
        }

        public Builder withChatPartialResultMap(final ChatPartialResultMap chatPartialResultMap) {
            this.chatPartialResultMap = chatPartialResultMap;
            return this;
        }

        public Builder withLspEncryptionManager(final LspEncryptionManager lspEncryptionManager) {
            this.lspEncryptionManager = lspEncryptionManager;
            return this;
        }

        public ChatCommunicationManager build() {
            return new ChatCommunicationManager(this);
        }

    }

}
