// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.lsp;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.services.LanguageServer;

import software.aws.toolkits.eclipse.amazonq.chat.models.EncryptedChatParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.EncryptedQuickActionParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.FollowUpClickParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericTabParams;
import software.aws.toolkits.eclipse.amazonq.lsp.model.GetConfigurationFromServerParams;
import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionParams;
import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionResponse;
import software.aws.toolkits.eclipse.amazonq.lsp.model.UpdateCredentialsPayload;
import software.aws.toolkits.eclipse.amazonq.views.model.Customization;

public interface AmazonQLspServer extends LanguageServer {

    @JsonRequest("aws/textDocument/inlineCompletionWithReferences")
    CompletableFuture<InlineCompletionResponse> inlineCompletionWithReferences(InlineCompletionParams params);

    @JsonRequest("aws/chat/sendChatPrompt")
    CompletableFuture<String> sendChatPrompt(EncryptedChatParams encryptedChatRequestParams);

    @JsonRequest("aws/chat/sendChatQuickAction")
    CompletableFuture<String> sendQuickAction(EncryptedQuickActionParams encryptedQuickActionParams);

    @JsonRequest("aws/chat/endChat")
    CompletableFuture<Boolean> endChat(GenericTabParams params);

    @JsonNotification("aws/chat/tabAdd")
    void tabAdd(GenericTabParams params);

    @JsonNotification("aws/chat/tabRemove")
    void tabRemove(GenericTabParams params);

    @JsonNotification("aws/chat/tabChange")
    void tabChange(GenericTabParams params);

    @JsonNotification("aws/chat/followUpClick")
    void followUpClick(FollowUpClickParams params);

    @JsonNotification("aws/chat/ready")
    void chatReady();

    @JsonRequest("aws/credentials/token/update")
    CompletableFuture<ResponseMessage> updateTokenCredentials(UpdateCredentialsPayload payload);

    @JsonRequest("aws/getConfigurationFromServer")
    CompletableFuture<List<Customization>> getConfigurationFromServer(GetConfigurationFromServerParams params);
}
