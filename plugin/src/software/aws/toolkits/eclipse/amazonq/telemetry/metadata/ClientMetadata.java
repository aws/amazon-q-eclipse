// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.telemetry.metadata;

public interface ClientMetadata {
    String getOSName();

    String getOSVersion();

    String getIdeName();

    String getIdeVersion();

    String getPluginName();

    String getPluginVersion();

    String getClientId();
}
