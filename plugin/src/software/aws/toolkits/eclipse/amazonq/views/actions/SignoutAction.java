package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.jface.action.Action;

import software.aws.toolkits.eclipse.amazonq.util.AuthStatusChangedListener;
import software.aws.toolkits.eclipse.amazonq.util.AuthUtils;
import software.aws.toolkits.eclipse.amazonq.views.AmazonQView;
import software.aws.toolkits.eclipse.amazonq.views.ToolkitLoginWebview;

public final class SignoutAction extends Action implements AuthStatusChangedListener {
    public SignoutAction() {
        setText("Sign out");
    }

    @Override
    public void run() {
        AuthUtils.invalidateToken();
        AmazonQView.showView(ToolkitLoginWebview.ID);
    }

    public void updateVisibility(final boolean isLoggedIn) {
        this.setEnabled(isLoggedIn);
    }

    @Override
    public void onAuthStatusChanged(final boolean isLoggedIn) {
        updateVisibility(isLoggedIn);
    }
}
