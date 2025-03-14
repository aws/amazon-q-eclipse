// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.ResourceFactory;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public final class WebviewAssetServer {

    private Server server;

    /**
     * Sets up virtual host mapping for the given path using Jetty server.
     * @param jsPath
     * @return boolean indicating if server can be successfully launched
     */
    public boolean resolve(final String jsPath) {
        try {
            server = new Server(0);
            var servletContext = new ContextHandler();
            servletContext.setContextPath("/");
            servletContext.addVirtualHosts(new String[] {"127.0.0.1"});

            var handler = new ResourceHandler();

            ResourceFactory resourceFactory = ResourceFactory.of(server);
            handler.setBaseResource(resourceFactory.newResource(jsPath));
            handler.setDirAllowed(true);
            servletContext.setHandler(handler);

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
}
