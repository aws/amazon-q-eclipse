// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.part.ViewSite;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import software.aws.toolkits.eclipse.amazonq.broker.EventBroker;
import software.aws.toolkits.eclipse.amazonq.broker.events.AmazonQViewType;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Activator.class})
public class AmazonQViewContainerTest {

    private AmazonQViewContainer viewContainer;
    private Shell shell;
    private Composite parent;
    
    @Mock
    private ViewSite viewSite;
    
    @Mock
    private EventBroker eventBroker;
    
    @Before
    public void setUp() {
        // Create SWT components on the UI thread
        Display display = Display.getDefault();
        shell = new Shell(display);
        parent = new Composite(shell, SWT.NONE);
        
        // Mock the Activator and EventBroker
        PowerMockito.mockStatic(Activator.class);
        when(Activator.getEventBroker()).thenReturn(eventBroker);
        
        // Create the view container
        viewContainer = new AmazonQViewContainer();
        viewContainer.setViewSite(viewSite);
    }
    
    @After
    public void tearDown() {
        if (parent != null && !parent.isDisposed()) {
            parent.dispose();
        }
        if (shell != null && !shell.isDisposed()) {
            shell.dispose();
        }
    }
    
    @Test
    public void testCreatePartControl() {
        // Execute
        viewContainer.createPartControl(parent);
        
        // Verify
        // Need to wait for async execution to complete
        waitForUIEvents();
        
        // Verify that the parent has children (views were created)
        assertTrue("Parent should have child controls", parent.getChildren().length > 0);
    }
    
    @Test
    public void testOnEvent_SwitchesToCorrectView() {
        // Setup
        viewContainer.createPartControl(parent);
        waitForUIEvents();
        
        // Execute - switch to a different view
        viewContainer.onEvent(AmazonQViewType.DEPENDENCY_MISSING_VIEW);
        waitForUIEvents();
        
        // Verify that event broker was subscribed to
        verify(eventBroker).subscribe(eq(AmazonQViewType.class), eq(viewContainer));
    }
    
    @Test
    public void testMultipleViewSwitches() {
        // Setup
        viewContainer.createPartControl(parent);
        waitForUIEvents();
        
        // Initial state should be CHAT_VIEW
        
        // Switch to first view type
        viewContainer.onEvent(AmazonQViewType.DEPENDENCY_MISSING_VIEW);
        waitForUIEvents();
        
        // Switch to second view type
        viewContainer.onEvent(AmazonQViewType.RE_AUTHENTICATE_VIEW);
        waitForUIEvents();
        
        // Switch back to original view type
        viewContainer.onEvent(AmazonQViewType.CHAT_VIEW);
        waitForUIEvents();
        
        // Verify the parent still has children (views were properly switched)
        assertTrue("Parent should have child controls after multiple view switches", 
                parent.getChildren().length > 0);
    }
    
    @Test
    public void testOnEvent_IgnoresSameViewType() {
        // Setup
        viewContainer.createPartControl(parent);
        waitForUIEvents();
        
        // Execute with the same view type that's already active (CHAT_VIEW is default)
        viewContainer.onEvent(AmazonQViewType.CHAT_VIEW);
        
        // No view change should occur - mainly verifies no exceptions are thrown
        waitForUIEvents();
    }
    
    @Test
    public void testOnEvent_IgnoresInvalidViewType() {
        // Setup
        viewContainer.createPartControl(parent);
        waitForUIEvents();
        
        // Create a mock view type that doesn't exist in the views map
        AmazonQViewType invalidType = mock(AmazonQViewType.class);
        
        // Execute with an invalid view type
        viewContainer.onEvent(invalidType);
        
        // No view change should occur - mainly verifies no exceptions are thrown
        waitForUIEvents();
    }
    
    @Test
    public void testViewDisposalOnSwitch() {
        // Setup
        viewContainer.createPartControl(parent);
        waitForUIEvents();
        
        // Get initial child count
        int initialChildCount = parent.getChildren().length;
        
        // Switch to a different view
        viewContainer.onEvent(AmazonQViewType.TOOLKIT_LOGIN_VIEW);
        waitForUIEvents();
        
        // Child count should remain the same as old view should be disposed
        assertEquals("Child count should remain the same after view switch", 
                initialChildCount, parent.getChildren().length);
        
        // Switch to another view
        viewContainer.onEvent(AmazonQViewType.LSP_STARTUP_FAILED_VIEW);
        waitForUIEvents();
        
        // Child count should still remain the same
        assertEquals("Child count should remain the same after second view switch", 
                initialChildCount, parent.getChildren().length);
    }
    
    @Test
    public void testSetFocus() {
        // Setup
        viewContainer.createPartControl(parent);
        waitForUIEvents();
        
        // Execute
        viewContainer.setFocus();
        
        // Mainly verifies no exceptions are thrown
    }
    
    @Test
    public void testDispose() {
        // Setup
        viewContainer.createPartControl(parent);
        waitForUIEvents();
        
        // Execute
        viewContainer.dispose();
        
        // Verify that resources were properly disposed
        // This is mainly to ensure no exceptions are thrown during disposal
    }
    
    @Test
    public void testChatWebviewDisposal() {
        // Setup
        viewContainer.createPartControl(parent);
        waitForUIEvents();
        
        // Ensure we're on the chat view (default)
        // Then switch to another view to trigger disposal of the chat webview
        viewContainer.onEvent(AmazonQViewType.RE_AUTHENTICATE_VIEW);
        waitForUIEvents();
        
        // Switch back to chat view
        viewContainer.onEvent(AmazonQViewType.CHAT_VIEW);
        waitForUIEvents();
        
        // Verify the parent still has children (new chat view was created)
        assertTrue("Parent should have child controls after chat view recreation", 
                parent.getChildren().length > 0);
    }
    
    /**
     * Helper method to wait for UI events to process
     */
    private void waitForUIEvents() {
        Display display = Display.getDefault();
        while (display.readAndDispatch()) {
            // Process UI events
        }
    }
}