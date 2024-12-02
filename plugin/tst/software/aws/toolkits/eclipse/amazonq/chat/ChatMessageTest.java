// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import software.aws.toolkits.eclipse.amazonq.chat.models.EncryptedChatParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.EncryptedQuickActionParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.FollowUpClickParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericTabParams;
import software.aws.toolkits.eclipse.amazonq.lsp.AmazonQLspServer;

public final class ChatMessageTest {

    @Mock
    private AmazonQLspServer amazonQLspServerMock;

    private final CompletableFuture<String> mockResponse = CompletableFuture.completedFuture("testResponse");

    private ChatMessage chatMessage;

    @BeforeEach
    void setupBeforeEach() {
        MockitoAnnotations.openMocks(this);
        when(amazonQLspServerMock.sendChatPrompt(any(EncryptedChatParams.class)))
                .thenReturn(mockResponse);
        when(amazonQLspServerMock.sendQuickAction(any(EncryptedQuickActionParams.class)))
                .thenReturn(mockResponse);
        when(amazonQLspServerMock.endChat(any(GenericTabParams.class)))
                .thenReturn(CompletableFuture.completedFuture(true));

        chatMessage = new ChatMessage(amazonQLspServerMock);
    }

    @Test
    void testSendChatPrompt() {
        EncryptedChatParams params = mock(EncryptedChatParams.class);
        CompletableFuture<String> response = chatMessage.sendChatPrompt(params);

        verify(amazonQLspServerMock).sendChatPrompt(params);
        assertTrue(response.join().equals("testResponse"));
    }

    @Test
    void testSendQuickAction() {
        EncryptedQuickActionParams params = mock(EncryptedQuickActionParams.class);
        CompletableFuture<String> response = chatMessage.sendQuickAction(params);

        verify(amazonQLspServerMock).sendQuickAction(params);
        assertTrue(response.join().equals("testResponse"));
    }

    @Test
    void testEndChat() {
        GenericTabParams params = mock(GenericTabParams.class);
        CompletableFuture<Boolean> response = chatMessage.endChat(params);

        verify(amazonQLspServerMock).endChat(params);
        assertTrue(response.join());
    }

    @Test
    void testSendChatReady() {
        chatMessage.sendChatReady();
        verify(amazonQLspServerMock).chatReady();
    }

    @Test
    void testSendTabAdd() {
        GenericTabParams params = mock(GenericTabParams.class);
        chatMessage.sendTabAdd(params);
        verify(amazonQLspServerMock).tabAdd(params);
    }

    @Test
    void sendTabRemove() {
        GenericTabParams params = mock(GenericTabParams.class);
        chatMessage.sendTabRemove(params);
        verify(amazonQLspServerMock).tabRemove(params);
    }

    @Test
    void sendTabChange() {
        GenericTabParams params = mock(GenericTabParams.class);
        chatMessage.sendTabChange(params);
        verify(amazonQLspServerMock).tabChange(params);
    }

    @Test
    void sendFollowUpClick() {
        FollowUpClickParams params = mock(FollowUpClickParams.class);
        chatMessage.followUpClick(params);
        verify(amazonQLspServerMock).followUpClick(params);
    }

}
