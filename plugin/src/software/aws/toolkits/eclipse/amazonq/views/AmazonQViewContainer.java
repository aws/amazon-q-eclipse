// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.part.ViewPart;

public class AmazonQViewContainer extends ViewPart {
    private Composite parentComposite;
    private StackLayout layout;
    private Map<String, BaseAmazonQView> views = new HashMap<>();
    String activeId;
    BaseAmazonQView activeView;

    public AmazonQViewContainer() {
        initializeViews();
    }
    public void initializeViews() {
        //init map containing all views
        var testViewA = new SomeTestView();
        var testViewB = new SomeOtherTestView();
        views.put(ViewConstants.TEST_VIEW_A_ID, testViewA);
        views.put(ViewConstants.TEST_VIEW_B_ID, testViewB);
    }

    public final void createPartControl(final Composite parent) {
        // ===== TEMPORARY TEST SETUP - TO BE REPLACED WITH EVENT BUS =====
        // this nested container structure is only for testing view switching functionality
        // once event bus + view router implemented:
        // 1. remove temporary container and button
        // 2. single StackLayout on parent
        // 3. view switching handled through event bus subscription
        parentComposite = new Composite(parent, SWT.NONE);
        GridLayout mainLayout = new GridLayout(1, false);
        parentComposite.setLayout(mainLayout);

        // Temporary test button - will be removed
        var button = new Button(parentComposite, SWT.PUSH);
        button.setText("Switch View");
        button.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));

        // Temporary nested container for StackLayout
        Composite stackContainer = new Composite(parentComposite, SWT.NONE);
        stackContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        layout = new StackLayout();
        stackContainer.setLayout(layout);

        // Create initial view in stack container
        Composite viewAComposite = views.get(ViewConstants.TEST_VIEW_A_ID).setupView(stackContainer);
        layout.topControl = viewAComposite;
        activeId = ViewConstants.TEST_VIEW_A_ID;

        // Temporary button listener - will be replaced by event bus subscription
        button.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(final Event event) {
                String newViewId = activeId.equals(ViewConstants.TEST_VIEW_A_ID)
                    ? ViewConstants.TEST_VIEW_B_ID
                    : ViewConstants.TEST_VIEW_A_ID;

                BaseAmazonQView newView = views.get(newViewId);
                layout.topControl = newView.setupView(stackContainer);
                activeId = newViewId;
                stackContainer.layout();
            }
        });
    }


    @Override
    public final void setFocus() {
        parentComposite.setFocus();

    }
}
