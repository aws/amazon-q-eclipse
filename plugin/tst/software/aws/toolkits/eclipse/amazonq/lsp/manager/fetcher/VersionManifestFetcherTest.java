// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.eclipse.core.internal.preferences.EclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import software.aws.toolkits.eclipse.amazonq.configuration.DefaultPluginStore;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspConstants;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.LoggingService;

public final class VersionManifestFetcherTest {
    private static final String INVALID_DATA = "{\r\n    \"schemaVersion\": \"0.1\",\r\n}";
    private String sampleManifestFile = "sample-manifest.json";
    private VersionManifestFetcher fetcher;
    private MockedStatic<Activator> mockedActivator;
    private LoggingService mockedLogger;
    private IEclipsePreferences testPreferences;

    @BeforeEach
    @SuppressWarnings("restriction")
    void setUp() {
        mockedLogger = mock(LoggingService.class);
        testPreferences = new EclipsePreferences();
        mockedActivator = mockStatic(Activator.class);
        mockedActivator.when(Activator::getLogger).thenReturn(mockedLogger);
        mockedActivator.when(Activator::getPluginStore).thenReturn(new DefaultPluginStore(testPreferences));
    }

    @AfterEach
    void tearDown() throws Exception {
        mockedActivator.close();
        testPreferences.clear();
    }

    @Test
    public void fetchWhenCacheEmptyAndNoUrl(@TempDir final Path tempDir) {
        var manifestPath = tempDir.resolve("manifest.json");
        fetcher = new VersionManifestFetcher(null, manifestPath);

        assertTrue(fetcher.fetch().isEmpty());
        assertFalse(cacheExists(manifestPath));
    }

    @Test
    public void fetchWhenCacheExistsAndNoUrl(@TempDir final Path tempDir) throws IOException, URISyntaxException {
        var manifestPath = tempDir.resolve("manifest.json");
        var resourcePath = getResourcePath(sampleManifestFile);
        copyFile(resourcePath.toAbsolutePath(), manifestPath);

        fetcher = new VersionManifestFetcher(null, manifestPath);

        var content = fetcher.fetch();
        assertTrue(content.isPresent());
        assertEquals(content.get().manifestSchemaVersion(), "0.1");
    }

    @Test
    public void fetchWhenCacheInvalidAndNoUrl(@TempDir final Path tempDir) throws IOException, URISyntaxException {
        var manifestPath = tempDir.resolve("manifest.json");
        Files.writeString(manifestPath, INVALID_DATA);

        fetcher = new VersionManifestFetcher(null, manifestPath);

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

        fetcher = new VersionManifestFetcher(LspConstants.CW_MANIFEST_URL, manifestPath);
        var content = fetcher.fetch();
        assertTrue(content.isPresent());
        // verify cache and etag is updated
        assertTrue(cacheExists(manifestPath));
        assertFalse(Activator.getPluginStore().get(LspConstants.CW_MANIFEST_URL).isEmpty());
    }

    @Test
    public void fetchWhenLocalCacheAndFetchFromRemote(@TempDir final Path tempDir)
            throws IOException, URISyntaxException {
        var manifestPath = tempDir.resolve("manifest.json");
        var resourcePath = getResourcePath(sampleManifestFile);
        copyFile(resourcePath.toAbsolutePath(), manifestPath);

        assertTrue(cacheExists(manifestPath));

        fetcher = new VersionManifestFetcher(LspConstants.CW_MANIFEST_URL, manifestPath);

        var content = fetcher.fetch();
        assertTrue(content.isPresent());

        // verify cache and etag updated
        assertTrue(cacheExists(manifestPath));
        assertFalse(Activator.getPluginStore().get(LspConstants.CW_MANIFEST_URL).isEmpty());
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
