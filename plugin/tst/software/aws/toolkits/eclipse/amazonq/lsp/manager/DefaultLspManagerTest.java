// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.manager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.io.IOException;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.model.Manifest;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher.LspFetcher;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspFetchResult;

import software.aws.toolkits.eclipse.amazonq.extensions.implementation.ActivatorStaticMockExtension;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher.ArtifactUtils;
import software.aws.toolkits.eclipse.amazonq.lsp.model.LanguageServerLocation;
import software.aws.toolkits.eclipse.amazonq.util.LoggingService;
import software.aws.toolkits.eclipse.amazonq.util.PluginArchitecture;
import software.aws.toolkits.eclipse.amazonq.util.PluginPlatform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

public class DefaultLspManagerTest {

    @RegisterExtension
    private static ActivatorStaticMockExtension activatorStaticMockExtension = new ActivatorStaticMockExtension();

    @TempDir
    private Path tempDir;

    @TempDir
    private Path serverDir;

    private static DefaultLspManager lspManager;
    private static LoggingService mockLogger;

    @BeforeEach
    public final void setUp() {
        mockLogger = activatorStaticMockExtension.getMock(LoggingService.class);
    }

    @Test
    void testGetLspInstallationOverride() throws IOException {
        initLspManager(PluginPlatform.MAC, PluginArchitecture.ARM_64);
        Path serverCommand = serverDir.resolve("node");
        Path lspArgsFile = serverDir.resolve("lspArgsFile");
        Files.createFile(lspArgsFile);
        Files.createFile(serverCommand);
        var expectedResult = setUpInstallResult(serverCommand);
        doReturn(expectedResult).when(lspManager).getLocalLspOverride();

        //confirm file does not have posix permission upon creation
        assertFalse(verifyPosixPermissions(serverCommand));

        try (MockedStatic<ArtifactUtils> mockedArtifactUtils = mockStatic(ArtifactUtils.class)) {
            mockedArtifactUtils.when(() -> ArtifactUtils.hasPosixFilePermissions(any(Path.class))).thenReturn(true);

            LspInstallResult result = lspManager.getLspInstallation();
            assertEquals(expectedResult, result);

            //verify override was called and accepted
            verify(lspManager).getLocalLspOverride();
            verify(mockLogger).info(String.format("Launching Amazon Q language server from local override location: %s, with command: %s and args: %s",
                    expectedResult.getServerDirectory(), expectedResult.getServerCommand(), expectedResult.getServerCommandArgs()));

            //verify file was given posix permissions
            mockedArtifactUtils.verify(() -> ArtifactUtils.hasPosixFilePermissions(any(Path.class)));
            assertTrue(verifyPosixPermissions(serverCommand));

            //verify calling getLspInstallation again returns same instance
            LspInstallResult secondResult = lspManager.getLspInstallation();
            assertEquals(result, secondResult);
        }
    }

    @Test
    void testGetLspInstallationWithoutOverride() throws IOException {
        initLspManager(PluginPlatform.MAC, PluginArchitecture.ARM_64);
        Path lspServerSubDir = serverDir.resolve(LspConstants.LSP_SERVER_FOLDER);
        Files.createDirectories(lspServerSubDir);
        Path lspArgsFile = lspServerSubDir.resolve("lspArgsFile");
        Path serverCommand = lspServerSubDir.resolve("node");
        Files.createFile(lspArgsFile);
        Files.createFile(serverCommand);
        assertFalse(verifyPosixPermissions(serverCommand));
        Manifest mockManifest = mock(Manifest.class);
        LspFetcher lspFetcher = mock(LspFetcher.class);
        doReturn(mockManifest).when(lspManager).fetchManifest();
        doReturn(lspFetcher).when(lspManager).createLspFetcher(mockManifest);
        var fetcherResult = setUpFetcherResult();
        when(lspFetcher.fetch(any(), any(), eq(tempDir))).thenReturn(fetcherResult);

        try (MockedStatic<ArtifactUtils> mockedArtifactUtils = mockStatic(ArtifactUtils.class)) {
            mockedArtifactUtils.when(() -> ArtifactUtils.hasPosixFilePermissions(any(Path.class))).thenReturn(true);
            LspInstallResult result = lspManager.getLspInstallation();

            assertEquals(LanguageServerLocation.Override, result.getLocation());
            assertEquals("node", result.getServerCommand());
            assertEquals("lspArgsFile", result.getServerCommandArgs());
            assertEquals(Paths.get(serverDir.toString(), LspConstants.LSP_SERVER_FOLDER).toString(), result.getServerDirectory());

            //verify override was called and accepted
            verify(lspManager).getLocalLspOverride();
            verify(lspManager).fetchManifest();
            verify(lspManager).createLspFetcher(mockManifest);
            verify(lspFetcher).fetch(any(), any(), eq(tempDir));
            verify(mockLogger, never()).info(any());
            mockedArtifactUtils.verify(() -> ArtifactUtils.hasPosixFilePermissions(any(Path.class)));
            assertTrue(verifyPosixPermissions(serverCommand));
        }
    }
//    @Test
//    void testValidateLsp() throws IOException {
//        Path serverCommand = serverDir.resolve("node");
//        Path lspArgsFile = serverDir.resolve("lspArgsFile");
//        Files.createFile(lspArgsFile);
//        Files.createFile(serverCommand);
//        assertFalse(verifyPosixPermissions(serverCommand));
//        var expectedResult = setUpInstallResult(serverCommand);
//        assertEquals("Success", lspManager.validateLsp(expectedResult));
//    }
    private void initLspManager(final PluginPlatform platform, final PluginArchitecture architecture) {
        lspManager = spy(DefaultLspManager.builder()
                .withDirectory(tempDir)
                .withManifestUrl("testManifestUrl")
                .withLspExecutablePrefix("lspArgsFile")
                .withPlatformOverride(platform)
                .withArchitectureOverride(architecture)
                .build());
    }
    private LspInstallResult setUpInstallResult(Path serverCommand) throws IOException {
        LspInstallResult result = new LspInstallResult();
        result.setLocation(LanguageServerLocation.Override);
        result.setServerDirectory(serverDir.toString());
        result.setServerCommand("node");
        result.setClientDirectory("/clientDir/");
        result.setServerCommandArgs("lspArgsFile");
        return result;
    }
    private LspFetchResult setUpFetcherResult() {
            return new LspFetchResult(serverDir.toString(), "version", LanguageServerLocation.Override);
    }

    private static boolean verifyPosixPermissions(final Path filePath) throws IOException {
        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(filePath);
        return permissions.contains(PosixFilePermission.OWNER_EXECUTE)
        && permissions.contains(PosixFilePermission.OWNER_READ)
        && permissions.contains(PosixFilePermission.OWNER_WRITE)
        && permissions.contains(PosixFilePermission.GROUP_READ)
        && permissions.contains(PosixFilePermission.GROUP_EXECUTE)
        && permissions.contains(PosixFilePermission.OTHERS_EXECUTE)
        && permissions.contains(PosixFilePermission.OTHERS_READ);
    }
}
