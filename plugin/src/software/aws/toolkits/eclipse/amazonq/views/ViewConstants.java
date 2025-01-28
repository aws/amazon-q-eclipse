// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

public final class ViewConstants {
    private ViewConstants() {
        // Prevent instantiation
    }

    public static final String COMMAND_FUNCTION_NAME = "ideCommand";
    public static final String PREFERENCE_STORE_PLUGIN_FIRST_STARTUP_KEY = "qEclipseFirstLoad";
    public static final String CHAT_ASSET_MISSING_VIEW_ID =
            "software.aws.toolkits.eclipse.amazonq.views.ChatAssetMissingView";
    public static final String DEPENDENCY_MISSING_VIEW_ID =
            "software.aws.toolkits.eclipse.amazonq.views.DependencyMissingView";
    public static final String REAUTHENTICATE_VIEW_ID =
            "software.aws.toolkits.eclipse.amazonq.views.ReauthenticateView";
}
