// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.eclipse.mylyn.commons.ui.dialogs.AbstractNotificationPopup;
import org.eclipse.swt.widgets.Display;

import com.github.markusbernhardt.proxy.ProxySearch;

import software.amazon.awssdk.utils.StringUtils;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.preferences.AmazonQPreferencePage;

public final class ProxyUtil {
    private static final String DEFAULT_PROXY_ENDPOINT = "https://amazonaws.com";
    private static volatile boolean hasSeenInvalidProxyNotification;

    private static ProxySelector proxySelector;

    private ProxyUtil() { } // Prevent initialization

    public static String getHttpsProxyUrl() {
        return getHttpsProxyUrlForEndpoint(DEFAULT_PROXY_ENDPOINT);
    }

    public static String getHttpsProxyUrlForEndpoint(final String endpointUrl) {
        try {
            String proxyPrefUrl = getHttpsProxyPreferenceUrl();
            if (!StringUtils.isEmpty(proxyPrefUrl)) {
                return proxyPrefUrl;
            }
        } catch (MalformedURLException e) {
            showInvalidProxyNotification();
            return null;
        }

        if (StringUtils.isEmpty(endpointUrl)) {
            return null;
        }

        URI endpointUri;
        try {
            endpointUri = new URI(endpointUrl);
        } catch (URISyntaxException e) {
            Activator.getLogger().error("Could not parse endpoint for proxy configuration: " + endpointUrl, e);
            return null;
        }

        return getProxyUrlFromSelector(getProxySelector(), endpointUri);
    }

    private static String getProxyUrlFromSelector(final ProxySelector proxySelector, final URI endpointUri) {
        if (proxySelector == null) {
            return null;
        }

        var proxies = proxySelector.select(endpointUri);
        if (proxies == null || proxies.isEmpty()) {
            return null;
        }

        return proxies.stream()
            .filter(p -> p.type() != Proxy.Type.DIRECT)
            .findFirst()
            .map(proxy -> createProxyUrl(proxy, endpointUri))
            .orElseGet(() -> {
                Activator.getLogger().info("No non-DIRECT proxies found for endpoint: " + endpointUri);
                return null;
            });
    }

    private static String createProxyUrl(final Proxy proxy, final URI endpointUri) {
        if (!(proxy.address() instanceof InetSocketAddress addr)) {
            return null;
        }

        String scheme = determineProxyScheme(proxy.type(), endpointUri);
        if (scheme == null) {
            return null;
        }

        String proxyUrl = String.format("%s://%s:%d", scheme, addr.getHostString(), addr.getPort());
        Activator.getLogger().info("Using proxy URL: " + proxyUrl + " for endpoint: " + endpointUri);
        return proxyUrl;
    }

    private static String determineProxyScheme(final Proxy.Type proxyType, final URI endpointUri) {
        return switch (proxyType) {
            case HTTP -> "http";
            case SOCKS -> "socks";
            default -> null;
        };
    }

    private static String getHttpsProxyPreferenceUrl() throws MalformedURLException {
        String prefValue = Activator.getDefault().getPreferenceStore()
            .getString(AmazonQPreferencePage.HTTPS_PROXY);

        if (StringUtils.isEmpty(prefValue)) {
            return null;
        }

        new URL(prefValue); // Validate URL format
        return prefValue;
    }

    private static void showInvalidProxyNotification() {
        if (!hasSeenInvalidProxyNotification) {
            hasSeenInvalidProxyNotification = true;
            Display.getDefault().asyncExec(() -> {
                AbstractNotificationPopup notification = new ToolkitNotification(
                    Display.getCurrent(),
                    Constants.INVALID_PROXY_CONFIGURATION_TITLE,
                    Constants.INVALID_PROXY_CONFIGURATION_BODY
                );
                notification.open();
            });
        }
    }

    public static SSLContext getCustomSslContext() {
        String customCertPath = getCustomCertPath();
        if (StringUtils.isEmpty(customCertPath)) {
            return null;
        }

        try {
            return createSslContextWithCustomCert(customCertPath);
        } catch (Exception e) {
            Activator.getLogger().error("Failed to set up SSL context. Additional certs will not be used.", e);
            return null;
        }
    }

    private static String getCustomCertPath() {
        String caCertPreference = Activator.getDefault().getPreferenceStore().getString(AmazonQPreferencePage.CA_CERT);
        return !StringUtils.isEmpty(caCertPreference) ? caCertPreference : System.getenv("NODE_EXTRA_CA_CERTS");
    }

    private static SSLContext createSslContextWithCustomCert(final String certPath) throws Exception {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null);

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);

        for (TrustManager tm : tmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager) {
                X509TrustManager xtm = (X509TrustManager) tm;
                for (X509Certificate cert : xtm.getAcceptedIssuers()) {
                    keyStore.setCertificateEntry(cert.getSubjectX500Principal().getName(), cert);
                }
            }
        }

        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        X509Certificate cert;

        try (FileInputStream fis = new FileInputStream(certPath)) {
            cert = (X509Certificate) certificateFactory.generateCertificate(fis);
        }

        keyStore.setCertificateEntry("custom-cert", cert);

        TrustManagerFactory customTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        customTmf.init(keyStore);

        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(null, customTmf.getTrustManagers(), null);
        Activator.getLogger().info("Picked up custom CA cert.");

        return sslContext;
    }

    static synchronized ProxySelector getProxySelector() {
        if (proxySelector == null) {
            ProxySearch proxySearch = new ProxySearch();
            proxySearch.addStrategy(ProxySearch.Strategy.ENV_VAR);
            proxySearch.addStrategy(ProxySearch.Strategy.JAVA);
            proxySearch.addStrategy(ProxySearch.Strategy.OS_DEFAULT);
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                proxySearch.addStrategy(ProxySearch.Strategy.IE);
            }
            proxySelector = proxySearch.getProxySelector();
        }
        return proxySelector;
    }
}
