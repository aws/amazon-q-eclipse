// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public final class AutoTriggerTopLevelListener {

    private IPartListener2 partListener;

    public AutoTriggerTopLevelListener(final IPartListener2 partListener) {
        this.partListener = partListener;
    }

    public void onStart() {
        PlatformUI.getWorkbench().addWindowListener(new IWindowListener() {

            @Override
            public void windowActivated(final IWorkbenchWindow window) {
                window.getPartService().addPartListener(partListener);
            }

            @Override
            public void windowDeactivated(final IWorkbenchWindow window) {
                window.getPartService().removePartListener(partListener);
            }

            @Override
            public void windowClosed(final IWorkbenchWindow window) {
                window.getPartService().removePartListener(partListener);
            }

            @Override
            public void windowOpened(final IWorkbenchWindow window) {
                window.getPartService().addPartListener(partListener);
            }

        });
    }

    public void onShutdown() {
        // TOOD: implement shutdown logic
    }
}
