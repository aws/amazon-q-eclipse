// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.extensions.implementation;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import software.aws.toolkits.eclipse.amazonq.extensions.api.StaticMockExtension;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspInstallResult;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspManager;
import software.aws.toolkits.eclipse.amazonq.providers.lsp.LspManagerProvider;

import static org.mockito.Mockito.mockStatic;

import java.util.Map;

public final class LspManagerProviderStaticMockExtension extends StaticMockExtension<LspManagerProvider>
        implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback {

    private MockedStatic<LspManagerProvider> lspManagerProviderStaticMock = null;

    @Override
    public MockedStatic<LspManagerProvider> getStaticMock() {
        return lspManagerProviderStaticMock;
    }

    @Override
    public void beforeAll(final ExtensionContext context) {
        lspManagerProviderStaticMock = mockStatic(LspManagerProvider.class);
    }

    @Override
    public void beforeEach(final ExtensionContext context) {
        LspManager lspManagerMock = Mockito.mock(LspManager.class);
        LspInstallResult lspInstallResult = Mockito.mock(LspInstallResult.class);

        lspManagerProviderStaticMock.when(LspManagerProvider::getInstance).thenReturn(lspManagerMock);
        Mockito.when(lspManagerMock.getLspInstallation()).thenReturn(lspInstallResult);

        Map<Class<?>, Object> newMocksMap = Map.of(
                LspManager.class, lspManagerMock,
                LspInstallResult.class, lspInstallResult
        );
        setMocksMap(newMocksMap);
    }

    @Override
    public void afterAll(final ExtensionContext context) {
        if (lspManagerProviderStaticMock != null) {
            lspManagerProviderStaticMock.close();
        }
    }

}
