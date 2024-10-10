// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

public enum PluginPlatform {
    WINDOWS("windows"),
    LINUX("linux"),
    MAC("darwin");

    private final String value;

    PluginPlatform(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
