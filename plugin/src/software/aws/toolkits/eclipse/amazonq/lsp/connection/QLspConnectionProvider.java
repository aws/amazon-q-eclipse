// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import software.amazon.awssdk.utils.StringUtils;
import software.aws.toolkits.eclipse.amazonq.broker.events.AmazonQLspState;
import software.aws.toolkits.eclipse.amazonq.lsp.encryption.DefaultLspEncryptionManager;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspManager;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher.RecordLspSetupArgs;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.preferences.AmazonQPreferencePage;
import software.aws.toolkits.eclipse.amazonq.providers.lsp.LspManagerProvider;
import software.aws.toolkits.eclipse.amazonq.telemetry.LanguageServerTelemetryProvider;
import software.aws.toolkits.eclipse.amazonq.telemetry.metadata.ExceptionMetadata;
import software.aws.toolkits.eclipse.amazonq.util.ArchitectureUtils;
import software.aws.toolkits.eclipse.amazonq.util.PluginPlatform;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;
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
            Activator.getEventBroker().post(AmazonQLspState.class, AmazonQLspState.FAILED);
            throw(e);
        }

    }

    @Override
    protected final void addEnvironmentVariables(final Map<String, String> env) {
        String httpsProxyUrl = ProxyUtil.getHttpsProxyUrl();
        String caCertPath = getCaCert();

        if (!StringUtils.isEmpty(httpsProxyUrl)) {
            env.put("HTTPS_PROXY", httpsProxyUrl);
        }
        if (!StringUtils.isEmpty(caCertPath)) {
            env.put("NODE_EXTRA_CA_CERTS", caCertPath);
            env.put("AWS_CA_BUNDLE", caCertPath);
        }
        if (ArchitectureUtils.isWindowsArm()) {
            env.put("DISABLE_INDEXING_LIBRARY", "true");
        }
        env.put("ENABLE_INLINE_COMPLETION", "true");
        env.put("ENABLE_TOKEN_PROVIDER", "true");

        if (needsPatchEnvVariables()) {
            Activator.getLogger().info("Adding required variables");
            addPatchVariables(env);
        }
    }

    private String getCaCert() {
        String caCertPreference = Activator.getDefault().getPreferenceStore().getString(AmazonQPreferencePage.CA_CERT);
        if (!StringUtils.isEmpty(caCertPreference)) {
            Activator.getLogger().info("Using user-defined CA cert: " + caCertPreference);
            return caCertPreference;
        }
        try {
            String pemContent = ProxyUtil.getCertificatesAsPem();
            if (StringUtils.isEmpty(pemContent)) {
                return null;
            }
            var tempPath = Files.createTempFile("eclipse-q-extra-ca", ".pem");
            Activator.getLogger().info("Injecting IDE trusted certificates from " + tempPath  + " into NODE_EXTRA_CA_CERTS");
            Files.write(tempPath, pemContent.getBytes());
            return tempPath.toString();
        } catch (Exception e) {
            Activator.getLogger().warn("Could not create temp CA cert file", e);
            return null;
        }
    }

    private boolean needsPatchEnvVariables() {
        return PluginUtils.getPlatform().equals(PluginPlatform.MAC);
    }

    private void addPatchVariables(final Map<String, String> env) {
        try {
            var shell = System.getenv("SHELL");
            if (shell == null || shell.isEmpty()) {
                shell = "/bin/zsh"; // fallback
            }
            String shellPath = null;
            var pb = new ProcessBuilder(shell, "-l", "-c", "-i", "/usr/bin/env");
            pb.redirectErrorStream(true);
            var process = pb.start();
            try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                     // Only look for PATH
                    if (line.startsWith("PATH=")) {
                        shellPath = line.substring(5); // 5 is the length of "PATH="
                        break;
                    }
                }
            }

            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }

            if (shellPath != null && !shellPath.isEmpty()) {
                env.put("PATH", shellPath);
            }
        } catch (Exception e) {
            Activator.getLogger().error("Error occurred when attempting to add path variable", e);
        }
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
                Activator.getEventBroker().post(AmazonQLspState.class, AmazonQLspState.FAILED);
                emitInitFailure(ExceptionMetadata.scrubException(e));
                Activator.getLogger().error("Error occured while initializing communication with Amazon Q Lsp Server", e);
            }
        } catch (Exception e) {
            Activator.getEventBroker().post(AmazonQLspState.class, AmazonQLspState.FAILED);
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
