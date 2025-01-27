// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.extensions.implementation.ActivatorStaticMockExtension;
import software.aws.toolkits.eclipse.amazonq.extensions.implementation.ArtifactUtilsStaticMockExtension;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher.ArtifactUtils;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher.LspFetcher;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher.RemoteLspFetcher;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.model.Manifest;
import software.aws.toolkits.eclipse.amazonq.telemetry.LanguageServerTelemetryProvider;
import software.aws.toolkits.eclipse.amazonq.util.LoggingService;
import software.aws.toolkits.eclipse.amazonq.util.PluginArchitecture;
import software.aws.toolkits.eclipse.amazonq.util.PluginPlatform;
import software.aws.toolkits.telemetry.TelemetryDefinitions.LanguageServerLocation;
import software.aws.toolkits.telemetry.TelemetryDefinitions.Result;

public class DefaultLspManagerTest {

    @RegisterExtension
    private static ActivatorStaticMockExtension activatorStaticMockExtension = new ActivatorStaticMockExtension();

    @RegisterExtension
    private static ArtifactUtilsStaticMockExtension artifactUtilStaticMockExtension = new ArtifactUtilsStaticMockExtension();

    @TempDir
    private Path tempDir;

    @TempDir
    private Path serverDir;

    private static DefaultLspManager lspManager;
    private static LoggingService mockLogger;
    private static Manifest mockManifest;
    private static LspFetcher mocklspFetcher;
    private static MockedStatic<ArtifactUtils> mockedStaticArtifactUtils;
    private MockedStatic<LanguageServerTelemetryProvider> mockTelemetryProvider;

    @BeforeAll
    public static void setupBeforeAll() {
        mockedStaticArtifactUtils = artifactUtilStaticMockExtension.getStaticMock();
    }

    @BeforeEach
    public final void setupBeforeEach() {
        mockLogger = activatorStaticMockExtension.getMock(LoggingService.class);
        mockTelemetryProvider = mockStatic(LanguageServerTelemetryProvider.class);
        mockManifest = mock(Manifest.class);
        mocklspFetcher = mock(LspFetcher.class);
    }

    @AfterEach
    final void tearDown() {
        mockTelemetryProvider.close();
    }

    // test case on both mac and Windows
    // exe file should be given posix permissions on Mac
    // non-applicable on Windows
    @ParameterizedTest
    @MethodSource("overrideParamProvider")
    final void testGetLspInstallationWithOverride(final boolean isWindows, final String exeFile) throws IOException {
        if (isWindows) {
            initLspManager(PluginPlatform.WINDOWS, PluginArchitecture.X86_64);
        } else {
            initLspManager(PluginPlatform.MAC, PluginArchitecture.ARM_64);
        }
        Path serverCommand = serverDir.resolve(exeFile);
        Path lspArgsFile = serverDir.resolve("lspArgsFile");
        Files.createFile(lspArgsFile);
        Files.createFile(serverCommand);
        var expectedResult = setUpInstallResult(exeFile);
        doReturn(expectedResult).when(lspManager).getLocalLspOverride();

        // confirm file does not have posix permission upon creation
        verifyPosixPermissions(serverCommand, false);

        LspInstallResult result = lspManager.getLspInstallation();
        assertEquals(expectedResult, result);

        // verify override was called and accepted
        verify(lspManager).getLocalLspOverride();
        verify(mockLogger).info(String.format(
                "Launching Amazon Q language server from local override location: %s, with command: %s and args: %s",
                expectedResult.getServerDirectory(), expectedResult.getServerCommand(),
                expectedResult.getServerCommandArgs()));

        // verify proper posix permissions given platform
        mockedStaticArtifactUtils.verify(() -> ArtifactUtils.hasPosixFilePermissions(any(Path.class)));
        verifyPosixPermissions(serverCommand, true);

        // verify calling getLspInstallation again returns same instance
        LspInstallResult secondResult = lspManager.getLspInstallation();
        assertEquals(result, secondResult);

        // test telemetry properly emitting when override is successful
        mockTelemetryProvider.verify(() -> LanguageServerTelemetryProvider.emitSetupGetServer(eq(Result.SUCCEEDED),
                argThat(arg -> arg.getLocation() == LanguageServerLocation.OVERRIDE && arg.getReason() == null)));
    }

    @Test
    void testGetLspInstallationWithoutOverride() throws IOException {
        initLspManager(PluginPlatform.MAC, PluginArchitecture.ARM_64);

        // set up server directory
        Path lspServerSubDir = serverDir.resolve(LspConstants.LSP_SERVER_FOLDER);
        Files.createDirectories(lspServerSubDir);
        Path lspArgsFile = lspServerSubDir.resolve("lspArgsFile");
        Path serverCommand = lspServerSubDir.resolve("node");
        Files.createFile(lspArgsFile);
        Files.createFile(serverCommand);
        verifyPosixPermissions(serverCommand, false);
        setUpFetchingTools();

        LspInstallResult result = lspManager.getLspInstallation();

        // verify proper parameters returned
        assertEquals(LanguageServerLocation.OVERRIDE, result.getLocation());
        assertEquals("node", result.getServerCommand());
        assertEquals("lspArgsFile", result.getServerCommandArgs());
        assertEquals(Paths.get(serverDir.toString(), LspConstants.LSP_SERVER_FOLDER).toString(),
                result.getServerDirectory());

        // verify flow of method calls
        verify(lspManager).getLocalLspOverride();
        verify(lspManager).fetchManifest();
        verify(lspManager).createLspFetcher(mockManifest);
        verify(mocklspFetcher).fetch(any(), any(), eq(tempDir), any());
        verify(mockLogger, never()).info(any());
        mockedStaticArtifactUtils.verify(() -> ArtifactUtils.hasPosixFilePermissions(any(Path.class)));
        verifyPosixPermissions(serverCommand, true);

        // verify calling getLspInstallation again returns same instance
        LspInstallResult secondResult = lspManager.getLspInstallation();
        assertEquals(result, secondResult);
    }

    @Test
    void testGetLspInstallationWithManifestException() {
        initLspManager(PluginPlatform.MAC, PluginArchitecture.ARM_64);
        AmazonQPluginException testException = new AmazonQPluginException(
                "Failed to retrieve Amazon Q language server manifest");
        doThrow(testException).when(lspManager).fetchManifest();

        Exception exception = assertThrows(AmazonQPluginException.class, () -> lspManager.getLspInstallation());
        assertEquals(testException, exception.getCause());
        verify(mockLogger).error(
                eq("Unable to resolve local language server installation. LSP features will be unavailable."),
                any(Throwable.class));
    }

    @Test
    void testGetLspInstallationWithServerDirectoryException() {
        initLspManager(PluginPlatform.MAC, PluginArchitecture.ARM_64);
        setUpFetchingTools();

        Exception exception = assertThrows(AmazonQPluginException.class, () -> lspManager.getLspInstallation());
        verify(mockLogger).error(
                eq("Unable to resolve local language server installation. LSP features will be unavailable."),
                any(Throwable.class));
        assertEquals("Error finding Amazon Q Language Server Working Directory", exception.getCause().getMessage());
    }

    @Test
    void testGetLspInstallationWithExeFileException() throws IOException {
        initLspManager(PluginPlatform.MAC, PluginArchitecture.ARM_64);
        Path lspServerSubDir = serverDir.resolve(LspConstants.LSP_SERVER_FOLDER);
        Files.createDirectories(lspServerSubDir);
        Path unknownFile = lspServerSubDir.resolve("unknownFile");
        Files.createFile(unknownFile);
        setUpFetchingTools();

        Exception exception = assertThrows(AmazonQPluginException.class, () -> lspManager.getLspInstallation());
        verify(mockLogger).error(
                eq("Unable to resolve local language server installation. LSP features will be unavailable."),
                any(Throwable.class));
        assertEquals("Error finding Amazon Q Language Server Command", exception.getCause().getMessage());
    }

    @Test
    void testGetLspInstallationWithLspFileException() throws IOException {
        initLspManager(PluginPlatform.MAC, PluginArchitecture.ARM_64);
        Path lspServerSubDir = serverDir.resolve(LspConstants.LSP_SERVER_FOLDER);
        Files.createDirectories(lspServerSubDir);
        Path exeFile = lspServerSubDir.resolve("node");
        Files.createFile(exeFile);
        setUpFetchingTools();

        Exception exception = assertThrows(AmazonQPluginException.class, () -> lspManager.getLspInstallation());
        verify(mockLogger).error(
                eq("Unable to resolve local language server installation. LSP features will be unavailable."),
                any(Throwable.class));
        assertEquals("Error finding Amazon Q Language Server Command Args", exception.getCause().getMessage());
    }

    @Test
    void testCreateFetcher() {
        initLspManager(PluginPlatform.MAC, PluginArchitecture.ARM_64);
        LspFetcher result = lspManager.createLspFetcher(mockManifest);
        assertNotNull(result);
        assertTrue(result instanceof RemoteLspFetcher);
    }

    @Test
    void testLocalOverrideIsUsedWhenValid() {
        LspInstallResult localOverride = new LspInstallResult();
        localOverride.setServerDirectory("/test/path");
        localOverride.setServerCommand("node");
        localOverride.setServerCommandArgs("--test");

        initLspManager(PluginPlatform.MAC, PluginArchitecture.ARM_64);
        doReturn(localOverride).when(lspManager).getLocalLspOverride();
    }

    private LspFetchResult setUpFetchingTools() {
        doReturn(mockManifest).when(lspManager).fetchManifest();
        doReturn(mocklspFetcher).when(lspManager).createLspFetcher(mockManifest);
        var fetcherResult = setUpFetcherResult();
        when(mocklspFetcher.fetch(any(), any(), eq(tempDir), any())).thenReturn(fetcherResult);
        return fetcherResult;
    }

    private static Stream<Arguments> overrideParamProvider() {
        return Stream.of(Arguments.of(false, "node"), Arguments.of(true, "node.exe"));
    }

    private void initLspManager(final PluginPlatform platform, final PluginArchitecture architecture) {
        lspManager = spy(DefaultLspManager.builder().withDirectory(tempDir).withManifestUrl("testManifestUrl")
                .withLspExecutablePrefix("lspArgsFile").withPlatformOverride(platform)
                .withArchitectureOverride(architecture).build());
    }

    private LspInstallResult setUpInstallResult(final String serverCommand) throws IOException {
        LspInstallResult result = new LspInstallResult();
        result.setLocation(LanguageServerLocation.OVERRIDE);
        result.setServerDirectory(serverDir.toString());
        result.setServerCommand(serverCommand);
        result.setClientDirectory("clientDir");
        result.setServerCommandArgs("lspArgsFile");
        return result;
    }

    private LspFetchResult setUpFetcherResult() {
        return new LspFetchResult(serverDir.toString(), "version", LanguageServerLocation.OVERRIDE);
    }

    private static void verifyPosixPermissions(final Path filePath, final boolean expectedResult) throws IOException {
        // skip verifying posix permissions if test run in windows environment
        if (isWindowsEnvironment(filePath)) {
            return;
        }
        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(filePath);
        var actualResult = permissions.contains(PosixFilePermission.OWNER_EXECUTE)
                && permissions.contains(PosixFilePermission.OWNER_READ)
                && permissions.contains(PosixFilePermission.OWNER_WRITE)
                && permissions.contains(PosixFilePermission.GROUP_READ)
                && permissions.contains(PosixFilePermission.GROUP_EXECUTE)
                && permissions.contains(PosixFilePermission.OTHERS_EXECUTE)
                && permissions.contains(PosixFilePermission.OTHERS_READ);
        assertEquals(expectedResult, actualResult);
    }

    private static boolean isWindowsEnvironment(final Path filePath) {
        return !filePath.getFileSystem().supportedFileAttributeViews().contains("posix");
    }
}
