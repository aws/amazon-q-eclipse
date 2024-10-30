// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.util.PluginArchitecture;
import software.aws.toolkits.eclipse.amazonq.util.PluginPlatform;

public class FileSystemLspFetcher {

    private final Path sourceFile;

    public FileSystemLspFetcher(final Builder builder) {
        this.sourceFile = builder.sourceFile;
    }

    public static Builder builder() {
        return new Builder();
    }

    public final boolean fetch(final PluginPlatform platform, final PluginArchitecture architecture, final Path destination) {
        try {
            if (Files.isDirectory(sourceFile)) {
                ArtifactUtils.copyDirectory(sourceFile, destination);
            } else if (sourceFile.toString().endsWith(".zip")) {
                ArtifactUtils.extractFile(sourceFile, destination);
            } else {
                throw new AmazonQPluginException("Unsupported source file type: " + sourceFile);
            }
        } catch (IOException e) {
            throw new AmazonQPluginException("Could not copy local LSP contents", e);
        }
        return true;
    }

    public static class Builder {
        private Path sourceFile;

        public final Builder withSourceFile(final Path sourceFile) {
            this.sourceFile = sourceFile;
            return this;
        }

        public final FileSystemLspFetcher build() {
            return new FileSystemLspFetcher(this);
        }
    }
}
