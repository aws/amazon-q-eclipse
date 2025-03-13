package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISources;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.IMenuService;
import org.eclipse.ui.services.IEvaluationService;

import software.aws.toolkits.eclipse.amazonq.broker.api.EventObserver;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public class AmazonQGlobalCommonActions extends AmazonQAbstractCommonActions implements EventObserver<AuthState> {

    private static final AmazonQGlobalCommonActions INSTANCE;
    private Actions actions;

    static {
        INSTANCE = new AmazonQGlobalCommonActions();
    }

    private AmazonQGlobalCommonActions() {
        actions = new Actions();
        fillPulldown();
        Activator.getEventBroker().subscribe(AuthState.class, this);
    }

    public static AmazonQGlobalCommonActions getInstance() {
        return INSTANCE;
    }

    @Override
    protected void fillPulldown() {
        Display.getDefault().asyncExec(() -> {
            final IMenuService menuService = PlatformUI.getWorkbench().getService(IMenuService.class);

            var contributionFactory = new MenuContributionFactory(
                    "software.aws.toolkits.eclipse.amazonq.toolbar.command");

            IMenuManager menuManager = new MenuManager();
            menuManager.add(actions.openQChatAction);
            addCommonMenuItems(menuManager, actions);
            contributionFactory.addContributionItemsFromMenu(menuManager);

            menuService.addContributionFactory(contributionFactory);
        });
    }

    @Override
    public void onEvent(final AuthState authState) {
        Display.getDefault().asyncExec(() -> {
            actions.setVisibility(authState);

            IEvaluationService evalService = PlatformUI.getWorkbench().getService(IEvaluationService.class);
            evalService.requestEvaluation(ISources.ACTIVE_MENU_NAME);
        });
    }

}
