// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.aws.toolkits.eclipse.amazonq.util.ObjectMapperFactory;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.views.model.CommandRequest;
import software.aws.toolkits.eclipse.amazonq.views.model.ParsedCommand;

public class LoginViewCommandParser implements ViewCommandParser {
    private final ObjectMapper objectMapper;

    public LoginViewCommandParser() {
        this.objectMapper = ObjectMapperFactory.getInstance();
    }

    @Override
    public final Optional<ParsedCommand> parseCommand(final Object[] arguments) {
        if (arguments.length > 0 && arguments[0] instanceof String) {
            String jsonString = (String) arguments[0];
            try {
                CommandRequest commandRequest = objectMapper.readValue(jsonString, CommandRequest.class);
                ParsedCommand parsedCommand = commandRequest.getParsedCommand();

                if (parsedCommand.getCommand() == null) {
                    return Optional.empty();
                }

                return Optional.ofNullable(parsedCommand);
            } catch (JsonProcessingException e) {
                Activator.getLogger().error("Error parsing webview command JSON: " + e.getMessage());
            }
        }
        return Optional.empty();
    }
}
