// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import org.eclipse.swt.browser.Browser;

import software.aws.toolkits.eclipse.amazonq.views.model.ParsedCommand;

public interface ViewActionHandler {
    void handleCommand(ParsedCommand parsedCommand, Browser browser);
}
