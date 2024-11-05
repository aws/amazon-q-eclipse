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
import software.aws.toolkits.eclipse.amazonq.lsp.encryption.LspEncryptionManager;
import software.aws.toolkits.eclipse.amazonq.lsp.encryption.DefaultLspEncryptionManager;

import java.util.Map;

import static org.mockito.Mockito.mockStatic;

public final class DefaultLspEncryptionManagerStaticMockExtension extends StaticMockExtension<DefaultLspEncryptionManager>
        implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

    private MockedStatic<DefaultLspEncryptionManager> lspEncryptionManagerStaticMock = null;

    @Override
    public MockedStatic<DefaultLspEncryptionManager> getStaticMock() {
        return lspEncryptionManagerStaticMock;
    }

    @Override
    public void beforeAll(final ExtensionContext context) {
        lspEncryptionManagerStaticMock = mockStatic(DefaultLspEncryptionManager.class);
    }

    @Override
    public void beforeEach(final ExtensionContext context) {
        LspEncryptionManager lspEncryptionManagerMock = Mockito.mock(DefaultLspEncryptionManager.class);
        lspEncryptionManagerStaticMock.when(DefaultLspEncryptionManager::getInstance).thenReturn(lspEncryptionManagerMock);

        Map<Class<?>, Object> newMocksMap = Map.of(
                LspEncryptionManager.class, lspEncryptionManagerMock
        );
        setMocksMap(newMocksMap);
    }

    @Override
    public void afterAll(final ExtensionContext context) {
        if (lspEncryptionManagerStaticMock != null) {
            lspEncryptionManagerStaticMock.close();
        }
    }

}
