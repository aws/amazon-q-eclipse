// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.internal.net.ProxyData;

class ProxyUtilTest {

    @Test
    void testIsValidProxySuccess() {
        IProxyData httpsProxy = new ProxyData(IProxyData.HTTPS_PROXY_TYPE);
        httpsProxy.setHost("127.0.0.1");
        httpsProxy.setPort(8888);
        assertEquals(true, ProxyUtil.isProxyValid(httpsProxy));

        IProxyData httpsProxyWithAuth = new ProxyData(IProxyData.HTTPS_PROXY_TYPE);
        httpsProxyWithAuth.setHost("127.0.0.1");
        httpsProxyWithAuth.setPort(8888);
        httpsProxyWithAuth.setUserid("user");
        httpsProxyWithAuth.setPassword("password");
        assertEquals(true, ProxyUtil.isProxyValid(httpsProxy));
    }

    @Test
    void testIsValidProxyFailure() {
        assertEquals(false, ProxyUtil.isProxyValid(null));

        IProxyData httpsProxyWithNullHost = new ProxyData(IProxyData.HTTPS_PROXY_TYPE);
        httpsProxyWithNullHost.setHost(null);
        httpsProxyWithNullHost.setPort(8888);
        assertEquals(false, ProxyUtil.isProxyValid(httpsProxyWithNullHost));

        IProxyData httpsProxyWithInvalidPort = new ProxyData(IProxyData.HTTPS_PROXY_TYPE);
        httpsProxyWithInvalidPort.setHost("127.0.0.1");
        httpsProxyWithInvalidPort.setPort(-1);
        assertEquals(false, ProxyUtil.isProxyValid(httpsProxyWithInvalidPort));
    }

    @Test
    void testCreateHttpsProxyHost() {
        IProxyData httpsProxy = new ProxyData(IProxyData.HTTPS_PROXY_TYPE);
        httpsProxy.setHost("127.0.0.1");
        httpsProxy.setPort(8888);
        assertEquals("https://127.0.0.1:8888", ProxyUtil.createHttpsProxyHost(httpsProxy));

        IProxyData httpsProxyWithAuth = new ProxyData(IProxyData.HTTPS_PROXY_TYPE);
        httpsProxyWithAuth.setHost("127.0.0.1");
        httpsProxyWithAuth.setPort(8888);
        httpsProxyWithAuth.setUserid("user");
        httpsProxyWithAuth.setPassword("password");
        assertEquals("http://user:password@127.0.0.1:8888", ProxyUtil.createHttpsProxyHost(httpsProxyWithAuth));
    }

    @Test
    void testUpdateAndGetProxyUrl() {
        String initialState = ProxyUtil.getHttpsProxyUrl();
        ProxyUtil.updateHttpsProxyUrl("https://127.0.0.1:8888");
        assertEquals("https://127.0.0.1:8888", ProxyUtil.getHttpsProxyUrl());
        ProxyUtil.updateHttpsProxyUrl(initialState);
    }
}
