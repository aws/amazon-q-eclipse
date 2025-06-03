// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

public final class ArchitectureUtils {

    private ArchitectureUtils() { }

    public static boolean isWindowsArm() {
        String processorIdentifier = System.getenv("PROCESSOR_IDENTIFIER");
        return processorIdentifier != null && processorIdentifier.contains("ARM") && PluginUtils.getPlatform().equals(PluginPlatform.WINDOWS);
    }

}
