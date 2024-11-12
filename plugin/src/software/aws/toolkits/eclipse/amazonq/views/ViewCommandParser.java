// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import java.util.Optional;

import software.aws.toolkits.eclipse.amazonq.views.model.ParsedCommand;

public interface ViewCommandParser {
    Optional<ParsedCommand> parseCommand(Object[] arguments);
}
