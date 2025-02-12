// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import software.aws.toolkits.eclipse.amazonq.providers.assets.ToolkitLoginWebViewAssetProvider;
import software.aws.toolkits.eclipse.amazonq.providers.assets.WebViewAssetProvider;
import software.aws.toolkits.eclipse.amazonq.views.actions.AmazonQCommonActions;

public final class ToolkitLoginWebview extends AmazonQView {

    public static final String ID = "software.aws.toolkits.eclipse.amazonq.views.ToolkitLoginWebview";

    private AmazonQCommonActions amazonQCommonActions;

    private final WebViewAssetProvider webViewAssetProvider;

    public ToolkitLoginWebview() {
        super();
        webViewAssetProvider = new ToolkitLoginWebViewAssetProvider();
        webViewAssetProvider.initialize();
    }

    @Override
    public Composite setupView(final Composite parent) {
        super.setupView(parent);

        setupParentBackground(parent);
        var result = setupBrowser(parent);

        if (!result) {
            return parent;
        }
        var browser = getBrowser();

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
        addFocusListener(parent, browser);

        amazonQCommonActions = getAmazonQCommonActions();
        setupAmazonQCommonActions();

        return parent;
    }

    @Override
    public void dispose() {
        var browser = getBrowser();
        if (browser != null && !browser.isDisposed()) {
            browser.dispose();
        }
        super.dispose();
    }
}
