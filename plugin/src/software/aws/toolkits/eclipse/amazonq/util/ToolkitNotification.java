// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.mylyn.commons.ui.dialogs.AbstractNotificationPopup;

public final class ToolkitNotification extends AbstractNotificationPopup {

    private final String title;
    private final String description;
    private Image infoIcon;

    public ToolkitNotification(final Display display, final String title, final String description) {
        super(display);
        this.title = title;
        this.description = description;
    }

    private Image getInfoIcon() {
        ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
        ImageDescriptor imageDescriptor = sharedImages.getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK);
        this.infoIcon = imageDescriptor.createImage();
        return this.infoIcon;
    }

    @Override
    protected void createContentArea(final Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, false));
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        Label iconLabel = new Label(container, SWT.NONE);
        iconLabel.setImage(getInfoIcon());
        iconLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

        Label notificationLabel = new Label(container, SWT.WRAP);
        notificationLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        notificationLabel.setText(this.description);
    }

    @Override
    protected String getPopupShellTitle() {
        return this.title;
    }

    @Override
    protected Point getInitialSize() {
        return new Point(60, 20);
    }

    @Override
    public boolean close() {
        if (this.infoIcon != null && !this.infoIcon.isDisposed()) {
            this.infoIcon.dispose();
        }
        return super.close();
    }
}
