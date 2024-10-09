// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import java.util.UUID;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.FrameworkUtil;

import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;

public final class ClientMetadata {
    private ClientMetadata() {
        // Prevent instantiation
    }

    private static final String CLIENT_ID_KEY = "clientId";

    private static final String OS_NAME = System.getProperty("os.name");
    private static final String OS_VERSION = System.getProperty("os.version");
    private static final String IDE_NAME = Platform.getProduct().getName();
    private static final String IDE_VERSION = System.getProperty("eclipse.buildId");
    private static final String PLUGIN_NAME = FrameworkUtil.getBundle(ClientMetadata.class).getSymbolicName();
    private static final String PLUGIN_VERSION = FrameworkUtil.getBundle(ClientMetadata.class).getVersion().toString();

    public static String getOSName() {
        return OS_NAME;
    }

    public static String getOSVersion() {
        return OS_VERSION;
    }

    public static String getIdeName() {
        return IDE_NAME;
    }

    public static String getIdeVersion() {
        return IDE_VERSION;
    }

    public static String getPluginName() {
        return PLUGIN_NAME;
    }

    public static String getPluginVersion() {
        return PLUGIN_VERSION;
    }

    public static String getClientId() {
        String clientId = PluginStore.get(CLIENT_ID_KEY);
        if (clientId == null) {
            synchronized (ClientMetadata.class) {
                clientId = PluginStore.get(CLIENT_ID_KEY);
                if (clientId == null) {
                    clientId = UUID.randomUUID().toString();
                    PluginStore.put(CLIENT_ID_KEY, clientId);
                }
            }
        }
        return clientId;
    }
}
