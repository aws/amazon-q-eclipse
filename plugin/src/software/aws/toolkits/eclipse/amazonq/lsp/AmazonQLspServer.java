// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.lsp;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.services.LanguageServer;

import software.aws.toolkits.eclipse.amazonq.chat.models.ChatRequestParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatResult;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericTabParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.QuickActionParams;
import software.aws.toolkits.eclipse.amazonq.lsp.model.GetConfigurationFromServerParams;
import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionParams;
import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionResponse;
import software.aws.toolkits.eclipse.amazonq.lsp.model.UpdateCredentialsPayload;
import software.aws.toolkits.eclipse.amazonq.views.model.Customization;

public interface AmazonQLspServer extends LanguageServer {

    @JsonRequest("aws/textDocument/inlineCompletionWithReferences")
    CompletableFuture<InlineCompletionResponse> inlineCompletionWithReferences(InlineCompletionParams params);

    @JsonRequest("aws/chat/sendChatPrompt")
    CompletableFuture<ChatResult> sendChatPrompt(ChatRequestParams chatRequestParams);

    @JsonRequest("aws/chat/sendChatQuickAction")
    CompletableFuture<ChatResult> sendQuickAction(QuickActionParams quickActionParams);

    @JsonNotification("aws/chat/tabAdd")
    void tabAdd(GenericTabParams params);

    @JsonNotification("aws/chat/ready")
    void chatReady();

    @JsonRequest("aws/credentials/token/update")
    CompletableFuture<ResponseMessage> updateTokenCredentials(UpdateCredentialsPayload payload);

    @JsonRequest("aws/getConfigurationFromServer")
    CompletableFuture<List<Customization>> getConfigurationFromServer(GetConfigurationFromServerParams params);
}
