// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.model;

import java.util.Optional;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public enum Command {
    // QChat
    CHAT_READY("aws/chat/ready"),
    CHAT_TAB_ADD("aws/chat/tabAdd"),
    CHAT_TAB_REMOVE("aws/chat/tabRemove"),
    CHAT_TAB_CHANGE("aws/chat/tabChange"),
    FILE_CLICK("aws/chat/fileClick"),
    CHAT_SEND_PROMPT("aws/chat/sendChatPrompt"),
    CHAT_PROMPT_OPTION_CHANGE("aws/chat/promptInputOptionChange"),
    CHAT_LINK_CLICK("aws/chat/linkClick"),
    CHAT_INFO_LINK_CLICK("aws/chat/infoLinkClick"),
    CHAT_SOURCE_LINK_CLICK("aws/chat/sourceLinkClick"),
    CHAT_QUICK_ACTION("aws/chat/sendChatQuickAction"),
    CHAT_END_CHAT("aws/chat/endChat"),
    CHAT_FEEDBACK("aws/chat/feedback"),
    CHAT_FOLLOW_UP_CLICK("aws/chat/followUpClick"),
    TELEMETRY_EVENT("telemetry/event"),
    CHAT_COPY_TO_CLIPBOARD("copyToClipboard"),
    CHAT_INSERT_TO_CURSOR_POSITION("insertToCursorPosition"),
    AUTH_FOLLOW_UP_CLICKED("authFollowUpClicked"), //Auth command handled in QChat webview
    DISCLAIMER_ACKNOWLEDGED("disclaimerAcknowledged"),
    LIST_CONVERSATIONS("aws/chat/listConversations"),
    CONVERSATION_CLICK("aws/chat/conversationClick"),
    CREATE_PROMPT("aws/chat/createPrompt"),
    PROMPT_OPTION_ACKNOWLEDGED("chatPromptOptionAcknowledged"),
    TAB_BAR_ACTION("aws/chat/tabBarAction"),
    GET_SERIALIZED_CHAT("aws/chat/getSerializedChat"),
    STOP_CHAT_RESPONSE("stopChatResponse"),
    BUTTON_CLICK("aws/chat/buttonClick"),
    CHAT_OPEN_TAB("aws/chat/openTab"),
    OPEN_SETTINGS("openSettings"),

    // Auth
    LOGIN_BUILDER_ID("loginBuilderId"),
    LOGIN_IDC("loginIdC"),
    CANCEL_LOGIN("cancelLogin"),
    ON_LOAD("onLoad"),
    ON_SELECT_PROFILE("onSelectProfile");

    private final String commandString;

    Command(final String commandString) {
        this.commandString = commandString;
    }

    public static Optional<Command> fromString(final String value) {
        for (Command command : Command.values()) {
            if (command.commandString.equals(value)) {
                return Optional.ofNullable(command);
            }
        }

        Activator.getLogger().info("Unregistered command parsed: " + value);
        return Optional.empty();
    }

    @Override
    public String toString() {
        return commandString;
    }
}
