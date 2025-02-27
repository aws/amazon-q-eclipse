package software.aws.toolkits.eclipse.amazonq.handlers;

import static software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils.getActiveTextEditor;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Display;

import software.aws.toolkits.eclipse.amazonq.inlineChat.QInlineChatSession;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.Constants;
import software.aws.toolkits.eclipse.amazonq.util.ToolkitNotification;

public class QTriggerInlineChatHandler extends AbstractHandler {

    @Override
    public final boolean isEnabled() {
        return Activator.getLoginService().getAuthState().isLoggedIn();
    }
    @Override
    public final synchronized Object execute(final ExecutionEvent event) throws ExecutionException {
        var editor = getActiveTextEditor();
        if (editor == null) {
            Activator.getLogger().info("Inline Chat triggered with no active editor. Returning.");
            return null;
        }

        if (QInlineChatSession.getInstance().isSessionActive()) {
            if (QInlineChatSession.getInstance().isDeciding() || QInlineChatSession.getInstance().isGenerating()) {
                showErrorNotification();
            }
            Activator.getLogger().info("Inline Chat triggered with existing session active. Returning.");
            return null;
        }

        boolean newSession = false;
        try {
            Activator.getLogger().info("INLINE CHAT TRIGGERED!");
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
    private void showErrorNotification() {
        var notification = new ToolkitNotification(Display.getCurrent(),
                "Amazon Q",
                Constants.INLINE_CHAT_MULTIPLE_TRIGGER_MESSAGE);
        notification.open();
    }
}
