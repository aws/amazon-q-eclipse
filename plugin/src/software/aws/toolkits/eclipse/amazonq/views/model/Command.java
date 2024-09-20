// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.views.model;

import java.util.Optional;

import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;

public enum Command {
    // QChat
    CHAT_READY("aws/chat/ready"),
    CHAT_TAB_ADD("aws/chat/tabAdd"),
    CHAT_SEND_PROMPT("aws/chat/sendChatPrompt"),
    TELEMETRY_EVENT("telemetry/event"),

    // Auth
    LOGIN_BUILDER_ID("loginBuilderId"),
    CANCEL_LOGIN("cancelLogin");

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

        PluginLogger.info("Unregistered command parsed: " + value);
        return Optional.empty();
    }

    public String toString() {
        return commandString;
    }
}
