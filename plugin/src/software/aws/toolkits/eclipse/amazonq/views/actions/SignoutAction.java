package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.jface.action.Action;

import software.aws.toolkits.eclipse.amazonq.customization.CustomizationUtil;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginDetails;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.AuthStatusChangedListener;
import software.aws.toolkits.eclipse.amazonq.util.Constants;
import software.aws.toolkits.eclipse.amazonq.util.ThreadingUtils;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;
import software.aws.toolkits.eclipse.amazonq.views.AmazonQView;
import software.aws.toolkits.eclipse.amazonq.views.ToolkitLoginWebview;

public final class SignoutAction extends Action implements AuthStatusChangedListener {
    public SignoutAction() {
        setText("Sign out");
    }

    @Override
    public void run() {
        ThreadingUtils.executeAsyncTask(() -> {
            try {
                LoginDetails loginDetails = Activator.getLoginService().getLoginDetails().get();
                if (loginDetails.getIsLoggedIn()) {
                    Activator.getLoginService().logout().get();
                    Activator.getLogger().info("Signed out of Amazon q");
                    Activator.getPluginStore().remove(Constants.CUSTOMIZATION_STORAGE_INTERNAL_KEY);
                    ThreadingUtils.executeAsyncTask(() -> CustomizationUtil.triggerChangeConfigurationNotification());
                    AmazonQView.showView(ToolkitLoginWebview.ID);
                }
            } catch (Exception e) {
                PluginUtils.showErrorDialog("Amazon Q", "An error occurred while attempting to sign out of Amazon Q. Please try again.");
                Activator.getLogger().error("Failed to sign out", e);
                return;
            }
        });
    }

    public void updateVisibility(final LoginDetails loginDetails) {
        this.setEnabled(loginDetails.getIsLoggedIn());
    }

    @Override
    public void onAuthStatusChanged(final LoginDetails loginDetails) {
        updateVisibility(loginDetails);
    }
}
