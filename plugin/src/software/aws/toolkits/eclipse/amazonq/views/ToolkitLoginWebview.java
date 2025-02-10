// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import software.aws.toolkits.eclipse.amazonq.providers.assets.ToolkitLoginWebViewAssetProvider;
import software.aws.toolkits.eclipse.amazonq.providers.assets.WebViewAssetProvider;
import software.aws.toolkits.eclipse.amazonq.telemetry.UiTelemetryProvider;
import software.aws.toolkits.eclipse.amazonq.views.actions.AmazonQCommonActions;

public final class ToolkitLoginWebview extends AmazonQView {

    public static final String ID = "software.aws.toolkits.eclipse.amazonq.views.ToolkitLoginWebview";

    private AmazonQCommonActions amazonQCommonActions;

    private final WebViewAssetProvider webViewAssetProvider;
    private final ViewCommandParser commandParser;
    private final ViewActionHandler actionHandler;

    private boolean isViewVisible = false;

    public ToolkitLoginWebview() {
        super();
        this.commandParser = new LoginViewCommandParser();
        this.actionHandler = new LoginViewActionHandler();
        this.webViewAssetProvider = new ToolkitLoginWebViewAssetProvider();
    }

    @Override
    public Composite setupView(final Composite parent) {
        super.setupView(parent);

        setupParentBackground(parent);
        var result = setupBrowser(parent);
        // if setup of amazon q view fails due to missing webview dependency, switch to
        // that view
        // and don't setup rest of the content
        if (!result) {
            return parent;
        }
        var browser = getBrowser();

        browser.setVisible(isViewVisible);
        browser.addProgressListener(new ProgressAdapter() {
            @Override
            public void completed(final ProgressEvent event) {
                Display.getDefault().asyncExec(() -> {
                    if (!browser.isDisposed()) {
                        isViewVisible = true;
                        browser.setVisible(isViewVisible);
                    }
                });
            }
        });

        new BrowserFunction(browser, ViewConstants.COMMAND_FUNCTION_NAME) {
            @Override
            public Object function(final Object[] arguments) {
                commandParser.parseCommand(arguments)
                        .ifPresent(command -> actionHandler.handleCommand(command, browser));
                return null;
            }
        };
        new BrowserFunction(browser, "telemetryEvent") {
            @Override
            public Object function(final Object[] arguments) {
                String clickEvent = (String) arguments[0];
                UiTelemetryProvider.emitClickEventMetric("auth_" + clickEvent);
                return null;
            }
        };

        amazonQCommonActions = getAmazonQCommonActions();
        browser.setText(webViewAssetProvider.getContent().get());

        return parent;
    }

    @Override
    public void dispose() {
        webViewAssetProvider.dispose();
        var browser = getBrowser();
        if (browser != null && !browser.isDisposed()) {
            browser.dispose();
        }
        super.dispose();
    }
}
