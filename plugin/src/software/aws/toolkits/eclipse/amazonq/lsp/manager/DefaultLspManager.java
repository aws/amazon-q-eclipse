// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.manager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;

import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher.ArtifactUtils;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher.LspFetcher;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher.RecordLspSetupArgs;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher.RemoteLspFetcher;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher.VersionManifestFetcher;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.model.Manifest;
import software.aws.toolkits.eclipse.amazonq.util.PluginArchitecture;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.telemetry.LanguageServerTelemetryProvider;
import software.aws.toolkits.eclipse.amazonq.util.PluginPlatform;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;
import software.aws.toolkits.eclipse.amazonq.util.ThreadingUtils;
import software.aws.toolkits.telemetry.TelemetryDefinitions.LanguageServerLocation;
import software.aws.toolkits.telemetry.TelemetryDefinitions.Result;

public final class DefaultLspManager implements LspManager {

    private final String manifestUrl;
    private final Path workingDirectory;
    private final String lspExecutablePrefix;
    private final PluginPlatform platformOverride;
    private final PluginArchitecture architectureOverride;
    private LspInstallResult installResult;

    private DefaultLspManager(final Builder builder) {
        this.manifestUrl = builder.manifestUrl;
        this.workingDirectory = builder.workingDirectory != null ? builder.workingDirectory : PluginUtils.getPluginDir(LspConstants.AMAZONQ_LSP_SUBDIRECTORY);
        this.lspExecutablePrefix = builder.lspExecutablePrefix;
        this.platformOverride = builder.platformOverride;
        this.architectureOverride = builder.architectureOverride;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public LspInstallResult getLspInstallation() {
        if (installResult != null) {
            return installResult;
        }
        try {
            var result = fetchLspInstallation();
            validateAndConfigureLsp(result);
            // store the installation result if validation succeeds
            installResult = result;
            return installResult;
        } catch (Exception e) {
            Activator.getLogger().error(
                    "Unable to resolve local language server installation. LSP features will be unavailable.", e);
            throw new AmazonQPluginException(e);
        }
    }

    private LspInstallResult fetchLspInstallation() {
        var startTime = Instant.now();
        // retrieve local lsp overrides and use that if valid
        var overrideResult = getLocalLspOverride();

        if (overrideResult != null && hasValidResult(overrideResult)) {
            Activator.getLogger().info(String.format("Launching Amazon Q language server from local override location: %s, with command: %s and args: %s",
                    overrideResult.getServerDirectory(), overrideResult.getServerCommand(), overrideResult.getServerCommandArgs()));
            emitGetServerWithOverride(startTime);
            return overrideResult;
        }
        Manifest manifest = fetchManifest();

        var platform = platformOverride != null ? platformOverride : PluginUtils.getPlatform();
        var architecture = architectureOverride != null ? architectureOverride : PluginUtils.getArchitecture();

        startTime = Instant.now();
        var lspFetcher = createLspFetcher(manifest);
        var fetchResult = lspFetcher.fetch(platform, architecture, workingDirectory, startTime);

        // initiate cleanup on a background thread
        initiateCleanup(lspFetcher);

        // set the command and args with the necessary values to launch the Q language server when retrieved from remote/local cache
        var result = new LspInstallResult();
        result.setLocation(fetchResult.location());
        result.setVersion(fetchResult.version());
        result.setServerDirectory(Paths.get(fetchResult.assetDirectory(), LspConstants.LSP_SERVER_FOLDER).toString());
        result.setClientDirectory(Paths.get(fetchResult.assetDirectory(), LspConstants.LSP_CLIENT_FOLDER).toString());
        result.setServerCommand(getNodeForPlatform());
        result.setServerCommandArgs(lspExecutablePrefix);

        return result;
    }

    private void initiateCleanup(final LspFetcher lspFetcher) {
        ThreadingUtils.executeAsyncTask(() -> {
            try {
                lspFetcher.cleanup(workingDirectory);
            } catch (Exception e) {
                // Silently log any errors and continue
                Activator.getLogger().error("Error occured during Amazon Q Language server cache cleanup", e);
            }
        });
    }

    private boolean hasValidResult(final LspInstallResult overrideResult) {
        var start = Instant.now();
        String errorMessage = null;
        try {
            validateLsp(overrideResult);
        } catch (Exception e) {
            Activator.getLogger().error(e.getMessage(), e);
            errorMessage = e.getMessage();
        } finally {
            emitValidate(LanguageServerLocation.OVERRIDE, errorMessage, start);
        }
        return (errorMessage == null);
    }

    LspInstallResult getLocalLspOverride() {
        var serverDirectory = getEnvironmentVariable("Q_SERVER_DIRECTORY");
        var clientDirectory = getEnvironmentVariable("Q_CLIENT_DIRECTORY");
        var serverCommand = getEnvironmentVariable("Q_SERVER_COMMAND");
        var serverCommandArgs = getEnvironmentVariable("Q_SERVER_COMMAND_ARGUMENTS");

        if (!serverDirectory.isEmpty() || !clientDirectory.isEmpty() || !serverCommand.isEmpty() || !serverCommandArgs.isEmpty()) {
            var result = new LspInstallResult();
            result.setLocation(LanguageServerLocation.OVERRIDE);
            result.setServerDirectory(serverDirectory);
            result.setClientDirectory(clientDirectory);
            result.setServerCommand(serverCommand);
            result.setServerCommandArgs(serverCommandArgs);
            return result;
        }

        return null; // Return null if none of the environment variables are set
    }

    private String getEnvironmentVariable(final String variableName) {
        return Optional.ofNullable(System.getenv(variableName)).orElse("");
    }

    Manifest fetchManifest() {
        var args = new RecordLspSetupArgs();
        var start = Instant.now();
        try {
            var manifestFetcher = new VersionManifestFetcher(manifestUrl);
            var manifest = manifestFetcher.fetch()
                    .orElseThrow(() -> new AmazonQPluginException("Failed to retrieve language server manifest"));

            emitGetManifest(Result.SUCCEEDED, start, args, manifest);
            return manifest;
        } catch (Exception e) {
            emitGetManifest(Result.FAILED, start, args, null);
            throw new AmazonQPluginException("Failed to retrieve Amazon Q language server manifest", e);
        }
    }

    private void emitGetManifest(final Result result, final Instant start, final RecordLspSetupArgs args, final Manifest manifest) {
        args.setDuration(Duration.between(start, Instant.now()).toMillis());
        if (manifest != null) {
            args.setManifestSchemaVersion(manifest.manifestSchemaVersion());
        }
        LanguageServerTelemetryProvider.emitSetupGetManifest(result, args);
    }
    private void emitGetServerWithOverride(final Instant start) {
        var args = new RecordLspSetupArgs();
        args.setDuration(Duration.between(start, Instant.now()).toMillis());
        args.setLocation(LanguageServerLocation.OVERRIDE);
        LanguageServerTelemetryProvider.emitSetupGetServer(Result.SUCCEEDED, args);
        return;
    }
    private void emitValidate(final LanguageServerLocation location, final String reason, final Instant start) {
        var args = new RecordLspSetupArgs();
        Result result = (reason == null) ? Result.SUCCEEDED : Result.FAILED;
        args.setDuration(Duration.between(start, Instant.now()).toMillis());
        args.setLocation(location);
        args.setReason(reason);
        LanguageServerTelemetryProvider.emitSetupValidate(result, args);
    }

    private void validateAndConfigureLsp(final LspInstallResult result) throws IOException {
        var start = Instant.now();
        String errorMessage = null;
        try {
            validateLsp(result);
            var serverDirPath = Paths.get(result.getServerDirectory());
            var nodeExecutable = serverDirPath.resolve(result.getServerCommand());
            makeExecutable(nodeExecutable);
        } catch (Exception e) {
            errorMessage = e.getMessage();
            throw e;
        } finally {
            emitValidate(result.getLocation(), errorMessage, start);
        }
    }

    private void validateLsp(final LspInstallResult result) {
        var serverDirPath = Paths.get(result.getServerDirectory());

        if (result.getServerDirectory().isEmpty() || !Files.exists(serverDirPath)) {
            throw new AmazonQPluginException("Error finding Amazon Q Language Server Working Directory");
        }

        var serverCommand = result.getServerCommand();
        var expectedServerCommand = getNodeForPlatform();
        if (!serverCommand.equalsIgnoreCase(expectedServerCommand) || !Files.exists(serverDirPath.resolve(serverCommand))) {
            throw new AmazonQPluginException("Error finding Amazon Q Language Server Command");
        }

        var serverCommandArgs = result.getServerCommandArgs();

        if (!serverCommandArgs.equalsIgnoreCase(lspExecutablePrefix) || !Files.exists(serverDirPath.resolve(serverCommandArgs))) {
            throw new AmazonQPluginException("Error finding Amazon Q Language Server Command Args");
        }
    }

    private String getNodeForPlatform() {
        var platform = platformOverride != null ? platformOverride : PluginUtils.getPlatform();
        return platform == PluginPlatform.WINDOWS ? LspConstants.NODE_EXECUTABLE_WINDOWS : LspConstants.NODE_EXECUTABLE_OSX;
    }

    LspFetcher createLspFetcher(final Manifest manifest) {
        return RemoteLspFetcher.builder()
                .withManifest(manifest)
                .build();
    }

    private static void makeExecutable(final Path filePath) throws IOException {
        if (!ArtifactUtils.hasPosixFilePermissions(filePath)) {
            return;
        }
        var permissions = new HashSet<>(Arrays.asList(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_EXECUTE));
        Files.setPosixFilePermissions(filePath, permissions);
    }

    public static class Builder {
        private String manifestUrl;
        private Path workingDirectory;
        private String lspExecutablePrefix;
        private PluginPlatform platformOverride;
        private PluginArchitecture architectureOverride;

        public final Builder withManifestUrl(final String manifestUrl) {
            this.manifestUrl = manifestUrl;
            return this;
        }

        public final Builder withDirectory(final Path workingDirectory) {
            this.workingDirectory = workingDirectory;
            return this;
        }

        public final Builder withLspExecutablePrefix(final String lspExecutablePrefix) {
            this.lspExecutablePrefix = lspExecutablePrefix;
            return this;
        }

        public final Builder withPlatformOverride(final PluginPlatform platformOverride) {
            this.platformOverride = platformOverride;
            return this;
        }

        public final Builder withArchitectureOverride(final PluginArchitecture architectureOverride) {
            this.architectureOverride = architectureOverride;
            return this;
        }

        public final DefaultLspManager build() {
            return new DefaultLspManager(this);
        }
    }
}
