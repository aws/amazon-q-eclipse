// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.telemetry.metadata;

import java.util.UUID;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.FrameworkUtil;

import software.amazon.awssdk.services.toolkittelemetry.model.AWSProduct;
import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;

public final class PluginClientMetadata implements ClientMetadata {
    private PluginClientMetadata() {
        // Prevent instantiation
    }

    private static final String CLIENT_ID_KEY = "clientId";

    private static final String OS_NAME = System.getProperty("os.name");
    private static final String OS_VERSION = System.getProperty("os.version");
    private static final String IDE_NAME = Platform.getProduct().getName();
    private static final String IDE_VERSION = System.getProperty("eclipse.buildId");
    private static final String PLUGIN_NAME = AWSProduct.AMAZON_Q_FOR_ECLIPSE.toString();
    private static final String PLUGIN_VERSION = FrameworkUtil.getBundle(PluginClientMetadata.class).getVersion().toString();

    private static final PluginClientMetadata INSTANCE = new PluginClientMetadata();

    public static ClientMetadata getInstance() {
        return INSTANCE;
    }

    public String getOSName() {
        return OS_NAME;
    }

    public String getOSVersion() {
        return OS_VERSION;
    }

    public String getIdeName() {
        return IDE_NAME;
    }

    public String getIdeVersion() {
        return IDE_VERSION;
    }

    public String getPluginName() {
        return PLUGIN_NAME;
    }

    public String getPluginVersion() {
        return PLUGIN_VERSION;
    }

    public String getClientId() {
        String clientId = PluginStore.getInstance().get(CLIENT_ID_KEY);
        if (clientId == null) {
            synchronized (PluginClientMetadata.class) {
                clientId = PluginStore.getInstance().get(CLIENT_ID_KEY);
                if (clientId == null) {
                    clientId = UUID.randomUUID().toString();
                    PluginStore.getInstance().put(CLIENT_ID_KEY, clientId);
                }
            }
        }
        return clientId;
    }
}
