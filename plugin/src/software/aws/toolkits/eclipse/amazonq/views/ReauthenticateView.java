// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import java.util.concurrent.CompletableFuture;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;

import software.aws.toolkits.eclipse.amazonq.lsp.auth.AuthStatusChangedListener;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.AuthStatusProvider;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.Constants;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;
import software.aws.toolkits.eclipse.amazonq.util.ThreadingUtils;
import software.aws.toolkits.eclipse.amazonq.views.actions.SignoutAction;


public final class ReauthenticateView extends CallToActionView implements AuthStatusChangedListener {
    public static final String ID = "software.aws.toolkits.eclipse.amazonq.views.ReauthenticateView";

    private static final String ICON_PATH = "icons/AmazonQ64.png";
    private static final String HEADER_LABEL = "Connection to Amazon Q Expired";
    private static final String DETAIL_MESSAGE = "Please re-authenticate to continue";
    private static final String BUTTON_LABEL = "Re-authenticate";
    private static final String LINK_LABEL = "Sign out";

    public ReauthenticateView() {
         // It is necessary for this view to be an `AuthStatusChangedListener` to switch the view back to Q Chat after the authentication
         // flow is successful. Without this listener, the re-authentication will succeed but the view will remain present.
        AuthStatusProvider.addAuthStatusChangeListener(this);
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
    protected String getButtonLabel() {
        return BUTTON_LABEL;
    }

    @Override
    protected SelectionListener getButtonHandler() {
        return new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                ThreadingUtils.executeAsyncTask(() -> {
                    try {
                        boolean loginOnInvalidToken = true;
                        Activator.getLoginService().reAuthenticate(loginOnInvalidToken).get();
                    } catch (Exception ex) {
                        PluginUtils.showErrorDialog("Amazon Q", Constants.RE_AUTHENTICATE_FAILURE_MESSAGE);
                        Activator.getLogger().error("Failed to re-authenticate", ex);
                    }
                });
            }
        };
    }

    @Override
    protected void setupButtonFooterContent(final Composite composite) {
        Link hyperlink = new Link(composite, SWT.NONE);
        hyperlink.setText("<a>" + LINK_LABEL + "</a>");
        hyperlink.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
        hyperlink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                SignoutAction signoutAction = new SignoutAction();
                signoutAction.run();
            }
        });
    }

    @Override
    public void onAuthStatusChanged(final AuthState authState) {
        Display.getDefault().asyncExec(() -> {
            if (authState.isLoggedIn()) {
                ViewVisibilityManager.showChatView();
            }
        });
    }

    @Override
    public void dispose() {
        AuthStatusProvider.removeAuthStatusChangeListener(this);
    }

    @Override
    protected CompletableFuture<Boolean> isViewDisplayable() {
        return CompletableFuture.completedFuture(Activator.getLoginService().getAuthState().isExpired());
    }

    @Override
    protected void showAlternateView() {
        ViewVisibilityManager.showChatView();
    }
}
