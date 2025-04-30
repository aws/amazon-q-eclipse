// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat;

import java.util.concurrent.CompletableFuture;

import software.aws.toolkits.eclipse.amazonq.chat.models.EncryptedChatParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.EncryptedQuickActionParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.FeedbackParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.FileClickParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.FollowUpClickParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericTabParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.InsertToCursorPositionParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.PromptInputOptionChangeParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericLinkClickParams;
import software.aws.toolkits.eclipse.amazonq.lsp.AmazonQLspServer;

public final class ChatMessage {
    private final AmazonQLspServer amazonQLspServer;

    public ChatMessage(final AmazonQLspServer amazonQLspServer) {
        this.amazonQLspServer = amazonQLspServer;
    }

    // Returns a ChatResult as an encrypted message {@link LspEncryptionManager#decrypt()}
    public CompletableFuture<String> sendChatPrompt(final EncryptedChatParams params) {
        return amazonQLspServer.sendChatPrompt(params);
    }

    public CompletableFuture<String> sendInlineChatPrompt(final EncryptedChatParams params) {
        return amazonQLspServer.sendInlineChatPrompt(params);
    }

    // Returns a ChatResult as an encrypted message {@link LspEncryptionManager#decrypt()}
    public CompletableFuture<String> sendQuickAction(final EncryptedQuickActionParams params) {
        return amazonQLspServer.sendQuickAction(params);
    }

    public CompletableFuture<Boolean> endChat(final GenericTabParams tabParams) {
        return amazonQLspServer.endChat(tabParams);
    }

    public void sendPromptInputOptionChange(final PromptInputOptionChangeParams optionChangeParams) {
        amazonQLspServer.promptInputOptionChange(optionChangeParams);
    }

    public void sendChatReady() {
        amazonQLspServer.chatReady();
    }

    public void sendTabAdd(final GenericTabParams tabParams) {
        amazonQLspServer.tabAdd(tabParams);
    }

    public void sendTabRemove(final GenericTabParams tabParams) {
        amazonQLspServer.tabRemove(tabParams);
    }

    public void sendTabChange(final GenericTabParams tabParams) {
        amazonQLspServer.tabChange(tabParams);
    }

    public void sendFileClick(final FileClickParams fileClickParams) {
        amazonQLspServer.fileClick(fileClickParams);
    }

    public void sendInfoLinkClickParams(final GenericLinkClickParams sourceLinkClickParams) {
        amazonQLspServer.infoLinkClick(sourceLinkClickParams);
    }

    public void sendLinkClickParams(final GenericLinkClickParams sourceLinkClickParams) {
        amazonQLspServer.linkClick(sourceLinkClickParams);
    }

    public void sendSourceLinkClickParams(final GenericLinkClickParams sourceLinkClickParams) {
        amazonQLspServer.sourceLinkClick(sourceLinkClickParams);
    }

    public void sendInsertToCursorPositionParams(final InsertToCursorPositionParams insertToCursorParams) {
        amazonQLspServer.insertToCursorPosition(insertToCursorParams);
    }

    public void followUpClick(final FollowUpClickParams followUpClickParams) {
        amazonQLspServer.followUpClick(followUpClickParams);
    }

    public void sendFeedback(final FeedbackParams feedbackParams) {
        amazonQLspServer.sendFeedback(feedbackParams);
    }

    public void sendTelemetryEvent(final Object params) {
        amazonQLspServer.sendTelemetryEvent(params);
    }
}
