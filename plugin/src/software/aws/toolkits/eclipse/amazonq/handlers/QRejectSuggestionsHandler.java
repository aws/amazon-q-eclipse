// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import software.aws.toolkits.eclipse.amazonq.util.QInvocationSession;

public class QRejectSuggestionsHandler extends AbstractHandler {

    @Override
    public final boolean isEnabled() {
        return QInvocationSession.getInstance().isActive();
    }

    @Override
    public final Object execute(final ExecutionEvent event) throws ExecutionException {
        QInvocationSession.getInstance().transitionToDecisionMade();
        QInvocationSession.getInstance().endImmediately();
        return null;
    }
}
