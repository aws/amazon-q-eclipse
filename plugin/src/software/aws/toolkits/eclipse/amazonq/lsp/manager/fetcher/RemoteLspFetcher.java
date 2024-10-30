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
import java.util.List;
import java.util.Optional;

import org.osgi.framework.VersionRange;

import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspConstants;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspFetchResult;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.model.ArtifactVersion;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.model.Content;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.model.Manifest;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.model.Target;
import software.aws.toolkits.eclipse.amazonq.lsp.model.LanguageServerLocation;
import software.aws.toolkits.eclipse.amazonq.util.HttpClientFactory;
import software.aws.toolkits.eclipse.amazonq.util.PluginArchitecture;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.PluginPlatform;

public final class RemoteLspFetcher implements LspFetcher {

    private static final int TIMEOUT_SECONDS = 10;

    private final Manifest manifest;
    private final VersionRange versionRange;
    private final boolean integrityChecking;
    private final HttpClient httpClient;

    private RemoteLspFetcher(final Builder builder) {
        this.manifest = builder.manifest;
        this.versionRange = builder.versionRange != null ? builder.versionRange : LspConstants.LSP_SUPPORTED_VERSION_RANGE;
        this.integrityChecking = builder.integrityChecking != null ? builder.integrityChecking : true;
        this.httpClient = builder.httpClient != null ? builder.httpClient : HttpClientFactory.getInstance();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public LspFetchResult fetch(final PluginPlatform platform, final PluginArchitecture architecture,
            final Path destination) {
        var artifactVersion = resolveVersion(manifest, platform, architecture);
        var target = resolveTarget(artifactVersion, platform, architecture);
        var serverVersion = artifactVersion.get().serverVersion();

        if (!target.isPresent()) {
            throw new AmazonQPluginException(
                    "No language server found for platform " + platform + " and architecture " + architecture);
        }

        var contents = target.get().contents();
        var downloadDirectory = Paths.get(destination.toString(), serverVersion.toString());

        // if the latest version is stored locally already and is valid, return that, else fetch the latest version
        if (hasValidCache(contents, downloadDirectory)) {
            logMessageWithLicense(String.format("Launching Amazon Q language server v%s from local cache %s",
                    serverVersion.toString(), downloadDirectory), artifactVersion.get().thirdPartyLicenses());
            return new LspFetchResult(downloadDirectory.toString(), serverVersion, LanguageServerLocation.Cache);
        }

        // delete invalid local cache
        ArtifactUtils.deleteDirectory(downloadDirectory);

        // if all lsp target contents are successfully downloaded from remote location,
        // return the download location
        if (downloadFromRemote(contents, downloadDirectory)) {
            logMessageWithLicense(String.format("Installing Amazon Q language server v%s to %s",
                    serverVersion.toString(), downloadDirectory.toString()),
                    artifactVersion.get().thirdPartyLicenses());
            return new LspFetchResult(downloadDirectory.toString(), serverVersion, LanguageServerLocation.Remote);
        }

        // if unable to retrieve / validate contents from remote location, cleanup
        // download cache
        ArtifactUtils.deleteDirectory(downloadDirectory);

        // TODO: Add fallback cached lsp resolution logic

        throw new AmazonQPluginException("Unable to find a compatible version of Amazon Q Language Server.");
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
        if (!attributionUrl.isEmpty()) {
            var attributionMessage =
                    String.format(" (Attribution notice for %s can be found at: %s)", LspConstants.CW_LSP_FILENAME, attributionUrl);
            Activator.getLogger().info(message + attributionMessage);
        } else {
            Activator.getLogger().info(message);
        }
    }

    private Optional<ArtifactVersion> resolveVersion(final Manifest manifestFile, final PluginPlatform platform, final PluginArchitecture architecture) {
        return manifestFile.versions().stream()
                .filter(version -> !version.isDelisted())
                .filter(version -> version.targets().stream()
                        .anyMatch(target -> target.platform().equalsIgnoreCase(platform.getValue()) && target.arch().equalsIgnoreCase(architecture.getValue())))
                .filter(version -> versionRange.includes(ArtifactUtils.parseVersion(version.serverVersion())))
                .findFirst();
    }

    private Optional<Target> resolveTarget(final Optional<ArtifactVersion> targetVersion, final PluginPlatform platform,
            final PluginArchitecture architecture) {
        return targetVersion.flatMap(version -> version.targets().stream()
                .filter(target -> target.platform().equalsIgnoreCase(platform.getValue()) && target.arch().equalsIgnoreCase(architecture.getValue()))
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
                if (ArtifactUtils.validateHash(tempFile, content.hashes(), true)) {
                    Files.copy(tempFile, destinationFile, StandardCopyOption.REPLACE_EXISTING);
                    Activator.getLogger().info("Downloaded " + content.filename() + " to " + downloadDirectory);
                    return true;
                } else {
                    ArtifactUtils.deleteDirectory(tempFolder);
                }
            } else {
                throw new AmazonQPluginException("Failed to download remote LSP artifact. Response code: " + response.statusCode());
            }
        } catch (Exception ex) {
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
            Activator.getLogger().error(String.format("Failed to extract zip files in %s", downloadDirectory), e);
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
            return false;
        }
        return true;
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
