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

import software.aws.toolkits.eclipse.amazonq.views.actions.AmazonQStaticActions;


public class AmazonQViewContainer extends ViewPart {
    private Composite parentComposite;
    private StackLayout layout;
    private Map<String, BaseAmazonQView> views;
    private String activeId;
    private BaseAmazonQView activeView;

    public AmazonQViewContainer() {
        initializeViews();
        //Activator.getBroker().subscribe(ViewId.class, this);
    }

    /*
     * ViewRouter.Initialize()
     * viewcontainer.init(activeViewId)
     */

    //1. showView(viewContainer.ID)
    //2. viewContainer.init(currentView)

    public void initializeViews() {

        //init map containing all views
        var dependencyMissingView = new DependencyMissingView();
        var chatAssetMissingView = new ChatAssetMissingView();
        var reAuthView = new ReauthenticateView();
        views = Map.of(
                ViewConstants.CHAT_ASSET_MISSING_VIEW_ID, chatAssetMissingView,
                ViewConstants.DEPENDENCY_MISSING_VIEW_ID, dependencyMissingView,
                ViewConstants.REAUTHENTICATE_VIEW_ID, reAuthView);

        //default view
        activeView = views.get(ViewConstants.REAUTHENTICATE_VIEW_ID);

        //activeView == current activeView
        //chatView as defaultView?

        //query router --> grab current activeView

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
        updateChildView(activeView, ViewConstants.REAUTHENTICATE_VIEW_ID);
    }

    /* change methodology for setupMenuActions -- move outside of viewContainer?
     * will need ability to switch between the two based on which view is displaying && authState
     * if viewId = static view --> new AmazonQStaticActions(getViewSite());
     * if viewId = common view --> new AmazonQCommonActions(AuthState, getViewSite());
     */
    private void setupStaticMenuActions() {
        new AmazonQStaticActions(getViewSite());
    }

    private void updateChildView(BaseAmazonQView newView, String newViewId) {
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

    public void onChangeViewEvent(String newViewId) {
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
        if (activeView != null) {
            activeView.dispose();
        }
        super.dispose();
    }
}
