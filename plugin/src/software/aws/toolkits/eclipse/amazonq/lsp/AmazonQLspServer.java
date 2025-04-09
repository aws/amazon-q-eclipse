// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.lsp;

import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.services.LanguageServer;

import software.aws.toolkits.eclipse.amazonq.chat.models.EncryptedChatParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.EncryptedQuickActionParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.FeedbackParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.FollowUpClickParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericTabParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenResult;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.InvalidateSsoTokenParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.InvalidateSsoTokenResult;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.ListProfilesResult;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.UpdateProfileParams;
import software.aws.toolkits.eclipse.amazonq.lsp.model.GetConfigurationFromServerParams;
import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionParams;
import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionResponse;
import software.aws.toolkits.eclipse.amazonq.lsp.model.LogInlineCompletionSessionResultsParams;
import software.aws.toolkits.eclipse.amazonq.lsp.model.LspServerConfigurations;
import software.aws.toolkits.eclipse.amazonq.lsp.model.UpdateCredentialsPayload;

public interface AmazonQLspServer extends LanguageServer {

    @JsonRequest("aws/textDocument/inlineCompletionWithReferences")
    CompletableFuture<InlineCompletionResponse> inlineCompletionWithReferences(InlineCompletionParams params);

    @JsonNotification("aws/logInlineCompletionSessionResults")
    void logInlineCompletionSessionResult(LogInlineCompletionSessionResultsParams params);

    @JsonRequest("aws/chat/sendChatPrompt")
    CompletableFuture<String> sendChatPrompt(EncryptedChatParams encryptedChatRequestParams);

    @JsonRequest("aws/chat/sendInlineChatPrompt")
    CompletableFuture<String> sendInlineChatPrompt(EncryptedChatParams encryptedChatRequestParams);

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

    @JsonNotification("aws/chat/feedback")
    void sendFeedback(FeedbackParams params);

    @JsonRequest("aws/credentials/token/update")
    CompletableFuture<ResponseMessage> updateTokenCredentials(UpdateCredentialsPayload payload);

    @JsonNotification("aws/credentials/token/delete")
    void deleteTokenCredentials();

    @JsonRequest("aws/getConfigurationFromServer")
    CompletableFuture<LspServerConfigurations> getConfigurationFromServer(GetConfigurationFromServerParams params);

    @JsonNotification("telemetry/event")
    void sendTelemetryEvent(Object params);

    @JsonRequest("aws/identity/listProfiles")
    CompletableFuture<ListProfilesResult> listProfiles();

    @JsonRequest("aws/identity/getSsoToken")
    CompletableFuture<GetSsoTokenResult> getSsoToken(GetSsoTokenParams params);

    @JsonRequest("aws/identity/invalidateSsoToken")
    CompletableFuture<InvalidateSsoTokenResult> invalidateSsoToken(InvalidateSsoTokenParams params);

    @JsonRequest("aws/identity/updateProfile")
    CompletableFuture<Void> updateProfile(UpdateProfileParams params);
}
