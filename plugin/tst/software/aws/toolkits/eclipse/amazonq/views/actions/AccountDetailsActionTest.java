// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import software.aws.toolkits.eclipse.amazonq.extensions.implementation.ActivatorStaticMockExtension;
import software.aws.toolkits.eclipse.amazonq.lsp.AmazonQLspServer;
import software.aws.toolkits.eclipse.amazonq.providers.lsp.LspProvider;
import software.aws.toolkits.eclipse.amazonq.telemetry.UiTelemetryProvider;

class AccountDetailsActionTest {

    @RegisterExtension
    private static ActivatorStaticMockExtension activatorMockExtension = new ActivatorStaticMockExtension();

    @Mock
    private AmazonQLspServer mockLspServer;

    @Mock
    private WorkspaceService mockWorkspaceService;

    private MockedStatic<UiTelemetryProvider> mockUiTelemetryProvider;

    private AccountDetailsAction accountDetailsAction;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mock static UiTelemetryProvider
        mockUiTelemetryProvider = mockStatic(UiTelemetryProvider.class);

        // Set up the mock chain: Activator -> LspProvider -> AmazonQLspServer -> WorkspaceService
        when(activatorMockExtension.getMock(LspProvider.class).getAmazonQServer())
            .thenReturn(CompletableFuture.completedFuture(mockLspServer));
        when(mockLspServer.getWorkspaceService()).thenReturn(mockWorkspaceService);
        when(mockWorkspaceService.executeCommand(any(ExecuteCommandParams.class)))
            .thenReturn(CompletableFuture.completedFuture(null));

        accountDetailsAction = new AccountDetailsAction();
    }

    @AfterEach
    void tearDown() {
        if (mockUiTelemetryProvider != null) {
            mockUiTelemetryProvider.close();
        }
    }

    @Test
    void testConstructor() {
        // Test that the action is properly initialized
        assertEquals("Account Details", accountDetailsAction.getText());
        assertEquals("View account details and subscription information", accountDetailsAction.getToolTipText());
    }

    @Test
    void testRunExecutesSubscriptionShowCommand() {
        // Act
        accountDetailsAction.run();

        // Verify telemetry is emitted
        mockUiTelemetryProvider.verify(() -> UiTelemetryProvider.emitClickEventMetric("accountDetails"));

        // Capture the command parameters
        ArgumentCaptor<ExecuteCommandParams> commandCaptor = ArgumentCaptor.forClass(ExecuteCommandParams.class);
        verify(mockWorkspaceService).executeCommand(commandCaptor.capture());

        // Verify the command details
        ExecuteCommandParams capturedCommand = commandCaptor.getValue();
        assertEquals("aws/chat/subscription/show", capturedCommand.getCommand());
        assertEquals(Collections.emptyList(), capturedCommand.getArguments());
    }

    @Test
    void testRunHandlesLspServerException() {
        // Arrange - Make getLspProvider throw an exception
        when(activatorMockExtension.getMock(LspProvider.class).getAmazonQServer())
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("LSP server unavailable")));

        // Act - Should not throw exception
        accountDetailsAction.run();

        // Verify telemetry is still emitted
        mockUiTelemetryProvider.verify(() -> UiTelemetryProvider.emitClickEventMetric("accountDetails"));
    }

    @Test
    void testRunHandlesCommandExecutionException() {
        // Arrange - Make executeCommand throw an exception
        when(mockWorkspaceService.executeCommand(any(ExecuteCommandParams.class)))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Command execution failed")));

        // Act - Should not throw exception
        accountDetailsAction.run();

        // Verify telemetry is emitted and command is attempted
        mockUiTelemetryProvider.verify(() -> UiTelemetryProvider.emitClickEventMetric("accountDetails"));
        verify(mockWorkspaceService).executeCommand(any(ExecuteCommandParams.class));
    }

    @Test
    void testSetVisible() {
        // Test the setVisible method
        accountDetailsAction.setVisible(true);
        // Note: setVisible calls setEnabled internally, which is inherited behavior
        // We can't easily test the enabled state without more complex mocking
    }

    @Test
    void testRunUsesProperLspServerAccess() {
        // Act
        accountDetailsAction.run();

        // Verify the proper LSP server access pattern is used
        verify(activatorMockExtension.getMock(LspProvider.class)).getAmazonQServer();
        verify(mockLspServer).getWorkspaceService();
    }
}
