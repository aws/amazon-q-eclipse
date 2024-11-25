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
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.exception.LspError;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspConstants;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspFetchResult;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.model.ArtifactVersion;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.model.Content;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.model.Manifest;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.model.Target;
import software.aws.toolkits.eclipse.amazonq.util.HttpClientFactory;
import software.aws.toolkits.eclipse.amazonq.util.PluginArchitecture;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.telemetry.LanguageServerTelemetryProvider;
import software.aws.toolkits.eclipse.amazonq.telemetry.metadata.ExceptionMetadata;
import software.aws.toolkits.eclipse.amazonq.util.PluginPlatform;
import software.aws.toolkits.telemetry.TelemetryDefinitions.LanguageServerLocation;
import software.aws.toolkits.telemetry.TelemetryDefinitions.Result;

public final class RemoteLspFetcher implements LspFetcher {

    private static final int TIMEOUT_SECONDS = 30;

    private final Manifest manifest;
    private final VersionRange versionRange;
    private final boolean integrityChecking;
    private final HttpClient httpClient;
    private RecordLspSetupArgs args = new RecordLspSetupArgs();

    private RemoteLspFetcher(final Builder builder) {
        this.manifest = builder.manifest;
        this.versionRange = builder.versionRange != null ? builder.versionRange : LspConstants.LSP_SUPPORTED_VERSION_RANGE;
        this.integrityChecking = builder.integrityChecking != null ? builder.integrityChecking : true;
        this.httpClient = builder.httpClient != null ? builder.httpClient : HttpClientFactory.getInstance();
    }

    public static Builder builder() {
        return new Builder();
    }

    private void emitGetServer(final Result result, final String serverVersion, final LanguageServerLocation location,
            final Instant start) {
        args.setDuration(Duration.between(start, Instant.now()).toMillis());
        args.setLocation(location);
        args.setLanguageServerVersion(serverVersion);
        if (manifest != null) {
            args.setManifestSchemaVersion(manifest.manifestSchemaVersion());
        }
        LanguageServerTelemetryProvider.emitSetupGetServer(result, args);
        args = new RecordLspSetupArgs();
    }
    private void setErrorReason(final String reason) {
        args.setReason(reason);
    }

    @Override
    public LspFetchResult fetch(final PluginPlatform platform, final PluginArchitecture architecture,
            final Path destination, final Instant start) {
        var artifactVersion = resolveVersion(manifest, platform, architecture, start);
        var target = resolveTarget(artifactVersion, platform, architecture);
        if (!target.isPresent()) {
            String failureReason = String.format(
                    "Unable to find a language server that satisfies one or more of these conditions:"
                    + " version in range [%s), matching system's architecture: %s and platform: %s",
                    versionRange.toString(), architecture, platform);
            setErrorReason(LspError.NO_COMPATIBLE_LSP.toString());
            emitGetServer(Result.FAILED, null, LanguageServerLocation.UNKNOWN, start);
            throw new AmazonQPluginException(failureReason);
        }

        var serverVersion = artifactVersion.get().serverVersion();
        var contents = target.get().contents();
        var downloadDirectory = Paths.get(destination.toString(), serverVersion.toString());

        // if the latest version is stored locally already and is valid, return that, else fetch the latest version
        if (hasValidCache(contents, downloadDirectory)) {
            logMessageWithLicense(String.format("Launching Amazon Q language server v%s from local cache %s",
                    serverVersion.toString(), downloadDirectory), artifactVersion.get().thirdPartyLicenses());
            emitGetServer(Result.SUCCEEDED, serverVersion, LanguageServerLocation.CACHE, start);
            return new LspFetchResult(downloadDirectory.toString(), serverVersion, LanguageServerLocation.CACHE);
        }

        // delete invalid local cache
        ArtifactUtils.deleteDirectory(downloadDirectory);

        // if all lsp target contents are successfully downloaded from remote location,
        // return the download location
        if (downloadFromRemote(contents, downloadDirectory)) {
            logMessageWithLicense(String.format("Installing Amazon Q language server v%s to %s",
                    serverVersion.toString(), downloadDirectory.toString()),
                    artifactVersion.get().thirdPartyLicenses());
            emitGetServer(Result.SUCCEEDED, serverVersion, LanguageServerLocation.REMOTE, start);
            return new LspFetchResult(downloadDirectory.toString(), serverVersion, LanguageServerLocation.REMOTE);
        }

        // if unable to retrieve / validate contents from remote location, cleanup
        // download cache
        ArtifactUtils.deleteDirectory(downloadDirectory);
        Activator.getLogger().info(String.format(
                "Unable to download Amazon Q language server version v%s. Attempting to fetch from fallback location",
                serverVersion));
        emitGetServer(Result.FAILED, serverVersion, LanguageServerLocation.REMOTE, start);

        // use the most compatible fallback cached lsp version
        var fallbackDir = getFallback(serverVersion, platform, architecture, destination);
        if (fallbackDir != null && !fallbackDir.toString().isEmpty()) {
            var fallbackVersion = fallbackDir.getFileName().toString();
            var fallBackLspVersion = manifest.versions().stream().filter(x -> x.serverVersion().equals(fallbackVersion))
                    .findFirst();

            logMessageWithLicense(String.format(
                    "Unable to install Amazon Q Language Server v%s. Launching a previous version from: %s",
                    serverVersion, fallbackDir.toString()), fallBackLspVersion.get().thirdPartyLicenses());
            emitGetServer(Result.SUCCEEDED, fallbackVersion, LanguageServerLocation.FALLBACK, start);
            return new LspFetchResult(fallbackDir.toString(), fallbackVersion, LanguageServerLocation.FALLBACK);
        }

        String failureReason = "Unable to find a compatible version of Amazon Q Language Server.";
        setErrorReason(LspError.NO_VALID_SERVER_FALLBACK.toString());
        emitGetServer(Result.FAILED, null, LanguageServerLocation.UNKNOWN, start);
        throw new AmazonQPluginException(failureReason);
    }

    public void cleanup(final Path destinationFolder) {
        if (manifest == null || manifest.versions().isEmpty()) {
            return;
        }
        deleteDelistedVersions(destinationFolder);
        deleteExtraVersions(destinationFolder);
    }

    private boolean hasValidCache(final List<Content> contents, final Path cacheDirectory) {
        boolean result = contents.stream().allMatch(content -> {
            Path filePath = Paths.get(cacheDirectory.toString(), content.filename());
            return Files.exists(filePath) && ArtifactUtils.validateHash(filePath, content.hashes(), false);
        });

        // Handle validation for zip files if hash matches
        return result && ensureUnzippedFoldersMatchZip(cacheDirectory, contents);
    }

    /*
     * For each zip file in contents, verify their unzipped folders have the same content files(by name)
     * If the check fails for any zip file, validation fails
     * Note: the actual content of a file is not validated
     */
    private boolean ensureUnzippedFoldersMatchZip(final Path cacheDirectory, final List<Content> contents) {
        return contents.stream().filter(content -> content.filename().endsWith(".zip")).allMatch(content -> {
            Path zipFile = cacheDirectory.resolve(content.filename());
            Path unzippedFolder = cacheDirectory.resolve(ArtifactUtils.getFilenameWithoutExtension(zipFile));
            return ArtifactUtils.copyMissingFilesFromZip(zipFile, unzippedFolder);
        });
    }

    private void logMessageWithLicense(final String message, final String attributionUrl) {
        if (attributionUrl != null && !attributionUrl.isEmpty()) {
            var attributionMessage =
                    String.format(" (Attribution notice for %s can be found at: %s)", LspConstants.CW_LSP_FILENAME, attributionUrl);
            Activator.getLogger().info(message + attributionMessage);
        } else {
            Activator.getLogger().info(message);
        }
    }

    private Optional<ArtifactVersion> resolveVersion(final Manifest manifestFile, final PluginPlatform platform,
            final PluginArchitecture architecture, final Instant start) {
        if (manifestFile == null) {
            String failureReason = "No valid manifest version data was received. An error could have caused this. Please check logs.";
            setErrorReason(LspError.INVALID_VERSION_MANIFEST.toString());
            emitGetServer(Result.FAILED, null, LanguageServerLocation.UNKNOWN, start);
            throw new AmazonQPluginException(failureReason);
        }

        return manifestFile.versions().stream()
                .filter(version -> isCompatibleVersion(version))
                .filter(version -> version.targets().stream()
                        .anyMatch(target -> hasRequiredTargetContent(target, platform, architecture)))
                .max(Comparator.comparing(
                        artifactVersion -> Version.parseVersion(artifactVersion.serverVersion())
                    ));
    }

    private boolean hasRequiredTargetContent(final Target target, final PluginPlatform platform, final PluginArchitecture architecture) {
        var result = isCompatibleTarget(target, platform, architecture);
        return result && target.contents() != null && !target.contents().isEmpty();
    }

    private boolean isCompatibleTarget(final Target target, final PluginPlatform platform,
            final PluginArchitecture architecture) {
        return target.platform().equalsIgnoreCase(platform.getValue()) && target.arch().equalsIgnoreCase(architecture.getValue());
    }

    private boolean isCompatibleVersion(final ArtifactVersion version) {
        return versionRange.includes(ArtifactUtils.parseVersion(version.serverVersion())) && !version.isDelisted();
    }

    private Optional<Target> resolveTarget(final Optional<ArtifactVersion> targetVersion, final PluginPlatform platform,
            final PluginArchitecture architecture) {
        return targetVersion.flatMap(version -> version.targets().stream()
                .filter(target -> isCompatibleTarget(target, platform, architecture))
                .findFirst());
    }

    private boolean downloadFromRemote(final List<Content> contents, final Path downloadDirectory) {
        boolean downloadResult = contents.parallelStream()
                .map(targetContent -> downloadContentFromRemote(targetContent, downloadDirectory))
                .allMatch(result -> result);

        // return false if download is not successful for any one target content
        // if successfully fetched from remote, unzip those in zip format and return result of the unzip operation.
        // If any target fails the unzip operation, the download is considered unsuccessful
        return downloadResult && extractZipFilesFromRemote(downloadDirectory);
    }

    private boolean downloadContentFromRemote(final Content content, final Path downloadDirectory) {
        try {
            Files.createDirectories(downloadDirectory);
            var destinationFile = downloadDirectory.resolve(content.filename());

            var request = HttpRequest.newBuilder()
                    .uri(URI.create(content.url()))
                    .timeout(java.time.Duration.ofSeconds(TIMEOUT_SECONDS))
                    .build();

            var tempFolder = Files.createTempDirectory("lsp-amazon-eclipse");
            var tempFile = tempFolder.resolve(content.filename());
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(tempFile));

            if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                // if validation succeeds move it to download directory
                // if it fails, delete the temp directory
                if (ArtifactUtils.validateHash(response.body(), content.hashes(), true)) {
                    Files.copy(response.body(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
                    Activator.getLogger().info("Downloaded " + content.filename() + " to " + downloadDirectory);
                    return true;
                } else {
                    ArtifactUtils.deleteDirectory(tempFolder);
                    setErrorReason(LspError.ARTIFACT_VALIDATION_ERROR.toString());
                }
            } else {
                setErrorReason(LspError.SERVER_REMOTE_FETCH_ERROR + "-" + response.statusCode());
                throw new AmazonQPluginException("Failed to download remote LSP artifact. Response code: " + response.statusCode());
            }
        } catch (Exception ex) {
            //TODO: account for these failures in telemtry emissions
            Activator.getLogger().error("Error downloading from remote", ex);
        }
        return false;
    }

    /*
     * Extracts any zip files found in the download directory
     * where remote assets have just been downloaded and returns true when each one is successfully unzipped
     */
    private boolean extractZipFilesFromRemote(final Path downloadDirectory) {
        try {
            return Files.walk(downloadDirectory)
                .filter(path -> path.toString().endsWith(".zip"))
                .allMatch(zipFile -> extractZip(zipFile, downloadDirectory));
        } catch (Exception e) {
            String errorMessage = String.format("Failed to extract zip files in %s", downloadDirectory);
            Activator.getLogger().error(errorMessage, e);
            setErrorReason(LspError.SERVER_ZIP_EXTRACTION_ERROR.toString());
            return false;
        }
    }

    /*
     * Unzips the given zip file into a folder of its own name and returns result of the operation
     */
    private boolean extractZip(final Path zipFile, final Path downloadDirectory) {
        var unzipFolder = downloadDirectory.resolve(ArtifactUtils.getFilenameWithoutExtension(zipFile));
        try {
            Files.createDirectories(unzipFolder);
            ArtifactUtils.extractFile(zipFile, unzipFolder);
        } catch (IOException e) {
            Activator.getLogger().error(String.format("Failed to extract zip contents for: %s, some features may not work", zipFile.toString()), e);
            setErrorReason(ExceptionMetadata.scrubException(LspError.SERVER_ZIP_EXTRACTION_ERROR.toString(), e));
            return false;
        }
        return true;
    }

    /*
     * Get fallback location representing the most compatible cached lsp version
     * @param expectedLspVersion: the lsp version with which the fallback version must be the most compatible to
     */
    private Path getFallback(final String expectedLspVersion, final PluginPlatform platform,
            final PluginArchitecture architecture, final Path destinationFolder) {

        var compatibleLspVersions = getCompatibleArtifactVersions();
        var cachedVersions = getCachedVersions(destinationFolder);
        var expectedServerVersion = ArtifactUtils.parseVersion(expectedLspVersion);

        // filter to get sorted list of compatible lsp versions that have a valid cache
        var sortedCachedLspVersions = compatibleLspVersions.stream()
                .filter(artifactVersion -> isValidCachedVersion(artifactVersion, expectedServerVersion, cachedVersions))
                .sorted(Comparator.comparing(x -> Version.parseVersion(x.serverVersion()), Comparator.reverseOrder()))
                .collect(Collectors.toList());

        var fallbackDir = sortedCachedLspVersions.stream()
                .map(x -> getValidLocalCacheDirectory(x, platform, architecture, destinationFolder))
                .filter(Objects::nonNull).findFirst().orElse(null);
        return fallbackDir;
    }

    /*
     * Validate the local cache directory of the given lsp version(matches expected hash)
     * If valid return cache directory, else return null
     */
    private Path getValidLocalCacheDirectory(final ArtifactVersion artifactVersion, final PluginPlatform platform,
            final PluginArchitecture architecture, final Path destinationFolder) {
        var target = resolveTarget(Optional.of(artifactVersion), platform, architecture);
        if (!target.isPresent() || target.get().contents() == null || target.get().contents().isEmpty()) {
            return null;
        }
        var cacheDir = Paths.get(destinationFolder.toString(), artifactVersion.serverVersion());

        var hasValidCache = hasValidCache(target.get().contents(), cacheDir);
        return hasValidCache ? cacheDir : null;
    }

    private boolean isValidCachedVersion(final ArtifactVersion lspVersion, final Version expectedServerVersion,
            final List<Version> cachedVersions) {
        var serverVersion = ArtifactUtils.parseVersion(lspVersion.serverVersion());

        return cachedVersions.contains(serverVersion) && (serverVersion.compareTo(expectedServerVersion) <= 0);
    }

    private List<Version> getCachedVersions(final Path destinationFolder) {
        try {
            return Files.list(destinationFolder)
                       .filter(Files::isDirectory)
                       .map(path -> path.getFileName().toString())
                       .filter(name -> name.matches("\\d+\\.\\d+\\.\\d+"))
                       .map(name -> getVersionedName(name))
                       .filter(Objects::nonNull)
                       .collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    private Version getVersionedName(final String filename) {
        try {
            return ArtifactUtils.parseVersion(filename);
        } catch (Exception e) {
            return null;
        }
    }

    private List<ArtifactVersion> getCompatibleArtifactVersions() {
        return manifest.versions().stream()
                .filter(version -> isCompatibleVersion(version))
                .toList();
    }

    private void deleteDelistedVersions(final Path destinationFolder) {
        var compatibleVersions = getCompatibleArtifactVersions().stream().map(x -> ArtifactUtils.parseVersion(x.serverVersion())).collect(Collectors.toList());
        var cachedVersions = getCachedVersions(destinationFolder);

        // delete de-listed versions in the toolkit compatible version range
        var delistedVersions = cachedVersions.stream().filter(x -> !compatibleVersions.contains(x) && versionRange.includes(x)).collect(Collectors.toList());
        if (delistedVersions.size() > 0) {
            Activator.getLogger().info(String.format("Cleaning up %s cached de-listed versions for Amazon Q Language Server", delistedVersions.size()));
        }
        delistedVersions.forEach(version -> {
            deleteCachedVersion(destinationFolder, version);
        });
    }

    private void deleteExtraVersions(final Path destinationFolder) {
        var cachedVersions = getCachedVersions(destinationFolder);
        // delete extra versions in the compatible toolkit version range except highest 2 versions
        var extraVersions = cachedVersions.stream()
                .filter(x -> versionRange.includes(x))
                .sorted(Comparator.reverseOrder())
                .skip(2)
                .collect(Collectors.toList());
        if (extraVersions.size() > 0) {
            Activator.getLogger().info(String.format("Cleaning up %s cached extra versions for Amazon Q Language Server", extraVersions.size()));
        }
        extraVersions.forEach(version -> {
            deleteCachedVersion(destinationFolder, version);
        });
    }

    private void deleteCachedVersion(final Path destinationFolder, final Version version) {
        var versionPath = destinationFolder.resolve(version.toString());
        ArtifactUtils.deleteDirectory(versionPath);
    }

    public static class Builder {
        private Manifest manifest;
        private VersionRange versionRange;
        private Boolean integrityChecking;
        private HttpClient httpClient;

        public final Builder withManifest(final Manifest manifest) {
            this.manifest = manifest;
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

        public final RemoteLspFetcher build() {
            return new RemoteLspFetcher(this);
        }
    }

}
