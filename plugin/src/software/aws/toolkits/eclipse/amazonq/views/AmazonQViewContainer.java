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

import io.reactivex.rxjava3.disposables.Disposable;
import software.aws.toolkits.eclipse.amazonq.broker.api.EventObserver;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.views.actions.AmazonQStaticActions;
import software.aws.toolkits.eclipse.amazonq.views.router.ViewId;


public class AmazonQViewContainer extends ViewPart implements EventObserver<ViewId> {
    private Composite parentComposite;
    private StackLayout layout;
    private Map<ViewId, BaseAmazonQView> views;
    private ViewId activeId;
    private BaseAmazonQView activeView;
    private Disposable viewChangeEventSubscription;

    public AmazonQViewContainer() {
        viewChangeEventSubscription = Activator.getEventBroker().subscribe(ViewId.class, this);
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

    public void initializeViews(ViewId currentActiveViewId) {

        //init map containing all views
        var dependencyMissingView = new DependencyMissingView();
        var chatAssetMissingView = new ChatAssetMissingView();
        var reAuthView = new ReauthenticateView();
        views = Map.of(
                ViewId.CHAT_ASSET_MISSING_VIEW, chatAssetMissingView,
                ViewId.DEPENDENCY_MISSING_VIEW, dependencyMissingView,
                ViewId.RE_AUTHENTICATE_VIEW, reAuthView);

        //default view passed in from router
        //possible we'll use chatView as default?
        activeView = views.get(currentActiveViewId);
    }

    public final void createPartControl(final Composite parent) {
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
        updateChildView(activeView, ViewId.RE_AUTHENTICATE_VIEW);
    }

    /* change methodology for setupMenuActions -- move outside of viewContainer?
     * will need ability to switch between the two based on which view is displaying && authState
     * if viewId = static view --> new AmazonQStaticActions(getViewSite());
     * if viewId = common view --> new AmazonQCommonActions(AuthState, getViewSite());
     */
    private void setupStaticMenuActions() {
        new AmazonQStaticActions(getViewSite());
    }

    private void updateChildView(BaseAmazonQView newView, ViewId newViewId) {
        Display.getDefault().asyncExec(() -> {

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
            activeId = newViewId;
        });
    }

    @Override
    public void onEvent(final ViewId newViewId) {
          if (activeId != null && activeId.equals(newViewId)) {
          return;
      }

      if (views.containsKey(newViewId)) {
          BaseAmazonQView newView = views.get(newViewId);
          if (!parentComposite.isDisposed()) {
              updateChildView(newView, newViewId);
          }
      }
    }

    @Override
    public final void setFocus() {
        parentComposite.setFocus();

    }

    @Override
    public void dispose() {
        viewChangeEventSubscription.dispose();
        if (activeView != null) {
            activeView.dispose();
        }
        super.dispose();
    }
}
