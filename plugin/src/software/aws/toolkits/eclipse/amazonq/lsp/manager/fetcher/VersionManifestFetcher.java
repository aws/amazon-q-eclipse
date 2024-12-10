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

import javax.net.ssl.SSLHandshakeException;

import org.eclipse.mylyn.commons.ui.dialogs.AbstractNotificationPopup;
import org.eclipse.swt.widgets.Display;

import com.fasterxml.jackson.databind.ObjectMapper;

import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.exception.LspError;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspConstants;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.model.Manifest;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.telemetry.LanguageServerTelemetryProvider;
import software.aws.toolkits.eclipse.amazonq.telemetry.metadata.ExceptionMetadata;
import software.aws.toolkits.eclipse.amazonq.util.Constants;
import software.aws.toolkits.eclipse.amazonq.util.HttpClientFactory;
import software.aws.toolkits.eclipse.amazonq.util.ObjectMapperFactory;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;
import software.aws.toolkits.eclipse.amazonq.util.ToolkitNotification;
import software.aws.toolkits.telemetry.TelemetryDefinitions.ManifestLocation;
import software.aws.toolkits.telemetry.TelemetryDefinitions.Result;

public final class VersionManifestFetcher {

    private static final int TIMEOUT_SECONDS = 30;
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.getInstance();

    private final String manifestUrl;
    private final HttpClient httpClient;
    private final Path manifestPath;

    public VersionManifestFetcher(final String manifestUrl) {
        this(manifestUrl, null);
    }

    public VersionManifestFetcher(final String manifestUrl, final Path manifestPath) {
        this.manifestUrl = manifestUrl;
        this.httpClient = HttpClientFactory.getInstance();
        this.manifestPath = manifestPath != null ? manifestPath : PluginUtils.getPluginDir(LspConstants.AMAZONQ_LSP_SUBDIRECTORY).resolve("manifest.json");
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
        var cachedEtag = Activator.getPluginStore().get(manifestUrl);
        // only use cached content if it is valid and a cached etag exists
        var etagToRequest = cachedManifest.isPresent() && cachedEtag != null ? cachedEtag : null;

        // fetch contents from remote if new version exists
        // if that fails, use cache as fallback
        try {
            var latestResponse = getResourceFromRemote(etagToRequest);
            // If not modified is returned cached content is latest
            if (latestResponse.statusCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
                Activator.getLogger().info("Version manifest contains latest content");
                emitGetManifest(cachedManifest.orElse(null), ManifestLocation.CACHE, null);
                return cachedManifest;
            }
            // validate latest manifest fetched from remote location and cache it
            var latestManifest = validateAndCacheLatest(latestResponse);
            emitGetManifest(latestManifest.orElse(null), ManifestLocation.REMOTE, null);
            return latestManifest;
        } catch (Exception e) {
            if (e.getCause() instanceof SSLHandshakeException) {
                Display.getCurrent().asyncExec(() -> {
                    AbstractNotificationPopup notification = new ToolkitNotification(Display.getCurrent(),
                            Constants.IDE_SSL_HANDSHAKE_TITLE,
                            Constants.IDE_SSL_HANDSHAKE_BODY);
                    notification.open();
                });
            }
            Activator.getLogger().error("Error fetching manifest from remote location", e);
            emitGetManifest(null, ManifestLocation.UNKNOWN, ExceptionMetadata.scrubException(LspError.MANIFEST_FETCH_ERROR.toString(), e));
            return cachedManifest;
        }
    }

    private void emitGetManifest(final Manifest manifest, final ManifestLocation location, final String reason) {
        //failure has already been emitted if this condition returns true
        if (manifest == null && reason == null) {
            return;
        }

        //handle cases of success or failure with reason
        Result result = (reason == null) ? Result.SUCCEEDED : Result.FAILED;
        var args = new RecordLspSetupArgs();
        args.setReason(reason);
        args.setManifestLocation(location);
        if (manifest != null) {
            args.setManifestSchemaVersion(manifest.manifestSchemaVersion());
        }
        LanguageServerTelemetryProvider.emitSetupGetManifest(result, args);
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
                    emitGetManifest(null, ManifestLocation.CACHE, LspError.INVALID_VERSION_MANIFEST.toString());
                }
                return manifest;
            }
        } catch (Exception e) {
            Activator.getLogger().error("Error fetching resource from cache", e);
            emitGetManifest(null, ManifestLocation.CACHE, ExceptionMetadata.scrubException(LspError.UNEXPECTED_MANIFEST_CACHE_ERROR.toString(), e));
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
                Activator.getPluginStore().put(manifestUrl, etag.get());
            }
            Activator.getLogger().info("Validated and fetched latest manifest");
            return manifest;
        } catch (Exception e) {
            Activator.getLogger().error("Failed to cache manifest file", e);
            emitGetManifest(null, ManifestLocation.REMOTE, ExceptionMetadata.scrubException(LspError.MANIFEST_REMOTE_FETCH_ERROR.toString(), e));
        }
        return Optional.empty();
    }
}
