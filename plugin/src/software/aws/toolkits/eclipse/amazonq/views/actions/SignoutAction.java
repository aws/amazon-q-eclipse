package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.jface.action.Action;

import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginDetails;
import software.aws.toolkits.eclipse.amazonq.util.AuthStatusChangedListener;
import software.aws.toolkits.eclipse.amazonq.util.DefaultLoginService;
import software.aws.toolkits.eclipse.amazonq.views.AmazonQView;
import software.aws.toolkits.eclipse.amazonq.views.ToolkitLoginWebview;

public final class SignoutAction extends Action implements AuthStatusChangedListener {
    public SignoutAction() {
        setText("Sign out");
    }

    @Override
    public void run() {
        DefaultLoginService.getInstance().logout();
        AmazonQView.showView(ToolkitLoginWebview.ID);
    }

    public void updateVisibility(final LoginDetails loginDetails) {
        this.setEnabled(loginDetails.getIsLoggedIn());
    }

    @Override
    public void onAuthStatusChanged(final LoginDetails loginDetails) {
        updateVisibility(loginDetails);
    }
}
