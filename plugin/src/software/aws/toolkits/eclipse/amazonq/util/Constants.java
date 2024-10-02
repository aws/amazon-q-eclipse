// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

public final class Constants {

    private Constants() {
        // to avoid initiation
    }

    public static final String CUSTOMIZATION_STORAGE_INTERNAL_KEY = "aws.q.customization.eclipse";
    public static final String LSP_CUSTOMIZATION_CONFIGURATION_KEY = "customization";
    public static final String LSP_CONFIGURATION_KEY = "aws.q";
    public static final String IDE_CUSTOMIZATION_NOTIFICATION_TITLE = "Amazon Q Customization";
    public static final String IDE_CUSTOMIZATION_NOTIFICATION_BODY_TEMPLATE = "Amazon Q inline suggestions are now coming from the %s";
    public static final String DEFAULT_Q_FOUNDATION_DISPLAY_NAME = "Amazon Q foundation (Default)";

}