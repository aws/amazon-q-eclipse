// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.customization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import software.aws.toolkits.eclipse.amazonq.configuration.customization.CustomizationUtil;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.extensions.implementation.ActivatorStaticMockExtension;
import software.aws.toolkits.eclipse.amazonq.lsp.AmazonQLspServer;
import software.aws.toolkits.eclipse.amazonq.lsp.model.GetConfigurationFromServerParams;
import software.aws.toolkits.eclipse.amazonq.lsp.model.LspServerConfigurations;
import software.aws.toolkits.eclipse.amazonq.providers.lsp.LspProvider;
import software.aws.toolkits.eclipse.amazonq.util.LoggingService;
import org.eclipse.lsp4j.DidChangeConfigurationParams;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.eclipse.lsp4j.services.WorkspaceService;
import software.aws.toolkits.eclipse.amazonq.views.model.Customization;

public final class CustomizationUtilTest {

    @RegisterExtension
    private static ActivatorStaticMockExtension activatorStaticMockExtension = new ActivatorStaticMockExtension();

    private LoggingService loggingServiceMock;
    private LspProvider lspProviderMock;
    private AmazonQLspServer amazonQLspServerMock;
    private WorkspaceService workspaceServiceMock;

    private final class ConfigurationResponse {
        private List<Customization> customizations = Arrays.asList(
                new Customization("arn", "name", "description")
        );

        public List<Customization> getCustomizations() {
            return customizations;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        loggingServiceMock = activatorStaticMockExtension.getMock(LoggingService.class);
        lspProviderMock = activatorStaticMockExtension.getMock(LspProvider.class);

        amazonQLspServerMock = Mockito.mock(AmazonQLspServer.class);
        workspaceServiceMock = Mockito.mock(WorkspaceService.class);

        when(lspProviderMock.getAmazonQServer())
                .thenReturn(CompletableFuture.completedFuture(amazonQLspServerMock));

        when(amazonQLspServerMock.getWorkspaceService())
                .thenReturn(workspaceServiceMock);
    }


    @Test
    void testTriggerChangeConfigurationNotification() {
        CustomizationUtil.triggerChangeConfigurationNotification();

        verify(loggingServiceMock).info("Triggering configuration pull from Amazon Q LSP server");
        verify(workspaceServiceMock).didChangeConfiguration(any(DidChangeConfigurationParams.class));
    }

    @Test
    void testTriggerChangeConfigurationNotificationWithException() {
        RuntimeException testException = new RuntimeException("Test exception");
        doThrow(testException)
                .when(lspProviderMock).getAmazonQServer();

        assertThrows(AmazonQPluginException.class, CustomizationUtil::triggerChangeConfigurationNotification);

        verify(loggingServiceMock).error(
                eq("Error occurred while sending change configuration notification to Amazon Q LSP server"),
                eq(testException)
        );
    }

    @Test
    void testListCustomizations() {
        Customization validCustomization = new Customization("arn", "name", "description");
        Customization invalidCustomization = new Customization("", "", "");
        Customization otherValidCustomization = new Customization("arn2", "name2", "description2");

        LspServerConfigurations testConfigurationResponse = new LspServerConfigurations(List.of(validCustomization,
                invalidCustomization, otherValidCustomization));

        when(amazonQLspServerMock.getConfigurationFromServer(any(GetConfigurationFromServerParams.class)))
                .thenReturn(CompletableFuture.completedFuture(testConfigurationResponse));

        CompletableFuture<List<Customization>> testFuture = CustomizationUtil.listCustomizations();
        List<Customization> result = testFuture.join();

        assertFalse(result.isEmpty());
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(c -> c.getName() != null && !c.getName().isEmpty()));
    }

    @Test
    void testListCustomizationWithException() {
        when(amazonQLspServerMock.getConfigurationFromServer(any(GetConfigurationFromServerParams.class)))
                .thenThrow(new RuntimeException("Test exception"));

        CompletableFuture<List<Customization>> future = CustomizationUtil.listCustomizations();

        CompletionException thrown = assertThrows(CompletionException.class, future::join);
        assertInstanceOf(AmazonQPluginException.class, thrown.getCause());

        verify(loggingServiceMock).error(eq("Error occurred while fetching the list of customizations"),
                any(Throwable.class));
    }

}
