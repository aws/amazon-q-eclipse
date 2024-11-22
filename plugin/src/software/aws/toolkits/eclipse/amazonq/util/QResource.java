// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.ui.services.IDisposable;

// TODO: May use it later
class QResource implements IDisposable {
    private final List<QResource> children = new ArrayList<>();
    private boolean isDisposed = false;

    public void addChild(final QResource child) {
        if (isDisposed) {
            throw new IllegalStateException("Cannot add a child to a disposed resource");
        }
        children.add(child);
    }

    @Override
    public void dispose() {
        if (!isDisposed) {
            isDisposed = true;
            for (QResource child : children) {
                child.dispose();
            }
            // Add actual resource disposal logic here
        }
    }

    public boolean isDisposed() {
        return isDisposed;
    }

    public static void register(final QResource parent, final QResource child) {
        parent.addChild(child);
    }
}
