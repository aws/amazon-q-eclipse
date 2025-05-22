// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.providers.assets;

import org.eclipse.swt.browser.Browser;

public abstract class WebViewAssetProvider {

    public abstract void injectAssets(Browser browser);

    public abstract void initialize();
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

}
