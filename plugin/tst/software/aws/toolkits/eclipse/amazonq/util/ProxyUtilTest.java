// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.MockedStatic;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;

import software.aws.toolkits.eclipse.amazonq.extensions.implementation.ActivatorStaticMockExtension;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.preferences.AmazonQPreferencePage;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public final class ProxyUtilTest {

    @RegisterExtension
    private static ActivatorStaticMockExtension activatorStaticMockExtension = new ActivatorStaticMockExtension();

    private IPreferenceStore preferenceStore;
    private ProxySelector proxySelector;

    @BeforeEach
    void setUp() {
        preferenceStore = mock(IPreferenceStore.class);
        proxySelector = mock(ProxySelector.class);

        Activator activatorMock = activatorStaticMockExtension.getMock(Activator.class);
        when(activatorMock.getPreferenceStore()).thenReturn(preferenceStore);
    }

    @Test
    void testNoProxyConfigurationReturnsNull() {
        when(preferenceStore.getString(AmazonQPreferencePage.HTTPS_PROXY)).thenReturn("");
        try (MockedStatic<ProxyUtil> proxyUtilMock = mockStatic(ProxyUtil.class, CALLS_REAL_METHODS)) {
            proxyUtilMock.when(ProxyUtil::getProxySelector).thenReturn(proxySelector);
            when(proxySelector.select(any())).thenReturn(Collections.emptyList());

            assertNull(ProxyUtil.getHttpsProxyUrl());
        }
    }

    @Test
    void testPreferenceProxyUrlTakesPrecedence() {
        String preferenceUrl = "http://preference.proxy:8888";
        when(preferenceStore.getString(AmazonQPreferencePage.HTTPS_PROXY)).thenReturn(preferenceUrl);

        assertEquals(preferenceUrl, ProxyUtil.getHttpsProxyUrlForEndpoint("https://foo.com"));
    }

    @Test
    void testSystemProxyConfiguration() {
        when(preferenceStore.getString(AmazonQPreferencePage.HTTPS_PROXY)).thenReturn("");

        try (MockedStatic<ProxyUtil> proxyUtilMock = mockStatic(ProxyUtil.class, CALLS_REAL_METHODS)) {
            proxyUtilMock.when(ProxyUtil::getProxySelector).thenReturn(proxySelector);

            Proxy httpProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy.example.com", 8080));
            when(proxySelector.select(any())).thenReturn(Arrays.asList(httpProxy));

            assertEquals("http://proxy.example.com:8080", ProxyUtil.getHttpsProxyUrlForEndpoint("https://foo.com"));
        }
    }

    @Test
    void testSocksProxyConfiguration() {
        when(preferenceStore.getString(AmazonQPreferencePage.HTTPS_PROXY)).thenReturn("");

        try (MockedStatic<ProxyUtil> proxyUtilMock = mockStatic(ProxyUtil.class, CALLS_REAL_METHODS)) {
            proxyUtilMock.when(ProxyUtil::getProxySelector).thenReturn(proxySelector);

            Proxy socksProxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("socks.example.com", 1080));
            when(proxySelector.select(any())).thenReturn(Arrays.asList(socksProxy));

            assertEquals("socks://socks.example.com:1080", ProxyUtil.getHttpsProxyUrlForEndpoint("https://foo.com"));
        }
    }

    @Test
    void testInvalidPreferenceProxyUrl() {
        when(preferenceStore.getString(AmazonQPreferencePage.HTTPS_PROXY)).thenReturn("invalid:url");

        try (MockedStatic<Display> displayMock = mockStatic(Display.class)) {
            Display mockDisplay = mock(Display.class);
            displayMock.when(Display::getDefault).thenReturn(mockDisplay);
            when(Display.getCurrent()).thenReturn(mockDisplay);

            assertNull(ProxyUtil.getHttpsProxyUrlForEndpoint("https://foo.com"));
        }
    }

    @Test
    void testDirectProxyReturnsNull() {
        when(preferenceStore.getString(AmazonQPreferencePage.HTTPS_PROXY)).thenReturn("");

        try (MockedStatic<ProxyUtil> proxyUtilMock = mockStatic(ProxyUtil.class, CALLS_REAL_METHODS)) {
            proxyUtilMock.when(ProxyUtil::getProxySelector).thenReturn(proxySelector);

            Proxy directProxy = Proxy.NO_PROXY;
            when(proxySelector.select(any())).thenReturn(Arrays.asList(directProxy));

            assertNull(ProxyUtil.getHttpsProxyUrlForEndpoint("https://foo.com"));
        }
    }

    @Test
    void testPreservesEndpointScheme() {
        when(preferenceStore.getString(AmazonQPreferencePage.HTTPS_PROXY)).thenReturn("");

        try (MockedStatic<ProxyUtil> proxyUtilMock = mockStatic(ProxyUtil.class, CALLS_REAL_METHODS)) {
            proxyUtilMock.when(ProxyUtil::getProxySelector).thenReturn(proxySelector);

            Proxy httpProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy.example.com", 8080));
            when(proxySelector.select(any())).thenReturn(Arrays.asList(httpProxy));

            assertEquals("http://proxy.example.com:8080", ProxyUtil.getHttpsProxyUrlForEndpoint("http://foo.com"));
        }
    }
}

