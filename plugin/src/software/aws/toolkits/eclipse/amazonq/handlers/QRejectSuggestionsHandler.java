// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextService;

import software.aws.toolkits.eclipse.amazonq.util.Constants;
import software.aws.toolkits.eclipse.amazonq.util.QInvocationSession;

public class QRejectSuggestionsHandler extends AbstractHandler {

    @Override
    public final boolean isEnabled() {
        IContextService contextService = PlatformUI.getWorkbench()
                .getService(IContextService.class);
        var activeContexts = contextService.getActiveContextIds();
        return activeContexts.contains(Constants.INLINE_SUGGESTIONS_CONTEXT_ID) && QInvocationSession.getInstance().isActive();
    }

    @Override
    public final Object execute(final ExecutionEvent event) throws ExecutionException {
        QInvocationSession.getInstance().transitionToDecisionMade();
        QInvocationSession.getInstance().endImmediately();
        return null;
    }
}
