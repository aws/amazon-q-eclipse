// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProxyUtilTest {

    @Test
    void testNoProxyConfigReturnsNull() {
        assertEquals(null, ProxyUtil.getHttpsProxyUrl(null, null));
    }

    @Test
    void testEnvVarProxyUrl() {
        String mockUrl = "http://foo.com:8888";
        assertEquals(mockUrl, ProxyUtil.getHttpsProxyUrl(mockUrl, null));
    }

    @Test
    void testPreferenceProxyUrl() {
        String mockUrl = "http://foo.com:8888";
        assertEquals(mockUrl, ProxyUtil.getHttpsProxyUrl(null, mockUrl));
    }

    @Test
    void testPreferenceProxyUrlPrecedence() {
        String mockUrl = "http://foo.com:8888";
        assertEquals(mockUrl, ProxyUtil.getHttpsProxyUrl("http://bar.com:8888", mockUrl));
    }

}
