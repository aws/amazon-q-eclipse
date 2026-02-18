// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.URL;
import java.net.http.HttpClient;
import java.time.Duration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import software.amazon.awssdk.utils.StringUtils;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public final class HttpClientFactory {

    private static volatile HttpClient instance;

    private HttpClientFactory() {
        // Prevent instantiation
    }

    public static HttpClient getInstance() {
        if (instance == null) {
            synchronized (HttpClientFactory.class) {
                if (instance == null) {
                    // TODO: do we need to use HttpClient.Version.HTTP_1_1 here?
                    var builder = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL);
                    var proxyUrl = ProxyUtil.getHttpsProxyUrl();
                    if (!StringUtils.isEmpty(proxyUrl)) {
                        InetSocketAddress proxyAddress = getProxyAddress(proxyUrl);
                        builder.proxy(ProxySelector.of(proxyAddress));
                        var proxyAuth = getProxyAuthenticator(proxyUrl);
                        if (proxyAuth != null) {
                            builder.authenticator(proxyAuth);
                        }
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

    private static Authenticator getProxyAuthenticator(final String proxyUrl) {
        try {
            URL url = new URL(proxyUrl);
            String userInfo = url.getUserInfo();
            if (userInfo == null || userInfo.isEmpty()) {
                return null;
            }
            int colonIndex = userInfo.indexOf(':');
            if (colonIndex < 0) {
                return null;
            }
            String username = userInfo.substring(0, colonIndex);
            String password = userInfo.substring(colonIndex + 1);
            return new java.net.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    Activator.getLogger().info(String.format(
                        "Proxy auth requested - scheme: %s, host: %s, port: %d, type: %s, prompt: %s",
                        getRequestingScheme(), 
                        getRequestingHost(), 
                        getRequestingPort(),
                        getRequestorType(), 
                        getRequestingPrompt())
                    );
                    
                    if (getRequestorType() == RequestorType.PROXY) {
                        return new PasswordAuthentication(username, password.toCharArray());
                    }
                    
                    return null;
                }
            };
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
