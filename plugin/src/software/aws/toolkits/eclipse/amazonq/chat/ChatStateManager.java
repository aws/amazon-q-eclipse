// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

public final class ChatStateManager {
    private static ChatStateManager instance;
    private Browser browser;
    private Composite dummyParent;
    private volatile boolean hasPreservedState  = false;

    public static synchronized ChatStateManager getInstance() {
        if (instance == null) {
            instance = new ChatStateManager();
        }
        return instance;
    }

    public synchronized Browser getBrowser(final Composite parent) {
        // if browser is null or disposed, return null
        if (browser == null || browser.isDisposed()) {
            return null;
        } else if (browser.getParent() != parent) {
            // Re-parent existing browser
            browser.setParent(parent);
            disposeDummyParent();
        }
        return browser;
    }

    public synchronized void updateBrowser(final Browser browser) {
        // resetting browser indicates that no state is preserved
        hasPreservedState = false;
        this.browser = browser;
    }

    public synchronized boolean hasPreservedState() {
        return hasPreservedState;
    }

    public void preserveBrowser() {
        if (browser != null && !browser.isDisposed()) {
            if (dummyParent == null || dummyParent.isDisposed()) {
                dummyParent = new Composite(
                        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                        SWT.NONE
                    );
                dummyParent.setVisible(false);
            }
            browser.setParent(dummyParent);
            hasPreservedState = true;
        }
    }

    private void disposeDummyParent() {
        if (dummyParent != null && !dummyParent.isDisposed()) {
            dummyParent.dispose();
            dummyParent = null;
        }
    }

    public void dispose() {
        if (browser != null && !browser.isDisposed()) {
            browser.dispose();
            browser = null;
        }
        disposeDummyParent();
        hasPreservedState = false;
    }
}
