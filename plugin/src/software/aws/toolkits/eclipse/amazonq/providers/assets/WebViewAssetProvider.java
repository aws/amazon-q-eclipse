// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.providers.assets;

import java.util.Optional;

import org.eclipse.swt.browser.Browser;

public abstract class WebViewAssetProvider {

    public abstract void injectAssets(final Browser browser);

    public abstract Optional<String> getContent();

    public abstract void dispose();

    protected final String getWaitFunction() {
        return """
                function waitForFunction(functionName, timeout = 30000) {
                    return new Promise((resolve, reject) => {
                        const startTime = Date.now();
                        const checkFunction = () => {
                            if (typeof window[functionName] === 'function') {
                                resolve(window[functionName]);
                            } else if (Date.now() - startTime > timeout) {
                                reject(new Error(`Timeout waiting for ${functionName}`));
                            } else {
                                setTimeout(checkFunction, 100);
                            }
                        };
                        checkFunction();
                    });
                }
                """;
    }

    protected final void disableBrowserContextMenu(final Browser browser) {
        browser.execute("document.oncontextmenu = e => e.preventDefault();");
    }

}
