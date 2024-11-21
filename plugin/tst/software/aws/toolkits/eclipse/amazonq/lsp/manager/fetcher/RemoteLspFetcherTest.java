package software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.eclipse.osgi.service.resolver.VersionRange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.extensions.implementation.ActivatorStaticMockExtension;
import software.aws.toolkits.eclipse.amazonq.extensions.implementation.ArtifactUtilsStaticMockExtension;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspFetchResult;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.model.ArtifactVersion;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.model.Content;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.model.Manifest;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.model.Target;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.telemetry.LanguageServerTelemetryProvider;
import software.aws.toolkits.eclipse.amazonq.util.LoggingService;
import software.aws.toolkits.eclipse.amazonq.util.PluginArchitecture;
import software.aws.toolkits.eclipse.amazonq.util.PluginPlatform;
import software.aws.toolkits.telemetry.TelemetryDefinitions.LanguageServerLocation;
import software.aws.toolkits.telemetry.TelemetryDefinitions.Result;

public final class RemoteLspFetcherTest {
    private static VersionRange versionRange = new VersionRange("[1.0.0, 2.0.0]");
    private final String sampleVersion = String.format("%s.7.0", versionRange.getLeft().getMajor());
    private LspFetcher lspFetcher;
    private Manifest sampleManifest;
    private final ArtifactVersion sampleLspVersion;

    @RegisterExtension
    private static ActivatorStaticMockExtension activatorStaticMockExtension = new ActivatorStaticMockExtension();

    @RegisterExtension
    private static ArtifactUtilsStaticMockExtension artifactUtilsStaticMockExtension = new ArtifactUtilsStaticMockExtension();

    private RemoteLspFetcherTest() {
        sampleLspVersion = createLspVersion(sampleVersion);
        sampleManifest = createManifest(List.of(sampleLspVersion));
        lspFetcher = createFetcher();
    }

    private MockedStatic<Activator> staticMockedActivator;
    private MockedStatic<ArtifactUtils> staticMockedArtifactUtils;
    private MockedStatic<LanguageServerTelemetryProvider> mockTelemetryProvider;

    private LoggingService mockLogger;

    @Mock
    private HttpClient httpClient;

    @TempDir(cleanup = CleanupMode.ALWAYS)
    private Path tempDir;

    @BeforeEach
    void setupBeforeEach() {
        MockitoAnnotations.openMocks(this);
        staticMockedActivator = activatorStaticMockExtension.getStaticMock();
        mockLogger = activatorStaticMockExtension.getMock(LoggingService.class);
        staticMockedArtifactUtils = artifactUtilsStaticMockExtension.getStaticMock();
        mockTelemetryProvider = mockStatic(LanguageServerTelemetryProvider.class);
    }

    @AfterEach
    void tearDown() {
        mockTelemetryProvider.close();
    }

    @Test
    void fetchWhenManifestIsNull() {
        sampleManifest = null;
        lspFetcher = createFetcher();
        var exception = assertThrows(AmazonQPluginException.class, () -> {
            lspFetcher.fetch(PluginPlatform.MAC, PluginArchitecture.ARM_64, tempDir, Instant.now());
        });
        assertExceptionThrownWithMessage(exception, "No valid manifest");
        mockTelemetryProvider.verify(() -> LanguageServerTelemetryProvider.emitSetupGetServer(eq(Result.FAILED),
                argThat(arg -> arg.getLocation() == LanguageServerLocation.UNKNOWN
                        && arg.getReason().contains("No valid manifest"))));
    }

    @ParameterizedTest
    @MethodSource("incompatibleManifestVersions")
    void fetchWhenNoMatchingVersionFound(final String version) {
        var lspVersion = createLspVersion(version);
        sampleManifest = createManifest(List.of(lspVersion));
        lspFetcher = createFetcher();

        var exception = assertThrows(AmazonQPluginException.class, () -> {
            lspFetcher.fetch(PluginPlatform.MAC, PluginArchitecture.ARM_64, tempDir, Instant.now());
        });
        assertExceptionThrownWithMessage(exception, "language server that satisfies one or more of these conditions");
        mockTelemetryProvider.verify(() -> LanguageServerTelemetryProvider.emitSetupGetServer(eq(Result.FAILED),
                argThat(arg -> arg.getLocation() == LanguageServerLocation.UNKNOWN && arg.getReason()
                        .contains("language server that satisfies one or more of these conditions"))));
    }

    @Test
    void fetchWhenNoMatchingPlatform() {
        var exception = assertThrows(AmazonQPluginException.class, () -> {
            lspFetcher.fetch(PluginPlatform.WINDOWS, PluginArchitecture.ARM_64, tempDir, Instant.now());
        });

        assertExceptionThrownWithMessage(exception, "language server that satisfies one or more of these conditions");
        mockTelemetryProvider.verify(() -> LanguageServerTelemetryProvider.emitSetupGetServer(eq(Result.FAILED),
                argThat(arg -> arg.getLocation() == LanguageServerLocation.UNKNOWN && arg.getReason()
                        .contains("language server that satisfies one or more of these conditions"))));
    }

    @Test
    void fetchWhenNoMatchingArchitecture() {
        var exception = assertThrows(AmazonQPluginException.class, () -> {
            lspFetcher.fetch(PluginPlatform.MAC, PluginArchitecture.X86_64, tempDir, Instant.now());
        });

        assertExceptionThrownWithMessage(exception, "language server that satisfies one or more of these conditions");
        mockTelemetryProvider.verify(() -> LanguageServerTelemetryProvider.emitSetupGetServer(eq(Result.FAILED),
                argThat(arg -> arg.getLocation() == LanguageServerLocation.UNKNOWN && arg.getReason()
                        .contains("language server that satisfies one or more of these conditions"))));
    }

    @Test
    void fetchWhenTargetContentEmpty() {
        var exception = assertThrows(AmazonQPluginException.class, () -> {
            lspFetcher.fetch(PluginPlatform.MAC, PluginArchitecture.ARM_64, tempDir, Instant.now());
        });

        assertExceptionThrownWithMessage(exception, "language server that satisfies one or more of these conditions");
        mockTelemetryProvider.verify(() -> LanguageServerTelemetryProvider.emitSetupGetServer(eq(Result.FAILED),
                argThat(arg -> arg.getLocation() == LanguageServerLocation.UNKNOWN && arg.getReason()
                        .contains("language server that satisfies one or more of these conditions"))));
    }

    @Test
    void fetchWhenCacheExists() throws IOException {
        var zipPath = Paths.get(tempDir.toString(), sampleVersion, "servers.zip");
        var unzippedPath = Paths.get(tempDir.toString(), sampleVersion, "servers");
        setupZipTargetContent(zipPath, sampleLspVersion);

        var result = lspFetcher.fetch(PluginPlatform.MAC, PluginArchitecture.ARM_64, tempDir, Instant.now());

        assertInstallResult(result, LanguageServerLocation.CACHE, sampleVersion);
        assertTrue(zipContentsMatchUnzipped(zipPath, unzippedPath));
        mockTelemetryProvider.verify(() -> LanguageServerTelemetryProvider.emitSetupGetServer(eq(Result.SUCCEEDED),
                argThat(arg -> arg.getLocation() == LanguageServerLocation.CACHE && arg.getReason() == null)));
    }

    @Test
    void fetchWhenCacheExistsForZipWithMissingFile() throws IOException {
        var zipPath = Paths.get(tempDir.toString(), sampleVersion, "servers.zip");
        var unzippedPath = Paths.get(tempDir.toString(), sampleVersion, "servers");
        setupZipTargetContent(zipPath, sampleLspVersion);

        var fileToBeDeleted = Files.list(unzippedPath).findFirst().get();
        ArtifactUtils.deleteFile(fileToBeDeleted);

        var result = lspFetcher.fetch(PluginPlatform.MAC, PluginArchitecture.ARM_64, tempDir, Instant.now());

        assertInstallResult(result, LanguageServerLocation.CACHE, sampleVersion);
        assertTrue(zipContentsMatchUnzipped(zipPath, unzippedPath));
        mockTelemetryProvider.verify(() -> LanguageServerTelemetryProvider.emitSetupGetServer(eq(Result.SUCCEEDED),
                argThat(arg -> arg.getLocation() == LanguageServerLocation.CACHE && arg.getReason() == null)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"sha384:1234", "md5:678", "abc", ""})
    void fetchWhenHashesDoNotMatch(final String hash) throws IOException, InterruptedException {
        setupFileTargetContent("foo.txt", sampleLspVersion, hash);

        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<Path>>any()))
                .thenThrow(new IOException("Simulated network error"));

        var exception = assertThrows(AmazonQPluginException.class, () -> {
            lspFetcher.fetch(PluginPlatform.MAC, PluginArchitecture.ARM_64, tempDir, Instant.now());
        });

        assertExceptionThrownWithMessage(exception, " find a compatible version");
        mockTelemetryProvider.verify(() -> LanguageServerTelemetryProvider.emitSetupGetServer(eq(Result.FAILED),
                argThat(arg -> arg.getLocation() == LanguageServerLocation.UNKNOWN
                        && arg.getReason().contains("find a compatible version"))));
    }

    @Test
    void fetchWhenFromRemote() throws IOException, InterruptedException {
        var zipPath = Paths.get(tempDir.toString(), "remote", "servers.zip");
        var unzippedPath = Paths.get(tempDir.toString(), "remote", "servers");
        setupZipTargetContent(zipPath, sampleLspVersion);

        var mockResponse = createMockHttpResponse(zipPath, HttpURLConnection.HTTP_OK);
        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<Path>>any()))
                .thenReturn(mockResponse);

        lspFetcher = createFetcher();

        var result = lspFetcher.fetch(PluginPlatform.MAC, PluginArchitecture.ARM_64, tempDir, Instant.now());

        assertInstallResult(result, LanguageServerLocation.REMOTE, sampleVersion);
        assertTrue(zipContentsMatchUnzipped(zipPath, unzippedPath));
    }

    @Test
    void fetchFromFallbackWhenRemoteReturnsHttpErrorAndNoFAllBackVersionFound()
            throws IOException, InterruptedException {
        var zipPath = Paths.get(tempDir.toString(), "remote", "servers.zip");
        setupZipTargetContent(zipPath, sampleLspVersion);

        var mockResponse = createMockHttpResponse(zipPath, HttpURLConnection.HTTP_BAD_REQUEST);
        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<Path>>any()))
                .thenReturn(mockResponse);

        lspFetcher = createFetcher();

        var exception = assertThrows(AmazonQPluginException.class, () -> {
            lspFetcher.fetch(PluginPlatform.MAC, PluginArchitecture.ARM_64, tempDir, Instant.now());
        });

        staticMockedArtifactUtils.verify(() -> ArtifactUtils.deleteDirectory(any(Path.class)), times(2));
        staticMockedArtifactUtils.verify(() -> ArtifactUtils.parseVersion(any(String.class)), times(4));

        assertExceptionThrownWithMessage(exception, "Unable to find a compatible version");
    }

    @Test
    void fetchFromFallbackWhenRemoteReturnsHttpError() throws IOException, InterruptedException {
        var remoteZipPath = Paths.get(tempDir.toString(), sampleVersion, "servers.zip");
        setupZipTargetContent(remoteZipPath, sampleLspVersion);
        ArtifactUtils.deleteFile(remoteZipPath);

        String testFallbackVersion = String.format("%s.2.5", versionRange.getLeft().getMajor());
        var zipPath = Paths.get(tempDir.toString(), testFallbackVersion, "servers.zip");
        var unzippedPath = Paths.get(tempDir.toString(), testFallbackVersion, "servers");
        ArtifactVersion testFallbackSampleLspVersion = createLspVersion(testFallbackVersion);

        setupZipTargetContent(zipPath, testFallbackSampleLspVersion);

        sampleManifest = createManifest(List.of(sampleLspVersion, testFallbackSampleLspVersion));

        var mockResponse = createMockHttpResponse(remoteZipPath, HttpURLConnection.HTTP_BAD_REQUEST);
        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<Path>>any()))
                .thenReturn(mockResponse);

        lspFetcher = createFetcher();
        var result = lspFetcher.fetch(PluginPlatform.MAC, PluginArchitecture.ARM_64, tempDir, Instant.now());

        assertInstallResult(result, LanguageServerLocation.FALLBACK, testFallbackVersion);
        assertTrue(zipContentsMatchUnzipped(zipPath, unzippedPath));
        mockTelemetryProvider.verify(() -> LanguageServerTelemetryProvider.emitSetupGetServer(eq(Result.SUCCEEDED),
                argThat(arg -> arg.getLocation() == LanguageServerLocation.FALLBACK && arg.getReason() == null)));
    }

    @Test
    void fetchWhenMultipleVersionsChooseLatest() throws IOException, InterruptedException {
        var oneAdditionalVersion = String.format("%s.8.3", versionRange.getLeft().getMajor());
        var oneAdditionalLspVersion = createLspVersion(oneAdditionalVersion);
        sampleManifest = createManifest(List.of(sampleLspVersion, oneAdditionalLspVersion));

        var zipPath = Paths.get(tempDir.toString(), "remote", "servers.zip");
        var unzippedPath = Paths.get(tempDir.toString(), "remote", "servers");

        setupZipTargetContent(zipPath, oneAdditionalLspVersion);

        var mockResponse = createMockHttpResponse(zipPath, HttpURLConnection.HTTP_OK);
        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<Path>>any()))
                .thenReturn(mockResponse);

        lspFetcher = createFetcher();
        var result = lspFetcher.fetch(PluginPlatform.MAC, PluginArchitecture.ARM_64, tempDir, Instant.now());

        var expectedAssetDirectory = Paths.get(tempDir.toString(), oneAdditionalVersion);
        assertEquals(expectedAssetDirectory.toString(), result.assetDirectory());
        assertEquals(LanguageServerLocation.REMOTE, result.location());
        assertEquals(oneAdditionalVersion, result.version());

        assertTrue(zipContentsMatchUnzipped(zipPath, unzippedPath));
        mockTelemetryProvider.verify(() -> LanguageServerTelemetryProvider.emitSetupGetServer(eq(Result.SUCCEEDED),
                argThat(arg -> arg.getLocation() == LanguageServerLocation.REMOTE && arg.getReason() == null
                        && arg.getLanguageServerVersion() == oneAdditionalVersion)));
    }

    private HttpResponse<Path> createMockHttpResponse(final Path file, final int statusCode) {
        @SuppressWarnings("unchecked")
        HttpResponse<Path> response = mock(HttpResponse.class);

        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(file);
        when(response.headers()).thenReturn(HttpHeaders.of(new HashMap<>(), (x, y) -> true));
        when(response.previousResponse()).thenReturn(Optional.empty());
        when(response.sslSession()).thenReturn(Optional.empty());
        when(response.version()).thenReturn(HttpClient.Version.HTTP_1_1);
        when(response.request()).thenReturn(null);
        when(response.uri()).thenReturn(null);

        return response;
    }

    private void assertInstallResult(final LspFetchResult result, final LanguageServerLocation expectedLocation,
            final String testVersion) {
        var expectedAssetDirectory = Paths.get(tempDir.toString(), testVersion);
        assertEquals(expectedAssetDirectory.toString(), result.assetDirectory());
        assertEquals(expectedLocation, result.location());
    }

    private void setupFileTargetContent(final String filename, final ArtifactVersion lspVersion, final String hash)
            throws IOException, FileNotFoundException {
        var sampleContentPath = Paths.get(tempDir.toString(), lspVersion.serverVersion(), filename);

        setupFile(sampleContentPath);

        var contentHash = hash == null ? ArtifactUtils.calculateHash(sampleContentPath) : hash;
        var content = new Content(filename, "https://example.com", List.of("sha384:" + contentHash), 0);
        lspVersion.targets().get(0).contents().add(content);
    }

    private void setupZipTargetContent(final Path zipPath, final ArtifactVersion lspVersion)
            throws IOException, FileNotFoundException {
        var unzippedPath = zipPath.getParent().resolve(ArtifactUtils.getFilenameWithoutExtension(zipPath));

        createTestFiles(unzippedPath.toString());
        createZipFile(unzippedPath, zipPath);

        var contentHash = ArtifactUtils.calculateHash(zipPath);
        var content = new Content(zipPath.getFileName().toString(), "https://example.com",
                List.of("sha384:" + contentHash), 0);
        lspVersion.targets().get(0).contents().add(content);
    }

    private boolean zipContentsMatchUnzipped(final Path zipPath, final Path unzippedFolder) {
        if (!Files.exists(zipPath) || !Files.isDirectory(unzippedFolder)) {
            return false;
        }

        try {
            var zipFiles = getFilesInZip(zipPath);
            var unzippedFiles = Files.list(unzippedFolder).map(Path::getFileName).map(Path::toString)
                    .collect(Collectors.toList());

            return zipFiles.stream().allMatch(unzippedFiles::contains);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private List<String> getFilesInZip(final Path zipPath) throws IOException {
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            return zip.stream().map(entry -> Paths.get(entry.getName()).getFileName().toString())
                    .collect(Collectors.toList());
        }
    }

    private void createTestFiles(final String directory) throws IOException {
        for (int i = 1; i <= 3; i++) {
            setupFile(Paths.get(directory, String.valueOf(i)));
        }
    }

    private void setupFile(final Path filePath) throws IOException {
        Files.createDirectories(filePath.getParent());

        if (Files.notExists(filePath)) {
            Files.writeString(filePath, "hello");
        }
    }

    private void createZipFile(final Path folderToZip, final Path zipFilePath) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFilePath))) {
            Files.walkFileTree(folderToZip, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    ZipEntry zipEntry = new ZipEntry(folderToZip.relativize(file).toString());
                    zos.putNextEntry(zipEntry);
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private static Stream<Arguments> incompatibleManifestVersions() {
        return Stream.of(Arguments.of(String.format("%s.0.2", versionRange.getLeft().getMajor() - 1)),
                Arguments.of(String.format("%s.0.2", versionRange.getRight().getMajor() + 1)),
                Arguments.of(String.format("%s.0.2", versionRange.getRight().getMajor() + 2)));
    }

    private void assertExceptionThrownWithMessage(final Exception exception, final String expectedMessage) {
        assertTrue(exception instanceof AmazonQPluginException);
        assertTrue(exception.getMessage().contains(expectedMessage));
    }

    private ArtifactVersion createLspVersion(final String version) {
        var content = new ArrayList<Content>();
        var target = new Target(PluginPlatform.MAC.getValue(), PluginArchitecture.ARM_64.getValue(), content);
        var targets = List.of(target);
        return new ArtifactVersion(version, false, null, null, null, null, targets);
    }

    private Manifest createManifest(final List<ArtifactVersion> lspVersions) {
        return new Manifest(null, null, null, false, lspVersions);
    }

    private LspFetcher createFetcher() {
        return new RemoteLspFetcher.Builder().withManifest(sampleManifest).withVersionRange(versionRange)
                .withHttpClient(httpClient).build();
    }
}
