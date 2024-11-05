// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import org.mockito.MockedStatic;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.util.PluginArchitecture;
import software.aws.toolkits.eclipse.amazonq.util.PluginPlatform;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;

public class FileSystemLspFetcherTest {

    @TempDir
    private Path tempDirSource;

    @TempDir
    private Path tempDirDest;

    @Test
    void testFetchSuccess() {
        try (MockedStatic<ArtifactUtils> mockedArtifactUtils = mockStatic(ArtifactUtils.class)) {
            FileSystemLspFetcher lspFetcher = FileSystemLspFetcher.builder().withSourceFile(tempDirSource).build();

            boolean result = assertDoesNotThrow(() -> lspFetcher.fetch(PluginPlatform.MAC, PluginArchitecture.ARM_64, tempDirDest));
            assertTrue(result);

            mockedArtifactUtils.verify(() -> ArtifactUtils.copyDirectory(tempDirSource, tempDirDest));
        }
    }
    @Test
    void testFetchSuccessWithZipFile() {
        Path zipFile = tempDirSource.resolve("srcFile.zip");
        try (MockedStatic<ArtifactUtils> mockedArtifactUtils = mockStatic(ArtifactUtils.class)) {
            FileSystemLspFetcher lspFetcher = FileSystemLspFetcher.builder().withSourceFile(zipFile).build();

            boolean result = assertDoesNotThrow(() -> lspFetcher.fetch(PluginPlatform.MAC, PluginArchitecture.ARM_64, tempDirDest));
            assertTrue(result);

            mockedArtifactUtils.verify(() -> ArtifactUtils.copyDirectory(zipFile, tempDirDest), never());
            mockedArtifactUtils.verify(() -> ArtifactUtils.extractFile(zipFile, tempDirDest));
        }
    }
    @Test
    void testFetchUnsupportedFileType() {
        Path unsupportedFile = tempDirSource.resolve("srcFile.txt");
        try (MockedStatic<ArtifactUtils> mockedArtifactUtils = mockStatic(ArtifactUtils.class)) {
            FileSystemLspFetcher lspFetcher = FileSystemLspFetcher.builder().withSourceFile(unsupportedFile).build();

            var exception = assertThrows(AmazonQPluginException.class, () -> {
                lspFetcher.fetch(PluginPlatform.MAC, PluginArchitecture.ARM_64, tempDirDest);
            });

            assertNotNull(exception);
            assertTrue(exception.getMessage().contains("Unsupported source file type: " + unsupportedFile));
            mockedArtifactUtils.verify(() -> ArtifactUtils.copyDirectory(unsupportedFile, tempDirDest), never());
            mockedArtifactUtils.verify(() -> ArtifactUtils.extractFile(unsupportedFile, tempDirDest), never());
        }
    }
    @Test
    void testFetchWithException() {
        try (MockedStatic<ArtifactUtils> mockedArtifactUtils = mockStatic(ArtifactUtils.class)) {
            mockedArtifactUtils.when(() -> ArtifactUtils.copyDirectory(tempDirSource, tempDirDest)).thenThrow(new IOException("test exception"));
            FileSystemLspFetcher lspFetcher = FileSystemLspFetcher.builder().withSourceFile(tempDirSource).build();

            var exception = assertThrows(AmazonQPluginException.class, () -> {
                lspFetcher.fetch(PluginPlatform.MAC, PluginArchitecture.ARM_64, tempDirDest);
            });

            assertNotNull(exception);
            assertTrue(exception.getMessage().contains("Could not copy local LSP contents"));
            mockedArtifactUtils.verify(() -> ArtifactUtils.copyDirectory(tempDirSource, tempDirDest));
            mockedArtifactUtils.verify(() -> ArtifactUtils.extractFile(tempDirSource, tempDirDest), never());
        }
    }

}
