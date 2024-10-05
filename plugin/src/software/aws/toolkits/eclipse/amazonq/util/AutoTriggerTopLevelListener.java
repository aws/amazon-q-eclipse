// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public final class AutoTriggerTopLevelListener<T extends IPartListener2 & IAutoTriggerListener>
        implements IAutoTriggerListener {

    private T partListener;

    public AutoTriggerTopLevelListener(final T partListener) {
        this.partListener = partListener;
    }

    @Override
    public void onStart() {
        PlatformUI.getWorkbench().addWindowListener(new IWindowListener() {

            @Override
            public void windowActivated(final IWorkbenchWindow window) {
                System.out.println("window listener added from activated");
                window.getPartService().addPartListener(partListener);
            }

            @Override
            public void windowDeactivated(final IWorkbenchWindow window) {
                System.out.println("window listener removed from deactivated");
                window.getPartService().removePartListener(partListener);
            }

            @Override
            public void windowClosed(final IWorkbenchWindow window) {
                System.out.println("window listener removed from closed");
                window.getPartService().removePartListener(partListener);
            }

            @Override
            public void windowOpened(final IWorkbenchWindow window) {
                System.out.println("window listener added from opened");
                window.getPartService().addPartListener(partListener);
            }
        });

        // Aside from adding the listeners to the window, we would also need to add the
        // listener actively for the first time
        // Because all of the subscribed events has already happened.
        System.out.println("onStart called from top level listener");
        partListener.onStart();
    }

    @Override
    public void onShutdown() {
        // TOOD: implement shutdown logic
    }
}
