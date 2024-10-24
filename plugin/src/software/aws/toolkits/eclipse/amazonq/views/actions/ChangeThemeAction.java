package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.browser.Browser;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import software.aws.toolkits.eclipse.amazonq.chat.ChatTheme;
import software.aws.toolkits.eclipse.amazonq.util.ThemeDetector;

public final class ChangeThemeAction extends Action {
    private final ThemeDetector themeDetector;
    private final ChatTheme chatTheme;
    private final Browser browser;
    private static final String SWITCH_TO_LIGHT_MODE_TEXT = "Switch to Light Mode";
    private static final String SWITCH_TO_DARK_MODE_TEXT = "Switch to Dark Mode";

    public ChangeThemeAction(final Browser browser) {
        this.themeDetector = new ThemeDetector();
        this.chatTheme = new ChatTheme();
        this.browser = browser;

        String text = SWITCH_TO_DARK_MODE_TEXT;

        if (themeDetector.isDarkTheme()) {
            text = SWITCH_TO_LIGHT_MODE_TEXT;
        }

        updateText(text);
        setImageDescriptor(
                PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_SYNCED));
    }

    @Override
    public void run() {
        if (themeDetector.isDarkTheme()) {
            updateText(SWITCH_TO_DARK_MODE_TEXT);
            themeDetector.setLightModePreference();
        } else {
            updateText(SWITCH_TO_LIGHT_MODE_TEXT);
            themeDetector.setDarkModePreference();
        }

        chatTheme.injectTheme(browser);
        browser.execute("changeTheme(" + themeDetector.isDarkTheme() + ");");
    }

    private void updateText(final String text) {
        setText(text);
        setToolTipText(text);
    }
}
