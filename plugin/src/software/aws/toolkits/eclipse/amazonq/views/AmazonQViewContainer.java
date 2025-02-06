// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.ViewPart;

import software.aws.toolkits.eclipse.amazonq.broker.api.EventObserver;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.views.actions.AmazonQStaticActions;
import software.aws.toolkits.eclipse.amazonq.views.router.AmazonQViewType;


public final class AmazonQViewContainer extends ViewPart implements EventObserver<AmazonQViewType> {
    private Composite parentComposite;
    private StackLayout layout;
    private Map<AmazonQViewType, BaseAmazonQView> views;
    private AmazonQViewType activeId;
    private BaseAmazonQView activeView;

    public AmazonQViewContainer() {
        Activator.getEventBroker().subscribe(AmazonQViewType.class, this);
    }

    /* Router should be initialized, then init view container
     * ViewRouter.Initialize()
     * viewcontainer.init(activeViewId)
     */

    /*
     * When container is disposed and being reopened, class will be recreated
     * need to call showView to reset viewContainer && follow up with init call to container
     * 1. showView(viewContainer.ID)
     * 2. viewContainer.init(currentView)
     */

    public void initializeViews(final AmazonQViewType currentActiveViewId) {

        //init map containing all views
        var dependencyMissingView = new DependencyMissingView();
        var chatAssetMissingView = new ChatAssetMissingView();
        var reAuthView = new ReauthenticateView();
        var lspFailedView = new LspStartUpFailedView();
        views = Map.of(
                AmazonQViewType.CHAT_ASSET_MISSING_VIEW, chatAssetMissingView,
                AmazonQViewType.DEPENDENCY_MISSING_VIEW, dependencyMissingView,
                AmazonQViewType.RE_AUTHENTICATE_VIEW, reAuthView,
                AmazonQViewType.LSP_STARTUP_FAILED_VIEW, lspFailedView
                );

        //default view passed in from router
        //possible we'll use chatView as default?
        activeId = currentActiveViewId;
    }

    public void createPartControl(final Composite parent) {
        parentComposite = parent;
        layout = new StackLayout();
        parent.setLayout(layout);

        //add base stylings
        GridLayout gridLayout = new GridLayout(1, false);
        gridLayout.marginLeft = 20;
        gridLayout.marginRight = 20;
        gridLayout.marginTop = 10;
        gridLayout.marginBottom = 10;
        parent.setLayout(gridLayout);

        setupStaticMenuActions();
        updateChildView();
    }

    /* change methodology for setupMenuActions -- move outside of viewContainer?
     * will need ability to switch between the two based on which view is displaying && authState
     * if viewId = static view --> new AmazonQStaticActions(getViewSite());
     * if viewId = common view --> new AmazonQCommonActions(AuthState, getViewSite());
     */
    private void setupStaticMenuActions() {
        new AmazonQStaticActions(getViewSite());
    }

    private void updateChildView() {
        Display.getDefault().asyncExec(() -> {
        	BaseAmazonQView newView = views.get(activeId);
        	
            if (activeView != null) {
                activeView.dispose();
                if (layout.topControl != null) {
                    layout.topControl.dispose();
                }
            }

            Composite newViewComposite = newView.setupView(parentComposite);
            GridData gridData = new GridData(SWT.CENTER, SWT.CENTER, true, true);
            newViewComposite.setLayoutData(gridData);

            layout.topControl = newViewComposite;
            parentComposite.layout(true, true);

            activeView = newView;
        });
    }

    @Override
    public void onEvent(final AmazonQViewType newViewId) {
      if (newViewId.equals(activeId) || !views.containsKey(newViewId)) {
          return;
      }
      activeId = newViewId;

      if (!parentComposite.isDisposed()) {
          updateChildView();
      }
    }

    @Override
    public void setFocus() {
        parentComposite.setFocus();

    }

    @Override
    public void dispose() {
        if (activeView != null) {
            activeView.dispose();
        }
        super.dispose();
    }
}
