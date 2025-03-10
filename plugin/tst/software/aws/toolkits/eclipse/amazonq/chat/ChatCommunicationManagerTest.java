// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ProgressParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.WorkDoneProgressNotification;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.swt.widgets.Display;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import software.aws.toolkits.eclipse.amazonq.chat.models.ChatItemAction;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatPrompt;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatRequestParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatResult;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommand;
import software.aws.toolkits.eclipse.amazonq.chat.models.CursorState;
import software.aws.toolkits.eclipse.amazonq.chat.models.EncryptedChatParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.EncryptedQuickActionParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.FeedbackParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.FeedbackPayload;
import software.aws.toolkits.eclipse.amazonq.chat.models.FollowUp;
import software.aws.toolkits.eclipse.amazonq.chat.models.FollowUpClickParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericTabParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.QuickActionParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.RecommendationContentSpan;
import software.aws.toolkits.eclipse.amazonq.chat.models.ReferenceTrackerInformation;
import software.aws.toolkits.eclipse.amazonq.chat.models.RelatedContent;
import software.aws.toolkits.eclipse.amazonq.chat.models.SourceLink;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.extensions.implementation.ActivatorStaticMockExtension;
import software.aws.toolkits.eclipse.amazonq.lsp.encryption.LspEncryptionManager;
import software.aws.toolkits.eclipse.amazonq.util.CodeReferenceLoggingService;
import software.aws.toolkits.eclipse.amazonq.util.JsonHandler;
import software.aws.toolkits.eclipse.amazonq.util.LoggingService;
import software.aws.toolkits.eclipse.amazonq.util.ProgressNotificationUtils;
import software.aws.toolkits.eclipse.amazonq.views.ChatUiRequestListener;
import software.aws.toolkits.eclipse.amazonq.views.model.ChatCodeReference;
import software.aws.toolkits.eclipse.amazonq.views.model.Command;

public final class ChatCommunicationManagerTest {
/*
    @Mock
    private JsonHandler jsonHandler;

    @Mock
    private LspEncryptionManager lspEncryptionManager;

    @Mock
    private ChatMessageProvider chatMessageProvider;

    @Mock
    private CompletableFuture<ChatMessageProvider> chatMessageProviderFuture;

    @Mock
    private ChatUiRequestListener chatUiRequestListener;

    @Mock
    private ChatPartialResultMap chatPartialResultMap;

    @Mock
    private Display display;

    private ChatCommunicationManager chatCommunicationManager;


    @BeforeEach
    void setupBeforeEach() {
        MockitoAnnotations.openMocks(this);
        // Make sure thenAcceptAsync runs on the main thread
        doAnswer(invocation -> {
            Consumer<? super ChatMessageProvider> consumer = invocation.getArgument(0);
            consumer.accept(chatMessageProvider);
            return CompletableFuture.completedFuture(null);
        }).when(chatMessageProviderFuture).thenAcceptAsync(ArgumentMatchers.<Consumer<? super ChatMessageProvider>>any());

        chatCommunicationManager = spy(ChatCommunicationManager.builder()
                .withJsonHandler(jsonHandler)
                .withLspEncryptionManager(lspEncryptionManager)
                .withChatMessageProvider(chatMessageProviderFuture)
                .withChatPartialResultMap(chatPartialResultMap)
                .build());

        when(lspEncryptionManager.encrypt(any(String.class))).thenReturn("encrypted-message");
        when(lspEncryptionManager.decrypt(any(String.class))).thenReturn("decrypted response");

    }

    @Nested
    class SendChatPromptTests {

        @RegisterExtension
        private static ActivatorStaticMockExtension activatorStaticMockExtension = new ActivatorStaticMockExtension();

        private final CursorState cursorState = new CursorState(new Range(new Position(0, 0), new Position(1, 1)));

        private final ChatRequestParams params = new ChatRequestParams(
            "tabId",
            new ChatPrompt("prompt", "escaped prompt", "command"),
            new TextDocumentIdentifier("textDocument"),
            cursorState
        );

        private final ChatItemAction chatItemAction = new ChatItemAction(
            "pillText", "prompt", false, "description", "button"
        );

        private final ReferenceTrackerInformation referenceTracker = new ReferenceTrackerInformation(
            "licenseName",
            "repository",
            "url",
            new RecommendationContentSpan(1, 2),
            "information"
        );

        private final ChatResult chatResult = new ChatResult(
            "body",
            "messageId",
            true,
            new RelatedContent("title", new SourceLink[]{new SourceLink("title", "url", "body")}),
            new FollowUp("text", new ChatItemAction[]{chatItemAction}),
            new ReferenceTrackerInformation[]{referenceTracker}
        );

        private CodeReferenceLoggingService codeReferenceLoggingService;

        @BeforeEach
        void setupBeforeEach() {
            chatCommunicationManager.setChatUiRequestListener(chatUiRequestListener);
            codeReferenceLoggingService = activatorStaticMockExtension.getMock(CodeReferenceLoggingService.class);
            doReturn(Optional.of("fileUri")).when(chatCommunicationManager).getOpenFileUri();
            doReturn(Optional.of(cursorState)).when(chatCommunicationManager).getSelectionRangeCursorState();
        }

        @Test
        void testChatSendPrompt() {
            when(jsonHandler.convertObject(any(Object.class), eq(ChatRequestParams.class)))
                    .thenReturn(params);
            when(chatMessageProvider.sendChatPrompt(any(String.class), any(EncryptedChatParams.class)))
                    .thenReturn(CompletableFuture.completedFuture("chat response"));

            when(jsonHandler.deserialize(any(String.class), eq(ChatResult.class)))
                    .thenReturn(chatResult);
            when(jsonHandler.serialize(any(ChatUIInboundCommand.class))).thenReturn("serializedObject");

            try (MockedStatic<Display> displayMock = mockStatic(Display.class)) {
                displayMock.when(Display::getDefault).thenReturn(display);
                chatCommunicationManager.sendMessageToChatServer(Command.CHAT_SEND_PROMPT, params);
            }

            verify(jsonHandler).convertObject(any(Object.class), eq(ChatRequestParams.class));
            verify(chatPartialResultMap).setEntry(any(String.class), eq("tabId"));
            verify(chatPartialResultMap).removeEntry(any(String.class));

            verify(lspEncryptionManager).encrypt(params);
            verify(chatMessageProvider).sendChatPrompt(eq("tabId"), any(EncryptedChatParams.class));
            verify(chatUiRequestListener).onSendToChatUi("serializedObject");
            verify(codeReferenceLoggingService).log(any(ChatCodeReference.class));
        }

        @Test
        void testChatSendPromptWithErrorCommunicatingWithServer() {
            when(jsonHandler.convertObject(any(Object.class), eq(ChatRequestParams.class)))
                    .thenReturn(params);
            when(chatMessageProvider.sendChatPrompt(any(String.class), any(EncryptedChatParams.class)))
                    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("error message")));
            when(jsonHandler.deserialize(any(String.class), eq(ChatResult.class)))
                    .thenReturn(chatResult);
            when(jsonHandler.serialize(any(ChatUIInboundCommand.class))).thenReturn("serializedObject");

            try (MockedStatic<Display> displayMock = mockStatic(Display.class)) {
                displayMock.when(Display::getDefault).thenReturn(display);
                chatCommunicationManager.sendMessageToChatServer(Command.CHAT_SEND_PROMPT, params);
            }

            verify(chatUiRequestListener).onSendToChatUi("serializedObject");
            verify(activatorStaticMockExtension.getMock(LoggingService.class)).error(argThat(message ->
                message.contains("An error occurred while processing chat request")));
        }

        @Test
        void testChatSendPromptWithErrorInResponse() {
            when(jsonHandler.convertObject(any(Object.class), eq(ChatRequestParams.class)))
                    .thenReturn(params);
            when(chatMessageProvider.sendChatPrompt(any(String.class), any(EncryptedChatParams.class)))
                    .thenReturn(CompletableFuture.completedFuture("chat response"));
            when(jsonHandler.deserialize(any(String.class), eq(ChatResult.class)))
                    .thenThrow(new RuntimeException("Test exception"));
            when(jsonHandler.serialize(any(ChatUIInboundCommand.class))).thenReturn("serializedObject");

            try (MockedStatic<Display> displayMock = mockStatic(Display.class)) {
                displayMock.when(Display::getDefault).thenReturn(display);
                chatCommunicationManager.sendMessageToChatServer(Command.CHAT_SEND_PROMPT, params);
            }

            verify(chatUiRequestListener).onSendToChatUi("serializedObject");
            verify(activatorStaticMockExtension.getMock(LoggingService.class)).error(argThat(message ->
                message.contains("An error occurred while processing chat response")));
        }

    }

    @Nested
    class SendQuickActionsTests {

        @RegisterExtension
        private static ActivatorStaticMockExtension activatorStaticMockExtension = new ActivatorStaticMockExtension();

        private final QuickActionParams quickActionParams = new QuickActionParams("tabId", "quickAction", "prompt");

        private final ChatItemAction chatItemAction = new ChatItemAction(
            "pillText", "prompt", false, "description", "button"
        );

        private final ReferenceTrackerInformation referenceTracker = new ReferenceTrackerInformation(
            "licenseName",
            "repository",
            "url",
            new RecommendationContentSpan(1, 2),
            "information"
        );

        private final ChatResult chatResult = new ChatResult(
                "body",
                "messageId",
                true,
                new RelatedContent("title", new SourceLink[]{new SourceLink("title", "url", "body")}),
                new FollowUp("text", new ChatItemAction[]{chatItemAction}),
                new ReferenceTrackerInformation[]{referenceTracker}
            );

        @BeforeEach
        void setupBeforeEach() {
            chatCommunicationManager.setChatUiRequestListener(chatUiRequestListener);
        }

        @Test
        void testSendQuickAction() {
            when(jsonHandler.convertObject(any(Object.class), eq(QuickActionParams.class)))
                    .thenReturn(quickActionParams);
            when(chatMessageProvider.sendQuickAction(any(String.class), any(EncryptedQuickActionParams.class)))
                    .thenReturn(CompletableFuture.completedFuture("chat response"));

            when(jsonHandler.deserialize(any(String.class), eq(ChatResult.class)))
                    .thenReturn(chatResult);
            when(jsonHandler.serialize(any(ChatUIInboundCommand.class))).thenReturn("serializedObject");

            chatCommunicationManager.sendMessageToChatServer(Command.CHAT_QUICK_ACTION, quickActionParams);

            verify(jsonHandler).convertObject(any(Object.class), eq(QuickActionParams.class));
            verify(chatPartialResultMap).setEntry(any(String.class), eq("tabId"));
            verify(chatPartialResultMap).removeEntry(any(String.class));

            verify(lspEncryptionManager).encrypt(quickActionParams);
            verify(chatMessageProvider).sendQuickAction(eq("tabId"), any(EncryptedQuickActionParams.class));
            verify(chatUiRequestListener).onSendToChatUi("serializedObject");
        }

        @Test
        void testChatSendPromptWithErrorCommunicatingWithServer() {
            when(jsonHandler.convertObject(any(Object.class), eq(QuickActionParams.class)))
                    .thenReturn(quickActionParams);
            when(chatMessageProvider.sendQuickAction(any(String.class), any(EncryptedQuickActionParams.class)))
                    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("error message")));

            when(jsonHandler.deserialize(any(String.class), eq(ChatResult.class)))
                    .thenReturn(chatResult);
            when(jsonHandler.serialize(any(ChatUIInboundCommand.class))).thenReturn("serializedObject");

            chatCommunicationManager.sendMessageToChatServer(Command.CHAT_QUICK_ACTION, quickActionParams);

            verify(chatUiRequestListener).onSendToChatUi("serializedObject");
            verify(activatorStaticMockExtension.getMock(LoggingService.class)).error(argThat(message ->
                message.contains("An error occurred while processing chat request")));
        }

        @Test
        void testChatSendPromptWithErrorInResponse() {
            when(jsonHandler.convertObject(any(Object.class), eq(QuickActionParams.class)))
                    .thenReturn(quickActionParams);
            when(chatMessageProvider.sendQuickAction(any(String.class), any(EncryptedQuickActionParams.class)))
                    .thenReturn(CompletableFuture.completedFuture("chat response"));

            when(jsonHandler.deserialize(any(String.class), eq(ChatResult.class)))
                    .thenThrow(new RuntimeException("Test exception"));
            when(jsonHandler.serialize(any(ChatUIInboundCommand.class))).thenReturn("serializedObject");

            chatCommunicationManager.sendMessageToChatServer(Command.CHAT_QUICK_ACTION, quickActionParams);

            verify(chatUiRequestListener).onSendToChatUi("serializedObject");
            verify(activatorStaticMockExtension.getMock(LoggingService.class)).error(argThat(message ->
                message.contains("An error occurred while processing chat response")));
        }

    }

    @Nested
    class SendChatReadyAndTelemetryEventTests {

        @Test
        void testSendQuickAction() {
            chatCommunicationManager.sendMessageToChatServer(Command.CHAT_READY, new Object());
            verify(chatMessageProvider).sendChatReady();
        }

        @Test
        void testTelemetryEvent() {
            chatCommunicationManager.sendMessageToChatServer(Command.TELEMETRY_EVENT, new Object());
            verify(chatMessageProvider).sendTelemetryEvent(any(Object.class));
        }

    }

    @Nested
    class SendTabUpdateTests {

        private final GenericTabParams genericTabParams = new GenericTabParams("tabId");

        @BeforeEach
        void setupBeforeEach() {
            when(jsonHandler.convertObject(any(Object.class), eq(GenericTabParams.class)))
                    .thenReturn(genericTabParams);
        }

        @Test
        void testChatTabAdd() {
            chatCommunicationManager.sendMessageToChatServer(Command.CHAT_TAB_ADD, genericTabParams);
            verify(chatMessageProvider).sendTabAdd(genericTabParams);
        }

        @Test
        void testChatTabRemove() {
            chatCommunicationManager.sendMessageToChatServer(Command.CHAT_TAB_REMOVE, genericTabParams);
            verify(chatMessageProvider).sendTabRemove(genericTabParams);
        }

        @Test
        void testChatTabChange() {
            chatCommunicationManager.sendMessageToChatServer(Command.CHAT_TAB_CHANGE, genericTabParams);
            verify(chatMessageProvider).sendTabChange(genericTabParams);
        }

        @Test
        void testEndChat() {
            chatCommunicationManager.sendMessageToChatServer(Command.CHAT_END_CHAT, genericTabParams);
            verify(chatMessageProvider).endChat(genericTabParams);
        }

    }

    @Nested
    class SendFollowUpClickTests {

        private final ChatItemAction chatItemAction = new ChatItemAction(
            "pillText", "prompt", false, "description", "button"
        );

        private final FollowUpClickParams followUpClickParams = new FollowUpClickParams("tabId", "messageId", chatItemAction);

        @BeforeEach
        void setupBeforeEach() {
            when(jsonHandler.convertObject(any(Object.class), eq(FollowUpClickParams.class)))
                    .thenReturn(followUpClickParams);
        }

        @Test
        void testFollowUpClick() {
            chatCommunicationManager.sendMessageToChatServer(Command.CHAT_FOLLOW_UP_CLICK, followUpClickParams);
            verify(chatMessageProvider).followUpClick(followUpClickParams);
        }

    }

    @Nested
    class SendFeedbackTests {

        private final FeedbackPayload feedbackPayload = new FeedbackPayload("messageId", "tabId", "selectedOption", "commend");
        private final FeedbackParams feedbackParams = new FeedbackParams("tabId", "eventId", feedbackPayload);

        @BeforeEach
        void setupBeforeEach() {
            when(jsonHandler.convertObject(any(Object.class), eq(FeedbackParams.class)))
                    .thenReturn(feedbackParams);
        }

        @Test
        void testFollowUpClick() {
            chatCommunicationManager.sendMessageToChatServer(Command.CHAT_FEEDBACK, feedbackParams);
            verify(chatMessageProvider).sendFeedback(feedbackParams);
        }

    }

    @Nested
    class HandlePartialResultProgressNotificationTests {

        @Mock
        private ProgressParams progressParams;

        @BeforeEach
        void setupBeforeEach() {
            MockitoAnnotations.openMocks(this);
            chatCommunicationManager.setChatUiRequestListener(chatUiRequestListener);
        }

        @Test
        void testWithNullTabId() {
            try (MockedStatic<ProgressNotificationUtils> progressNotificationUtilsMock = mockStatic(ProgressNotificationUtils.class)) {
                progressNotificationUtilsMock
                        .when(() -> ProgressNotificationUtils.getToken(any(ProgressParams.class)))
                        .thenReturn("token");
                when(chatPartialResultMap.getValue(any(String.class))).thenReturn(null);

                chatCommunicationManager.handlePartialResultProgressNotification(progressParams);

                verifyNoInteractions(lspEncryptionManager);
                verifyNoInteractions(jsonHandler);
                verifyNoInteractions(chatUiRequestListener);
            }
        }

        @Test
        void testWithEmptyTabId() {
            try (MockedStatic<ProgressNotificationUtils> progressNotificationUtilsMock = mockStatic(ProgressNotificationUtils.class)) {
                progressNotificationUtilsMock
                        .when(() -> ProgressNotificationUtils.getToken(any(ProgressParams.class)))
                        .thenReturn("token");
                when(chatPartialResultMap.getValue(any(String.class))).thenReturn("");

                chatCommunicationManager.handlePartialResultProgressNotification(progressParams);

                verifyNoInteractions(lspEncryptionManager);
                verifyNoInteractions(jsonHandler);
                verifyNoInteractions(chatUiRequestListener);
            }
        }

        @Test
        void testIncorrectParamsObject() {
            try (MockedStatic<ProgressNotificationUtils> progressNotificationUtilsMock = mockStatic(ProgressNotificationUtils.class)) {
                progressNotificationUtilsMock
                        .when(() -> ProgressNotificationUtils.getToken(any(ProgressParams.class)))
                        .thenReturn("token");
                when(chatPartialResultMap.getValue(any(String.class))).thenReturn("tabId");

                Either<WorkDoneProgressNotification, Object> either = mock(Either.class);
                when(either.isLeft()).thenReturn(true);
                when(progressParams.getValue()).thenReturn(either);

                assertThrows(AmazonQPluginException.class, () -> chatCommunicationManager.handlePartialResultProgressNotification(progressParams));

                verifyNoInteractions(lspEncryptionManager);
                verifyNoInteractions(jsonHandler);
                verifyNoInteractions(chatUiRequestListener);
            }
        }

        @Test
        void testIncorrectChatPartialResult() {
            try (MockedStatic<ProgressNotificationUtils> progressNotificationUtilsMock = mockStatic(ProgressNotificationUtils.class)) {
                progressNotificationUtilsMock
                        .when(() -> ProgressNotificationUtils.getToken(any(ProgressParams.class)))
                        .thenReturn("token");
                when(chatPartialResultMap.getValue(any(String.class))).thenReturn("tabId");

                Either<WorkDoneProgressNotification, Object> either = mock(Either.class);
                when(either.getRight()).thenReturn(new Object());
                when(progressParams.getValue()).thenReturn(either);

                progressNotificationUtilsMock
                    .when(() -> ProgressNotificationUtils.getObject(any(ProgressParams.class), eq(String.class)))
                    .thenReturn("chatPartialResult");

                ChatResult chatResult = mock(ChatResult.class);

                when(jsonHandler.deserialize(any(String.class), eq(ChatResult.class))).thenReturn(chatResult);
                when(chatResult.body()).thenReturn(null);

                chatCommunicationManager.handlePartialResultProgressNotification(progressParams);

                verifyNoInteractions(chatUiRequestListener);
            }
        }

        @Test
        void testChatPartialResult() {
            try (MockedStatic<ProgressNotificationUtils> progressNotificationUtilsMock = mockStatic(ProgressNotificationUtils.class)) {
                progressNotificationUtilsMock
                        .when(() -> ProgressNotificationUtils.getToken(any(ProgressParams.class)))
                        .thenReturn("token");
                when(chatPartialResultMap.getValue(any(String.class))).thenReturn("tabId");

                Either<WorkDoneProgressNotification, Object> either = mock(Either.class);
                when(either.getRight()).thenReturn(new Object());
                when(progressParams.getValue()).thenReturn(either);

                progressNotificationUtilsMock
                    .when(() -> ProgressNotificationUtils.getObject(any(ProgressParams.class), eq(String.class)))
                    .thenReturn("chatPartialResult");

                ChatResult chatResult = mock(ChatResult.class);

                when(jsonHandler.deserialize(any(String.class), eq(ChatResult.class))).thenReturn(chatResult);
                when(chatResult.body()).thenReturn("body");
                when(jsonHandler.serialize(any(ChatUIInboundCommand.class))).thenReturn("serializedObject");

                chatCommunicationManager.handlePartialResultProgressNotification(progressParams);

                verify(chatUiRequestListener).onSendToChatUi("serializedObject");
            }
        }

    }

    @Test
    void sendMessageToChatServerFails() {
        when(jsonHandler.convertObject(any(Object.class), eq(ChatRequestParams.class)))
                .thenThrow(new RuntimeException("Test exception"));

        try (MockedStatic<Display> displayMock = mockStatic(Display.class)) {
            displayMock.when(Display::getDefault).thenReturn(display);
            assertThrows(AmazonQPluginException.class, () -> chatCommunicationManager.sendMessageToChatServer(Command.CHAT_SEND_PROMPT, new Object()));
        }
    }
*/
}
