// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class QSendToPromptHandler extends AbstractQChatEditorActionsHandler {

    @Override
    public final Object execute(final ExecutionEvent event) throws ExecutionException {
        executeSendToPromptCommand();
        return null;
    }
}

