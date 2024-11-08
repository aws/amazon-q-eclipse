// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.ChatAssetProvider;

public final class ChatAssetMissingView extends BaseView {
    public static final String ID = "software.aws.toolkits.eclipse.amazonq.views.ChatAssetMissingView";

    private static final String ICON_PATH = "icons/AmazonQ64.png";
    private static final String HEADER_LABEL = "Error loading Q chat.";
    private static final String DETAIL_MESSAGE = "Restart Eclipse or review error logs for troubleshooting";
    private ChatAssetProvider chatAssetProvider;

    public ChatAssetMissingView() {
        this.chatAssetProvider = new ChatAssetProvider();
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
            try {
                Optional<String> chatAsset = chatAssetProvider.get();
                return !chatAsset.isPresent();
            } catch (Exception ex) {
                Activator.getLogger().error("Failed to verify Chat content is retrievable", ex);
                return true; // Safer to display chat asset missing view by default than give access
            }
        });
    }

    @Override
    protected void showAlternateView() {
        AmazonQView.showView(AmazonQChatWebview.ID);
    }

    @Override
    public void dispose() {
        chatAssetProvider.dispose();
        super.dispose();
    }
}
