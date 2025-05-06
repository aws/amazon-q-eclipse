// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.lsp;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.services.LanguageServer;

import software.aws.toolkits.eclipse.amazonq.chat.models.ButtonClickResult;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenResult;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.InvalidateSsoTokenParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.InvalidateSsoTokenResult;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.ListProfilesResult;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.UpdateProfileParams;
import software.aws.toolkits.eclipse.amazonq.lsp.model.GetConfigurationFromServerParams;
import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionResponse;
import software.aws.toolkits.eclipse.amazonq.lsp.model.LspServerConfigurations;
import software.aws.toolkits.eclipse.amazonq.lsp.model.UpdateCredentialsPayload;

public interface AmazonQLspServer extends LanguageServer {

    @JsonRequest("aws/textDocument/inlineCompletionWithReferences")
    CompletableFuture<InlineCompletionResponse> inlineCompletionWithReferences(Object params);

    @JsonNotification("aws/logInlineCompletionSessionResults")
    void logInlineCompletionSessionResult(Object params);

    @JsonRequest("aws/chat/sendChatPrompt")
    CompletableFuture<String> sendChatPrompt(Object encryptedChatRequestParams);

    @JsonRequest("aws/chat/sendInlineChatPrompt")
    CompletableFuture<String> sendInlineChatPrompt(Object encryptedChatRequestParams);

    @JsonRequest("aws/chat/sendChatQuickAction")
    CompletableFuture<String> sendQuickAction(Object encryptedQuickActionParams);

    @JsonRequest("aws/chat/endChat")
    CompletableFuture<Boolean> endChat(Object params);

    @JsonNotification("aws/chat/tabAdd")
    void tabAdd(Object params);

    @JsonNotification("aws/chat/tabRemove")
    void tabRemove(Object params);

    @JsonNotification("aws/chat/tabChange")
    void tabChange(Object params);

    @JsonNotification("aws/chat/fileClick")
    void fileClick(Object params);

    @JsonNotification("aws/chat/infoLinkClick")
    void infoLinkClick(Object params);

    @JsonNotification("aws/chat/linkClick")
    void linkClick(Object params);

    @JsonNotification("aws/chat/sourceLinkClick")
    void sourceLinkClick(Object params);

    @JsonNotification("aws/chat/followUpClick")
    void followUpClick(Object params);

    @JsonNotification("aws/chat/promptInputOptionChange")
    void promptInputOptionChange(Object params);

    @JsonNotification("aws/chat/ready")
    void chatReady();

    @JsonNotification("aws/chat/feedback")
    void sendFeedback(Object params);

    @JsonNotification("aws/chat/insertToCursorPosition")
    void insertToCursorPosition(Object params);

    @JsonRequest("aws/credentials/token/update")
    CompletableFuture<ResponseMessage> updateTokenCredentials(UpdateCredentialsPayload payload);

    @JsonNotification("aws/credentials/token/delete")
    void deleteTokenCredentials();

    @JsonRequest("aws/getConfigurationFromServer")
    CompletableFuture<LspServerConfigurations> getConfigurationFromServer(GetConfigurationFromServerParams params);

    @JsonNotification("telemetry/event")
    void sendTelemetryEvent(Object params);

    @JsonRequest("aws/chat/listConversations")
    CompletableFuture<Object> listConversations(Object params);

    @JsonRequest("aws/chat/conversationClick")
    CompletableFuture<Object> conversationClick(Object params);

    @JsonRequest("aws/identity/listProfiles")
    CompletableFuture<ListProfilesResult> listProfiles();

    @JsonRequest("aws/identity/getSsoToken")
    CompletableFuture<GetSsoTokenResult> getSsoToken(GetSsoTokenParams params);

    @JsonRequest("aws/identity/invalidateSsoToken")
    CompletableFuture<InvalidateSsoTokenResult> invalidateSsoToken(InvalidateSsoTokenParams params);

    @JsonRequest("aws/identity/updateProfile")
    CompletableFuture<Void> updateProfile(UpdateProfileParams params);

    @JsonNotification("aws/chat/createPrompt")
    CompletableFuture<Void> createPrompt(Object params);

    @JsonRequest("aws/chat/tabBarAction")
    CompletableFuture<Object> tabBarAction(Object params);

    @JsonRequest("aws/chat/getSerializedChat")
    CompletableFuture<Object> getSerializedActions(Object params);

    @JsonRequest("aws/chat/buttonClick")
    CompletableFuture<ButtonClickResult> buttonClick(Object params);
}
