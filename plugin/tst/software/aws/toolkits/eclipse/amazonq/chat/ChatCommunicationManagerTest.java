// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommand;
import software.aws.toolkits.eclipse.amazonq.chat.models.CursorState;
import software.aws.toolkits.eclipse.amazonq.chat.models.EncryptedChatParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.EncryptedQuickActionParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.FeedbackParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.FeedbackPayload;
import software.aws.toolkits.eclipse.amazonq.chat.models.FollowUpClickParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericTabParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.QuickActionParams;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.extensions.implementation.ActivatorStaticMockExtension;
import software.aws.toolkits.eclipse.amazonq.lsp.AmazonQLspServer;
import software.aws.toolkits.eclipse.amazonq.lsp.encryption.LspEncryptionManager;
import software.aws.toolkits.eclipse.amazonq.providers.lsp.LspProvider;
import software.aws.toolkits.eclipse.amazonq.util.JsonHandler;
import software.aws.toolkits.eclipse.amazonq.util.ObjectMapperFactory;
import software.aws.toolkits.eclipse.amazonq.util.ProgressNotificationUtils;
import software.aws.toolkits.eclipse.amazonq.views.model.Command;

public final class ChatCommunicationManagerTest {

    @RegisterExtension
    private static ActivatorStaticMockExtension activatorStaticMockExtension = new ActivatorStaticMockExtension();

    @Mock
    private JsonHandler jsonHandler;

    @Mock
    private LspEncryptionManager lspEncryptionManager;

    @Mock
    private ChatPartialResultMap chatPartialResultMap;

    @Mock
    private Display display;

    @Mock
    private AmazonQLspServer amazonQLspServer;

    private ChatCommunicationManager chatCommunicationManager;

    @BeforeEach
    void setupBeforeEach() {
        MockitoAnnotations.openMocks(this);

        chatCommunicationManager = spy(ChatCommunicationManager.builder()
                .withJsonHandler(jsonHandler)
                .withLspEncryptionManager(lspEncryptionManager)
                .withChatPartialResultMap(chatPartialResultMap)
                .build());

        when(lspEncryptionManager.encrypt(anyString())).thenReturn("encrypted-message");
        when(lspEncryptionManager.decrypt(anyString())).thenReturn("decrypted response");

        CompletableFuture<AmazonQLspServer> serverFuture = mock(CompletableFuture.class);
        when(activatorStaticMockExtension.getMock(LspProvider.class).getAmazonQServer()).thenReturn(serverFuture);
        doAnswer(invocation -> {
            Consumer<? super AmazonQLspServer> consumer = invocation.getArgument(0);
            consumer.accept(amazonQLspServer);
            return CompletableFuture.completedFuture(null);
        }).when(serverFuture).thenAcceptAsync(ArgumentMatchers.<Consumer<? super AmazonQLspServer>>any(), any());

        doAnswer(invocation -> Optional.of("fileUri")).when(chatCommunicationManager).getOpenFileUri();
        CursorState cursorState = new CursorState(new Range(new Position(0, 0), new Position(1, 1)));
        doAnswer(invocation -> Optional.of(cursorState)).when(chatCommunicationManager).getSelectionRangeCursorState();
    }

    @Nested
    class RequestManagementTests {
        @Test
        void testCancelInflightRequests() throws Exception {
            CompletableFuture<String> mockFuture = mock(CompletableFuture.class);

            Map<String, CompletableFuture<String>> inflightRequestMap = new ConcurrentHashMap<>();
            inflightRequestMap.put("tabId", mockFuture);

            Field inflightRequestField = ChatCommunicationManager.class.getDeclaredField("inflightRequestByTabId");
            inflightRequestField.setAccessible(true);
            inflightRequestField.set(chatCommunicationManager, inflightRequestMap);

            chatCommunicationManager.cancelInflightRequests("tabId");

            verify(mockFuture).cancel(true);
            assertTrue(inflightRequestMap.isEmpty());
        }
    }

    @Nested
    class ChatPromptTests {
        private final ChatMessage params = new ChatMessage(new ChatRequestParams("tabId",
                new ChatPrompt("prompt", "escaped prompt", "command", Collections.emptyList()),
                new TextDocumentIdentifier("textDocument"), Collections.emptyList(), Collections.emptyList()));

        private final String jsonString = "{"
                + "  \"type\": \"answer\","
                + "  \"header\": {"
                + "    \"type\": \"answer\","
                + "    \"body\": \"body\","
                + "    \"status\": {"
                + "      \"status\": \"success\","
                + "      \"text\": \"Success\""
                + "    }"
                + "  },"
                + "  \"buttons\": [],"
                + "  \"body\": \"body\","
                + "  \"messageId\": \"messageId\","
                + "  \"canBeVoted\": true,"
                + "  \"relatedContent\": {"
                + "    \"title\": \"title\","
                + "    \"content\": ["
                + "      {"
                + "        \"title\": \"title\","
                + "        \"url\": \"url\","
                + "        \"body\": \"body\""
                + "      }"
                + "    ]"
                + "  },"
                + "  \"followUp\": {"
                + "    \"text\": \"text\","
                + "    \"options\": ["
                + "      {"
                + "        \"pillText\": \"pillText\","
                + "        \"prompt\": \"prompt\","
                + "        \"isEnabled\": true,"
                + "        \"description\": \"description\","
                + "        \"button\": \"button\""
                + "      }"
                + "    ]"
                + "  },"
                + "  \"codeReference\": ["
                + "    {"
                + "      \"licenseName\": \"licenseName\","
                + "      \"repository\": \"repository\","
                + "      \"url\": \"url\","
                + "      \"contentSpan\": {"
                + "        \"start\": 1,"
                + "        \"end\": 2"
                + "      },"
                + "      \"information\": \"information\""
                + "    }"
                + "  ]"
                + "}";

        @Test
        void testSuccessfulChatPromptSending() throws Exception {
            CompletableFuture<String> completedFuture = CompletableFuture.completedFuture("chat response");

            when(amazonQLspServer.sendChatPrompt(any(EncryptedChatParams.class)))
                    .thenReturn(completedFuture);

            when(jsonHandler.deserialize(anyString(), eq(Map.class)))
                    .thenReturn(ObjectMapperFactory.getInstance().readValue(jsonString, Map.class));

            CompletableFuture<AmazonQLspServer> serverFuture = CompletableFuture.completedFuture(amazonQLspServer);
            when(activatorStaticMockExtension.getMock(LspProvider.class).getAmazonQServer()).thenReturn(serverFuture);

            try (MockedStatic<Display> displayMock = mockStatic(Display.class)) {
                displayMock.when(Display::getDefault).thenReturn(display);

                chatCommunicationManager.sendMessageToChatServer(Command.CHAT_SEND_PROMPT, params);

                Thread.sleep(1000);
            }

            verify(chatPartialResultMap).setEntry(anyString(), eq("tabId"));
            verify(chatPartialResultMap).removeEntry(anyString());
            verify(lspEncryptionManager).encrypt(params.getData());
            verify(lspEncryptionManager).decrypt(anyString());
        }

        @Test
        void testChatPromptWithResponseDeserializationError() throws Exception {
            CompletableFuture<String> completedFuture = CompletableFuture.completedFuture("chat response");

            when(amazonQLspServer.sendChatPrompt(any(EncryptedChatParams.class))).thenReturn(completedFuture);

            RuntimeException deserializeException = new RuntimeException("Test exception");
            when(jsonHandler.deserialize(anyString(), eq(Map.class))).thenThrow(deserializeException);

            CompletableFuture<AmazonQLspServer> serverFuture = CompletableFuture.completedFuture(amazonQLspServer);
            when(activatorStaticMockExtension.getMock(LspProvider.class).getAmazonQServer()).thenReturn(serverFuture);

            when(lspEncryptionManager.decrypt(anyString())).thenReturn("some-json-data");

            try (MockedStatic<Display> displayMock = mockStatic(Display.class)) {
                displayMock.when(Display::getDefault).thenReturn(display);

                chatCommunicationManager.sendMessageToChatServer(Command.CHAT_SEND_PROMPT, params);

                Thread.sleep(1000);
            }

            verify(chatPartialResultMap).setEntry(anyString(), eq("tabId"));
            verify(chatPartialResultMap).removeEntry(anyString());
            verify(lspEncryptionManager).encrypt(params.getData());
            verify(lspEncryptionManager).decrypt(anyString());
        }
    }

    @Nested
    class SendQuickActionsTests {

      private final ChatMessage quickActionParams = new ChatMessage(
              new QuickActionParams("tabId", "quickAction", "prompt"));

      private final String jsonString = "{"
              + "  \"type\": \"answer\","
              + "  \"header\": {"
              + "    \"type\": \"answer\","
              + "    \"body\": \"body\","
              + "    \"status\": {"
              + "      \"status\": \"success\","
              + "      \"text\": \"Success\""
              + "    }"
              + "  },"
              + "  \"buttons\": [],"
              + "  \"body\": \"body\","
              + "  \"messageId\": \"messageId\","
              + "  \"canBeVoted\": true,"
              + "  \"relatedContent\": {"
              + "    \"title\": \"title\","
              + "    \"content\": ["
              + "      {"
              + "        \"title\": \"title\","
              + "        \"url\": \"url\","
              + "        \"body\": \"body\""
              + "      }"
              + "    ]"
              + "  },"
              + "  \"followUp\": {"
              + "    \"text\": \"text\","
              + "    \"options\": ["
              + "      {"
              + "        \"pillText\": \"pillText\","
              + "        \"prompt\": \"prompt\","
              + "        \"isEnabled\": false,"
              + "        \"description\": \"description\","
              + "        \"button\": \"button\""
              + "      }"
              + "    ]"
              + "  },"
              + "  \"codeReference\": ["
              + "    {"
              + "      \"licenseName\": \"licenseName\","
              + "      \"repository\": \"repository\","
              + "      \"url\": \"url\","
              + "      \"contentSpan\": {"
              + "        \"start\": 1,"
              + "        \"end\": 2"
              + "      },"
              + "      \"information\": \"information\""
              + "    }"
              + "  ]"
              + "}";

      @Test
      void testSendQuickAction() throws Exception {
          CompletableFuture<String> completedFuture = CompletableFuture.completedFuture("chat response");

          when(amazonQLspServer.sendQuickAction(any(EncryptedQuickActionParams.class)))
                  .thenReturn(completedFuture);

          when(jsonHandler.deserialize(anyString(), eq(Map.class)))
                  .thenReturn(ObjectMapperFactory.getInstance().readValue(jsonString, Map.class));

          CompletableFuture<AmazonQLspServer> serverFuture = CompletableFuture.completedFuture(amazonQLspServer);
          when(activatorStaticMockExtension.getMock(LspProvider.class).getAmazonQServer()).thenReturn(serverFuture);

          when(lspEncryptionManager.decrypt(anyString())).thenReturn("some-json-data");

          try (MockedStatic<Display> displayMock = mockStatic(Display.class)) {
              displayMock.when(Display::getDefault).thenReturn(display);

              chatCommunicationManager.sendMessageToChatServer(Command.CHAT_QUICK_ACTION, quickActionParams);

              Thread.sleep(1000);
          }

          verify(chatPartialResultMap).setEntry(anyString(), eq("tabId"));
          verify(chatPartialResultMap).removeEntry(anyString());
          verify(lspEncryptionManager).encrypt(eq(quickActionParams.getData()));
      }

      @Test
      void testChatSendPromptWithErrorInResponse() throws Exception {
          CompletableFuture<String> completedFuture = CompletableFuture.completedFuture("chat response");

          when(amazonQLspServer.sendQuickAction(any(EncryptedQuickActionParams.class))).thenReturn(completedFuture);

          RuntimeException deserializeException = new RuntimeException("Test exception");
          when(jsonHandler.deserialize(anyString(), eq(Map.class))).thenThrow(deserializeException);

          CompletableFuture<AmazonQLspServer> serverFuture = CompletableFuture.completedFuture(amazonQLspServer);
          when(activatorStaticMockExtension.getMock(LspProvider.class).getAmazonQServer()).thenReturn(serverFuture);

          when(lspEncryptionManager.decrypt(anyString())).thenReturn("some-json-data");

          try (MockedStatic<Display> displayMock = mockStatic(Display.class)) {
              displayMock.when(Display::getDefault).thenReturn(display);

              chatCommunicationManager.sendMessageToChatServer(Command.CHAT_QUICK_ACTION, quickActionParams);

              Thread.sleep(1000);
          }

          verify(chatPartialResultMap).setEntry(anyString(), eq("tabId"));
          verify(chatPartialResultMap).removeEntry(anyString());
      }
  }

  @Nested
  class SendChatReadyAndTelemetryEventTests {
      @Test
      void testSendChatReady() throws Exception {
          CompletableFuture<AmazonQLspServer> serverFuture = CompletableFuture.completedFuture(amazonQLspServer);
          when(activatorStaticMockExtension.getMock(LspProvider.class).getAmazonQServer()).thenReturn(serverFuture);

          CountDownLatch latch = new CountDownLatch(1);

          doAnswer(invocation -> {
              latch.countDown();
              return null;
          }).when(amazonQLspServer).chatReady();

          chatCommunicationManager.sendMessageToChatServer(Command.CHAT_READY, new ChatMessage(new Object()));

          assertTrue(latch.await(2, TimeUnit.SECONDS), "Async operation did not complete in time");
          verify(amazonQLspServer).chatReady();
      }

      @Test
      void testTelemetryEvent() throws Exception {
          CompletableFuture<AmazonQLspServer> serverFuture = CompletableFuture.completedFuture(amazonQLspServer);
          when(activatorStaticMockExtension.getMock(LspProvider.class).getAmazonQServer()).thenReturn(serverFuture);

          ChatMessage message = new ChatMessage(new Object());

          try (MockedStatic<Display> displayMock = mockStatic(Display.class)) {
              displayMock.when(Display::getDefault).thenReturn(display);

              chatCommunicationManager.sendMessageToChatServer(Command.TELEMETRY_EVENT, message);

              Thread.sleep(1000);
          }

          verify(amazonQLspServer).sendTelemetryEvent(message.getData());
      }
  }

  @Nested
  class SendTabUpdateTests {
      private final ChatMessage genericTabParams = new ChatMessage(new GenericTabParams("tabId"));

      @BeforeEach
      void setUp() {
          CompletableFuture<AmazonQLspServer> serverFuture = CompletableFuture.completedFuture(amazonQLspServer);
          when(activatorStaticMockExtension.getMock(LspProvider.class).getAmazonQServer()).thenReturn(serverFuture);
      }

      @Test
      void testChatTabAdd() throws Exception {
          CountDownLatch latch = new CountDownLatch(1);

          doAnswer(invocation -> {
              latch.countDown();
              return null;
          }).when(amazonQLspServer).tabAdd(genericTabParams.getData());

          chatCommunicationManager.sendMessageToChatServer(Command.CHAT_TAB_ADD, genericTabParams);

          assertTrue(latch.await(2, TimeUnit.SECONDS), "Async operation did not complete in time");
          verify(amazonQLspServer).tabAdd(genericTabParams.getData());
      }

      @Test
      void testChatTabRemove() throws Exception {
          CountDownLatch latch = new CountDownLatch(1);

          doAnswer(invocation -> {
              latch.countDown();
              return null;
          }).when(amazonQLspServer).tabRemove(genericTabParams.getData());

          chatCommunicationManager.sendMessageToChatServer(Command.CHAT_TAB_REMOVE, genericTabParams);
          assertTrue(latch.await(2, TimeUnit.SECONDS), "Async operation did not complete in time");

          verify(amazonQLspServer).tabRemove(genericTabParams.getData());
      }

      @Test
      void testChatTabChange() throws Exception {
          CountDownLatch latch = new CountDownLatch(1);

          doAnswer(invocation -> {
              latch.countDown();
              return null;
          }).when(amazonQLspServer).tabChange(genericTabParams.getData());

          chatCommunicationManager.sendMessageToChatServer(Command.CHAT_TAB_CHANGE, genericTabParams);
          assertTrue(latch.await(2, TimeUnit.SECONDS), "Async operation did not complete in time");
          verify(amazonQLspServer).tabChange(genericTabParams.getData());
      }

      @Test
      void testEndChat() throws Exception {
          CountDownLatch latch = new CountDownLatch(1);

          doAnswer(invocation -> {
              latch.countDown();
              return null;
          }).when(amazonQLspServer).endChat(genericTabParams.getData());

          chatCommunicationManager.sendMessageToChatServer(Command.CHAT_END_CHAT, genericTabParams);
          assertTrue(latch.await(2, TimeUnit.SECONDS), "Async operation did not complete in time");

          verify(amazonQLspServer).endChat(genericTabParams.getData());
      }
  }

  @Nested
  class SendFollowUpClickTests {
      private final ChatItemAction chatItemAction = new ChatItemAction("pillText", "prompt", false, "description",
              "button");

      private final ChatMessage followUpClickParams = new ChatMessage(
              new FollowUpClickParams("tabId", "messageId", chatItemAction));

      @BeforeEach
      void setupBeforeEach() {
          CompletableFuture<AmazonQLspServer> serverFuture = CompletableFuture.completedFuture(amazonQLspServer);
          when(activatorStaticMockExtension.getMock(LspProvider.class).getAmazonQServer()).thenReturn(serverFuture);
      }

      @Test
      void testFollowUpClick() throws Exception {
          CountDownLatch latch = new CountDownLatch(1);

          doAnswer(invocation -> {
              latch.countDown();
              return null;
          }).when(amazonQLspServer).followUpClick(followUpClickParams.getData());

          chatCommunicationManager.sendMessageToChatServer(Command.CHAT_FOLLOW_UP_CLICK, followUpClickParams);

          assertTrue(latch.await(2, TimeUnit.SECONDS), "Async operation did not complete in time");
          verify(amazonQLspServer).followUpClick(followUpClickParams.getData());
      }
  }

  @Nested
  class SendFeedbackTests {
      private final FeedbackPayload feedbackPayload = new FeedbackPayload("messageId", "tabId", "selectedOption",
              "commend");
      private final ChatMessage feedbackParams = new ChatMessage(
              new FeedbackParams("tabId", "eventId", feedbackPayload));

      @BeforeEach
      void setupBeforeEach() {
          CompletableFuture<AmazonQLspServer> serverFuture = CompletableFuture.completedFuture(amazonQLspServer);
          when(activatorStaticMockExtension.getMock(LspProvider.class).getAmazonQServer()).thenReturn(serverFuture);
      }

      @Test
      void testSendFeedback() throws Exception {
          CountDownLatch latch = new CountDownLatch(1);

          doAnswer(invocation -> {
              latch.countDown();
              return null;
          }).when(amazonQLspServer).sendFeedback(feedbackParams.getData());

          chatCommunicationManager.sendMessageToChatServer(Command.CHAT_FEEDBACK, feedbackParams);

          assertTrue(latch.await(2, TimeUnit.SECONDS), "Async operation did not complete in time");
          verify(amazonQLspServer).sendFeedback(feedbackParams.getData());
      }
  }

  @Nested
  class HandlePartialResultProgressNotificationTests {
      @Mock
      private ProgressParams progressParams;

      @BeforeEach
      void setupBeforeEach() {
          MockitoAnnotations.openMocks(this);
          CompletableFuture<AmazonQLspServer> serverFuture = mock(CompletableFuture.class);
          when(activatorStaticMockExtension.getMock(LspProvider.class).getAmazonQServer())
                  .thenReturn(serverFuture);
          doAnswer(invocation -> {
              Consumer<? super AmazonQLspServer> consumer = invocation.getArgument(0);
              consumer.accept(amazonQLspServer);
              return CompletableFuture.completedFuture(null);
          }).when(serverFuture).thenAcceptAsync(ArgumentMatchers.<Consumer<? super AmazonQLspServer>>any(), any());
      }

      @Test
      void testWithNullTabId() {
          try (MockedStatic<ProgressNotificationUtils> progressNotificationUtilsMock = mockStatic(ProgressNotificationUtils.class)) {
              progressNotificationUtilsMock
                      .when(() -> ProgressNotificationUtils.getToken(any(ProgressParams.class)))
                      .thenReturn("token");
              when(chatPartialResultMap.getValue(anyString())).thenReturn(null);

              chatCommunicationManager.handlePartialResultProgressNotification(progressParams);

              verifyNoInteractions(lspEncryptionManager);
              verifyNoInteractions(jsonHandler);
          }
      }

      @Test
      void testWithEmptyTabId() {
          try (MockedStatic<ProgressNotificationUtils> progressNotificationUtilsMock = mockStatic(ProgressNotificationUtils.class)) {
              progressNotificationUtilsMock
                      .when(() -> ProgressNotificationUtils.getToken(any(ProgressParams.class)))
                      .thenReturn("token");
              when(chatPartialResultMap.getValue(anyString())).thenReturn("");

              chatCommunicationManager.handlePartialResultProgressNotification(progressParams);

              verifyNoInteractions(lspEncryptionManager);
              verifyNoInteractions(jsonHandler);
          }
      }

      @Test
      void testIncorrectParamsObject() {
          ConcurrentHashMap<String, Object> partialResultLocks = new ConcurrentHashMap<>();
          partialResultLocks.put("token", new Object());

          try {
              Field partialResultLocksField = ChatCommunicationManager.class.getDeclaredField("partialResultLocks");
              partialResultLocksField.setAccessible(true);
              partialResultLocksField.set(chatCommunicationManager, partialResultLocks);
          } catch (Exception e) {
              throw new RuntimeException(e);
          }

          try (MockedStatic<ProgressNotificationUtils> progressNotificationUtilsMock = mockStatic(ProgressNotificationUtils.class)) {
              progressNotificationUtilsMock
                      .when(() -> ProgressNotificationUtils.getToken(any(ProgressParams.class)))
                      .thenReturn("token");
              when(chatPartialResultMap.getValue(anyString())).thenReturn("tabId");

              Either<WorkDoneProgressNotification, Object> either = mock(Either.class);
              when(either.isLeft()).thenReturn(true);
              when(progressParams.getValue()).thenReturn(either);

              assertThrows(AmazonQPluginException.class,
                      () -> chatCommunicationManager.handlePartialResultProgressNotification(progressParams));

              verifyNoInteractions(lspEncryptionManager);
              verifyNoInteractions(jsonHandler);
          }
      }

      @Test
      void testIncorrectChatPartialResult() {
          try (MockedStatic<ProgressNotificationUtils> progressNotificationUtilsMock = mockStatic(ProgressNotificationUtils.class)) {
              progressNotificationUtilsMock
                      .when(() -> ProgressNotificationUtils.getToken(any(ProgressParams.class)))
                      .thenReturn("token");
              when(chatPartialResultMap.getValue(anyString())).thenReturn("tabId");

              Either<WorkDoneProgressNotification, Object> either = mock(Either.class);
              when(either.getRight()).thenReturn(new Object());
              when(progressParams.getValue()).thenReturn(either);

              progressNotificationUtilsMock
                  .when(() -> ProgressNotificationUtils.getObject(any(ProgressParams.class), eq(String.class)))
                  .thenReturn("chatPartialResult");

              Map<String, Object> chatResult = mock(Map.class);

              when(jsonHandler.deserialize(anyString(), eq(Map.class))).thenReturn(chatResult);
              when(chatResult.get(any())).thenReturn(null);

              chatCommunicationManager.handlePartialResultProgressNotification(progressParams);
          }
      }

      @Test
      void testChatPartialResult() {
          try (MockedStatic<ProgressNotificationUtils> progressNotificationUtilsMock = mockStatic(ProgressNotificationUtils.class)) {
              progressNotificationUtilsMock
                      .when(() -> ProgressNotificationUtils.getToken(any(ProgressParams.class)))
                      .thenReturn("token");
              when(chatPartialResultMap.getValue(anyString())).thenReturn("tabId");

              Either<WorkDoneProgressNotification, Object> either = mock(Either.class);
              when(either.getRight()).thenReturn(new Object());
              when(progressParams.getValue()).thenReturn(either);

              progressNotificationUtilsMock
                  .when(() -> ProgressNotificationUtils.getObject(any(ProgressParams.class), eq(String.class)))
                  .thenReturn("chatPartialResult");

              Map<String, Object> chatResult = mock(Map.class);

              when(jsonHandler.deserialize(anyString(), eq(Map.class))).thenReturn(chatResult);
              when(chatResult.get("body")).thenReturn("body");
              when(jsonHandler.serialize(any(ChatUIInboundCommand.class))).thenReturn("serializedObject");

              chatCommunicationManager.handlePartialResultProgressNotification(progressParams);
          }
      }
  }

}
