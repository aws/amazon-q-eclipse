// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.connection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import software.aws.toolkits.eclipse.amazonq.extensions.implementation.ActivatorStaticMockExtension;
import software.aws.toolkits.eclipse.amazonq.extensions.implementation.DefaultLspEncryptionManagerStaticMockExtension;
import software.aws.toolkits.eclipse.amazonq.extensions.implementation.LspManagerProviderStaticMockExtension;
import software.aws.toolkits.eclipse.amazonq.extensions.implementation.ProxyUtilsStaticMockExtension;
import software.aws.toolkits.eclipse.amazonq.lsp.encryption.LspEncryptionManager;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspInstallResult;
import org.eclipse.lsp4e.server.ProcessStreamConnectionProvider;
import software.aws.toolkits.eclipse.amazonq.util.LoggingService;
import software.aws.toolkits.eclipse.amazonq.util.ProxyUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

public final class QLspConnectionProviderTest {

    @RegisterExtension
    private static ActivatorStaticMockExtension activatorStaticMockExtension = new ActivatorStaticMockExtension();

    @RegisterExtension
    private static LspManagerProviderStaticMockExtension lspManagerProviderStaticMockExtension
            = new LspManagerProviderStaticMockExtension();

    @RegisterExtension
    private static DefaultLspEncryptionManagerStaticMockExtension lspEncryptionManagerStaticMockExtension
            = new DefaultLspEncryptionManagerStaticMockExtension();

    @RegisterExtension
    private static ProxyUtilsStaticMockExtension proxyUtilsStaticMockExtension = new ProxyUtilsStaticMockExtension();

    private static final class TestProcessConnectionProvider extends ProcessStreamConnectionProvider {

        TestProcessConnectionProvider(final List<String> commands, final String workingDirectory) {
            super(commands, workingDirectory);
        }

    }

    private static final class TestQLspConnectionProvider extends QLspConnectionProvider {

        TestQLspConnectionProvider() throws IOException {
            super();
        }

        public void testAddEnvironmentVariables(final Map<String, String> env) {
            super.addEnvironmentVariables(env);
        }

    }

    @Test
    void testConstructorInitializesCorrectly() throws IOException {
        Optional<LspInstallResult> lspInstallResultMockOptional = lspManagerProviderStaticMockExtension.getMock(
                LspInstallResult.class
        );
        lspInstallResultMockOptional.ifPresent(mock -> {
            Mockito.when(mock.getServerDirectory()).thenReturn("/test/dir");
            Mockito.when(mock.getServerCommand()).thenReturn("server.js");
            Mockito.when(mock.getServerCommandArgs()).thenReturn("--test-arg");
        });

        var provider = new QLspConnectionProvider();

        List<String> expectedCommands = List.of(
                "/test/dir/server.js",
                "--test-arg",
                "--nolazy",
                "--inspect=5599",
                "--stdio",
                "--set-credentials-encryption-key"
        );

        TestProcessConnectionProvider testProcessConnectionProvider = new TestProcessConnectionProvider(
                expectedCommands,
                "/test/dir"
        );

        assertTrue(((ProcessStreamConnectionProvider) testProcessConnectionProvider).equals(provider));
    }

    @Test
    void testAddEnvironmentVariablesWithoutProxy() throws IOException {
        Optional<LspInstallResult> lspInstallResultMockOptional = lspManagerProviderStaticMockExtension.getMock(
                LspInstallResult.class
        );
        lspInstallResultMockOptional.ifPresent(mock -> {
            Mockito.when(mock.getServerDirectory()).thenReturn("/test/dir");
            Mockito.when(mock.getServerCommand()).thenReturn("server.js");
            Mockito.when(mock.getServerCommandArgs()).thenReturn("");
        });

        MockedStatic<ProxyUtil> proxyUtilStaticMock = proxyUtilsStaticMockExtension.getStaticMock();
        proxyUtilStaticMock.when(ProxyUtil::getHttpsProxyUrl).thenReturn("");

        Map<String, String> env = new HashMap<>();

        var provider = new TestQLspConnectionProvider();
        provider.testAddEnvironmentVariables(env);

        assertEquals("true", env.get("ENABLE_INLINE_COMPLETION"));
        assertEquals("true", env.get("ENABLE_TOKEN_PROVIDER"));
        assertFalse(env.containsKey("HTTPS_PROXY"));
    }

    @Test
    void testAddEnvironmentVariablesWithProxy() throws IOException {
        Optional<LspInstallResult> lspInstallResultMockOptional = lspManagerProviderStaticMockExtension.getMock(
                LspInstallResult.class
        );
        lspInstallResultMockOptional.ifPresent(mock -> {
            Mockito.when(mock.getServerDirectory()).thenReturn("/test/dir");
            Mockito.when(mock.getServerCommand()).thenReturn("server.js");
            Mockito.when(mock.getServerCommandArgs()).thenReturn("");
        });

        MockedStatic<ProxyUtil> proxyUtilStaticMock = proxyUtilsStaticMockExtension.getStaticMock();
        proxyUtilStaticMock.when(ProxyUtil::getHttpsProxyUrl).thenReturn("http://proxy:8080");

        Map<String, String> env = new HashMap<>();

        var provider = new TestQLspConnectionProvider();
        provider.testAddEnvironmentVariables(env);

        assertEquals("true", env.get("ENABLE_INLINE_COMPLETION"));
        assertEquals("true", env.get("ENABLE_TOKEN_PROVIDER"));
        assertEquals("http://proxy:8080", env.get("HTTPS_PROXY"));
    }

    @Test
    void testStartInitializesEncryptedCommunication() throws IOException {
        Optional<LspInstallResult> lspInstallResultMockOptional = lspManagerProviderStaticMockExtension.getMock(
                LspInstallResult.class
        );
        lspInstallResultMockOptional.ifPresent(mock -> {
            Mockito.when(mock.getServerDirectory()).thenReturn("/test/dir");
            Mockito.when(mock.getServerCommand()).thenReturn("server.js");
            Mockito.when(mock.getServerCommandArgs()).thenReturn("");
        });

        var provider = Mockito.spy(new QLspConnectionProvider());
        doNothing().when(provider).startProcess();

        var mockOutputStream = Mockito.mock(OutputStream.class);
        doReturn(mockOutputStream).when(provider).getOutputStream();

        provider.start();

        Optional<LoggingService> loggerMockOptional = activatorStaticMockExtension.getMock(LoggingService.class);
        loggerMockOptional.ifPresent(mock ->
                verify(mock).info("Initializing encrypted communication with Amazon Q Lsp Server"));

        Optional<LspEncryptionManager> lspEncryptionManagerMockOptional =
                lspEncryptionManagerStaticMockExtension.getMock(LspEncryptionManager.class);
        lspEncryptionManagerMockOptional.ifPresent(mock ->
                verify(mock).initializeEncrypedCommunication(mockOutputStream));
    }

    @Test
    void testStartLogsErrorOnException() throws IOException {
        Optional<LspInstallResult> lspInstallResultMockOptional = lspManagerProviderStaticMockExtension.getMock(
                LspInstallResult.class
        );
        lspInstallResultMockOptional.ifPresent(mock -> {
            Mockito.when(mock.getServerDirectory()).thenReturn("/test/dir");
            Mockito.when(mock.getServerCommand()).thenReturn("server.js");
            Mockito.when(mock.getServerCommandArgs()).thenReturn("");
        });

        var provider = Mockito.spy(new QLspConnectionProvider());
        doNothing().when(provider).startProcess();

        var mockOutputStream = Mockito.mock(OutputStream.class);
        doReturn(mockOutputStream).when(provider).getOutputStream();

        RuntimeException testException = new RuntimeException("Test error");
        Optional<LspEncryptionManager> lspEncryptionManagerMockOptional =
                lspEncryptionManagerStaticMockExtension.getMock(LspEncryptionManager.class);

        lspEncryptionManagerMockOptional.ifPresent(mock ->
                doThrow(testException).when(mock)
                        .initializeEncrypedCommunication(any()));

        provider.start();

        Optional<LoggingService> loggingServiceMockOptional = activatorStaticMockExtension.getMock(LoggingService.class);
        loggingServiceMockOptional.ifPresent(mock -> verify(mock).error(
                "Error occured while initializing encrypted communication with Amazon Q Lsp Server",
                testException));
    }

}
