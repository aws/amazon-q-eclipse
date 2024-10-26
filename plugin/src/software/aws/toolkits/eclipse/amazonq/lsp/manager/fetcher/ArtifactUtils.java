// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
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

    public static boolean validateHash(final Path file, final List<String> hashes, final boolean strict) throws IOException, NoSuchAlgorithmException {
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
}
