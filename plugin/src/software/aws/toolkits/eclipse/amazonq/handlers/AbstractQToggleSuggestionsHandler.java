// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;

import software.aws.toolkits.eclipse.amazonq.util.QInvocationSession;

public abstract class AbstractQToggleSuggestionsHandler extends AbstractHandler {
    public enum Direction {
        FORWARD, BACKWARD
    }

    private Direction direction = Direction.FORWARD;

    @Override
    public final boolean isEnabled() {
        QInvocationSession qInvocationSessionInstance = QInvocationSession.getInstance();
        return qInvocationSessionInstance != null && !qInvocationSessionInstance.hasBeenTypedahead()
                && qInvocationSessionInstance.isPreviewingSuggestions();
    }

    @Override
    public Object execute(final ExecutionEvent event) {
        QInvocationSession qInvocationSessionInstance = QInvocationSession.getInstance();

        switch (direction) {
        case FORWARD:
            qInvocationSessionInstance.incrementCurentSuggestionIndex();
            break;
        case BACKWARD:
            qInvocationSessionInstance.decrementCurrentSuggestionIndex();
            break;
        }

        return null;
    }

    protected void setCommandDirection(final Direction direction) {
        this.direction = direction;
    }
}
