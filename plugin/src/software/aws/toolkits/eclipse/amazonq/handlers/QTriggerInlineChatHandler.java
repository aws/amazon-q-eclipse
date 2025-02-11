package software.aws.toolkits.eclipse.amazonq.handlers;

import static software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils.getActiveTextEditor;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.views.QInlineChatSession;

public class QTriggerInlineChatHandler extends AbstractHandler {

    @Override
    public final boolean isEnabled() {
        // TODO: add logic to only trigger on conditions
        return true;
    }
    @Override
    public final synchronized Object execute(final ExecutionEvent event) throws ExecutionException {
        var editor = getActiveTextEditor();
        if (editor == null) {
            Activator.getLogger().info("Inline Chat triggered with no active editor. Returning.");
            return null;
        }

        if (QInlineChatSession.getInstance().isSessionActive()) {
            Activator.getLogger().info("Inline Chat triggered with existing session active. Returning.");
            return null;
        }

        boolean newSession = false;
        try {
            newSession = QInlineChatSession.getInstance().startSession(editor);
        } catch (Exception e) {
            Activator.getLogger().error("Session start interrupted", e);
        }

        if (!newSession) {
            Activator.getLogger().warn("Failed to start inline chat session.");
            return null;
        }
        
        return null;
    }
}
