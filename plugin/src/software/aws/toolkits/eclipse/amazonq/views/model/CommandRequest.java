// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.views.model;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CommandRequest(
        @JsonProperty("command") String commandString,
        @JsonProperty("params") Object params) {
    public Optional<ParsedCommand> getParsedCommand() {
        Optional<Command> command = Command.fromString(commandString);

        if (!command.isPresent()) {
            return Optional.empty();
        }
        
        ParsedCommand parsedCommand = new ParsedCommand(command.get(), params);
        return Optional.ofNullable(parsedCommand);
    }
}
