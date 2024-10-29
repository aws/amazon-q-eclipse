// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionItem;
import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionReference;

import static software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils.shouldIndentVertically;

public class QInlineRendererListener implements PaintListener {

    private PopupDialog popup;

    @Override
    public final void paintControl(final PaintEvent e) {
        var qInvocationSessionInstance = QInvocationSession.getInstance();
        if (!qInvocationSessionInstance.isPreviewingSuggestions()) {
            return;
        }

        var gc = e.gc;
        var widget = qInvocationSessionInstance.getViewer().getTextWidget();
        var invocationLine = widget.getLineAtOffset(qInvocationSessionInstance.getInvocationOffset());
        var segments = qInvocationSessionInstance.getSegments();
        var caretLine = widget.getLineAtOffset(widget.getCaretOffset());
        int numSuggestionLines = qInvocationSessionInstance.getNumSuggestionLines();

        if (shouldIndentVertically(widget, caretLine) && qInvocationSessionInstance.isPreviewingSuggestions()) {
            Point textExtent = gc.stringExtent(" ");
            int height = textExtent.y * (numSuggestionLines - (caretLine - invocationLine) - 1);
            qInvocationSessionInstance.setVerticalIndent(caretLine + 1, height);
        } else if (caretLine + 1 == (invocationLine + numSuggestionLines)) {
            qInvocationSessionInstance.unsetVerticalIndent(caretLine + 1);
        }

        for (int i = 0; i < segments.size(); i++) {
            segments.get(i).render(gc, widget.getCaretOffset());
        }
    }

    public final void onNewSuggestion() {
        if (popup != null) {
            popup.close();
        }
        QInvocationSession session = QInvocationSession.getInstance();
        InlineCompletionItem currentSuggestion = session.getCurrentSuggestion();
        InlineCompletionReference[] referencesForCurrentSuggestion = currentSuggestion.getReferences();
        if (referencesForCurrentSuggestion == null || referencesForCurrentSuggestion.length == 0) {
            return;
        }
        var widget = session.getViewer().getTextWidget();
        popup = new PopupDialog(widget.getShell(), PopupDialog.INFOPOPUPRESIZE_SHELLSTYLE, false, false, false, false, false,
                null, null) {
            @Override
            protected Point getInitialLocation(final Point initialSize) {
                Point location = widget.getLocationAtOffset(session.getInvocationOffset());
                location.y -= widget.getLineHeight() * 1.1;
                return widget.toDisplay(location);
            }

            @Override
            protected Control createDialogArea(final Composite parent) {
                Composite composite = (Composite) super.createDialogArea(parent);

                // Add a label to display the message
                Label infoLabel = new Label(composite, SWT.NONE);
                StringBuffer licenseNames = new StringBuffer();
                for (int i = 0; i < referencesForCurrentSuggestion.length; i++) {
                    licenseNames.append(referencesForCurrentSuggestion[i].getLicenseName());
                    if (i != referencesForCurrentSuggestion.length - 1) {
                        licenseNames.append(" + ");
                    }
                }
                int currentSuggestionNumber = session.getCurrentSuggestionNumber();
                int totalNumberOfSuggestions = session.getNumberOfSuggestions();
                String tipToDisplay = "Suggestion (License: " + licenseNames.toString() + ") " + currentSuggestionNumber
                        + " / " + totalNumberOfSuggestions;
                infoLabel.setText(tipToDisplay);

                return composite;
            }
        };
        popup.open();
    }

    public final void beforeRemoval() {
        if (popup != null) {
            popup.close();
        }
    }
}
