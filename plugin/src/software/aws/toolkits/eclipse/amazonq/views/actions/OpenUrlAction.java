package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.jface.action.Action;

import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;
import software.aws.toolkits.eclipse.amazonq.views.model.ExternalLink;

public class OpenUrlAction extends Action {
    private String url;

    public OpenUrlAction(final String actionText, final ExternalLink link) {
        setText(actionText);
        this.url = link.getValue();
    }

    @Override
    public final void run() {
        PluginUtils.openWebpage(this.url);
    }
}
