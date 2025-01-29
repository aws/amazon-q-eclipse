// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import software.aws.toolkits.eclipse.amazonq.util.ChatAssetProvider;

public final class ChatAssetMissingView extends BaseAmazonQView {
    public static final String ID = "software.aws.toolkits.eclipse.amazonq.views.ChatAssetMissingView";

    private static final String ICON_PATH = "icons/AmazonQ64.png";
    private static final String HEADER_LABEL = "Error loading Q chat.";
    private static final String DETAIL_MESSAGE = "Restart Eclipse or review error logs for troubleshooting";
    private ChatAssetProvider chatAssetProvider;
    private Image icon;
    private Composite container;

    public ChatAssetMissingView() {
        this.chatAssetProvider = new ChatAssetProvider();
    }

    @Override
    public Composite setupView(final Composite parentComposite) {
        container = new Composite(parentComposite, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 20;
        layout.marginHeight = 10;
        container.setLayout(layout);

        Label iconLabel = new Label(container, SWT.NONE);
        icon = loadImage(ICON_PATH);
        if (icon != null) {
            iconLabel.setImage(icon);
            iconLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));

            iconLabel.addDisposeListener(e -> {
                if (icon != null && !icon.isDisposed()) {
                    icon.dispose();
                }
            });
        }

        Label headerLabel = new Label(container, SWT.CENTER | SWT.WRAP);
        headerLabel.setText(HEADER_LABEL);
        headerLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        var font = magnifyFontSize(parentComposite, parentComposite.getFont(), 18);
        headerLabel.setFont(font);

        headerLabel.addDisposeListener(e -> {
            if (font != null && !font.isDisposed()) {
                font.dispose();
            }
        });

        Label detailLabel = new Label(container, SWT.CENTER | SWT.WRAP);
        detailLabel.setText(DETAIL_MESSAGE);
        detailLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        return container;
    }

    @Override
    public void dispose() {
        if (chatAssetProvider != null) {
            chatAssetProvider.dispose();
            chatAssetProvider = null;
        }
    }
}
