// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.ViewPart;

import io.reactivex.rxjava3.disposables.Disposable;
import software.aws.toolkits.eclipse.amazonq.broker.api.EventObserver;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.views.actions.AmazonQStaticActions;
import software.aws.toolkits.eclipse.amazonq.views.router.AmazonQViewType;


public final class AmazonQViewContainer extends ViewPart implements EventObserver<AmazonQViewType> {
    public static final String ID = "software.aws.toolkits.eclipse.amazonq.views.AmazonQViewContainer";

    private Composite parentComposite;
    private StackLayout layout;
    private Map<AmazonQViewType, BaseAmazonQView> views;
    private AmazonQViewType activeViewType;
    private BaseAmazonQView currentView;
    private Disposable activeViewTypeSubscription;
    private final ReentrantLock containerLock;

    public AmazonQViewContainer() {
        activeViewType = AmazonQViewType.CHAT_VIEW;
        containerLock = new ReentrantLock(true);

        Activator.getEventBroker().subscribe(AmazonQViewType.class, this);

        views = Map.of(
                AmazonQViewType.CHAT_ASSET_MISSING_VIEW, new DependencyMissingView(),
                AmazonQViewType.DEPENDENCY_MISSING_VIEW, new ChatAssetMissingView(),
                AmazonQViewType.RE_AUTHENTICATE_VIEW, new ReauthenticateView(),
                AmazonQViewType.LSP_STARTUP_FAILED_VIEW, new LspStartUpFailedView(),
                AmazonQViewType.CHAT_VIEW, new AmazonQChatWebview(),
                AmazonQViewType.TOOLKIT_LOGIN_VIEW, new ToolkitLoginWebview()
        );
    }

    @Override
    public void createPartControl(final Composite parent) {
        layout = new StackLayout();
        parent.setLayout(layout);

        GridLayout gridLayout = new GridLayout(1, false);
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        parent.setLayout(gridLayout);

        parentComposite = parent;

        setupStaticMenuActions();
        updateChildView();
    }

    private void setupStaticMenuActions() {
        new AmazonQStaticActions(getViewSite());
    }

    private void updateChildView() {
        Display.getDefault().asyncExec(() -> {
            containerLock.lock();
            BaseAmazonQView newView = views.get(activeViewType);

            if (currentView != null) {
                Control[] children = parentComposite.getChildren();
                for (Control child : children) {
                    if (child != null && !child.isDisposed()) {
                        child.dispose();
                    }
                }

                currentView.dispose();
            }

            if (activeViewType == AmazonQViewType.CHAT_VIEW || activeViewType == AmazonQViewType.TOOLKIT_LOGIN_VIEW) {
                ((AmazonQView) newView).setViewSite(getViewSite());
            }

            Composite newViewComposite = newView.setupView(parentComposite);
            GridData gridData = new GridData(SWT.CENTER, SWT.CENTER, true, true);
            newViewComposite.setLayoutData(gridData);

            layout.topControl = newViewComposite;
            parentComposite.layout(true, true);

            currentView = newView;
            containerLock.unlock();
        });
    }

    @Override
    public void onEvent(final AmazonQViewType newViewType) {
        if (newViewType.equals(activeViewType) || !views.containsKey(newViewType)) {
          return;
      }

      containerLock.lock();
      activeViewType = newViewType;
      containerLock.unlock();

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
        if (currentView != null) {
            currentView.dispose();
        }

        super.dispose();
    }
}
