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

import org.eclipse.lsp4j.ProgressParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.swt.widgets.Display;

import software.aws.toolkits.eclipse.amazonq.broker.api.EventObserver;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommand;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommandName;
import software.aws.toolkits.eclipse.amazonq.chat.models.CursorState;
import software.aws.toolkits.eclipse.amazonq.chat.models.EncryptedChatParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.EncryptedQuickActionParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ErrorParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.InlineChatRequestParams;
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
import software.aws.toolkits.eclipse.amazonq.util.WorkspaceUtils;
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
    private final CompletableFuture<ChatMessageProvider> chatMessageProvider;
    private final ChatPartialResultMap chatPartialResultMap;
    private final LspEncryptionManager lspEncryptionManager;

    private final BlockingQueue<ChatUIInboundCommand> commandQueue;

    private final Map<String, Long> lastProcessedTimeMap = new ConcurrentHashMap<>();

    private static final int MINIMUM_PARTIAL_RESPONSE_LENGTH = 50;
    private static final int MIN_DELAY_BETWEEN_PARTIALS = 50;
    private static final int MAX_DELAY_BETWEEN_PARTIALS = 2500;
    private static final int CHAR_COUNT_FOR_MAX_DELAY = 5000;

    private final ConcurrentHashMap<String, Object> partialResultLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> finalResultProcessed = new ConcurrentHashMap<>();

    private CompletableFuture<ChatUiRequestListener> chatUiRequestListenerFuture;
    private CompletableFuture<ChatUiRequestListener> inlineChatListenerFuture;

    private final String inlineChatTabId = "123456789";

    private ChatCommunicationManager(final Builder builder) {
        this.jsonHandler = builder.jsonHandler != null ? builder.jsonHandler : new JsonHandler();
        this.chatMessageProvider = builder.chatMessageProvider != null ? builder.chatMessageProvider
                : ChatMessageProvider.createAsync();
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

    public void sendMessageToChatServer(final Command command, final ChatMessage message) {
        chatMessageProvider.thenAcceptAsync(chatMessageProvider -> {
            try {
                switch (command) {
                case CHAT_SEND_PROMPT:
                    message.addValueForKey("context", message.getValueForKey("prompt.context"));
                    addEditorState(message, true);
                    sendEncryptedChatMessage(message.getValueAsString("tabId"), token -> {
                        String encryptedMessage = lspEncryptionManager.encrypt(message.getData());

                        EncryptedChatParams encryptedChatRequestParams = new EncryptedChatParams(encryptedMessage,
                                token);

                        return chatMessageProvider.sendChatPrompt(message.getValueAsString("tabId"),
                                new ChatMessage(encryptedChatRequestParams));
                    });
                    break;
                case CHAT_PROMPT_OPTION_CHANGE:
                    chatMessageProvider.sendPromptInputOptionChange(message);
                    break;
                case CHAT_QUICK_ACTION:
                    sendEncryptedChatMessage(message.getValueAsString("tabId"), token -> {
                        String encryptedMessage = lspEncryptionManager.encrypt(message.getData());

                        EncryptedQuickActionParams encryptedQuickActionParams = new EncryptedQuickActionParams(
                                encryptedMessage, token);

                        return chatMessageProvider.sendQuickAction(message.getValueAsString("tabId"),
                                new ChatMessage(encryptedQuickActionParams));
                    });
                    break;
                case CHAT_READY:
                    ThreadingUtils.executeAsyncTask(() -> {
                        chatMessageProvider.sendChatReady();
                        startCommandQueueProcessor();
                    });
                    break;
                case CHAT_TAB_ADD:
                    chatMessageProvider.sendTabAdd(message);
                    break;
                case CHAT_TAB_REMOVE:
                    chatMessageProvider.sendTabRemove(message);
                    break;
                case CHAT_TAB_CHANGE:
                    chatMessageProvider.sendTabChange(message);
                    break;
                case FILE_CLICK:
                    chatMessageProvider.sendFileClick(message);
                    break;
                case CHAT_INFO_LINK_CLICK:
                    chatMessageProvider.sendInfoLinkClick(message);
                    break;
                case CHAT_LINK_CLICK:
                    chatMessageProvider.sendLinkClick(message);
                    break;
                case CHAT_SOURCE_LINK_CLICK:
                    chatMessageProvider.sendSourceLinkClick(message);
                    break;
                case CHAT_FOLLOW_UP_CLICK:
                    chatMessageProvider.followUpClick(message);
                    break;
                case CHAT_END_CHAT:
                    chatMessageProvider.endChat(message);
                    break;
                case CHAT_INSERT_TO_CURSOR_POSITION:
                    chatMessageProvider.sendTelemetryEvent(message);
                    break;
                case CHAT_FEEDBACK:
                    chatMessageProvider.sendFeedback(message);
                    break;
                case STOP_CHAT_RESPONSE:
                    chatMessageProvider.cancelInflightRequests(message.getValueAsString("tabId"));
                    break;
                case TELEMETRY_EVENT:
                    chatMessageProvider.sendTelemetryEvent(message);
                    break;
                case LIST_CONVERSATIONS:
                    ThreadingUtils.executeAsyncTask(() -> {
                        try {
                            Object response = chatMessageProvider.sendListConversations(message).get();
                            var listConversationsCommand = ChatUIInboundCommand.createCommand("aws/chat/listConversations", response);
                            Activator.getEventBroker().post(ChatUIInboundCommand.class, listConversationsCommand);
                        } catch (Exception e) {
                            Activator.getLogger().error("Error processing listConversations: " + e);
                        }
                    });
                    break;
                case CONVERSATION_CLICK:
                    ThreadingUtils.executeAsyncTask(() -> {
                        try {
                            Object response = chatMessageProvider.sendConversationClick(message).get();
                            var conversationClickCommand = ChatUIInboundCommand.createCommand("aws/chat/conversationClick", response);
                            Activator.getEventBroker().post(ChatUIInboundCommand.class, conversationClickCommand);
                        } catch (Exception e) {
                            Activator.getLogger().error("Error processing conversationClick: " + e);
                        }
                    });
                case CREATE_PROMPT:
                    chatMessageProvider.sendCreatePrompt(message);
                    break;
                case TAB_BAR_ACTION:
                    ThreadingUtils.executeAsyncTask(() -> {
                        try {
                            Object response = chatMessageProvider.sendTabBarActions(message).get();
                            var tabBarActionsCommand = ChatUIInboundCommand.createCommand("aws/chat/tabBarAction", response);
                            Activator.getEventBroker().post(ChatUIInboundCommand.class, tabBarActionsCommand);
                        } catch (Exception e) {
                            Activator.getLogger().error("Error processing tabBarActions: " + e);
                        }
                    });
                case BUTTON_CLICK:
                    chatMessageProvider.sendButtonClick(message);
                    ThreadingUtils.scheduleAsyncTaskWithDelay(() -> {
                        WorkspaceUtils.refreshAllProjects();
                    }, 1000);
                    break;
                default:
                    throw new AmazonQPluginException("Unexpected command received from Chat UI: " + command.toString());
                }
            } catch (Exception e) {
                throw new AmazonQPluginException("Error occurred when sending message to server", e);
            }
        }, ThreadingUtils.getWorkerPool());
    }

    public void sendInlineChatMessageToChatServer(final InlineChatRequestParams params) {
        chatMessageProvider.thenAcceptAsync(chatMessageProvider -> {
            try {
                ChatMessage chatRequestParams = new ChatMessage(params);
                addEditorState(chatRequestParams, false);
                sendEncryptedChatMessage(inlineChatTabId, token -> {
                    String encryptedMessage = lspEncryptionManager.encrypt(chatRequestParams.getData());

                    EncryptedChatParams encryptedChatRequestParams = new EncryptedChatParams(encryptedMessage, token);
                    return chatMessageProvider.sendInlineChatPrompt(new ChatMessage(encryptedChatRequestParams));
                });
            } catch (Exception e) {
                throw new AmazonQPluginException("Error occurred when sending message to server", e);
            }
        });
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

    private CompletableFuture<Object> sendEncryptedChatMessage(final String tabId,
            final Function<String, CompletableFuture<String>> action) {
        // Retrieving the chat result is expected to be a long-running process with
        // intermittent progress notifications being sent
        // from the LSP server. The progress notifications provide a token and a partial
        // result Object - we are utilizing a token to
        // ChatMessage mapping to acquire the associated ChatMessage so we can formulate
        // a message for the UI.
        String partialResultToken = addPartialChatMessage(tabId);
        registerPartialResultToken(partialResultToken);

        return action.apply(partialResultToken).handle((encryptedChatResult, exception) -> {
            // The mapping entry no longer needs to be maintained once the final result is
            // retrieved.
            if (exception != null) {
                if (exception instanceof CancellationException
                        || exception.getCause() instanceof CancellationException) {
                    handleCancellation(tabId)
                            .thenRunAsync(() -> {
                                try {
                                    Thread.sleep(1000);
                                    removePartialChatMessage(partialResultToken);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            })
                            .whenComplete((res, ex) -> {
                                partialResultLocks.remove(partialResultToken);
                                finalResultProcessed.remove(partialResultToken);
                            });
                    return null;
                }
                Activator.getLogger()
                        .error("An error occurred while processing chat request: " + exception.getMessage());
                sendErrorToUi(tabId, exception);
                removePartialChatMessage(partialResultToken);
                partialResultLocks.remove(partialResultToken);
                finalResultProcessed.remove(partialResultToken);
                return null;
            } else {
                removePartialChatMessage(partialResultToken);
                try {
                    finalResultProcessed.put(partialResultToken, true);
                    String serializedData = lspEncryptionManager.decrypt(encryptedChatResult);
                    Map<String, Object> result = jsonHandler.deserialize(serializedData, Map.class);
                    if (result.containsKey("codeReference")) {
                        ReferenceTrackerInformation[] codeReferences = ObjectMapperFactory.getInstance().convertValue(
                                result.get("codeReference"),
                                ReferenceTrackerInformation[].class);
                        if (codeReferences != null && codeReferences.length >= 1) {
                            ChatCodeReference chatCodeReference = new ChatCodeReference(codeReferences);
                            Activator.getCodeReferenceLoggingService().log(chatCodeReference);
                        }
                    }

                    // show chat response in Chat UI
                    String command = (inlineChatTabId.equals(tabId))
                            ? ChatUIInboundCommandName.InlineChatPrompt.getValue()
                            : ChatUIInboundCommandName.ChatPrompt.getValue();
                    ChatUIInboundCommand chatUIInboundCommand = new ChatUIInboundCommand(
                            command, tabId, result, false, null);
                    sendMessageToChatUI(chatUIInboundCommand);
                    return result;
                } catch (Exception e) {
                    Activator.getLogger()
                            .error("An error occurred while processing chat response received: " + e.getMessage());
                    sendErrorToUi(tabId, e);
                    partialResultLocks.remove(partialResultToken);
                    finalResultProcessed.remove(partialResultToken);
                    return null;
                }
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

        var errorParams = new ErrorParams(tabId, null, "", "");
        ChatUIInboundCommand inbound = new ChatUIInboundCommand(
                ChatUIInboundCommandName.ErrorMessage.getValue(), tabId, errorParams, false, null);
        sendMessageToChatUI(inbound);
        return CompletableFuture.completedFuture(null);
    }

    private void sendErrorToUi(final String tabId, final Throwable exception) {
        String errorTitle = "An error occurred while processing your request.";
        String errorMessage = String.format("Details: %s", exception.getMessage());
        ErrorParams errorParams = new ErrorParams(tabId, null, errorMessage, errorTitle);
        // show error in Chat UI
        ChatUIInboundCommand chatUIInboundCommand = new ChatUIInboundCommand(
                ChatUIInboundCommandName.ErrorMessage.getValue(), tabId, errorParams, false, null);
        sendMessageToChatUI(chatUIInboundCommand);
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

    /*
     * Handles chat progress notifications from the Amazon Q LSP server. - Process
     * partial results for Chat messages if provided token is maintained by
     * ChatCommunicationManager - Other notifications are ignored at this time. -
     * Sends a partial chat prompt message to the webview.
     */
    public void handlePartialResultProgressNotification(final ProgressParams params) {
        String token = ProgressNotificationUtils.getToken(params);
        String tabId = getPartialChatMessage(token);

        if (tabId == null || tabId.isEmpty() || partialResultLocks.get(token) == null) {
            return;
        }

        synchronized (partialResultLocks.get(token)) {
            if (Boolean.TRUE.equals(finalResultProcessed.get(token))) {
                return;
            }

            // Check to ensure Object is sent in params
            if (params.getValue().isLeft() || Objects.isNull(params.getValue().getRight())) {
                throw new AmazonQPluginException(
                        "Error handling partial result notification: expected value of type Object");
            }

            String encryptedPartialChatResult = ProgressNotificationUtils.getObject(params, String.class);
            String serializedData = lspEncryptionManager.decrypt(encryptedPartialChatResult);
            long currentTime = System.currentTimeMillis();
            Map<String, Object> partialChatResult = jsonHandler.deserialize(serializedData, Map.class);
            if (partialChatResult != null) {
                Object body = partialChatResult.get("body");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> additionalMessages = (List<Map<String, Object>>) partialChatResult
                        .get("additionalMessages");

                // If there are no additional messages, apply the delay check
                boolean noAdditionalMessages = (additionalMessages == null || additionalMessages.isEmpty());
                if (noAdditionalMessages) {
                    if (body == null || (body instanceof String)) {
                        String bodyString = (String) body;
                        Long lastProcessedTime = lastProcessedTimeMap.get(tabId);
                        if (lastProcessedTime != null) {
                            int currentDelay = calculateDelay(bodyString);
                            if ((currentTime - lastProcessedTime) < currentDelay) {
                                return;
                            }
                        }
                    }
                } else {
                    WorkspaceUtils.refreshAllProjects();
                }

                boolean noBody = (body == null || (body instanceof String && ((String) body).length() < MINIMUM_PARTIAL_RESPONSE_LENGTH));
                if (noBody && noAdditionalMessages) {
                    return;
                }
            }

            String command = (inlineChatTabId.equals(tabId))
                ? ChatUIInboundCommandName.InlineChatPrompt.getValue()
                : ChatUIInboundCommandName.ChatPrompt.getValue();

            ChatUIInboundCommand chatUIInboundCommand = new ChatUIInboundCommand(
                    command, tabId, partialChatResult, true, null);

            sendMessageToChatUI(chatUIInboundCommand);

            // Update the last processed time for this tab
            lastProcessedTimeMap.put(tabId, currentTime);
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
        ThreadingUtils.executeAsyncTask(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    ChatUIInboundCommand command = commandQueue.take();
                    sendMessageToChatUI(command);
                    while ((command = commandQueue.poll()) != null) {
                        sendMessageToChatUI(command);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    Activator.getLogger().error("Error processing command from queue", e);
                }
            }
        });
    }


    public static final class Builder {

        private JsonHandler jsonHandler;
        private CompletableFuture<ChatMessageProvider> chatMessageProvider;
        private ChatPartialResultMap chatPartialResultMap;
        private LspEncryptionManager lspEncryptionManager;

        public Builder withJsonHandler(final JsonHandler jsonHandler) {
            this.jsonHandler = jsonHandler;
            return this;
        }

        public Builder withChatMessageProvider(final CompletableFuture<ChatMessageProvider> chatMessageProvider) {
            this.chatMessageProvider = chatMessageProvider;
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
