// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPartService;
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

        // Similar to how part listener needs to tell its child listener (i.e. document
        // listener), the part listener needs to actively be attached to the part that
        // contains the editors.
        // Without this, because the window listener is attached after the subscribed
        // events has already happened, the part listener will not be attached unless
        // you trigger one of the subscribed events
        Display.getDefault().timerExec(1000, new Runnable() {
            @Override
            public void run() {
                IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                if (activeWindow == null) {
                    Display.getDefault().timerExec(1000, this);
                    return;
                }
                IPartService partService = activeWindow.getPartService();
                if (partService == null) {
                    Display.getDefault().timerExec(1000, this);
                    return;
                }
                partService.addPartListener(partListener);
            }
        });

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
