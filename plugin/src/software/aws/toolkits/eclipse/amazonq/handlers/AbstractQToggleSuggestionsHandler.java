// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

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

    /**
     * Executes the command when the user triggers the handler.
     * <p>
     * Subclasses overriding this method should ensure that the following conditions are met:
     * </p>
     * <ul>
     *     <li>The method should make sure to set a direction of toggle</li>
     *     <li>The method should make sure to call the super's counter part of this method</li>
     * </ul>
     *
     * @param event The execution event that triggered the handler.
     * @return The result of the execution, or <code>null</code> if there is no result.
     *
     * @implSpec
     * Implementations should call {@code super.execute(event)} at the end to delegate the actual movement.
     */
    @Override
    public synchronized Object execute(final ExecutionEvent event) {
        QInvocationSession qInvocationSessionInstance = QInvocationSession.getInstance();

        switch (direction) {
        case FORWARD:
            qInvocationSessionInstance.incrementCurentSuggestionIndex();
            break;
        case BACKWARD:
            qInvocationSessionInstance.decrementCurrentSuggestionIndex();
            break;
        default:
            qInvocationSessionInstance.incrementCurentSuggestionIndex();
        }

        return null;
    }

    protected final void setCommandDirection(final Direction direction) {
        this.direction = direction;
    }
}
