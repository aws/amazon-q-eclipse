// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.extensions.implementation;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.MockedStatic;
import software.aws.toolkits.eclipse.amazonq.extensions.api.StaticMockExtension;
import software.aws.toolkits.eclipse.amazonq.util.ProxyUtil;

import static org.mockito.Mockito.mockStatic;

public final class ProxyUtilsStaticMockExtension extends StaticMockExtension<ProxyUtil>
        implements BeforeAllCallback, AfterAllCallback {

    private MockedStatic<ProxyUtil> proxyUtilStaticMock;

    @Override
    public MockedStatic<ProxyUtil> getStaticMock() {
        return proxyUtilStaticMock;
    }

    @Override
    public void beforeAll(final ExtensionContext context) {
        proxyUtilStaticMock = mockStatic(ProxyUtil.class);
    }

    @Override
    public void afterAll(final ExtensionContext context) {
        if (proxyUtilStaticMock != null) {
            proxyUtilStaticMock.close();
        }
    }

}
