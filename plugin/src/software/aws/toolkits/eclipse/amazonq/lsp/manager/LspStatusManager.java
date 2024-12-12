// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.manager;

import software.aws.toolkits.eclipse.amazonq.views.ViewVisibilityManager;

public final class LspStatusManager {

    private LspStatusManager() {
        //prevent
    }

    private static LspState lspState = LspState.PENDING;

    public static boolean lspFailed() {
        return (lspState == LspState.FAILED);
    }
    public static void setToActive() {
        lspState = LspState.ACTIVE;
        ViewVisibilityManager.showDefaultView("restart");
    }
    public static void setToFailed() {
        if (lspState != LspState.FAILED) {
            ViewVisibilityManager.showLspStartUpFailedView("update");
            lspState = LspState.FAILED;
        }
    }
    public static LspState getLspState() {
        return lspState;
    }
}
