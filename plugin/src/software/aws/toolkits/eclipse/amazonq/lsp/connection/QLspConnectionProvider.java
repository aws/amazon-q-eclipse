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

import software.amazon.awssdk.utils.StringUtils;
import software.aws.toolkits.eclipse.amazonq.lsp.encryption.DefaultLspEncryptionManager;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspManager;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspStatusManager;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher.RecordLspSetupArgs;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.preferences.AmazonQPreferencePage;
import software.aws.toolkits.eclipse.amazonq.providers.LspManagerProvider;
import software.aws.toolkits.eclipse.amazonq.telemetry.LanguageServerTelemetryProvider;
import software.aws.toolkits.eclipse.amazonq.telemetry.metadata.ExceptionMetadata;
import software.aws.toolkits.eclipse.amazonq.util.ProxyUtil;
import software.aws.toolkits.telemetry.TelemetryDefinitions.Result;

public class QLspConnectionProvider extends AbstractLspConnectionProvider {

    public QLspConnectionProvider() throws IOException {
        super();
        try {
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
        } catch (Exception e) {
            LspStatusManager.getInstance().setToFailed();
            throw(e);
        }

    }

    @Override
    protected final void addEnvironmentVariables(final Map<String, String> env) {
        String httpsProxyPreference = ProxyUtil.getHttpsProxyUrl();
        String caCertPreference = Activator.getDefault().getPreferenceStore().getString(AmazonQPreferencePage.CA_CERT);
        if (!StringUtils.isEmpty(httpsProxyPreference)) {
            env.put("HTTPS_PROXY", httpsProxyPreference);
        }
        if (!StringUtils.isEmpty(caCertPreference)) {
            env.put("NODE_EXTRA_CA_CERTS", caCertPreference);
        }
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
                LspStatusManager.getInstance().setToFailed();
                emitInitFailure(ExceptionMetadata.scrubException(e));
                Activator.getLogger().error("Error occured while initializing communication with Amazon Q Lsp Server", e);
            }
        } catch (Exception e) {
            LspStatusManager.getInstance().setToFailed();
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
