// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class WebviewAssetServerTest {

    private static final int HTTP_OK = 200;
    private static final int HTTP_NOT_FOUND = 404;

    @Test
    void getContentTypeResolvesKnownExtensionsCaseInsensitively() {
        assertEquals("text/javascript; charset=utf-8", WebviewAssetServer.getContentType("amazonq-ui.js"));
        assertEquals("text/javascript; charset=utf-8", WebviewAssetServer.getContentType("AMAZONQ-UI.JS"));
        assertEquals("text/css; charset=utf-8", WebviewAssetServer.getContentType("styles.css"));
        assertEquals("application/json; charset=utf-8", WebviewAssetServer.getContentType("manifest.json"));
    }

    @Test
    void getContentTypeFallsBackToOctetStreamForUnknownOrMissingExtension() {
        assertEquals("application/octet-stream", WebviewAssetServer.getContentType("archive.unknownext"));
        assertEquals("application/octet-stream", WebviewAssetServer.getContentType("no-extension"));
        assertEquals("application/octet-stream", WebviewAssetServer.getContentType("trailing-dot."));
    }

    @Test
    void servesRequestedAssetWithResolvedContentType(@TempDir final Path tempDir) throws Exception {
        String expectedContents = "window.amazonQChat = {};";
        Files.writeString(tempDir.resolve("amazonq-ui.js"), expectedContents);

        WebviewAssetServer server = new WebviewAssetServer();
        try {
            assertTrue(server.resolve(tempDir.toString()));

            HttpResponse<String> response = get(server, "amazonq-ui.js");

            assertEquals(HTTP_OK, response.statusCode());
            assertEquals(expectedContents, response.body());
            assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("text/javascript"));
        } finally {
            server.stop();
        }
    }

    @Test
    void servesNestedAsset(@TempDir final Path tempDir) throws Exception {
        Path nested = tempDir.resolve("assets").resolve("app.css");
        Files.createDirectories(nested.getParent());
        Files.writeString(nested, "body { margin: 0; }");

        WebviewAssetServer server = new WebviewAssetServer();
        try {
            assertTrue(server.resolve(tempDir.toString()));

            HttpResponse<String> response = get(server, "assets/app.css");

            assertEquals(HTTP_OK, response.statusCode());
            assertEquals("body { margin: 0; }", response.body());
            assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("text/css"));
        } finally {
            server.stop();
        }
    }

    @Test
    void returnsNotFoundForMissingAsset(@TempDir final Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("amazonq-ui.js"), "noop");

        WebviewAssetServer server = new WebviewAssetServer();
        try {
            assertTrue(server.resolve(tempDir.toString()));

            HttpResponse<String> response = get(server, "does-not-exist.js");

            assertEquals(HTTP_NOT_FOUND, response.statusCode());
        } finally {
            server.stop();
        }
    }

    private static HttpResponse<String> get(final WebviewAssetServer server, final String assetPath) throws Exception {
        // Connect over 127.0.0.1 to satisfy the server's virtual host restriction, regardless of the host returned by getUri().
        int port = URI.create(server.getUri()).getPort();
        URI target = URI.create("http://127.0.0.1:" + port + "/" + assetPath);
        HttpRequest request = HttpRequest.newBuilder(target).GET().build();
        HttpClient client = HttpClient.newHttpClient();
        return client.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}
