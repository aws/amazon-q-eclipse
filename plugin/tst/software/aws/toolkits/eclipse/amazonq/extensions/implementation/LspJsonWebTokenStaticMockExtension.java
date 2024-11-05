// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.extensions.implementation;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.MockedStatic;
import software.aws.toolkits.eclipse.amazonq.extensions.api.StaticMockExtension;
import software.aws.toolkits.eclipse.amazonq.lsp.encryption.LspJsonWebToken;

import javax.crypto.SecretKey;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

public final class LspJsonWebTokenStaticMockExtension extends StaticMockExtension<LspJsonWebToken>
        implements BeforeAllCallback, AfterAllCallback {

    private MockedStatic<LspJsonWebToken> lspJsonWebTokenStaticMock = null;

    @Override
    public MockedStatic<LspJsonWebToken> getStaticMock() {
        return lspJsonWebTokenStaticMock;
    }

    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {
        lspJsonWebTokenStaticMock = mockStatic(LspJsonWebToken.class);

        lspJsonWebTokenStaticMock.when(() -> LspJsonWebToken.encrypt(any(SecretKey.class), any(Object.class)))
                .thenReturn("Mocked Encrypted Value");
        lspJsonWebTokenStaticMock.when(() -> LspJsonWebToken.decrypt(any(SecretKey.class), any(String.class)))
                .thenReturn("Mocked Decrypted Value");
    }

    @Override
    public void afterAll(final ExtensionContext context) throws Exception {
        if (lspJsonWebTokenStaticMock != null) {
            lspJsonWebTokenStaticMock.close();
        }
    }

}
