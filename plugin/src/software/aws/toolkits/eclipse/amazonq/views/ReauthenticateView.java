// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;


import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.telemetry.UiTelemetryProvider;
import software.aws.toolkits.eclipse.amazonq.util.Constants;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;
import software.aws.toolkits.eclipse.amazonq.util.ThreadingUtils;
import software.aws.toolkits.eclipse.amazonq.views.actions.SignoutAction;


public final class ReauthenticateView extends CallToActionView {
    public static final String ID = "software.aws.toolkits.eclipse.amazonq.views.ReauthenticateView";

    private static final String ICON_PATH = "icons/AmazonQ64.png";
    private static final String HEADER_LABEL = "Connection to Amazon Q Expired";
    private static final String DETAIL_MESSAGE = "Please re-authenticate to continue";
    private static final String BUTTON_LABEL = "Re-authenticate";
    private static final String LINK_LABEL = "Sign out";

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
                UiTelemetryProvider.emitClickEventMetric("auth_reAuthenticateButton");
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
    protected void updateButtonStyle(final Button button) {
        resizeButtonFont(button, 16);

        Color backgroundColor = new Color(51, 118, 205);
        Color foregroundColor = new Color(255, 255, 255);

        button.addListener(SWT.Paint, e -> {
            button.setBackground(backgroundColor);
            button.setForeground(foregroundColor);

            button.setAlignment(SWT.CENTER);

            GridData gridData = (GridData) button.getLayoutData();
            if (gridData != null) {
                gridData.widthHint = 400;
                gridData.heightHint = 50;
                gridData.verticalIndent = 10;
                gridData.horizontalAlignment = SWT.CENTER;

                // Ensure proper layout update
                Composite current = button.getParent();
                while (current != null) {
                    current.layout(true, true);
                    current = current.getParent();
                }
            }
        });
    }

    private void resizeButtonFont(final Button button, final int newFontSize) {
        Font currentFont = button.getFont();
        FontData[] fontData = currentFont.getFontData();
        for (FontData fd : fontData) {
            fd.setHeight(newFontSize);
        }
        Font newFont = new Font(button.getDisplay(), fontData);
        button.setFont(newFont);
        button.addDisposeListener(e -> newFont.dispose());
    }

}
