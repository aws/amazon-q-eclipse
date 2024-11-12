package software.aws.toolkits.eclipse.amazonq.views;

import java.util.Set;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import software.aws.toolkits.eclipse.amazonq.chat.ChatStateManager;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public final class ViewVisibilityManager {
    private ViewVisibilityManager() {
        // prevent instantiation
    }

    private static final String TOOLKIT_LOGIN_VIEW = ToolkitLoginWebview.ID;
    private static final String CHAT_VIEW = AmazonQChatWebview.ID;
    private static final String DEPENDENCY_MISSING_VIEW = DependencyMissingView.ID;
    private static final String RE_AUTHENTICATE_VIEW = ReauthenticateView.ID;
    private static final String CHAT_ASSET_MISSING_VIEW = ChatAssetMissingView.ID;
    private static final String CODE_REFERENCE_VIEW = AmazonQCodeReferenceView.ID;
    private static final String ERROR_LOG_VIEW = "org.eclipse.pde.runtime.LogView";

    private static final Set<String> MUTUALLY_EXCLUSIVE_VIEWS = Set.of(
            TOOLKIT_LOGIN_VIEW,
            CHAT_VIEW,
            DEPENDENCY_MISSING_VIEW,
            RE_AUTHENTICATE_VIEW,
            CHAT_ASSET_MISSING_VIEW
    );

    public static void showLoginView() {
        showMutuallyExclusiveView(TOOLKIT_LOGIN_VIEW);
    }

    public static void showChatView() {
        showMutuallyExclusiveView(CHAT_VIEW);
    }

    public static void showDependencyMissingView() {
        showMutuallyExclusiveView(DEPENDENCY_MISSING_VIEW);
    }

    public static void showReAuthView() {
        showMutuallyExclusiveView(RE_AUTHENTICATE_VIEW);
    }

    public static void showChatAssetMissingView() {
        showMutuallyExclusiveView(CHAT_ASSET_MISSING_VIEW);
    }

    public static void showCodeReferenceView() {
        showView(CODE_REFERENCE_VIEW);
    }

    public static void showErrorLogView() {
        showView(ERROR_LOG_VIEW);
    }

    private static void showMutuallyExclusiveView(final String viewId) {
        if (!MUTUALLY_EXCLUSIVE_VIEWS
        .contains(viewId)) {
            Activator.getLogger().error("Failed to show view. You must add the view " + viewId + " to MUTUALLY_EXCLUSIVE_VIEWS Set");
            return;
        }

        Display.getDefault().execute(() -> {
            IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            if (window != null) {
                IWorkbenchPage page = window.getActivePage();
                if (page != null) {
                    // Hide other Views
                    IViewReference[] viewReferences = page.getViewReferences();
                    for (IViewReference viewRef : viewReferences) {
                        if (MUTUALLY_EXCLUSIVE_VIEWS.contains(viewRef.getId()) && !viewRef.getId().equalsIgnoreCase(viewId)) {
                            // if Q chat view is being hidden to show a different Amazon Q view
                            // clear chat preserved state
                            if (viewRef.getId().equalsIgnoreCase(AmazonQChatWebview.ID)) {
                                ChatStateManager.getInstance().dispose();
                            }
                            try {
                                page.hideView(viewRef);
                            } catch (Exception e) {
                                Activator.getLogger().error("Error occurred while hiding view " + viewId, e);
                            }
                        }
                    }
                    // Show requested view
                    try {
                        page.showView(viewId);
                        Activator.getLogger().info("Showing view " + viewId);
                    } catch (Exception e) {
                        Activator.getLogger().error("Error occurred while showing view " + viewId, e);
                    }
                }
            }
        });
    }

    private static void showView(final String viewId) {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                try {
                    page.showView(viewId);
                    Activator.getLogger().info("Showing view " + viewId);
                } catch (PartInitException e) {
                    Activator.getLogger().error("Error occurred while opening view " + viewId, e);
                }
            }
        }
    }
}
