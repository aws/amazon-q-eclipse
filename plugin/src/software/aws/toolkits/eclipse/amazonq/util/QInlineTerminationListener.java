// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;

public final class QInlineTerminationListener implements FocusListener {

    @Override
    public void focusGained(final FocusEvent e) {
        // noop
        return;
    }

    @Override
    public void focusLost(final FocusEvent e) {
        QInvocationSession session = QInvocationSession.getInstance();
        if (session.isActive()) {
            session.endImmediately();
        }
    }
}
