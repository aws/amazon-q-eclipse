// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewSite;

import io.reactivex.rxjava3.disposables.Disposable;
import software.aws.toolkits.eclipse.amazonq.broker.api.EventObserver;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public final class AmazonQLocalCommonActions extends AmazonQAbstractCommonActions implements EventObserver<AuthState> {
    private final Actions actions;
    private final IViewSite viewSite;
    private final Disposable authStateSubscription;
    private IMenuManager menuManager;

    public AmazonQLocalCommonActions(final IViewSite viewSite) {
        this.viewSite = viewSite;

        menuManager = viewSite.getActionBars().getMenuManager();
        actions = new Actions();

        fillPulldown();
        authStateSubscription = Activator.getEventBroker().subscribe(AuthState.class, this);
    }

    @Override
    protected void fillPulldown() {
        addCommonMenuItems(menuManager, actions);
    }

    @Override
    public void onEvent(final AuthState authState) {
        actions.setVisibility(authState);

        Display.getDefault().asyncExec(() -> {
            viewSite.getActionBars().getMenuManager().markDirty();
            viewSite.getActionBars().getMenuManager().update(true);
        });
    }

    public void dispose() {
        if (authStateSubscription != null) {
            authStateSubscription.dispose();
        }

        if (actions != null) {
            actions.dispose();
        }

        if (menuManager != null) {
            menuManager.dispose();
        }
    }

}
