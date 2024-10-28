// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;
import software.aws.toolkits.eclipse.amazonq.extensions.ActivatorStaticMockExtension;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspConstants;

@ExtendWith(ActivatorStaticMockExtension.class)
public final class VersionManifestFetcherTest {
    private static final String INVALID_DATA = "{\r\n    \"schemaVersion\": \"0.1\",\r\n}";
    private String sampleManifestFile = "sample-manifest.json";
    private VersionManifestFetcher fetcher;

    @Test
    public void fetchWhenCacheEmptyAndNoUrl(@TempDir final Path tempDir) {
        var manifestPath = tempDir.resolve("manifest.json");
        fetcher = new VersionManifestFetcher(null, null, manifestPath);

        assertTrue(fetcher.fetch().isEmpty());
        assertFalse(cacheExists(manifestPath));
    }

    @Test
    public void fetchWhenCacheExistsAndNoUrl(@TempDir final Path tempDir) throws IOException, URISyntaxException {
        var manifestPath = tempDir.resolve("manifest.json");
        var resourcePath = getResourcePath(sampleManifestFile);
        copyFile(resourcePath.toAbsolutePath(), manifestPath);

        fetcher = new VersionManifestFetcher(null, null, manifestPath);

        var content = fetcher.fetch();
        assertTrue(content.isPresent());
        assertEquals(content.get().manifestSchemaVersion(), "0.1");
    }

    @Test
    public void fetchWhenCacheInvalidAndNoUrl(@TempDir final Path tempDir) throws IOException, URISyntaxException {
        var manifestPath = tempDir.resolve("manifest.json");
        Files.writeString(manifestPath, INVALID_DATA);

        fetcher = new VersionManifestFetcher(null, null, manifestPath);

        assertTrue(cacheExists(manifestPath));

        assertTrue(fetcher.fetch().isEmpty());
        // verify cache is deleted if validation of cached copy fails
        assertFalse(cacheExists(manifestPath));
    }

    @Test
    public void fetchWhenNoCacheAndFetchFromRemote(@TempDir final Path tempDir)
            throws IOException, URISyntaxException {
        var manifestPath = tempDir.resolve("manifest.json");

        assertFalse(cacheExists(manifestPath));

        fetcher = new VersionManifestFetcher(LspConstants.CW_MANIFEST_URL, null, manifestPath);
        var content = fetcher.fetch();
        assertTrue(content.isPresent());
        // verify cache and etag is updated
        assertTrue(cacheExists(manifestPath));
        assertFalse(PluginStore.get(LspConstants.CW_MANIFEST_URL).isEmpty());
    }

    @Test
    public void fetchWhenLocalCacheAndFetchFromRemote(@TempDir final Path tempDir)
            throws IOException, URISyntaxException {
        var manifestPath = tempDir.resolve("manifest.json");
        var resourcePath = getResourcePath(sampleManifestFile);
        copyFile(resourcePath.toAbsolutePath(), manifestPath);

        assertTrue(cacheExists(manifestPath));

        fetcher = new VersionManifestFetcher(LspConstants.CW_MANIFEST_URL, null, manifestPath);

        var content = fetcher.fetch();
        assertTrue(content.isPresent());

        // verify cache and etag updated
        assertTrue(cacheExists(manifestPath));
        assertFalse(PluginStore.get(LspConstants.CW_MANIFEST_URL).isEmpty());
    }

    private boolean cacheExists(final Path manifestPath) {
        return Files.exists(manifestPath);
    }

    private void copyFile(final Path sourcePath, final Path destinationPath) throws IOException {
        Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private Path getResourcePath(final String resourceName) throws URISyntaxException {
        var resourceUrl = getClass().getClassLoader().getResource(resourceName);
        if (resourceUrl == null) {
            throw new IllegalArgumentException("Resource not found: " + resourceName);
        }
        return Paths.get(resourceUrl.toURI());
    }
}
