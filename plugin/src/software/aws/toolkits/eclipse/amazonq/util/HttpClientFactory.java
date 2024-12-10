// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProxySelector;
import java.net.URL;
import java.net.http.HttpClient;
import java.time.Duration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

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
                    var proxyUrl = ProxyUtil.getHttpsProxyUrl();
                    if (!StringUtils.isEmpty(proxyUrl)) {
                        InetSocketAddress proxyAddress = getProxyAddress(proxyUrl);
                        builder.proxy(ProxySelector.of(proxyAddress));
                    }
                    var sslContext = ProxyUtil.getCustomSslContext();
                    if (sslContext == null) {
                        try {
                            sslContext = SSLContext.getInstance("TLSv1.2");
                            sslContext.init(null, null, null);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to create SSLContext for TLS 1.2", e);
                        }
                    }
                    SSLParameters sslParams = new SSLParameters();
                    sslParams.setProtocols(new String[]{"TLSv1.2"});
                    instance = builder.connectTimeout(Duration.ofSeconds(10))
                            .sslContext(sslContext)
                            .sslParameters(sslParams)
                            .build();
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
