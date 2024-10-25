// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;

import org.osgi.framework.VersionRange;

import com.fasterxml.jackson.databind.ObjectMapper;

import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspConstants;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.model.ArtifactVersion;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.model.Manifest;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.model.Target;
import software.aws.toolkits.eclipse.amazonq.util.HttpClientFactory;
import software.aws.toolkits.eclipse.amazonq.util.ObjectMapperFactory;
import software.aws.toolkits.eclipse.amazonq.util.PluginArchitecture;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.PluginPlatform;

public final class RemoteManifestLspFetcher implements LspFetcher {

    private static final int TIMEOUT_SECONDS = 10;
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.getInstance();

    private final String manifestUrl;
    private final VersionRange versionRange;
    private final boolean integrityChecking;
    private final HttpClient httpClient;

    private RemoteManifestLspFetcher(final Builder builder) {
        this.manifestUrl = builder.manifestUrl;
        this.versionRange = builder.versionRange != null ? builder.versionRange : LspConstants.LSP_SUPPORTED_VERSION_RANGE;
        this.integrityChecking = builder.integrityChecking != null ? builder.integrityChecking : true;
        this.httpClient = builder.httpClient != null ? builder.httpClient : HttpClientFactory.getInstance();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean fetch(final PluginPlatform platform, final PluginArchitecture architecture, final Path destination) {
        Manifest manifestFile = null;
        try {
            var manifestFetcher = new VersionManifestFetcher(manifestUrl, httpClient);
            manifestFile = manifestFetcher.fetch()
                    .orElseThrow(() -> new AmazonQPluginException("Failed to retrieve language server manifest"));
        } catch (Exception e) {
            throw new AmazonQPluginException("Failed to retrieve language server manifest", e);
        }
        var version = resolveVersion(manifestFile, platform, architecture);
        var target = resolveTarget(version, platform, architecture);

        if (target.isPresent()) {
            var contents = target.get().contents();
            for (var content : contents) {
                try {
                    downloadFile(content.url(), destination, content.filename(), content.hashes());
                } catch (Exception e) {
                    throw new AmazonQPluginException("Failed to download file " + content.filename() + " from URL " + content.url(), e);
                }
            }
            return true;
        } else {
            throw new AmazonQPluginException("No language server found for platform " + platform + " and architecture " + architecture);
        }
    }

    private Optional<ArtifactVersion> resolveVersion(final Manifest manifestFile, final PluginPlatform platform, final PluginArchitecture architecture) {
        return manifestFile.versions().stream()
                .filter(version -> !version.isDelisted())
                .filter(version -> version.targets().stream()
                        .anyMatch(target -> target.platform().equalsIgnoreCase(platform.getValue()) && target.arch().equalsIgnoreCase(architecture.getValue())))
                .filter(version -> versionRange.includes(ArtifactUtils.parseVersion(version.serverVersion())))
                .findFirst();
    }

    private Optional<Target> resolveTarget(final Optional<ArtifactVersion> targetVersion, final PluginPlatform platform,
            final PluginArchitecture architecture) {
        return targetVersion.flatMap(version -> version.targets().stream()
                .filter(target -> target.platform().equalsIgnoreCase(platform.getValue()) && target.arch().equalsIgnoreCase(architecture.getValue()))
                .findFirst());
    }

    private void downloadFile(final String fileUrl, final Path destination, final String filename, final List<String> expectedHashes)
            throws IOException, InterruptedException, NoSuchAlgorithmException {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(fileUrl))
                .timeout(java.time.Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();

        var destinationFile = destination.resolve(filename);
        if (Files.exists(destinationFile)) {
            // If the file exists locally, check if the hashes match
            if (this.integrityChecking && ArtifactUtils.validateHash(destinationFile, expectedHashes, false)) {
                Activator.getLogger().info(filename + " already exists and matches the expected checksum. Skipping download.");
                return;
            } else {
                Activator.getLogger().info(filename + " already exists but doesn't match the expected checksum. Redownloading file.");
            }
        }

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(destinationFile));

        if (response.statusCode() == HttpURLConnection.HTTP_OK) {
            Activator.getLogger().info("Downloaded " + filename + " to " + destination);

            if (this.integrityChecking) {
                ArtifactUtils.validateHash(destinationFile, expectedHashes, true);
            }

            if (filename.endsWith(".zip")) {
                Activator.getLogger().info("Extracting contents of " + filename);
                ArtifactUtils.extractFile(response.body(), destination);
            }
        } else {
            throw new AmazonQPluginException("Failed to download remote LSP artifact. Response code: " + response.statusCode());
        }
    }

    public static class Builder {
        private String manifestUrl;
        private VersionRange versionRange;
        private Boolean integrityChecking;
        private HttpClient httpClient;

        public final Builder withManifestUrl(final String manifestUrl) {
            this.manifestUrl = manifestUrl;
            return this;
        }

        public final Builder withVersionRange(final VersionRange versionRange) {
            this.versionRange = versionRange;
            return this;
        }

        public final Builder withIntegrityChecking(final boolean integrityChecking) {
            this.integrityChecking = integrityChecking;
            return this;
        }

        public final Builder withHttpClient(final HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public final RemoteManifestLspFetcher build() {
            return new RemoteManifestLspFetcher(this);
        }
    }

}
