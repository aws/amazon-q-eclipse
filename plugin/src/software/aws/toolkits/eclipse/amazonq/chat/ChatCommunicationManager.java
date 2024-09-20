// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat;

import org.eclipse.swt.browser.Browser;

import software.aws.toolkits.eclipse.amazonq.chat.models.ChatRequestParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatResult;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommand;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericTabParams;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.util.JsonHandler;
import software.aws.toolkits.eclipse.amazonq.views.model.Command;

public final class ChatCommunicationManager {

    private final JsonHandler jsonHandler;
    private final ChatMessageProvider chatMessageProvider;

    public ChatCommunicationManager() {
        this.jsonHandler = new JsonHandler();
        this.chatMessageProvider = new ChatMessageProvider();
    }

    /*
     * Sends a message to the Amazon Q LSP server.
     */
    public ChatResult sendMessageToChatServer(final Command command, final Object params) {

           switch (command) {
               case CHAT_SEND_PROMPT:
                   ChatRequestParams chatRequestParams = jsonHandler.convertObject(params, ChatRequestParams.class);
                   ChatResult result = chatMessageProvider.sendChatPrompt(chatRequestParams);
                   return result;
               case CHAT_READY:
                   chatMessageProvider.sendChatReady();
                   return null;
               case CHAT_TAB_ADD:
                   GenericTabParams tabParams = jsonHandler.convertObject(params, GenericTabParams.class);
                   chatMessageProvider.sendTabAdd(tabParams);
                   return null;
               default:
                   throw new AmazonQPluginException("Unhandled command in ChatCommunicationManager: " + command.toString());
           }
    }

    /**
     * Sends a message to the webview.
     *
     * See handlers in Flare chat-client:
     * https://github.com/aws/language-servers/blob/9226fb4ed10dc54f1719b14a5b1dac1807641f79/chat-client/src/client/chat.ts#L67-L101
     */
    public void sendMessageToChatUI(final Browser browser, final ChatUIInboundCommand command) {
        String message = this.jsonHandler.serialize(command);
        String script = "window.postMessage(" + message + ");";
        browser.getDisplay().asyncExec(() -> {
            browser.evaluate(script);
        });
    }
}
