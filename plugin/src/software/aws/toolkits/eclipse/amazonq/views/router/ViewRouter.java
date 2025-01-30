// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.router;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import software.aws.toolkits.eclipse.amazonq.broker.api.EventObserver;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspState;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.views.router.ViewRouter.PluginState;

public final class ViewRouter implements EventObserver<PluginState> {

    public record PluginState(
            AuthState authState, LspState lspState
    ) { }

    private ViewId activeViewId;

    public ViewRouter() {
        /* Combine AuthState and LspState streams to emit latest values from both stream on event: */
        Observable<AuthState> authStateObservable = Activator.getEventBroker().ofObservable(AuthState.class);
        Observable<LspState> lspStateObservable = Activator.getEventBroker().ofObservable(LspState.class);
        Observable.combineLatest(authStateObservable, lspStateObservable, PluginState::new)
                .observeOn(Schedulers.computation())
                .subscribe(this::onEvent);
    }

    @Override
    public void onEvent(final PluginState pluginState) {
        refreshActiveView(pluginState);
    }

    private void refreshActiveView(final PluginState pluginState) {
        ViewId newActiveViewId;

        if (isDependencyMissing()) { // TODO: dependency missing check logic needs to be implemented
            newActiveViewId = ViewId.DEPENDENCY_MISSING_VIEW;
        } else if (pluginState.lspState == LspState.FAILED) {
            newActiveViewId = ViewId.LSP_STARTUP_FAILED_VIEW;
        } else if (pluginState.lspState == LspState.PENDING) { // TODO: this might make sense in ChatView
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
