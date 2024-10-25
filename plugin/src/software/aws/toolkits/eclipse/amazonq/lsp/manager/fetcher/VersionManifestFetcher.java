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
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspConstants;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.model.Manifest;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.HttpClientFactory;
import software.aws.toolkits.eclipse.amazonq.util.ObjectMapperFactory;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;

public final class VersionManifestFetcher {

    private static final int TIMEOUT_SECONDS = 10;
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.getInstance();

    private final String manifestUrl;
    private final HttpClient httpClient;
    private final Path manifestPath;

    public VersionManifestFetcher(final String manifestUrl, final HttpClient httpClient) {
        this(manifestUrl, httpClient, null);
    }

    public VersionManifestFetcher(final String manifestUrl, final HttpClient httpClient, final Path manifestPath) {
        this.manifestUrl = manifestUrl;
        this.httpClient = httpClient != null ? httpClient : HttpClientFactory.getInstance();
        this.manifestPath = manifestPath != null ? manifestPath : PluginUtils.getPluginDir(LspConstants.LSP_SUBDIRECTORY).resolve("manifest.json");
    }

    /*
     *  Fetch manifest such that if cache has the latest contents(by comparing e-tags with online version)
     *  and is valid use that, else fetch from remote
     */
    public Optional<Manifest> fetch() {
        var cachedManifest = getResourceFromCache();
        // if a remote location does not exist, default to content in the download cache
        if (manifestUrl == null) {
            return cachedManifest;
        }
        var cachedEtag = PluginStore.get(manifestUrl);
        // only use cached content if it is valid and a cached etag exists
        var etagToRequest = cachedManifest.isPresent() && cachedEtag != null ? cachedEtag : null;

        // fetch contents from remote if new version exists
        // if that fails, use cache as fallback
        try {
            var latestResponse = getResourceFromRemote(etagToRequest);
            // If not modified is returned cached content is latest
            if (latestResponse.statusCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
                Activator.getLogger().info("Version manifest contains latest content");
                return cachedManifest;
            }
            // validate latest manifest fetched from remote location and cache it
            return validateAndCacheLatest(latestResponse);
        } catch (Exception e) {
            Activator.getLogger().error("Error fetching manifest from remote location", e);
            return cachedManifest;
        }
    }

    /*
     * Fetch manifest from local cache
     */
    private Optional<Manifest> getResourceFromCache() {
        try {
            if (Files.exists(manifestPath)) {
                String cacheContent = Files.readString(manifestPath);
                // verify if cached version is parseable and valid, if it is corrupted delete the
                // cached fallback version
                var manifest = validateManifest(cacheContent);
                if (manifest.isEmpty()) {
                    ArtifactUtils.deleteFile(manifestPath);
                    Activator.getLogger().info("Failed to validate cached manifest file");
                }
                return manifest;
            }
        } catch (Exception e) {
            Activator.getLogger().error("Error fetching resource from cache", e);
        }
        return Optional.empty();
    }

    /*
     * Verifies if provided content represents a valid lsp version manifest
     */
    private Optional<Manifest> validateManifest(final String content) {
        try {
            var manifest = OBJECT_MAPPER.readValue(content, Manifest.class);
            var version = ArtifactUtils.parseVersion(manifest.manifestSchemaVersion());
            if (version.getMajor() == LspConstants.MANIFEST_MAJOR_VERSION) {
                return Optional.of(manifest);
            }
        } catch (Exception e) {
            // swallow error
        }
        return Optional.empty();
    }

    /*
     * Requests for new content but additionally uses the given E-Tag. If no E-Tag
     * is given, it behaves as a normal request.
     * @returns response of the request
     */
    private HttpResponse<String> getResourceFromRemote(final String etag) throws IOException, InterruptedException {
        var requestBuilder = HttpRequest.newBuilder().uri(URI.create(manifestUrl))
                .timeout(java.time.Duration.ofSeconds(TIMEOUT_SECONDS));

        Optional.ofNullable(etag).ifPresent(tag -> requestBuilder.header("If-None-Match", tag));
        // TODO: Add retry to the web request
        var response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != HttpURLConnection.HTTP_OK && response.statusCode() != HttpURLConnection.HTTP_NOT_MODIFIED) {
            throw new AmazonQPluginException("Unexpected response code when fetching manifest: " + response.statusCode());
        }
        return response;
    }

    /*
     * Validates the latest contents retrieved from remote location and if valid,
     * updates cache(content + etag)
     */
    private Optional<Manifest> validateAndCacheLatest(final HttpResponse<String> response) {
        try {
            var manifest = validateManifest(response.body());
            if (!manifest.isPresent()) {
                throw new AmazonQPluginException("Failed to validate manifest fetched from remote");
            }
            Files.write(manifestPath, response.body().getBytes(), StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            var etag = response.headers().firstValue("ETag");
            if (etag.isPresent()) {
                PluginStore.put(manifestUrl, etag.get());
            }
            Activator.getLogger().info("Validated and fetched latest manifest");
            return manifest;
        } catch (Exception e) {
            Activator.getLogger().error("Failed to cache manifest file", e);
        }
        return Optional.empty();
    }
}
