// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.manager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.HashSet;

import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher.LspFetcher;
import software.aws.toolkits.eclipse.amazonq.util.PluginArchitecture;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;
import software.aws.toolkits.eclipse.amazonq.util.PluginPlatform;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;

public final class DefaultLspManager implements LspManager {

    private final LspFetcher fetcher;
    private final Path workingDirectory;
    private final String lspExecutablePrefix;
    private final PluginPlatform platformOverride;
    private final PluginArchitecture architectureOverride;

    private DefaultLspManager(final Builder builder) {
        this.fetcher = builder.fetcher;
        this.workingDirectory = builder.workingDirectory != null ? builder.workingDirectory : PluginUtils.getAwsDirectory(LspConstants.LSP_SUBDIRECTORY);
        this.lspExecutablePrefix = builder.lspExecutablePrefix;
        this.platformOverride = builder.platformOverride;
        this.architectureOverride = builder.architectureOverride;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public LspInstallation getLspInstallation() {
        try {
            var platform = platformOverride != null ? platformOverride : PluginUtils.getPlatform();
            var architecture = architectureOverride != null ? architectureOverride : PluginUtils.getArchitecture();
            fetcher.fetch(platform, architecture, workingDirectory);

            var nodeExecutable = findFileWithPrefix(workingDirectory, LspConstants.NODE_EXECUTABLE_PREFIX);
            var lspJs = findFileWithPrefix(workingDirectory, lspExecutablePrefix);

            if (nodeExecutable == null || lspJs == null) {
                throw new RuntimeException("Could not find node executable or LSP file in the downloaded contents");
            }

            makeExecutable(nodeExecutable);

            return new LspInstallation(nodeExecutable, lspJs);
        } catch (Exception e) {
            PluginLogger.error("Unable to resolve local language server installation. LSP features will be unavailable.", e);
            throw new AmazonQPluginException(e);
        }
    }

    private static void makeExecutable(final Path filePath) throws IOException {
        if (!hasPosixFilePermissions(filePath)) {
            return;
        }
        var permissions = new HashSet<>(Arrays.asList(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_EXECUTE));
        Files.setPosixFilePermissions(filePath, permissions);
    }

    private static boolean hasPosixFilePermissions(final Path path) {
        return path.getFileSystem().supportedFileAttributeViews().contains("posix");
    }

    private static Path findFileWithPrefix(final Path directory, final String prefix) throws IOException {
        try (var paths = Files.walk(directory)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(prefix))
                    .findFirst()
                    .orElse(null);
        }
    }

    public static class Builder {
        private LspFetcher fetcher;
        private Path workingDirectory;
        private String lspExecutablePrefix;
        private PluginPlatform platformOverride;
        private PluginArchitecture architectureOverride;

        public final Builder withFetcher(final LspFetcher fetcher) {
            this.fetcher = fetcher;
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
