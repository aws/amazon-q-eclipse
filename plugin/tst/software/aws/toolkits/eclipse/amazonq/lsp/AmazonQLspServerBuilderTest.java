// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.messages.Message;
import org.eclipse.lsp4j.jsonrpc.messages.RequestMessage;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.FrameworkUtil;

import software.aws.toolkits.eclipse.amazonq.extensions.implementation.ActivatorStaticMockExtension;
import software.aws.toolkits.eclipse.amazonq.lsp.model.AwsExtendedInitializeResult;
import software.aws.toolkits.eclipse.amazonq.lsp.model.AwsServerCapabilities;
import software.aws.toolkits.eclipse.amazonq.telemetry.metadata.ClientMetadata;
import software.aws.toolkits.eclipse.amazonq.telemetry.metadata.PluginClientMetadata;

class AmazonQLspServerBuilderTest {

    @RegisterExtension
    private static ActivatorStaticMockExtension activatorMockExtension = new ActivatorStaticMockExtension();

    @Mock
    private ClientMetadata mockClientMetadata;

    @Mock
    private Launcher<AmazonQLspServer> mockLauncher;

    @Mock
    private AmazonQLspServer mockServer;

    private MockedStatic<Platform> mockPlatform;
    private MockedStatic<FrameworkUtil> mockFrameworkUtil;

    private AmazonQLspServerBuilder builder;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Mock static dependencies for PluginClientMetadata initialization
        mockPlatform = mockStatic(Platform.class, RETURNS_DEEP_STUBS);
        mockPlatform.when(() -> Platform.getProduct().getName()).thenReturn("Eclipse IDE");

        mockFrameworkUtil = mockStatic(FrameworkUtil.class, RETURNS_DEEP_STUBS);
        mockFrameworkUtil.when(() -> FrameworkUtil.getBundle(PluginClientMetadata.class).getVersion().toString())
                .thenReturn("2.3.1.test");

        // Set up mock launcher
        when(mockLauncher.getRemoteProxy()).thenReturn(mockServer);

        builder = new AmazonQLspServerBuilder();

        // Use reflection to set the static launcher field
        Field launcherField = AmazonQLspServerBuilder.class.getDeclaredField("launcher");
        launcherField.setAccessible(true);
        launcherField.set(null, mockLauncher);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mockPlatform != null) {
            mockPlatform.close();
        }
        if (mockFrameworkUtil != null) {
            mockFrameworkUtil.close();
        }

        // Clean up the static launcher field
        Field launcherField = AmazonQLspServerBuilder.class.getDeclaredField("launcher");
        launcherField.setAccessible(true);
        launcherField.set(null, null);
    }

    @Test
    void testGetInitializationOptionsIncludesSubscriptionDetails() {
        // Use reflection to access the private method for testing
        try {
            java.lang.reflect.Method method = AmazonQLspServerBuilder.class
                .getDeclaredMethod("getInitializationOptions", ClientMetadata.class);
            method.setAccessible(true);

            @SuppressWarnings("unchecked")
            Map<String, Object> initOptions = (Map<String, Object>) method.invoke(builder, mockClientMetadata);

            // Verify the structure
            assertNotNull(initOptions);
            assertTrue(initOptions.containsKey("aws"));

            @SuppressWarnings("unchecked")
            Map<String, Object> awsOptions = (Map<String, Object>) initOptions.get("aws");
            assertTrue(awsOptions.containsKey("awsClientCapabilities"));

            @SuppressWarnings("unchecked")
            Map<String, Object> clientCapabilities = (Map<String, Object>) awsOptions.get("awsClientCapabilities");
            assertTrue(clientCapabilities.containsKey("q"));

            @SuppressWarnings("unchecked")
            Map<String, Object> qOptions = (Map<String, Object>) clientCapabilities.get("q");

            // Verify subscriptionDetails is set to true
            assertTrue(qOptions.containsKey("subscriptionDetails"));
            assertEquals(true, qOptions.get("subscriptionDetails"));

            // Verify other expected capabilities are present
            assertTrue(qOptions.containsKey("developerProfiles"));
            assertTrue(qOptions.containsKey("customizationsWithMetadata"));
            assertTrue(qOptions.containsKey("mcp"));
            assertTrue(qOptions.containsKey("pinnedContextEnabled"));
            assertTrue(qOptions.containsKey("modelSelection"));

        } catch (Exception e) {
            throw new RuntimeException("Failed to test getInitializationOptions", e);
        }
    }

    @Test
    void testWrapMessageConsumerHandlesInitializeRequest() {
        // Create a mock initialize request
        RequestMessage initRequest = new RequestMessage();
        initRequest.setMethod("initialize");

        InitializeParams initParams = new InitializeParams();
        initRequest.setParams(initParams);

        // Create a mock consumer
        org.eclipse.lsp4j.jsonrpc.MessageConsumer mockConsumer = mock(org.eclipse.lsp4j.jsonrpc.MessageConsumer.class);

        // Get the wrapped consumer
        org.eclipse.lsp4j.jsonrpc.MessageConsumer wrappedConsumer = builder.wrapMessageConsumer(mockConsumer);

        // Act
        wrappedConsumer.consume(initRequest);

        // Verify the consumer was called
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(mockConsumer).consume(messageCaptor.capture());

        // Verify the message was processed
        Message processedMessage = messageCaptor.getValue();
        assertTrue(processedMessage instanceof RequestMessage);
        RequestMessage processedRequest = (RequestMessage) processedMessage;

        // Verify initialization options were set
        InitializeParams processedParams = (InitializeParams) processedRequest.getParams();
        assertNotNull(processedParams.getInitializationOptions());
        assertNotNull(processedParams.getClientInfo());
    }

    @Test
    void testWrapMessageConsumerHandlesInitializeResponse() {
        // Create a mock initialize response
        ResponseMessage initResponse = new ResponseMessage();
        AwsExtendedInitializeResult result = mock(AwsExtendedInitializeResult.class);
        AwsServerCapabilities serverCapabilities = mock(AwsServerCapabilities.class);

        // Mock the server capabilities to return subscription details
        Map<String, Object> chatOptions = Map.of("subscriptionDetails", true);
        when(serverCapabilities.chatOptions()).thenReturn(chatOptions);
        when(result.getAwsServerCapabilities()).thenReturn(serverCapabilities);

        initResponse.setResult(result);

        // Create a mock consumer
        org.eclipse.lsp4j.jsonrpc.MessageConsumer mockConsumer = mock(org.eclipse.lsp4j.jsonrpc.MessageConsumer.class);

        // Get the wrapped consumer
        org.eclipse.lsp4j.jsonrpc.MessageConsumer wrappedConsumer = builder.wrapMessageConsumer(mockConsumer);

        // Act
        wrappedConsumer.consume(initResponse);

        // Verify the consumer was called
        verify(mockConsumer).consume(initResponse);

        // Verify server capabilities were processed (this would trigger logging in real implementation)
        verify(result, org.mockito.Mockito.atLeastOnce()).getAwsServerCapabilities();
        verify(serverCapabilities, org.mockito.Mockito.atLeastOnce()).chatOptions();
    }

    @Test
    void testWrapMessageConsumerHandlesNonInitializeMessage() {
        // Create a non-initialize message
        RequestMessage otherRequest = new RequestMessage();
        otherRequest.setMethod("textDocument/completion");

        // Create a mock consumer
        org.eclipse.lsp4j.jsonrpc.MessageConsumer mockConsumer = mock(org.eclipse.lsp4j.jsonrpc.MessageConsumer.class);

        // Get the wrapped consumer
        org.eclipse.lsp4j.jsonrpc.MessageConsumer wrappedConsumer = builder.wrapMessageConsumer(mockConsumer);

        // Act
        wrappedConsumer.consume(otherRequest);

        // Verify the consumer was called with the original message
        verify(mockConsumer).consume(otherRequest);
    }

    @Test
    void testWrapMessageConsumerHandlesResponseWithoutAwsExtendedResult() {
        // Create a response without AwsExtendedInitializeResult
        ResponseMessage response = new ResponseMessage();
        response.setResult("some other result");

        // Create a mock consumer
        org.eclipse.lsp4j.jsonrpc.MessageConsumer mockConsumer = mock(org.eclipse.lsp4j.jsonrpc.MessageConsumer.class);

        // Get the wrapped consumer
        org.eclipse.lsp4j.jsonrpc.MessageConsumer wrappedConsumer = builder.wrapMessageConsumer(mockConsumer);

        // Act - Should not throw exception
        wrappedConsumer.consume(response);

        // Verify the consumer was called
        verify(mockConsumer).consume(response);
    }

    @Test
    void testWrapMessageConsumerHandlesChatOptionsAsNonMap() {
        // Create a mock initialize response with chatOptions as non-Map
        ResponseMessage initResponse = new ResponseMessage();
        AwsExtendedInitializeResult result = mock(AwsExtendedInitializeResult.class);
        AwsServerCapabilities serverCapabilities = mock(AwsServerCapabilities.class);

        // Mock chatOptions as a non-Map object
        when(serverCapabilities.chatOptions()).thenReturn("not a map");
        when(result.getAwsServerCapabilities()).thenReturn(serverCapabilities);

        initResponse.setResult(result);

        // Create a mock consumer
        org.eclipse.lsp4j.jsonrpc.MessageConsumer mockConsumer = mock(org.eclipse.lsp4j.jsonrpc.MessageConsumer.class);

        // Get the wrapped consumer
        org.eclipse.lsp4j.jsonrpc.MessageConsumer wrappedConsumer = builder.wrapMessageConsumer(mockConsumer);

        // Act - Should not throw exception
        wrappedConsumer.consume(initResponse);

        // Verify the consumer was called
        verify(mockConsumer).consume(initResponse);
    }
}
