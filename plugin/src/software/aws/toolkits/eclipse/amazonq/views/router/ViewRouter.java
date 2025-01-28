// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.router;

import software.aws.toolkits.eclipse.amazonq.broker.api.EventObserver;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspState;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public final class ViewRouter {

    private final EventObserver<AuthState> authStateObserver = new EventObserver<>() {
        @Override
        public void onEvent(final AuthState newAuthState) {
            authState = newAuthState;
            refreshActiveView();
        }
    };

    private final EventObserver<LspState> lspStateObserver = new EventObserver<>() {
        @Override
        public void onEvent(final LspState newLspState) {
            lspState = newLspState;
            refreshActiveView();
        }
    };

    private ViewId activeViewId;
    
    // this state needs to be maintained to ensure correct resolution in refreshActiveView
    private LspState lspState;
    private AuthState authState;

    public ViewRouter() {
        activeViewId = ViewId.TOOLKIT_LOGIN_VIEW;
        lspState = null;
        authState = null;

        Activator.getEventBroker().subscribe(AuthState.class, authStateObserver);
        Activator.getEventBroker().subscribe(LspState.class, lspStateObserver);
    }

    private void refreshActiveView() {
        ViewId newActiveViewId = ViewId.TOOLKIT_LOGIN_VIEW;

        if (isDependencyMissing()) { // TODO: dependency missing check logic needs to be implemented
            newActiveViewId = ViewId.DEPENDENCY_MISSING_VIEW;
        } else if (lspState != null) {
            if (lspState == LspState.FAILED) {
                newActiveViewId = ViewId.LSP_STARTUP_FAILED_VIEW;
            } else if (lspState == LspState.PENDING) {
                newActiveViewId = ViewId.LSP_INITIALIZING_VIEW;
            }
        } else if (isChatUIAssetMissing()) { // TODO: chat missing logic needs to be implemented
            newActiveViewId = ViewId.CHAT_ASSET_MISSING_VIEW;
        } else if (authState != null) {
            if (authState.isLoggedOut()) {
                newActiveViewId = ViewId.TOOLKIT_LOGIN_VIEW;
            } else if (authState.isExpired()) {
                newActiveViewId = ViewId.RE_AUTHENTICATE_VIEW;
            }
        } else {
            newActiveViewId = ViewId.CHAT_VIEW;
        }

        if (activeViewId != newActiveViewId) {
            activeViewId = newActiveViewId;
            notifyActiveViewChange();
        }
    }

    private void notifyActiveViewChange() {
        Activator.getEventBroker().post(activeViewId);
    }

    // TODO: replace with relevant checks
    private boolean isDependencyMissing() {
        return false;
    }

    // TODO: replace with relevant checks
    private boolean isChatUIAssetMissing() {
        return false;
    }

}
