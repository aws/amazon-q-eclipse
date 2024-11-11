package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewSite;

import jakarta.inject.Inject;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginDetails;
import software.aws.toolkits.eclipse.amazonq.util.AuthStatusChangedListener;
import software.aws.toolkits.eclipse.amazonq.views.DialogContributionItem;
import software.aws.toolkits.eclipse.amazonq.views.FeedbackDialog;

public final class FeedbackDialogContributionItem implements AuthStatusChangedListener {
    private static final String SHARE_FEEDBACK_MENU_ITEM_TEXT = "Share Feedback...";

    @Inject
    private Shell shell;
    private IViewSite viewSite;

    private DialogContributionItem feedbackDialogContributionItem;

    public FeedbackDialogContributionItem(final IViewSite viewSite) {
        this.viewSite = viewSite;
        feedbackDialogContributionItem = new DialogContributionItem(
                new FeedbackDialog(shell),
                SHARE_FEEDBACK_MENU_ITEM_TEXT
        );
    }

    public void updateVisibility(final LoginDetails loginDetails) {
        feedbackDialogContributionItem.setVisible(loginDetails.getIsLoggedIn());
        Display.getDefault().asyncExec(() -> {
            viewSite.getActionBars().getMenuManager().markDirty();
            viewSite.getActionBars().getMenuManager().update(true);
        });
    }

    public DialogContributionItem getDialogContributionItem() {
        return feedbackDialogContributionItem;
    }

    @Override
    public void onAuthStatusChanged(final LoginDetails loginDetails) {
        updateVisibility(loginDetails);
    }
}
