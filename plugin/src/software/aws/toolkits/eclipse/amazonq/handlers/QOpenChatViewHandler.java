package software.aws.toolkits.eclipse.amazonq.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.views.AmazonQChatWebview;

public class QOpenChatViewHandler extends AbstractHandler {
    @Override
    public final Object execute(ExecutionEvent event) {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                try {
                    page.showView(AmazonQChatWebview.ID);
                } catch (PartInitException e) {
                    Activator.getLogger().error("Error occurred while opening Amazon Q view", e);
                }
            }
        }
        return null;
    }
}
