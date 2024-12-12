// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;
import java.util.concurrent.CompletableFuture;

import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspStatusManager;

public final class LspStartUpFailedView extends BaseView {
    public static final String ID = "software.aws.toolkits.eclipse.amazonq.views.LspStartUpFailedView";

    private static final String ICON_PATH = "icons/AmazonQ64.png";
    private static final String HEADER_LABEL = "Language Server failed to start.";
    private static final String DETAIL_MESSAGE = "Restart Eclipse or review error logs for troubleshooting";

    /* TODO: After refactor of LSP error handling is completed,
     * add logic to base detail_message on error code returned from exception
     *  */
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
    protected void showAlternateView() {
        ViewVisibilityManager.showDefaultView("restart");
    }

    @Override
    protected CompletableFuture<Boolean> isViewDisplayable() {
        return CompletableFuture.completedFuture(LspStatusManager.lspFailed());
    }

}
