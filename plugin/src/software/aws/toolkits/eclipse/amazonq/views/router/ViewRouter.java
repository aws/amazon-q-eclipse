// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.router;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import software.aws.toolkits.eclipse.amazonq.broker.api.EventObserver;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspState;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.providers.assets.WebViewAssetState;
import software.aws.toolkits.eclipse.amazonq.providers.browser.BrowserCompatibilityState;

/**
 * Routes to appropriate views based on the combined auth and lsp states (plugin
 * state). This router observes plugin state changes and updates the active view
 * accordingly, broadcasting view changes through the event broker.
 */
public final class ViewRouter implements EventObserver<PluginState> {

    private AmazonQViewType activeView;

    /**
     * Constructs a ViewRouter with the specified builder configuration. Initializes
     * state observation and sets up view routing logic. Primarily useful for
     * testing and injecting observables. When none are passed, the router get the
     * observables directly from the event broker and combines them to create the
     * PluginState stream.
     *
     * @param builder The builder containing auth and lsp state observables
     */
    private ViewRouter(final Builder builder) {
        if (builder.authStateObservable == null) {
            builder.authStateObservable = Activator.getEventBroker().ofObservable(AuthState.class);
        }

        if (builder.lspStateObservable == null) {
            builder.lspStateObservable = Activator.getEventBroker().ofObservable(LspState.class);
        }

        if (builder.browserCompatibilityStateObservable == null) {
            builder.browserCompatibilityStateObservable = Activator.getEventBroker()
                    .ofObservable(BrowserCompatibilityState.class);
        }

        if (builder.webViewAssetStateObservable == null) {
            builder.webViewAssetStateObservable = Activator.getEventBroker().ofObservable(WebViewAssetState.class);
        }

        /**
         * Combines all state observables into a single stream that emits a new PluginState
         * whenever any individual state changes. The combined stream:
         * - Waits for initial events from all observables before emitting
         * - Creates new PluginState from latest values of each observable upon update to any single stream
         */
        Observable.combineLatest(builder.authStateObservable, builder.lspStateObservable,
                builder.browserCompatibilityStateObservable, builder.webViewAssetStateObservable, PluginState::new)
                .observeOn(Schedulers.computation()).subscribe(this::onEvent);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Handles plugin state changes by refreshing the active view.
     *
     * @param pluginState The current combined state auth and lsp state of the plugin
     */
    @Override
    public void onEvent(final PluginState pluginState) {
        refreshActiveView(pluginState);
    }

    /**
     * Determines and sets the appropriate view based on the order of resolution.
     * View selection follows a priority order:
     * 1. Dependency Missing: can browsers be created.
     * 2. LSP Startup Failed: has the language server initialization failed (not pending/active).
     * 3. Chat UI Asset Missing: have chat assets been fetched and available?
     * 4. Authentication Logged out: if user logged out, needs to login again.
     * 5. Authentication Expired: if auth has expired, needs to be refreshed.
     * 5. Chat View: happy path.
     *
     * @param pluginState The current combined auth and lsp state of the plugin
     */
    private void refreshActiveView(final PluginState pluginState) {
        AmazonQViewType newActiveView;

        if (pluginState.browserCompatibilityState().isDependencyMissing()) {
            newActiveView = AmazonQViewType.DEPENDENCY_MISSING_VIEW;
        } else if (pluginState.lspState().hasFailed()) {
            newActiveView = AmazonQViewType.LSP_STARTUP_FAILED_VIEW;
        } else if (pluginState.webViewAssetState().isDependencyMissing()) {
            newActiveView = AmazonQViewType.CHAT_ASSET_MISSING_VIEW;
        } else if (pluginState.authState().isLoggedOut()) {
            newActiveView = AmazonQViewType.TOOLKIT_LOGIN_VIEW;
        } else if (pluginState.authState().isExpired()) {
            newActiveView = AmazonQViewType.RE_AUTHENTICATE_VIEW;
        } else {
            newActiveView = AmazonQViewType.CHAT_VIEW;
        }

        updateActiveView(newActiveView);
    }

    /**
     * Updates the active view if it has changed and notifies observers of the
     * change.
     *
     * @param newActiveViewId The new view to be activated
     */
    private void updateActiveView(final AmazonQViewType newActiveViewId) {
        if (activeView != newActiveViewId) {
            activeView = newActiveViewId;
            notifyActiveViewChange();
        }
    }

    /**
     * Broadcasts the active view change through the event broker.
     */
    private void notifyActiveViewChange() {
        Activator.getEventBroker().post(AmazonQViewType.class, activeView);
    }

    public static final class Builder {

        private Observable<AuthState> authStateObservable;
        private Observable<LspState> lspStateObservable;
        private Observable<BrowserCompatibilityState> browserCompatibilityStateObservable;
        private Observable<WebViewAssetState> webViewAssetStateObservable;

        public Builder withAuthStateObservable(final Observable<AuthState> authStateObservable) {
            this.authStateObservable = authStateObservable;
            return this;
        }

        public Builder withLspStateObservable(final Observable<LspState> lspStateObservable) {
            this.lspStateObservable = lspStateObservable;
            return this;
        }

        public Builder withBrowserCompatibilityStateObservable(
                final Observable<BrowserCompatibilityState> browserCompatibilityState) {
            this.browserCompatibilityStateObservable = browserCompatibilityState;
            return this;
        }

        public Builder withWebViewAssetStateObservable(final Observable<WebViewAssetState> webViewAssetState) {
            this.webViewAssetStateObservable = webViewAssetState;
            return this;
        }

        public Builder withWebViewAssetStateObservable(final Observable<WebViewAssetState> webViewAssetState) {
            this.webViewAssetState = webViewAssetState;
            return this;
        }

        public ViewRouter build() {
            return new ViewRouter(this);
        }

    }

}
