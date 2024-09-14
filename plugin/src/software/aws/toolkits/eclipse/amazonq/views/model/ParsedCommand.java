// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.views.model;

import java.util.Optional;

public class ParsedCommand {

    private final Command command;
    private final Object params;

    public ParsedCommand(final Command command, final Optional<Object> params) {
        this.command = command;
        this.params = params;
    }

    public final Command getCommand() {
        return this.command;
    }

    public final Object getParams() {
        return this.params;
    }

}
