// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProxySelector;
import java.net.URL;
import java.net.http.HttpClient;

import software.amazon.awssdk.utils.StringUtils;

public final class HttpClientFactory {

    private static volatile HttpClient instance;

    private HttpClientFactory() {
        // Prevent instantiation
    }

    public static HttpClient getInstance() {
        if (instance == null) {
            synchronized (HttpClientFactory.class) {
                if (instance == null) {
                    var builder = HttpClient.newBuilder();
                    var proxyUrl = ProxyUtil.getHttpsProxyUrlEnvVar();
                    if (!StringUtils.isEmpty(proxyUrl)) {
                        InetSocketAddress proxyAddress = getProxyAddress(proxyUrl);
                        builder.proxy(ProxySelector.of(proxyAddress));
                    }
                    var customSslContext = ProxyUtil.getCustomSslContext();
                    if (customSslContext != null) {
                        builder.sslContext(ProxyUtil.getCustomSslContext());
                    }
                    instance = builder.build();
                }
            }
        }
        return instance;
    }

    private static InetSocketAddress getProxyAddress(final String proxyUrl) {
        try {
            URL url = new URL(proxyUrl);
            return new InetSocketAddress(url.getHost(), url.getPort());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid proxy URL: " + proxyUrl, e);
        }
    }
}
