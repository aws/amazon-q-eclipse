// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.connection;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import software.aws.toolkits.eclipse.amazonq.lsp.encryption.DefaultLspEncryptionManager;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspManager;
import software.aws.toolkits.eclipse.amazonq.providers.LspManagerProvider;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public class QLspConnectionProvider extends AbstractLspConnectionProvider {

    public QLspConnectionProvider() throws IOException {
        super();
        LspManager lspManager = LspManagerProvider.getInstance();
        var lspInstallResult = lspManager.getLspInstallation();

        setWorkingDirectory(lspInstallResult.getServerDirectory());

        var serverCommand = Paths.get(lspInstallResult.getServerDirectory(), lspInstallResult.getServerCommand());
        List<String> commands = new ArrayList<>();
        commands.add(serverCommand.toString());
        commands.add(lspInstallResult.getServerCommandArgs());
        commands.add("--stdio");
        commands.add("--set-credentials-encryption-key");
        setCommands(commands);
    }

    @Override
    protected final void addEnvironmentVariables(final Map<String, String> env) {
        env.put("ENABLE_INLINE_COMPLETION", "true");
        env.put("ENABLE_TOKEN_PROVIDER", "true");
    }

    @Override
    public final void start() throws IOException {
        startProcess();

        Activator.getLogger().info("Initializing encrypted communication with Amazon Q Lsp Server");

        try {
            DefaultLspEncryptionManager lspEncryption = DefaultLspEncryptionManager.getInstance();
            OutputStream serverStdIn = getOutputStream();

            lspEncryption.initializeEncrypedCommunication(serverStdIn);
        } catch (Exception e) {
            Activator.getLogger().error("Error occured while initializing encrypted communication with Amazon Q Lsp Server", e);
        }
    }

    protected final void startProcess() throws IOException {
        super.start();
    }
}
