// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.manager;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.views.ViewVisibilityManager;

public final class LspStatusManager {

    private static final LspStatusManager INSTANCE;
    private LspState lspState;

    static {
        INSTANCE = new LspStatusManager();
    }

    private LspStatusManager() {
        lspState = LspState.PENDING;
        Activator.getEventBroker().post(lspState);
    }

    public static LspStatusManager getInstance() {
        return INSTANCE;
    }


    public boolean lspFailed() {
        return (lspState == LspState.FAILED);
    }

    public void setToActive() {
        lspState = LspState.ACTIVE;
        Activator.getEventBroker().post(lspState);
        ViewVisibilityManager.showDefaultView("restart");
    }

    public void setToFailed() {
        if (lspState != LspState.FAILED) {
            ViewVisibilityManager.showLspStartUpFailedView("update");
            lspState = LspState.FAILED;
            Activator.getEventBroker().post(lspState);
        }
    }

    public LspState getLspState() {
        return lspState;
    }
}
