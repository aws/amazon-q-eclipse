// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat.models;

public enum ChatUIInboundCommandName {
    ChatPrompt("aws/chat/sendChatPrompt"), // This is the odd one out, it follows the same message name as the request.

    SendToPrompt("sendToPrompt"),
    ErrorMessage("errorMessage"),
    InsertToCursorPosition("insertToCursorPosition"),
    AuthFollowUpClicked("authFollowUpClicked"),
    GenericCommand("genericCommand");

    private final String commandString;

    ChatUIInboundCommandName(final String commandString) {
        this.commandString = commandString;
    }

    public String toString() {
        return commandString;
    }
}
