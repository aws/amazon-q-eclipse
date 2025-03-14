// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.model;

public class ParsedCommand {

    private final Command command;
    private final Object params;

    public ParsedCommand(final Command command, final Object params) {
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
