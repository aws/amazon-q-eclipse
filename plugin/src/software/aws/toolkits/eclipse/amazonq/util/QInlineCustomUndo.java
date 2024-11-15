package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public final class QInlineCustomUndo extends AbstractHandler {

    @Override
    public Object execute(final ExecutionEvent event) throws ExecutionException {
        System.out.println("Custom undo hit");
        return null;
    }
}
