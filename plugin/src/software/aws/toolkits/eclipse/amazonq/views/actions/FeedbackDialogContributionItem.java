package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PlatformUI;

import jakarta.inject.Inject;
import software.aws.toolkits.eclipse.amazonq.util.AuthStatusChangedListener;
import software.aws.toolkits.eclipse.amazonq.views.DialogContributionItem;
import software.aws.toolkits.eclipse.amazonq.views.FeedbackDialog;

public class FeedbackDialogContributionItem implements AuthStatusChangedListener {
    private static final String SHARE_FEEDBACK_MENU_ITEM_TEXT = "Share Feedback";
    
    @Inject
    private Shell shell;
    private IViewSite viewSite;
    
    DialogContributionItem feedbackDialogContributionItem;
    
    public FeedbackDialogContributionItem(IViewSite viewSite) {
        this.viewSite = viewSite;
        feedbackDialogContributionItem = new DialogContributionItem(
                new FeedbackDialog(shell),
                SHARE_FEEDBACK_MENU_ITEM_TEXT,
                PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_LCL_LINKTO_HELP)
        );
    }
    
    public void updateVisibility(final boolean isLoggedIn) {
        feedbackDialogContributionItem.setVisible(isLoggedIn);
        Display.getDefault().asyncExec(() -> {
            viewSite.getActionBars().getMenuManager().markDirty();
            viewSite.getActionBars().getMenuManager().update(true);
        });
    }
    
    public DialogContributionItem getDialogContributionItem() {
        return feedbackDialogContributionItem;
    }

    @Override
    public void onAuthStatusChanged(boolean isLoggedIn) {
        updateVisibility(isLoggedIn);
    }
}
