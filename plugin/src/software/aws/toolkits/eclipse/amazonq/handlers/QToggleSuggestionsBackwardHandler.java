// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.handlers;

import org.eclipse.core.commands.ExecutionEvent;

public class QToggleSuggestionsBackwardHandler extends AbstractQToggleSuggestionsHandler {
    // Actual command handler logic consolidated in parent class
    @Override
    public final Object execute(final ExecutionEvent event) {
        super.setCommandDirection(Direction.BACKWARD);

        return super.execute(event);
    }
}
