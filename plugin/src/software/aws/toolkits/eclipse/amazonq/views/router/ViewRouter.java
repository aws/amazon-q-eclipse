// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.router;

import io.reactivex.rxjava3.core.Observable;
import software.aws.toolkits.eclipse.amazonq.broker.api.EventObserver;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspState;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public final class ViewRouter {

    public static final record PluginState (
            AuthState authState, LspState lspState
    ) { }

    private ViewId activeViewId;
<<<<<<< HEAD

    // this state needs to be maintained to ensure correct resolution in refreshActiveView
    private LspState lspState;
    private AuthState authState;
=======
>>>>>>> 66aae23 (Add listeners for combined state streams and view update request)

    public ViewRouter() {
        activeViewId = ViewId.CHAT_VIEW;

        EventObserver<ActiveViewUpdateRequest> activeViewUpdateRequestObserver = new EventObserver<>() {
            @Override
            public void onEvent(final ActiveViewUpdateRequest request) {
                if (activeViewId != request.currentActiveView()) {
                    notifyActiveViewChange();
                }
            }
        };
        Activator.getEventBroker().subscribe(ActiveViewUpdateRequest.class, activeViewUpdateRequestObserver);

        EventObserver<PluginState> pluginStateObserver = new EventObserver<>() {
            @Override
            public void onEvent(final PluginState pluginState) {
                refreshActiveView(pluginState);
            }
        };
        /* Combine AuthState and LspState streams to emit latest values from both stream on event: */
        Observable<AuthState> authStateObservable = Activator.getEventBroker().ofObservable(AuthState.class);
        Observable<LspState> lspStateObservable = Activator.getEventBroker().ofObservable(LspState.class);
        Observable.combineLatest(authStateObservable, lspStateObservable, PluginState::new)
                .subscribe(event -> pluginStateObserver.onEvent(event));
    }

    private void refreshActiveView(final PluginState pluginState) {
        ViewId newActiveViewId = ViewId.CHAT_VIEW;

        if (isDependencyMissing()) { // TODO: dependency missing check logic needs to be implemented
            newActiveViewId = ViewId.DEPENDENCY_MISSING_VIEW;
        } else if (pluginState.lspState == LspState.FAILED) {
            newActiveViewId = ViewId.LSP_STARTUP_FAILED_VIEW;
        } else if (pluginState.lspState == LspState.PENDING) {
            newActiveViewId = ViewId.LSP_INITIALIZING_VIEW;
        } else if (isChatUIAssetMissing()) { // TODO: chat missing logic needs to be implemented
            newActiveViewId = ViewId.CHAT_ASSET_MISSING_VIEW;
        } else if (pluginState.authState.isLoggedOut()) {
            newActiveViewId = ViewId.TOOLKIT_LOGIN_VIEW;
        } else if (pluginState.authState.isExpired()) {
            newActiveViewId = ViewId.RE_AUTHENTICATE_VIEW;
        } else {
            newActiveViewId = ViewId.CHAT_VIEW;
        }

        updateActiveView(newActiveViewId);
    }

    private void updateActiveView(final ViewId newActiveViewId) {
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
