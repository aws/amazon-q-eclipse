// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;

public final class AutoTriggerDocumentListener implements IDocumentListener {

    @Override
    public void documentAboutToBeChanged(final DocumentEvent e) {
        return;
    }

    @Override
    public void documentChanged(final DocumentEvent e) {
        if (!shouldSendQuery(e)) {
            return;
        }
        // TODO: implement querying logic
    }

    private boolean shouldSendQuery(final DocumentEvent e) {
        if (e.getText().length() <= 0) {
            return false;
        }
        
        // TODO: implement other logic to prevent unnecessary firing

        return true;
    }
}
