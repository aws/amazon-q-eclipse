// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.ISourceProviderService;

import io.reactivex.rxjava3.disposables.Disposable;
import software.aws.toolkits.eclipse.amazonq.broker.api.EventObserver;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

/**
 * Manages authentication state for the Amazon Q Eclipse plugin toolbar display.
 *
 * This provider is responsible for maintaining the 'is_logged_in' variable state
 * through the org.eclipse.ui.services extension point. The authentication state
 * determines which toolbar icon is displayed:
 *
 * - When authenticated: Shows the standard Amazon Q toolbar icon
 * - When unauthenticated: Shows the unauthenticated state icon
 *
 * @see plugin.xml for extension point configuration
 * @see software.aws.toolkits.eclipse.amazonq.toolbar
 * @see software.aws.toolkits.eclipse.amazonq.toolbar-unauthenticated
 */
public final class AuthSourceProvider extends AbstractSourceProvider implements EventObserver<AuthState> {
    public static final String IS_LOGGED_IN_VARIABLE_ID = "is_logged_in";
    private boolean isLoggedIn = false;
    private Disposable authStateSubscription;

    public AuthSourceProvider() {
        authStateSubscription = Activator.getEventBroker().subscribe(AuthState.class, this);
        isLoggedIn = Activator.getLoginService().getAuthState().isLoggedIn();
    }

    @Override
    public Map<String, Object> getCurrentState() {
        Map<String, Object> state = new HashMap<>();
        state.put(IS_LOGGED_IN_VARIABLE_ID, isLoggedIn);
        return state;
    }

    @Override
    public void dispose() {
        // Reset the authenticated state
        isLoggedIn = false;

        // Notify listeners that this provider is being disposed
        fireSourceChanged(ISources.WORKBENCH, IS_LOGGED_IN_VARIABLE_ID, null);

        if (authStateSubscription != null) {
            authStateSubscription.dispose();
            authStateSubscription = null;
        }
    }

    @Override
    public String[] getProvidedSourceNames() {
        return new String[] {IS_LOGGED_IN_VARIABLE_ID};
    }

    public void setIsLoggedIn(final Boolean isLoggedIn) {
        this.isLoggedIn = isLoggedIn;
        fireSourceChanged(ISources.WORKBENCH, IS_LOGGED_IN_VARIABLE_ID, isLoggedIn);
    }

    public static AuthSourceProvider getProvider() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        ISourceProviderService sourceProviderService = workbench
                .getService(ISourceProviderService.class);
        AuthSourceProvider provider = (AuthSourceProvider) sourceProviderService
                .getSourceProvider(AuthSourceProvider.IS_LOGGED_IN_VARIABLE_ID);
        return provider;
    }

    @Override
    public void onEvent(final AuthState authState) {
        boolean isLoggedIn = authState.isLoggedIn();
        Display.getDefault().asyncExec(() -> {
            setIsLoggedIn(isLoggedIn);
        });
    }
}
