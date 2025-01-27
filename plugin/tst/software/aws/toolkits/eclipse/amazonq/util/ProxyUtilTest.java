// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import org.eclipse.swt.widgets.Display;

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

    @Test
    void testEnvVarInvalidProxyUrl() {
        try (MockedStatic<Display> displayMock = mockStatic(Display.class)) {
            Display mockDisplay = mock(Display.class);
            displayMock.when(Display::getDefault).thenReturn(mockDisplay);
            String mockUrl = "127.0.0.1:8000";
            assertEquals(null, ProxyUtil.getHttpsProxyUrl(mockUrl, null));
        }
    }

    @Test
    void testPreferenceInvalidProxyUrl() {
        try (MockedStatic<Display> displayMock = mockStatic(Display.class)) {
            Display mockDisplay = mock(Display.class);
            displayMock.when(Display::getDefault).thenReturn(mockDisplay);
            String mockUrl = "127.0.0.1:8000";
            assertEquals(null, ProxyUtil.getHttpsProxyUrl(null, mockUrl));
        }
    }

}
