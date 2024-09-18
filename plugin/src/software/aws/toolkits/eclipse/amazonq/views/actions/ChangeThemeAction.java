package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

public final class ChangeThemeAction extends Action {
    private Browser browser;
    private boolean darkMode = Display.isSystemDarkTheme();

    public ChangeThemeAction(final Browser browser) {
        this.browser = browser;
        setText("Change Color");
        setToolTipText("Change the color");
        setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_SYNCED));
    }

    @Override
    public void run() {
        darkMode = !darkMode;
        browser.execute("changeTheme(" + darkMode + ");");
    }

}
