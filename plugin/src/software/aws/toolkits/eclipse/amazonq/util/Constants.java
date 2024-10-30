// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

public final class Constants {

    private Constants() {
        // to avoid initiation
    }

    public static final String CUSTOMIZATION_STORAGE_INTERNAL_KEY = "aws.q.customization.eclipse";
    public static final String LSP_CUSTOMIZATION_CONFIGURATION_KEY = "customization";
    public static final String LSP_ENABLE_TELEMETRY_EVENTS_CONFIGURATION_KEY = "enableTelemetryEventsToDestination";
    public static final String LSP_OPT_OUT_TELEMETRY_CONFIGURATION_KEY = "optOutTelemetry";
    public static final String LSP_Q_CONFIGURATION_KEY = "aws.q";
    public static final String LSP_CW_CONFIGURATION_KEY = "aws.codeWhisperer";
    public static final String LSP_CW_OPT_OUT_KEY = "shareCodeWhispererContentWithAWS";
    public static final String IDE_CUSTOMIZATION_NOTIFICATION_TITLE = "Amazon Q Customization";
    public static final String IDE_CUSTOMIZATION_NOTIFICATION_BODY_TEMPLATE = "Amazon Q inline suggestions are now coming from the %s";
    public static final String DEFAULT_Q_FOUNDATION_DISPLAY_NAME = "Amazon Q foundation (Default)";
    public static final String LOGIN_TYPE_KEY = "LOGIN_TYPE";
    public static final String LOGIN_IDC_PARAMS_KEY = "IDC_PARAMS";
    public static final String PROXY_UPDATE_NOTIFICATION_TITLE = "Proxy settings changed";
    public static final String PROXY_UPDATE_NOTIFICATION_DESCRIPTION = "Proxy changes detected. Please restart the extension for it to take effect";
    public static final String AWS_BUILDER_ID_URL = "https://view.awsapps.com/start";
    public static final String IDC_PROFILE_NAME = "eclipse-q-profile";
    public static final String IDC_SESSION_NAME = "eclipse-q-session";
    public static final String IDC_PROFILE_KIND = "SsoTokenProfile";
}
