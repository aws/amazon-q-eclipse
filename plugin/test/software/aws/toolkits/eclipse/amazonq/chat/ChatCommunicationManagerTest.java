// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CancellationException;
import java.util.function.Function;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ProgressParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import software.aws.toolkits.eclipse.amazonq.chat.models.ButtonClickParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatRequestParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommand;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommandName;
import software.aws.toolkits.eclipse.amazonq.chat.models.CursorState;
import software.aws.toolkits.eclipse.amazonq.chat.models.EncryptedChatParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ErrorParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.FeedbackParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericTabParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.InlineChatRequestParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.PromptInputOptionChangeParams;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.encryption.LspEncryptionManager;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.JsonHandler;
import software.aws.toolkits.eclipse.amazonq.views.ChatUiRequestListener;
import software.aws.toolkits.eclipse.amazonq.views.model.Command;

@RunWith(MockitoJUnitRunner.class)
public class ChatCommunicationManagerTest {

    private ChatCommunicationManager chatCommunicationManager;

    @Mock
    private JsonHandler mockJsonHandler;

    @Mock
    private ChatMessageProvider mockChatMessageProvider;

    @Mock
    private ChatPartialResultMap mockChatPartialResultMap;

    @Mock
    private LspEncryptionManager mockLspEncryptionManager;

    @Mock
    private ChatUiRequestListener mockChatUiRequestListener;

    @Mock
    private ChatUiRequestListener mockInlineChatRequestListener;

    private CompletableFuture<ChatMessageProvider> mockChatMessageProviderFuture;

    @Before
    public void setUp() {
        mockChatMessageProviderFuture = CompletableFuture.completedFuture(mockChatMessageProvider);

        chatCommunicationManager = ChatCommunicationManager.builder()
                .withJsonHandler(mockJsonHandler)
                .withChatMessageProvider(mockChatMessageProviderFuture)
                .withChatPartialResultMap(mockChatPartialResultMap)
                .withLspEncryptionManager(mockLspEncryptionManager)
                .build();

        chatCommunicationManager.setChatUiRequestListener(mockChatUiRequestListener);
        chatCommunicationManager.setInlineChatRequestListener(mockInlineChatRequestListener);
    }

    @Test
    public void testGetInstance() {
        ChatCommunicationManager instance = ChatCommunicationManager.getInstance();
        assertNotNull(instance);
        // Second call should return the same instance
        assertEquals(instance, ChatCommunicationManager.getInstance());
    }

    @Test
    public void testSendChatPrompt() {
        // Setup
        ChatRequestParams chatRequestParams = new ChatRequestParams();
        chatRequestParams.setTabId("test-tab-id");
        
        when(mockJsonHandler.convertObject(any(), eq(ChatRequestParams.class))).thenReturn(chatRequestParams);
        when(mockLspEncryptionManager.encrypt(any())).thenReturn("encrypted-message");
        when(mockChatMessageProvider.sendChatPrompt(anyString(), any(EncryptedChatParams.class)))
            .thenReturn(CompletableFuture.completedFuture("encrypted-response"));
        when(mockLspEncryptionManager.decrypt(anyString())).thenReturn("{\"key\":\"value\"}");
        
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("key", "value");
        when(mockJsonHandler.deserialize(anyString(), eq(Map.class))).thenReturn(resultMap);
        
        // Execute
        chatCommunicationManager.sendMessageToChatServer(Command.CHAT_SEND_PROMPT, chatRequestParams);
        
        // Verify
        verify(mockChatMessageProvider).sendChatPrompt(eq("test-tab-id"), any(EncryptedChatParams.class));
        verify(mockChatPartialResultMap).setEntry(anyString(), eq("test-tab-id"));
    }

    @Test
    public void testSendInlineChatMessage() {
        // Setup
        InlineChatRequestParams inlineChatRequestParams = new InlineChatRequestParams();
        
        when(mockJsonHandler.convertObject(any(), eq(InlineChatRequestParams.class))).thenReturn(inlineChatRequestParams);
        when(mockLspEncryptionManager.encrypt(any())).thenReturn("encrypted-message");
        when(mockChatMessageProvider.sendInlineChatPrompt(any(EncryptedChatParams.class)))
            .thenReturn(CompletableFuture.completedFuture("encrypted-response"));
        when(mockLspEncryptionManager.decrypt(anyString())).thenReturn("{\"key\":\"value\"}");
        
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("key", "value");
        when(mockJsonHandler.deserialize(anyString(), eq(Map.class))).thenReturn(resultMap);
        
        // Execute
        chatCommunicationManager.sendInlineChatMessageToChatServer(inlineChatRequestParams);
        
        // Verify
        verify(mockChatMessageProvider).sendInlineChatPrompt(any(EncryptedChatParams.class));
        verify(mockChatPartialResultMap).setEntry(anyString(), eq("123456789"));
    }

    @Test
    public void testHandlePartialResultProgressNotification() {
        // Setup
        String token = UUID.randomUUID().toString();
        String tabId = "test-tab-id";
        
        ProgressParams progressParams = new ProgressParams();
        progressParams.setToken(token);
        
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put("body", "partial response content");
        progressParams.setValue(Either.forRight("encrypted-partial-result"));
        
        when(mockChatPartialResultMap.getValue(token)).thenReturn(tabId);
        when(mockLspEncryptionManager.decrypt("encrypted-partial-result")).thenReturn("{\"body\":\"partial response content\"}");
        
        Map<String, Object> partialResultMap = new HashMap<>();
        partialResultMap.put("body", "partial response content");
        when(mockJsonHandler.deserialize(anyString(), eq(Map.class))).thenReturn(partialResultMap);
        
        // Mock serialization of ChatUIInboundCommand
        when(mockJsonHandler.serialize(any(ChatUIInboundCommand.class))).thenReturn("{\"command\":\"aws/chat/prompt\"}");
        
        // Execute
        chatCommunicationManager.handlePartialResultProgressNotification(progressParams);
        
        // Verify
        verify(mockChatUiRequestListener).onSendToChatUi(anyString());
    }

    @Test
    public void testHandlePartialResultProgressNotificationForInlineChat() {
        // Setup
        String token = UUID.randomUUID().toString();
        String tabId = "123456789"; // This is the inline chat tab ID
        
        ProgressParams progressParams = new ProgressParams();
        progressParams.setToken(token);
        
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put("body", "partial response content");
        progressParams.setValue(Either.forRight("encrypted-partial-result"));
        
        when(mockChatPartialResultMap.getValue(token)).thenReturn(tabId);
        when(mockLspEncryptionManager.decrypt("encrypted-partial-result")).thenReturn("{\"body\":\"partial response content\"}");
        
        Map<String, Object> partialResultMap = new HashMap<>();
        partialResultMap.put("body", "partial response content");
        when(mockJsonHandler.deserialize(anyString(), eq(Map.class))).thenReturn(partialResultMap);
        
        // Mock serialization of ChatUIInboundCommand
        when(mockJsonHandler.serialize(any(ChatUIInboundCommand.class))).thenReturn("{\"command\":\"aws/chat/inlinePrompt\"}");
        
        // Execute
        chatCommunicationManager.handlePartialResultProgressNotification(progressParams);
        
        // Verify
        verify(mockInlineChatRequestListener).onSendToChatUi(anyString());
    }

    @Test
    public void testSendPromptInputOptionChange() {
        // Setup
        PromptInputOptionChangeParams params = new PromptInputOptionChangeParams();
        
        when(mockJsonHandler.convertObject(any(), eq(PromptInputOptionChangeParams.class))).thenReturn(params);
        
        // Execute
        chatCommunicationManager.sendMessageToChatServer(Command.CHAT_PROMPT_OPTION_CHANGE, params);
        
        // Verify
        verify(mockChatMessageProvider).sendPromptInputOptionChange(params);
    }

    @Test
    public void testSendFeedback() {
        // Setup
        FeedbackParams params = new FeedbackParams();
        
        when(mockJsonHandler.convertObject(any(), eq(FeedbackParams.class))).thenReturn(params);
        
        // Execute
        chatCommunicationManager.sendMessageToChatServer(Command.CHAT_FEEDBACK, params);
        
        // Verify
        verify(mockChatMessageProvider).sendFeedback(params);
    }

    @Test
    public void testStopChatResponse() {
        // Setup
        GenericTabParams params = new GenericTabParams("test-tab-id");
        
        when(mockJsonHandler.convertObject(any(), eq(GenericTabParams.class))).thenReturn(params);
        
        // Execute
        chatCommunicationManager.sendMessageToChatServer(Command.STOP_CHAT_RESPONSE, params);
        
        // Verify
        verify(mockChatMessageProvider).cancelInflightRequests("test-tab-id");
    }

    @Test
    public void testButtonClick() {
        // Setup
        ButtonClickParams params = new ButtonClickParams();
        
        when(mockJsonHandler.convertObject(any(), eq(ButtonClickParams.class))).thenReturn(params);
        
        // Execute
        chatCommunicationManager.sendMessageToChatServer(Command.BUTTON_CLICK, params);
        
        // Verify
        verify(mockChatMessageProvider).sendButtonClick(params);
    }

    @Test
    public void testRemoveListener() {
        // Setup
        ChatUiRequestListener newListener = mock(ChatUiRequestListener.class);
        chatCommunicationManager.setChatUiRequestListener(newListener);
        
        // Execute
        chatCommunicationManager.removeListener(newListener);
        
        // Create a test command
        ChatUIInboundCommand command = new ChatUIInboundCommand(
            ChatUIInboundCommandName.ChatPrompt.getValue(), "test-tab", new HashMap<>(), false, null);
        when(mockJsonHandler.serialize(any(ChatUIInboundCommand.class))).thenReturn("{}");
        
        // Send the command
        chatCommunicationManager.onEvent(command);
        
        // Verify the listener was removed and not called
        verify(newListener, never()).onSendToChatUi(anyString());
    }

    @Test
    public void testAddEditorState() {
        // Create a subclass to test protected methods
        ChatCommunicationManager testManager = new ChatCommunicationManager(ChatCommunicationManager.builder()
                .withJsonHandler(mockJsonHandler)
                .withChatMessageProvider(mockChatMessageProviderFuture)
                .withChatPartialResultMap(mockChatPartialResultMap)
                .withLspEncryptionManager(mockLspEncryptionManager)) {
            
            @Override
            protected Optional<String> getOpenFileUri() {
                return Optional.of("file:///test/path.java");
            }
            
            @Override
            protected Optional<CursorState> getSelectionRangeCursorState() {
                Position start = new Position(1, 0);
                Position end = new Position(1, 10);
                Range range = new Range(start, end);
                return Optional.of(new CursorState(range));
            }
        };
        
        // Setup
        ChatRequestParams params = new ChatRequestParams();
        
        // Execute
        testManager.addEditorState(params, true);
        
        // Verify
        assertNotNull(params.getTextDocument());
        assertEquals("file:///test/path.java", params.getTextDocument().getUri());
        assertNotNull(params.getCursorState());
        assertEquals(1, params.getCursorState().size());
    }

    @Test
    public void testHandleCancellation() {
        // Setup - Create a method to access the private method
        ChatCommunicationManager testManager = new ChatCommunicationManager(ChatCommunicationManager.builder()
                .withJsonHandler(mockJsonHandler)
                .withChatMessageProvider(mockChatMessageProviderFuture)
                .withChatPartialResultMap(mockChatPartialResultMap)
                .withLspEncryptionManager(mockLspEncryptionManager)) {
            
            public void testHandleCancellation(String tabId) {
                super.handleCancellation(tabId);
            }
        };
        
        testManager.setChatUiRequestListener(mockChatUiRequestListener);
        
        // Mock serialization
        ArgumentCaptor<ChatUIInboundCommand> commandCaptor = ArgumentCaptor.forClass(ChatUIInboundCommand.class);
        when(mockJsonHandler.serialize(commandCaptor.capture())).thenReturn("{}");
        
        // Execute
        testManager.testHandleCancellation("test-tab-id");
        
        // Verify
        verify(mockChatUiRequestListener).onSendToChatUi(anyString());
        
        // Verify the command contains error params
        ChatUIInboundCommand capturedCommand = commandCaptor.getValue();
        assertEquals(ChatUIInboundCommandName.ErrorMessage.getValue(), capturedCommand.command());
        assertTrue(capturedCommand.params() instanceof ErrorParams);
    }

    @Test
    public void testSendEncryptedChatMessageWithException() {
        // Create a subclass to test private method
        ChatCommunicationManager testManager = new ChatCommunicationManager(ChatCommunicationManager.builder()
                .withJsonHandler(mockJsonHandler)
                .withChatMessageProvider(mockChatMessageProviderFuture)
                .withChatPartialResultMap(mockChatPartialResultMap)
                .withLspEncryptionManager(mockLspEncryptionManager)) {
            
            public CompletableFuture<Object> testSendEncryptedChatMessage(String tabId, Function<String, CompletableFuture<String>> action) {
                return super.sendEncryptedChatMessage(tabId, action);
            }
        };
        
        testManager.setChatUiRequestListener(mockChatUiRequestListener);
        
        // Setup
        String tabId = "test-tab-id";
        String token = "test-token";
        
        when(mockChatPartialResultMap.getValue(anyString())).thenReturn(tabId);
        when(mockJsonHandler.serialize(any(ChatUIInboundCommand.class))).thenReturn("{}");
        
        // Create a function that throws an exception
        Function<String, CompletableFuture<String>> exceptionAction = tokenParam -> {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("Test exception"));
            return future;
        };
        
        // Execute
        testManager.testSendEncryptedChatMessage(tabId, exceptionAction);
        
        // Verify error handling
        ArgumentCaptor<ChatUIInboundCommand> commandCaptor = ArgumentCaptor.forClass(ChatUIInboundCommand.class);
        verify(mockJsonHandler).serialize(commandCaptor.capture());
        
        ChatUIInboundCommand capturedCommand = commandCaptor.getValue();
        assertEquals(ChatUIInboundCommandName.ErrorMessage.getValue(), capturedCommand.command());
        assertTrue(capturedCommand.params() instanceof ErrorParams);
    }

    @Test
    public void testSendEncryptedChatMessageWithCancellation() {
        // Create a subclass to test private method
        ChatCommunicationManager testManager = new ChatCommunicationManager(ChatCommunicationManager.builder()
                .withJsonHandler(mockJsonHandler)
                .withChatMessageProvider(mockChatMessageProviderFuture)
                .withChatPartialResultMap(mockChatPartialResultMap)
                .withLspEncryptionManager(mockLspEncryptionManager)) {
            
            public CompletableFuture<Object> testSendEncryptedChatMessage(String tabId, Function<String, CompletableFuture<String>> action) {
                return super.sendEncryptedChatMessage(tabId, action);
            }
        };
        
        testManager.setChatUiRequestListener(mockChatUiRequestListener);
        
        // Setup
        String tabId = "test-tab-id";
        String token = "test-token";
        
        when(mockChatPartialResultMap.getValue(anyString())).thenReturn(tabId);
        when(mockJsonHandler.serialize(any(ChatUIInboundCommand.class))).thenReturn("{}");
        
        // Create a function that throws a cancellation exception
        Function<String, CompletableFuture<String>> cancellationAction = tokenParam -> {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(new CancellationException("Request cancelled"));
            return future;
        };
        
        // Execute
        testManager.testSendEncryptedChatMessage(tabId, cancellationAction);
        
        // Verify cancellation handling
        ArgumentCaptor<ChatUIInboundCommand> commandCaptor = ArgumentCaptor.forClass(ChatUIInboundCommand.class);
        verify(mockJsonHandler).serialize(commandCaptor.capture());
        
        ChatUIInboundCommand capturedCommand = commandCaptor.getValue();
        assertEquals(ChatUIInboundCommandName.ErrorMessage.getValue(), capturedCommand.command());
        assertTrue(capturedCommand.params() instanceof ErrorParams);
    }
}