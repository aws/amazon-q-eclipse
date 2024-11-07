// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import software.aws.toolkits.eclipse.amazonq.util.ChatContentProvider;

public final class ChatAssetMissingView extends BaseView {
    public static final String ID = "software.aws.toolkits.eclipse.amazonq.views.ChatAssetMissingView";

    private static final String ICON_PATH = "icons/AmazonQ64.png";
    private static final String HEADER_LABEL = "Error loading Q chat.";
    private static final String DETAIL_MESSAGE = "Restart Eclipse or review error logs for troubleshooting";
    private ChatContentProvider chatContentProvider;

    public ChatAssetMissingView() {
        this.chatContentProvider = new ChatContentProvider();
    }

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

    @Override
    protected CompletableFuture<Boolean> isViewDisplayable() {
        return CompletableFuture.supplyAsync(() -> {
            Optional<String> content = chatContentProvider.getContent();
            return !content.isPresent();
        });
    }

    @Override
    protected void handleNonDisplayableView() {
        AmazonQView.showView(AmazonQChatWebview.ID);
    }
}
