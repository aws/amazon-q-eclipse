// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.views.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CommandRequest(
        @JsonProperty("command") String commandString,
        @JsonProperty("params") Object params) {

    public ParsedCommand getParsedCommand() {
        Command command = Command.fromString(commandString).orElse(null);
        ParsedCommand parsedCommand = new ParsedCommand(command, params);
        return parsedCommand;
    }
}
