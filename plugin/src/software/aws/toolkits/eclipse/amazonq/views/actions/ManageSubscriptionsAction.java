package software.aws.toolkits.eclipse.amazonq.views.actions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jface.action.Action;
import org.eclipse.lsp4j.ExecuteCommandParams;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.telemetry.UiTelemetryProvider;

public class ManageSubscriptionsAction extends Action {

    public ManageSubscriptionsAction() {
        setText("Manage Subscriptions");
    }

    @Override
    public void run() {
        UiTelemetryProvider.emitClickEventMetric("manageSubscriptions");
        Activator.getLspProvider().getAmazonQServer()
            .thenAccept(server -> {
                ExecuteCommandParams params = new ExecuteCommandParams();
                params.setCommand("aws/chat/manageSubscription");
                
                server.getWorkspaceService().executeCommand(params).exceptionally(ex -> {
                    // Log error if needed
                    return null;
                });
            });
    }
    
    public void setVisible(final boolean isVisible) {
        super.setEnabled(isVisible);
    }
}
