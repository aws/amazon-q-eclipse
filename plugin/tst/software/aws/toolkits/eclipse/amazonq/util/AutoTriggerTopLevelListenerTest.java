// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class AutoTriggerTopLevelListenerTest {
    private static MockedStatic<PlatformUI> platformUIMockStatic;
    private static IWorkbench workbenchMock;
    private static IWorkbenchWindow windowMock;
    private static IAutoTriggerPartListener partListenerMock;
    private static IPartService partServiceMock;

    @BeforeAll
    public static void setUp() {
        platformUIMockStatic = mockStatic(PlatformUI.class);
        workbenchMock = mock(IWorkbench.class);
        platformUIMockStatic.when(PlatformUI::getWorkbench).thenReturn(workbenchMock);
        windowMock = mock(IWorkbenchWindow.class);
        partServiceMock = mock(IPartService.class);
        when(windowMock.getPartService()).thenReturn(partServiceMock);
        when(workbenchMock.getActiveWorkbenchWindow()).thenReturn(windowMock);
        partListenerMock = mock(IAutoTriggerPartListener.class);
    }

    @AfterAll
    public static void tearDown() {
        platformUIMockStatic.close();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    void testOnStart() {
        // Test case:
        // - Window listener is added
        // - Part listener's onStart is called
        AutoTriggerTopLevelListener listener = new AutoTriggerTopLevelListener();
        listener.addPartListener(partListenerMock);

        listener.onStart();
        verify(workbenchMock, times(1)).addWindowListener(any(IWindowListener.class));
        verify(partListenerMock, times(1)).onStart();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    void testOnShutdown() {
        // Test case:
        // - Part listener's onShutdown is called
        // - Window listener is removed
        AutoTriggerTopLevelListener listener = new AutoTriggerTopLevelListener();
        listener.addPartListener(partListenerMock);

        listener.onStart();
        listener.onShutdown();
        verify(partListenerMock, times(1)).onShutdown();
        verify(partServiceMock, times(1)).removePartListener(partListenerMock);
        verify(workbenchMock, times(1)).removeWindowListener(any(IWindowListener.class));
    }
}

// Made for testing only
interface IAutoTriggerPartListener extends IPartListener2, IAutoTriggerListener {

}
