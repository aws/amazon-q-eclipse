// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public abstract class CallToActionView extends BaseView {
    private String buttonLabel;
    private SelectionListener buttonHandler;

    protected abstract String getButtonLabel();
    protected abstract SelectionListener getButtonHandler();
    protected abstract void setupButtonFooterContent(Composite composite);

    @Override
     protected final void setupView() {
        super.setupView();
        this.buttonLabel = getButtonLabel();
        this.buttonHandler = getButtonHandler();
        setupButton(getContentComposite());
        setupButtonFooterContent(getContentComposite());
    }

    private void setupButton(final Composite composite) {
        var button = new Button(composite, SWT.PUSH);
        button.setText(buttonLabel);
        button.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
        button.addSelectionListener(buttonHandler);
    }
}
