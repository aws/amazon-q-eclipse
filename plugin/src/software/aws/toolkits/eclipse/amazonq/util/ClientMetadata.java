// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.FrameworkUtil;

public final class ClientMetadata {
    private ClientMetadata() {
        // Prevent instantiation
    }

    private static final String OS_NAME = System.getProperty("os.name");
    private static final String OS_VERSION = System.getProperty("os.version");
    private static final String IDE_NAME = Platform.getProduct().getName();
    private static final String IDE_VERSION = Platform.getProduct().getDefiningBundle().getVersion().toString();
    public static final String PLUGIN_NAME = FrameworkUtil.getBundle(ClientMetadata.class).getSymbolicName();
    public static final String PLUGIN_VERSION = FrameworkUtil.getBundle(ClientMetadata.class).getVersion().toString();

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
}
