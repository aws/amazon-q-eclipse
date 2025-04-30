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
import software.aws.toolkits.eclipse.amazonq.chat.models.BaseChatRequestParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ButtonClickParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatRequestParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommand;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommandName;
import software.aws.toolkits.eclipse.amazonq.chat.models.CursorState;
import software.aws.toolkits.eclipse.amazonq.chat.models.EncryptedChatParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.EncryptedQuickActionParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ErrorParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.FeedbackParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.FileClickParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.FollowUpClickParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericLinkClickParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericTabParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.InlineChatRequestParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.InsertToCursorPositionParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.PromptInputOptionChangeParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.QuickActionParams;
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
    private final CompletableFuture<ChatMessageProvider> chatMessageProvider;
    private final ChatPartialResultMap chatPartialResultMap;
    private final LspEncryptionManager lspEncryptionManager;

    private final BlockingQueue<ChatUIInboundCommand> commandQueue;

    private final Map<String, Long> lastProcessedTimeMap = new ConcurrentHashMap<>();

    private static final int MINIMUM_PARTIAL_RESPONSE_LENGTH = 50;
    private static final int MIN_DELAY_BETWEEN_PARTIALS = 0;
    private static final int MAX_DELAY_BETWEEN_PARTIALS = 2500;
    private static final int CHAR_COUNT_FOR_MAX_DELAY = 5000;

    private final ConcurrentHashMap<String, Object> partialResultLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> finalResultProcessed = new ConcurrentHashMap<>();

    private CompletableFuture<ChatUiRequestListener> chatUiRequestListenerFuture;
    private CompletableFuture<ChatUiRequestListener> inlineChatListenerFuture;

    private volatile boolean isActive = false;
    private volatile boolean isQueueProcessorRunning = false;
    private volatile Thread queueProcessorThread;

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

    public void sendMessageToChatServer(final Command command, final Object params) {
        if (!isQueueProcessorRunning || (queueProcessorThread != null && !queueProcessorThread.isAlive())) {
            isQueueProcessorRunning = false;
            startCommandQueueProcessor();
        }
        chatMessageProvider.thenAcceptAsync(chatMessageProvider -> {
            try {
                switch (command) {
                    case CHAT_SEND_PROMPT:
                        ChatRequestParams chatRequestParams = jsonHandler.convertObject(params, ChatRequestParams.class);
                        chatRequestParams.setContext(chatRequestParams.getPrompt().context());
                        addEditorState(chatRequestParams, true);
                        sendEncryptedChatMessage(chatRequestParams.getTabId(), token -> {
                            String encryptedMessage = lspEncryptionManager.encrypt(chatRequestParams);

                            EncryptedChatParams encryptedChatRequestParams = new EncryptedChatParams(encryptedMessage,
                                    token);

                            return chatMessageProvider.sendChatPrompt(chatRequestParams.getTabId(),
                                    encryptedChatRequestParams);
                        });
                        break;
                    case CHAT_PROMPT_OPTION_CHANGE:
                        PromptInputOptionChangeParams promptInputOptionChangeParams = jsonHandler.convertObject(params,
                                PromptInputOptionChangeParams.class);
                        chatMessageProvider.sendPromptInputOptionChange(promptInputOptionChangeParams);
                        break;
                    case CHAT_QUICK_ACTION:
                        QuickActionParams quickActionParams = jsonHandler.convertObject(params, QuickActionParams.class);
                        sendEncryptedChatMessage(quickActionParams.getTabId(), token -> {
                            String encryptedMessage = lspEncryptionManager.encrypt(quickActionParams);

                            EncryptedQuickActionParams encryptedQuickActionParams = new EncryptedQuickActionParams(
                                    encryptedMessage, token);

                            return chatMessageProvider.sendQuickAction(quickActionParams.getTabId(),
                                    encryptedQuickActionParams);
                        });
                        break;
                    case CHAT_READY:
                        ThreadingUtils.executeAsyncTask(() -> {
                            chatMessageProvider.sendChatReady();
                            startCommandQueueProcessor();
                        });
                        break;
                    case CHAT_TAB_ADD:
                        GenericTabParams tabParamsForAdd = jsonHandler.convertObject(params, GenericTabParams.class);
                        chatMessageProvider.sendTabAdd(tabParamsForAdd);
                        break;
                    case CHAT_TAB_REMOVE:
                        GenericTabParams tabParamsForRemove = jsonHandler.convertObject(params, GenericTabParams.class);
                        lastProcessedTimeMap.remove(tabParamsForRemove.tabId());
                        chatMessageProvider.sendTabRemove(tabParamsForRemove);
                        break;
                    case CHAT_TAB_CHANGE:
                        GenericTabParams tabParamsForChange = jsonHandler.convertObject(params, GenericTabParams.class);
                        chatMessageProvider.sendTabChange(tabParamsForChange);
                        break;
                    case FILE_CLICK:
                        FileClickParams fileClickParams = jsonHandler.convertObject(params, FileClickParams.class);
                        chatMessageProvider.sendFileClick(fileClickParams);
                        break;
                    case CHAT_INFO_LINK_CLICK:
                        chatMessageProvider.sendInfoLinkClick((GenericLinkClickParams) params);
                        break;
                    case CHAT_LINK_CLICK:
                        chatMessageProvider.sendLinkClick((GenericLinkClickParams) params);
                        break;
                    case CHAT_SOURCE_LINK_CLICK:
                        chatMessageProvider.sendSourceLinkClick((GenericLinkClickParams) params);
                        break;
                    case CHAT_FOLLOW_UP_CLICK:
                        FollowUpClickParams followUpClickParams = jsonHandler.convertObject(params,
                                FollowUpClickParams.class);
                        chatMessageProvider.followUpClick(followUpClickParams);
                        break;
                    case CHAT_END_CHAT:
                        GenericTabParams tabParamsForEndChat = jsonHandler.convertObject(params, GenericTabParams.class);
                        chatMessageProvider.endChat(tabParamsForEndChat);
                        break;
                    case CHAT_INSERT_TO_CURSOR_POSITION:
                        chatMessageProvider.sendInsertToCursorPositionParams((InsertToCursorPositionParams) params);
                        chatMessageProvider.sendTelemetryEvent(params);
                        break;
                    case CHAT_FEEDBACK:
                        var feedbackParams = jsonHandler.convertObject(params, FeedbackParams.class);
                        chatMessageProvider.sendFeedback(feedbackParams);
                        break;
                    case STOP_CHAT_RESPONSE:
                        var stopResponseParams = jsonHandler.convertObject(params, GenericTabParams.class);
                        chatMessageProvider.cancelInflightRequests(stopResponseParams.tabId());
                        break;
                    case TELEMETRY_EVENT:
                        chatMessageProvider.sendTelemetryEvent(params);
                        break;
                    case LIST_CONVERSATIONS:
                        ThreadingUtils.executeAsyncTask(() -> {
                            try {
                                Object response = chatMessageProvider.sendListConversations(params).get();
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
                                Object response = chatMessageProvider.sendConversationClick(params).get();
                                var conversationClickCommand = ChatUIInboundCommand.createCommand("aws/chat/conversationClick", response);
                                Activator.getEventBroker().post(ChatUIInboundCommand.class, conversationClickCommand);
                            } catch (Exception e) {
                                Activator.getLogger().error("Error processing conversationClick: " + e);
                            }
                        });
                        break;
                    case CREATE_PROMPT:
                        chatMessageProvider.sendCreatePrompt(params);
                        break;
                    case TAB_BAR_ACTION:
                        ThreadingUtils.executeAsyncTask(() -> {
                            try {
                                Object response = chatMessageProvider.sendTabBarActions(params).get();
                                var tabBarActionsCommand = ChatUIInboundCommand.createCommand("aws/chat/tabBarAction", response);
                                Activator.getEventBroker().post(ChatUIInboundCommand.class, tabBarActionsCommand);
                            } catch (Exception e) {
                                Activator.getLogger().error("Error processing tabBarActions: " + e);
                            }
                        });
                        break;
                    case BUTTON_CLICK:
                        ButtonClickParams buttonClickParams = jsonHandler.convertObject(params, ButtonClickParams.class);
                        chatMessageProvider.sendButtonClick(buttonClickParams);
                        break;
                    default:
                        throw new AmazonQPluginException("Unexpected command received from Chat UI: " + command.toString());
                }
            } catch (Exception e) {
                throw new AmazonQPluginException("Error occurred when sending message to server", e);
            }
        }, ThreadingUtils.getWorkerPool());
    }

    public void sendInlineChatMessageToChatServer(final Object params) {
        chatMessageProvider.thenAcceptAsync(chatMessageProvider -> {
            try {
                InlineChatRequestParams chatRequestParams = jsonHandler.convertObject(params, InlineChatRequestParams.class);
                addEditorState(chatRequestParams, false);
                sendEncryptedChatMessage(inlineChatTabId, token -> {
                    String encryptedMessage = lspEncryptionManager.encrypt(chatRequestParams);

                    EncryptedChatParams encryptedChatRequestParams = new EncryptedChatParams(encryptedMessage, token);
                    return chatMessageProvider.sendInlineChatPrompt(encryptedChatRequestParams);
                });
            } catch (Exception e) {
                throw new AmazonQPluginException("Error occurred when sending message to server", e);
            }
        });
    }

    private BaseChatRequestParams addEditorState(final BaseChatRequestParams chatRequestParams, final boolean addCursorState) {
        // only include files that are accessible via lsp which have absolute paths
        getOpenFileUri().ifPresent(filePathUri -> {
            chatRequestParams.setTextDocument(new TextDocumentIdentifier(filePathUri));
            if (addCursorState) {
                getSelectionRangeCursorState().ifPresent(cursorState -> chatRequestParams.setCursorState(Arrays.asList(cursorState)));
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

    public void activate() {
        this.isActive = true;
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
                    if (!isActive) {
                        break;
                    }
                    ChatUIInboundCommand command = commandQueue.take();
                    sendMessageToChatUI(command);
                    while ((command = commandQueue.poll()) != null) {
                        sendMessageToChatUI(command);
                    }
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
