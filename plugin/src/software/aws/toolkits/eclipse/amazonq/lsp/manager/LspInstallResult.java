// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.manager;

import software.aws.toolkits.eclipse.amazonq.lsp.model.LanguageServerLocation;

public final class LspInstallResult {
    private String serverDirectory;
    private String clientDirectory;
    private String serverCommand;
    private String serverCommandArgs;
    private String version;
    private LanguageServerLocation location;

    public String getServerDirectory() {
        return serverDirectory;
    }

    public void setServerDirectory(final String serverDirectory) {
        this.serverDirectory = serverDirectory;
    }

    public String getClientDirectory() {
        return clientDirectory;
    }

    public void setClientDirectory(final String clientDirectory) {
        this.clientDirectory = clientDirectory;
    }

    public String getServerCommand() {
        return serverCommand;
    }

    public void setServerCommand(final String serverCommand) {
        this.serverCommand = serverCommand;
    }

    public String getServerCommandArgs() {
        return serverCommandArgs;
    }

    public void setServerCommandArgs(final String serverCommandArgs) {
        this.serverCommandArgs = serverCommandArgs;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public LanguageServerLocation getLocation() {
        return location;
    }

    public void setLocation(final LanguageServerLocation location) {
        this.location = location;
    }
}
