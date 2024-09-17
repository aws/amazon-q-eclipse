// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.connection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspManager;
import software.aws.toolkits.eclipse.amazonq.providers.LspManagerProvider;

public class QLspConnectionProvider extends AbstractLspConnectionProvider {

    public QLspConnectionProvider() throws IOException {
        super();
        LspManager lspManager = LspManagerProvider.getInstance();
        List<String> commands = new ArrayList<>();
        commands.add(lspManager.getLspInstallation().nodeExecutable().toString());
        commands.add(lspManager.getLspInstallation().lspJs().toString());
        commands.add("--nolazy");
        commands.add("--inspect=5599");
        commands.add("--stdio");
        setCommands(commands);
    }

    @Override
    protected final void addEnvironmentVariables(final Map<String, String> env) {
        env.put("ENABLE_INLINE_COMPLETION", "true");
        env.put("ENABLE_TOKEN_PROVIDER", "true");
    }

}
