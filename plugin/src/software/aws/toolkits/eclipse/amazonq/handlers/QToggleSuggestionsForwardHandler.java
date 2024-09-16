// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.handlers;

import org.eclipse.core.commands.ExecutionEvent;

public class QToggleSuggestionsForwardHandler extends AbstractQToggleSuggestionsHandler {
    // Actual command handler logic consolidated in parent class
    @Override
    public final Object execute(final ExecutionEvent event) {
        super.setCommandDirection(Direction.FORWARD);

        return super.execute(event);
    }
}
