// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.PluginPlatform;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;

public final class DependencyMissingView extends CallToActionView {
    public static final String ID = "software.aws.toolkits.eclipse.amazonq.views.DependencyMissingView";

    private static final String ICON_PATH = "icons/AmazonQ64.png";
    private static final String BUTTON_LABEL = "Install";
    private static final String LINK_LABEL = "Why is this required?";
    private static final String EDGE_INSTALL = "https://go.microsoft.com/fwlink/p/?LinkId=2124703";
    private static final String WEBKIT_INSTALL = "https://webkitgtk.org/";
    private static final String EDGE_LEARN_MORE =
            "https://git.eclipse.org/r/plugins/gitiles/platform/eclipse.platform.swt/+/refs/heads/master/bundles/org.eclipse.swt/Readme.WebView2.md";
    private static final String WEBKIT_LEARN_MORE =
            "https://git.eclipse.org/r/plugins/gitiles/platform/eclipse.platform.swt/+/refs/heads/master/bundles/org.eclipse.swt/Readme.Linux.md";


    private PluginPlatform platform;

    public DependencyMissingView() {
        platform = PluginUtils.getPlatform();
    }

    @Override
    protected String getIconPath() {
        return ICON_PATH;
    }

    @Override
    protected String getHeaderLabel() {
        return String.format("Amazon Q requires %s, install and restart Eclipse to proceed", getDependency());
    }

    @Override
    protected String getDetailMessage() {
        return String.format("Installing this package will provide access to %s within the Amazon Q extension.", getDependency());
    }

    @Override
    protected String getButtonLabel() {
        return BUTTON_LABEL;
    }

    @Override
    protected SelectionListener getButtonHandler() {
        String url = getInstallUrl();
        return openSelectionInWeb(url);
    }

    @Override
    protected void setupButtonFooterContent(final Composite composite) {
        String url = getLearnMoreUrl();
        Link hyperlink = new Link(composite, SWT.NONE);
        hyperlink.setText("<a>" + LINK_LABEL + "</a>");
        hyperlink.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
        hyperlink.addSelectionListener(openSelectionInWeb(url));
    }

    private String getInstallUrl() {
        return platform == PluginPlatform.WINDOWS ? EDGE_INSTALL : WEBKIT_INSTALL;

    }

    private String getLearnMoreUrl() {
        return platform == PluginPlatform.WINDOWS ? EDGE_LEARN_MORE : WEBKIT_LEARN_MORE;
    }

    private SelectionAdapter openSelectionInWeb(final String url) {
        return new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                try {
                    if (url != null) {
                        PluginUtils.openWebpage(url);
                    }
                } catch (Exception ex) {
                    Activator.getLogger().error("Error occured when attempting open url", ex);
                }
            }
        };
    }

    private String getDependency() {
        return PluginUtils.getPlatform() == PluginPlatform.WINDOWS ? "WebView2" : "WebKit";
    }
}
