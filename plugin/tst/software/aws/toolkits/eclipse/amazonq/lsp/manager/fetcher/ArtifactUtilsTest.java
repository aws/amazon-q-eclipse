package software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.osgi.framework.Version;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArtifactUtilsTest {

    @Test
    void testExtractFile(@TempDir final Path tempDir) throws IOException {
        Path zipFile = tempDir.resolve("test.zip");
        createTestZipFile(zipFile, "foo.txt", "dir/bar.txt");

        Path extractDir = tempDir.resolve("extract");
        ArtifactUtils.extractFile(zipFile, extractDir);

        assertTrue(Files.exists(extractDir.resolve("foo.txt")));
        assertTrue(Files.exists(extractDir.resolve("dir/bar.txt")));
    }

    @Test
    void testParseVersion() {
        Version version = ArtifactUtils.parseVersion("1.2.3");
        assertEquals(1, version.getMajor());
        assertEquals(2, version.getMinor());
        assertEquals(3, version.getMicro());
    }

    @Test
    void testValidateHashSuccess(@TempDir final Path tempDir) throws IOException, NoSuchAlgorithmException {
        Path file = tempDir.resolve("foo.txt");
        Files.writeString(file, "Test!");
        List<String> hashes = Arrays.asList("sha384:cf6ee7334a7fd9cb24a4a6a0fd9bb6eadc73c6724c7ddfe983b82dcff68164247788de24a5b601c95748111b368db4e2");

        boolean result = ArtifactUtils.validateHash(file, hashes, true);

        assertTrue(result);
    }

    @Test
    void testValidateHashFailure(@TempDir final Path tempDir) throws IOException, NoSuchAlgorithmException {
        Path file = tempDir.resolve("file.txt");
        Files.writeString(file, "Test!");
        List<String> hashes = Arrays.asList("sha384:invalidhash");

        assertThrows(IOException.class, () -> ArtifactUtils.validateHash(file, hashes, true));
    }

    @Test
    void testHasPosixFilePermissions() {
        Path mockPath = mock(Path.class);
        FileSystem mockFileSystem = mock(FileSystem.class);
        when(mockPath.getFileSystem()).thenReturn(mockFileSystem);

        //Posix Systems
        when(mockFileSystem.supportedFileAttributeViews()).thenReturn(Collections.singleton("posix"));
        assertTrue(ArtifactUtils.hasPosixFilePermissions(mockPath));

        //Windows/Non-Posix Systems
        when(mockFileSystem.supportedFileAttributeViews()).thenReturn(Collections.singleton("basic"));
        assertFalse(ArtifactUtils.hasPosixFilePermissions(mockPath));
    }

    private void createTestZipFile(final Path zipFile, final String... fileNames) throws IOException {
        try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            for (String fileName : fileNames) {
                ZipEntry entry = new ZipEntry(fileName);
                zipOut.putNextEntry(entry);
                zipOut.write("Foo".getBytes());
                zipOut.closeEntry();
            }
        }
    }
}
