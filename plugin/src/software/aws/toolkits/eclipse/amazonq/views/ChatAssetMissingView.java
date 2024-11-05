// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

public final class ChatAssetMissingView extends BaseView {
    public static final String ID = "software.aws.toolkits.eclipse.amazonq.views.ChatAssetMissingView";

    private static final String ICON_PATH = "icons/AmazonQ64.png";
    private static final String HEADER_LABEL = "Error loading Q chat.";
    private static final String DETAIL_MESSAGE = "Restart Eclipse or review error logs for troubleshooting";
    @Override
    protected String getIconPath() {
        return ICON_PATH;
    }

    @Override
    protected String getHeaderLabel() {
        return HEADER_LABEL;
    }

    @Override
    protected String getDetailMessage() {
        return DETAIL_MESSAGE;
    }

}
