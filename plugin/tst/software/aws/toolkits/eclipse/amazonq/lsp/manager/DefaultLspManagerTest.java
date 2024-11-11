// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.manager;

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.io.IOException;

import software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher.LspFetcher;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher.ArtifactUtils;
import software.aws.toolkits.eclipse.amazonq.util.LoggingService;
import software.aws.toolkits.eclipse.amazonq.util.PluginArchitecture;
import software.aws.toolkits.eclipse.amazonq.util.PluginPlatform;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DefaultLspManagerTest {

    @TempDir
    private Path tempDir;

    @Mock
    private LspFetcher mockFetcher;

    @Mock
    private LoggingService mockLogger;

    @BeforeEach
    public final void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /*
    @Test
    public void testGetLspInstallationWithException() throws IOException {
        try (MockedStatic<Activator> mockedActivator = mockStatic(Activator.class)) {
            mockedActivator.when(Activator::getLogger).thenReturn(mockLogger);
            assertThrows(AmazonQPluginException.class, () -> managerHelper().getLspInstallation());
            verify(mockLogger)
                .error(eq("Unable to resolve local language server installation. LSP features will be unavailable."), any(RuntimeException.class));
        }
    }

    @Test
    public void testGetLspInstallationPosixMachine() throws IOException {
        getLspInstallationHelper(true);
    }

    @Test
    public void testGetLspInstallationNonPosixMachine() throws IOException {
        getLspInstallationHelper(false);
    }
    */

    private void getLspInstallationHelper(final boolean isPosix) throws IOException {
        try (MockedStatic<ArtifactUtils> mockedArtifactUtils = mockStatic(ArtifactUtils.class)) {
            mockedArtifactUtils.when(() -> ArtifactUtils.hasPosixFilePermissions(any())).thenReturn(isPosix);
            String nodeFile = "nodeExecFileTest";
            String lspFile = "lspExecFileTest";
            Files.createFile(tempDir.resolve(nodeFile));
            Files.createFile(tempDir.resolve(lspFile));
            Path nodeFilePath = tempDir.resolve(nodeFile);

            assertFalse(verifyPosixPermissions(nodeFilePath));
            DefaultLspManager lspManager = managerHelper();
            var result = lspManager.getLspInstallation();

            assertTrue(result.getServerCommand().endsWith(nodeFile));
            assertTrue(result.getServerCommandArgs().endsWith(lspFile));
            assertEquals(isPosix, verifyPosixPermissions(nodeFilePath));
            verify(mockFetcher).fetch(any(PluginPlatform.class), any(PluginArchitecture.class), any(Path.class));
        }
    }


    private DefaultLspManager managerHelper() {
        return new DefaultLspManager.Builder()
        .withDirectory(tempDir)
        .withPlatformOverride(PluginPlatform.MAC)
        .withArchitectureOverride(PluginArchitecture.ARM_64)
        .withLspExecutablePrefix("lsp")
        .build();
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
