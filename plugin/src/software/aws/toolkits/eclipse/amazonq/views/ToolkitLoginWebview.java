// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import software.aws.toolkits.eclipse.amazonq.broker.api.EventObserver;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.providers.assets.ToolkitLoginWebViewAssetProvider;
import software.aws.toolkits.eclipse.amazonq.providers.assets.WebViewAssetProvider;
import software.aws.toolkits.eclipse.amazonq.views.actions.AmazonQViewCommonActions;
import software.aws.toolkits.eclipse.amazonq.views.model.UpdateRedirectUrlCommand;

public final class ToolkitLoginWebview extends AmazonQView implements EventObserver<UpdateRedirectUrlCommand> {

    public static final String ID = "software.aws.toolkits.eclipse.amazonq.views.ToolkitLoginWebview";

    private AmazonQViewCommonActions amazonQCommonActions;
    private Browser browser;
    private final WebViewAssetProvider webViewAssetProvider;

    public ToolkitLoginWebview() {
        super();
        webViewAssetProvider = new ToolkitLoginWebViewAssetProvider();
        webViewAssetProvider.initialize();
        Activator.getEventBroker().subscribe(UpdateRedirectUrlCommand.class, this);
    }

    @Override
    public Composite setupView(final Composite parent) {
        super.setupView(parent);
        setupParentBackground(parent);

        browser = getAndAttachBrowser(parent);

        if (browser == null || browser.isDisposed()) {
            browser = setupBrowser(parent);
            if (browser == null) {
                return parent;
            }

            browser.setVisible(false);
            browser.addProgressListener(new ProgressAdapter() {
                @Override
                public void completed(final ProgressEvent event) {
                    Display.getDefault().asyncExec(() -> {
                        if (!browser.isDisposed()) {
                            browser.setVisible(true);
                        }
                    });
                }
            });

            webViewAssetProvider.injectAssets(browser);
        }

        addFocusListener(parent, browser);
        amazonQCommonActions = getAmazonQCommonActions();
        setupAmazonQCommonActions();

        parent.addDisposeListener(e -> this.preserveBrowser());

        return parent;
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    public void onEvent(final UpdateRedirectUrlCommand redirectUrlCommand) {
        Display.getDefault().asyncExec(() -> {
            var browser = getBrowser();
            String command = "ideClient.updateRedirectUrl('" + redirectUrlCommand.redirectUrl() + "')";
            browser.execute(command);
        });
    }
}
