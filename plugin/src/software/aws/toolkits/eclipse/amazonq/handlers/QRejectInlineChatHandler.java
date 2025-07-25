// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextService;

import software.aws.toolkits.eclipse.amazonq.inlineChat.InlineChatSession;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.Constants;

public class QRejectInlineChatHandler extends AbstractHandler {

    @Override
    public final boolean isEnabled() {
        IContextService contextService = PlatformUI.getWorkbench().getService(IContextService.class);
        var activeContexts = contextService.getActiveContextIds();

        return activeContexts.contains(Constants.INLINE_CHAT_CONTEXT_ID) && InlineChatSession.getInstance().isDeciding();
    }

    @Override
    public final synchronized Object execute(final ExecutionEvent event) throws ExecutionException {
        try {
            InlineChatSession.getInstance().handleDecision(false);
        } catch (Exception e) {
            Activator.getLogger().error("Declining inline chat results failed with: " + e.getMessage(), e);
        }
        return null;
    }
}
