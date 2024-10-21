// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.connection;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspManager;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;

@Component(service = AuthLspConnectionProvider.class)
public class AuthLspConnectionProvider extends AbstractLspConnectionProvider {

    @Reference private LspManager lspManager;

    public AuthLspConnectionProvider() throws IOException, URISyntaxException {
        super();
        var authJs = PluginUtils.getResource("auth/packages/server/dist/index.js");
        var authJsPath = Path.of(authJs.toURI()).toString();

        List<String> commands = new ArrayList<>();
        commands.add(lspManager.getLspInstallation().nodeExecutable().toString());
        commands.add(authJsPath);
        commands.add("--nolazy");
        commands.add("--inspect=5599");
        commands.add("--stdio");
        setCommands(commands);
    }

    @Override
    protected void addEnvironmentVariables(final Map<String, String> env) { }

}
