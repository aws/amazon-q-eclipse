// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.connection;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspManager;
import software.aws.toolkits.eclipse.amazonq.providers.LspManagerProvider;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;

public class AuthLspConnectionProvider extends AbstractLspConnectionProvider {

    public AuthLspConnectionProvider(final URL authJsUrl, final LspManager lspManager) throws IOException, URISyntaxException {
        super();
        var authJsPath = Path.of(authJsUrl.toURI()).toString();

        List<String> commands = new ArrayList<>();
        commands.add(lspManager.getLspInstallation().nodeExecutable().toString());
        commands.add(authJsPath + "/packages/server/dist/index.js");
        commands.add("--nolazy");
        commands.add("--inspect=5599");
        commands.add("--stdio");
        setCommands(commands);
    }

    public AuthLspConnectionProvider() throws IOException, URISyntaxException {
        this(PluginUtils.getResource("auth/"), LspManagerProvider.getInstance());
    }

    @Override
    protected void addEnvironmentVariables(final Map<String, String> env) { }

}
