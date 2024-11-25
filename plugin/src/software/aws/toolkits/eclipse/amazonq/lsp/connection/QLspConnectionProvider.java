// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.connection;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import software.aws.toolkits.eclipse.amazonq.lsp.encryption.DefaultLspEncryptionManager;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspManager;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher.RecordLspSetupArgs;
import software.aws.toolkits.eclipse.amazonq.providers.LspManagerProvider;
import software.aws.toolkits.eclipse.amazonq.telemetry.LanguageServerTelemetryProvider;
import software.aws.toolkits.eclipse.amazonq.telemetry.metadata.ExceptionMetadata;
import software.aws.toolkits.telemetry.TelemetryDefinitions.Result;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public class QLspConnectionProvider extends AbstractLspConnectionProvider {

    public QLspConnectionProvider() throws IOException {
        super();
        LanguageServerTelemetryProvider.setAllStartPoint(Instant.now());
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
        LanguageServerTelemetryProvider.setInitStartPoint(Instant.now());
        try {
            startProcess();

            Activator.getLogger().info("Initializing communication with Amazon Q Lsp Server");

            try {
                DefaultLspEncryptionManager lspEncryption = DefaultLspEncryptionManager.getInstance();
                OutputStream serverStdIn = getOutputStream();

                lspEncryption.initializeEncryptedCommunication(serverStdIn);
            } catch (Exception e) {
                emitInitFailure(ExceptionMetadata.scrubException(e));
                Activator.getLogger().error("Error occured while initializing communication with Amazon Q Lsp Server", e);
            }
        } catch (Exception e) {
            emitInitFailure(ExceptionMetadata.scrubException(e));
            throw e;
        }
    }

    protected final void startProcess() throws IOException {
        super.start();
    }
    private void emitInitFailure(final String reason) {
        var args = new RecordLspSetupArgs();
        args.setReason(reason);
        LanguageServerTelemetryProvider.emitSetupInitialize(Result.FAILED, args);
    }
}
