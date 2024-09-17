// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.connection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import software.aws.toolkits.eclipse.amazonq.providers.LspManagerProvider;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;

public class AuthLspConnectionProvider extends AbstractLspConnectionProvider {

    public AuthLspConnectionProvider() throws IOException {
        super();
        var authJs = PluginUtils.getResource("auth/packages/server/dist/index.js");
        var lspManager = LspManagerProvider.getInstance();

        List<String> commands = new ArrayList<>();
        commands.add(lspManager.getLspInstallation().nodeExecutable().toString());
        commands.add(authJs.getPath());
        commands.add("--nolazy");
        commands.add("--inspect=5599");
        commands.add("--stdio");
        setCommands(commands);
    }

    @Override
    protected void addEnvironmentVariables(final Map<String, String> env) { }

}
