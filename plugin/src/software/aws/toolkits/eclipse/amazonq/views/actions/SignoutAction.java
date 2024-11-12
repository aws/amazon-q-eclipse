package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.jface.action.Action;

import software.aws.toolkits.eclipse.amazonq.customization.CustomizationUtil;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.AuthStatusChangedListener;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.Constants;
import software.aws.toolkits.eclipse.amazonq.util.ThreadingUtils;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;
import software.aws.toolkits.eclipse.amazonq.views.AmazonQView;
import software.aws.toolkits.eclipse.amazonq.views.ToolkitLoginWebview;
import software.aws.toolkits.eclipse.amazonq.telemetry.UiTelemetryProvider;

public final class SignoutAction extends Action implements AuthStatusChangedListener {
    public SignoutAction() {
        setText("Sign out");
    }

    @Override
    public void run() {
        UiTelemetryProvider.emitClickEventMetric("auth_signOut");
        ThreadingUtils.executeAsyncTask(() -> {
            try {
                AuthState authState = Activator.getLoginService().getAuthState();
                if (!authState.isLoggedOut()) {
                    Activator.getLoginService().logout().get();
                }
            } catch (Exception e) {
                PluginUtils.showErrorDialog("Amazon Q", "An error occurred while attempting to sign out of Amazon Q. Please try again.");
                Activator.getLogger().error("Failed to sign out", e);
                return;
            }

            Activator.getLogger().info("Signed out of Amazon q");
            Activator.getPluginStore().remove(Constants.CUSTOMIZATION_STORAGE_INTERNAL_KEY);
            ThreadingUtils.executeAsyncTask(() -> CustomizationUtil.triggerChangeConfigurationNotification());
            AmazonQView.showView(ToolkitLoginWebview.ID);
        });
    }

    public void updateVisibility(final AuthState authState) {
        this.setEnabled(authState.isLoggedIn());
    }

    @Override
    public void onAuthStatusChanged(final AuthState authState) {
        updateVisibility(authState);
    }
}
