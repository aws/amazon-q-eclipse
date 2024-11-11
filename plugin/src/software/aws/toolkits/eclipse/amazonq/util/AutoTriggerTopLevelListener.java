// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public final class AutoTriggerTopLevelListener<T extends IPartListener2 & IAutoTriggerListener>
        implements IAutoTriggerListener {

    private T partListener;
    private IWindowListener windowListener;
    private IWorkbenchWindow activeWindow;

    public AutoTriggerTopLevelListener() {

    }

    public AutoTriggerTopLevelListener(final T partListener) {
        this.partListener = partListener;
    }

    public void addPartListener(final T partListener) {
        this.partListener = partListener;
    }

    public T getPartListener() {
        return partListener;
    }

    @Override
    public void onStart() {
        windowListener = new IWindowListener() {

            @Override
            public void windowActivated(final IWorkbenchWindow window) {
                activeWindow = window;
                window.getPartService().addPartListener(partListener);
            }

            @Override
            public void windowDeactivated(final IWorkbenchWindow window) {
                // noop
            }

            @Override
            public void windowClosed(final IWorkbenchWindow window) {
                partListener.onShutdown();
                window.getPartService().removePartListener(partListener);
            }

            @Override
            public void windowOpened(final IWorkbenchWindow window) {
                activeWindow = window;
                window.getPartService().addPartListener(partListener);
            }
        };
        PlatformUI.getWorkbench().addWindowListener(windowListener);
        activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

        // Aside from adding the listeners to the window, we would also need to add the
        // listener actively for the first time
        // Because all of the subscribed events has already happened.
        partListener.onStart();
    }

    @Override
    public void onShutdown() {
        partListener.onShutdown();
        if (activeWindow != null) {
            activeWindow.getPartService().removePartListener(partListener);
        }
        PlatformUI.getWorkbench().removeWindowListener(windowListener);
    }
}
