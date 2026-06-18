// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.Callback;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public final class WebviewAssetServer {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private static final Map<String, String> CONTENT_TYPES_BY_EXTENSION = Map.ofEntries(
            Map.entry("js", "text/javascript; charset=utf-8"),
            Map.entry("mjs", "text/javascript; charset=utf-8"),
            Map.entry("css", "text/css; charset=utf-8"),
            Map.entry("html", "text/html; charset=utf-8"),
            Map.entry("htm", "text/html; charset=utf-8"),
            Map.entry("json", "application/json; charset=utf-8"),
            Map.entry("map", "application/json; charset=utf-8"),
            Map.entry("svg", "image/svg+xml"),
            Map.entry("png", "image/png"),
            Map.entry("gif", "image/gif"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("ico", "image/x-icon"),
            Map.entry("wasm", "application/wasm"),
            Map.entry("woff", "font/woff"),
            Map.entry("woff2", "font/woff2"),
            Map.entry("ttf", "font/ttf"),
            Map.entry("eot", "application/vnd.ms-fontobject"));

    private Server server;

    /**
     * Sets up virtual host mapping for the given path using Jetty server.
     * @param jsPath the absolute path to the directory containing the assets to serve
     * @return boolean indicating if server can be successfully launched
     */
    public boolean resolve(final String jsPath) {
        try {
            final Path baseDirectory = Path.of(jsPath).toAbsolutePath().normalize();

            server = new Server(0);
            var servletContext = new ContextHandler();
            servletContext.setContextPath("/");
            servletContext.addVirtualHosts(new String[] {"127.0.0.1"});

            servletContext.setHandler(new StaticFileHandler(baseDirectory));

            server.setHandler(servletContext);
            server.start();
            return true;

        } catch (Exception e) {
            stop();
            Activator.getLogger().error("Error occurred while attempting to start a virtual server for " + jsPath, e);
            return false;
        }
    }

    public String getUri() {
        return server.getURI().toString();
    }

    public void stop() {
        if (server != null) {
            try {
                server.stop();
            } catch (Exception e) {
                Activator.getLogger().error("Error occurred when attempting to stop the virtual server", e);
            }
        }
    }

    /**
     * Resolves the HTTP content type to advertise for the given file name based on its extension.
     * @param fileName the name of the file being served
     * @return the matching content type, or {@code application/octet-stream} when the extension is unknown
     */
    static String getContentType(final String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return DEFAULT_CONTENT_TYPE;
        }
        String extension = fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        return CONTENT_TYPES_BY_EXTENSION.getOrDefault(extension, DEFAULT_CONTENT_TYPE);
    }

    /**
     * Serves files from a fixed base directory using {@link java.nio.file.Files}.
     *
     * <p>
     * This intentionally avoids Jetty's {@code ResourceHandler} / {@code PathResource#resolve}, which throws
     * {@link InvalidPathException} on Windows with the Jetty version bundled in newer Eclipse releases. There, the
     * requested path is combined into a URI-style string such as {@code /C:/Users/.../amazonq-ui.js} and then passed to
     * {@link Path#resolve(String)}, which is illegal on Windows (the leading slash before the drive letter). Resolving
     * the request path relative to the base directory ourselves keeps the resulting path valid on every platform.
     * See <a href="https://github.com/aws/amazon-q-eclipse/issues/560">issue #560</a>.
     * </p>
     */
    private static final class StaticFileHandler extends Handler.Abstract {

        private final Path baseDirectory;

        StaticFileHandler(final Path baseDirectory) {
            this.baseDirectory = baseDirectory;
        }

        @Override
        public boolean handle(final Request request, final Response response, final Callback callback) {
            String pathInContext = Request.getPathInContext(request);
            if (pathInContext == null || pathInContext.isEmpty() || "/".equals(pathInContext)) {
                Response.writeError(request, response, callback, HttpStatus.NOT_FOUND_404);
                return true;
            }

            // Strip leading slashes so the request path is resolved relative to (not as a sibling of) the base directory.
            String relativePath = pathInContext;
            while (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }

            Path resolved;
            try {
                resolved = baseDirectory.resolve(relativePath).normalize();
            } catch (InvalidPathException e) {
                Response.writeError(request, response, callback, HttpStatus.BAD_REQUEST_400);
                return true;
            }

            // Guard against path traversal outside of the served base directory.
            if (!resolved.startsWith(baseDirectory)) {
                Response.writeError(request, response, callback, HttpStatus.FORBIDDEN_403);
                return true;
            }

            if (!Files.isRegularFile(resolved) || !Files.isReadable(resolved)) {
                Response.writeError(request, response, callback, HttpStatus.NOT_FOUND_404);
                return true;
            }

            try {
                byte[] contents = Files.readAllBytes(resolved);
                response.setStatus(HttpStatus.OK_200);
                response.getHeaders().put(HttpHeader.CONTENT_TYPE, getContentType(resolved.getFileName().toString()));
                response.write(true, ByteBuffer.wrap(contents), callback);
            } catch (IOException e) {
                Response.writeError(request, response, callback, HttpStatus.INTERNAL_SERVER_ERROR_500, "Unable to read requested asset");
            }
            return true;
        }
    }
}
