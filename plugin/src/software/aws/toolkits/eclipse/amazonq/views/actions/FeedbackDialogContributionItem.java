// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.swt.widgets.Shell;

import jakarta.inject.Inject;
import software.aws.toolkits.eclipse.amazonq.views.DialogContributionItem;
import software.aws.toolkits.eclipse.amazonq.views.FeedbackDialog;

public final class FeedbackDialogContributionItem {
    private static final String SHARE_FEEDBACK_MENU_ITEM_TEXT = "Share Feedback...";

    @Inject
    private Shell shell;

    private DialogContributionItem feedbackDialogContributionItem;

    public FeedbackDialogContributionItem() {
        feedbackDialogContributionItem = new DialogContributionItem(
                new FeedbackDialog(shell),
                SHARE_FEEDBACK_MENU_ITEM_TEXT
        );
    }

    public void setVisible(final boolean isVisible) {
        feedbackDialogContributionItem.setVisible(isVisible);
    }

    public DialogContributionItem getDialogContributionItem() {
        return feedbackDialogContributionItem;
    }

}
