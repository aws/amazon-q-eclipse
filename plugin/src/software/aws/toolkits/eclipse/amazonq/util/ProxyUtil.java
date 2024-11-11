// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.eclipse.core.net.proxy.IProxyData;
import software.amazon.awssdk.utils.StringUtils;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public final class ProxyUtil {

    private ProxyUtil() {
        // Prevent initialization
    }

    private static String proxyHttpsUrl = "";

    public static String getHttpsProxyUrl() {
        return proxyHttpsUrl;
    }

    public static String getHttpsProxyUrlEnvVar() {
        return System.getenv("HTTPS_PROXY");
    }

    public static void updateHttpsProxyUrl(final String proxyHost) {
        proxyHttpsUrl = proxyHost;
    }

    public static boolean isProxyValid(final IProxyData proxyData) {
        if (proxyData == null) {
            return false;
        }
        String host = proxyData.getHost();
        int port = proxyData.getPort();
        String user = proxyData.getUserId();
        String password = proxyData.getPassword();
        return (
            (!StringUtils.isEmpty(host) && port != -1 && !StringUtils.isEmpty(user) && !StringUtils.isEmpty(password))
            || (!StringUtils.isEmpty(host) && port != -1)
        );
    }

    public static String createHttpsProxyHost(final IProxyData proxyData) {
        String host = proxyData.getHost();
        int port = proxyData.getPort();
        String user = proxyData.getUserId();
        String password = proxyData.getPassword();
        String proxiedHost = "";
        if (!StringUtils.isEmpty(host) && port != -1 && !StringUtils.isEmpty(user) && !StringUtils.isEmpty(password)) {
            return "http://" + user + ":" + password + "@" + host + ":" + Integer.toString(port);
        } else if (!StringUtils.isEmpty(host) && port != -1) {
            return "https://" + host + ":" + Integer.toString(port);
        }
        return proxiedHost;
    }

    public static SSLContext getCustomSslContext() {
        try {
            String customCertPath = System.getenv("NODE_EXTRA_CA_CERTS");
            if (customCertPath != null && !customCertPath.isEmpty()) {
                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                X509Certificate cert;

                try (FileInputStream fis = new FileInputStream(customCertPath)) {
                    cert = (X509Certificate) certificateFactory.generateCertificate(fis);
                }

                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(null, null);
                keyStore.setCertificateEntry("custom-cert", cert);

                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(keyStore);

                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, tmf.getTrustManagers(), null);
                Activator.getLogger().info("Picked up custom CA cert.");

                return sslContext;
            }
        } catch (Exception e) {
            Activator.getLogger().error("Failed to set up SSL context. Additional certs will not be used.", e);
        }
        return null;
    }

}
