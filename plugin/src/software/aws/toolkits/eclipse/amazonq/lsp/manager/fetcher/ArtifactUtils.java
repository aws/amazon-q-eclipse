// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.codec.digest.DigestUtils;
import org.osgi.framework.Version;

import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public final class ArtifactUtils {
    private ArtifactUtils() {
        // Prevent instantiation
    }

    public static void extractFile(final Path zipFilePath, final Path destination) throws IOException {
        try (var zipFile = new ZipFile(zipFilePath.toFile())) {
            for (Enumeration<? extends ZipEntry> entries = zipFile.entries(); entries.hasMoreElements();) {
                var entry = entries.nextElement();
                var entryPath = destination.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    if (Files.exists(entryPath)) {
                        Files.delete(entryPath);
                    }
                    Files.copy(zipFile.getInputStream(entry), entryPath);
                }
            }
        }
    }

    public static void copyDirectory(final Path source, final Path target) throws IOException {
        Files.walk(source)
             .forEach(sourcePath -> {
                 try {
                     Path targetPath = target.resolve(source.relativize(sourcePath));
                     if (Files.isDirectory(sourcePath)) {
                         Files.createDirectories(targetPath);
                     } else {
                         Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                     }
                 } catch (IOException e) {
                     throw new AmazonQPluginException("Could not copy directory contents", e);
                 }
             });
    }

    public static Version parseVersion(final String versionString) {
        return new Version(versionString);
    }

    public static boolean validateHash(final Path file, final List<String> hashes, final boolean strict) {
        try {
            var expectedHash = hashes.stream()
                    .filter(hash -> hash.startsWith("sha384:"))
                    .map(hash -> hash.substring(7))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No SHA-384 hash found in manifest"));

            var actualHash = DigestUtils.sha384Hex(new FileInputStream(file.toFile()));

            if (!actualHash.equalsIgnoreCase(expectedHash)) {
                if (strict) {
                    throw new IOException("Hash mismatch for file " + file.getFileName() + ". Expected: " + expectedHash + ", Actual: " + actualHash);
                }
                return false;
            }
            return true;
        } catch (Exception e) {
            Activator.getLogger().error("Error validating hash for " + file.getFileName(), e);
        }
        return false;
    }

    public static boolean deleteFile(final Path filePath) {
        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
            return true;
        } catch (IOException e) {
            Activator.getLogger().info("Error deleting file: " + filePath.toString());
            return false;
        }
    }

    public static boolean deleteDirectory(final Path dirPath) {
        try {
            if (!Files.exists(dirPath)) {
                return false;
            }
            Files
            .walk(dirPath)
            .sorted(Comparator.reverseOrder())
            .forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            return true;
        } catch (IOException e) {
            Activator.getLogger().error(String.format("Error deleting directory: %s", dirPath), e);
            return false;
        }
    }

    public static String getFilenameWithoutExtension(final Path path) {
        return path.getFileName().toString().replaceFirst("[.][^.]+$", "");
    }

    /*
     * Copies missing files from zip into a destination folder which is intended to
     * be an unzipped version of the zip file
     */
    public static boolean copyMissingFilesFromZip(final Path zipFile, final Path unzippedFolder) {
        try {
            Files.createDirectories(unzippedFolder);

            try (var zip = new ZipFile(zipFile.toFile())) {
                // iterate over zip contents in parallel for improved performance
                zip.stream().parallel().forEach(entry -> {
                    Path entryPath = unzippedFolder.resolve(entry.getName());
                    try {
                        // if the entry is a directory, create the directory in the destination folder if not present
                        if (entry.isDirectory()) {
                            Files.createDirectories(entryPath);
                        // if the entry is a file, copy the file to the destination folder if not present
                        } else if (!Files.exists(entryPath)) {
                            Files.createDirectories(entryPath.getParent());
                            Files.copy(zip.getInputStream(entry), entryPath);
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }
            return true;
        } catch (IOException | UncheckedIOException e) {
            Activator.getLogger().error("Error when attempting to copy missing contents from zip file: " + zipFile.toString(), e);
            return false;
        }
    }

    public static boolean hasPosixFilePermissions(final Path filePath) {
        return filePath.getFileSystem().supportedFileAttributeViews().contains("posix");
    }
}
