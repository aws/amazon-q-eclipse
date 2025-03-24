// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import software.aws.toolkits.eclipse.amazonq.chat.models.GenericCommandVerb;

public class QRefactorHandler extends AbstractQChatEditorActionsHandler {

    @Override
    public final Object execute(final ExecutionEvent event) throws ExecutionException {
        executeGenericCommand(GenericCommandVerb.Refactor.getValue());
        return null;
    }
}

