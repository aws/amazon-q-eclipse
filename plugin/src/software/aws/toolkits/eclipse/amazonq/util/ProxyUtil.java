// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import java.io.FileInputStream;
import java.net.URI;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.mylyn.commons.ui.dialogs.AbstractNotificationPopup;
import org.eclipse.swt.widgets.Display;

import software.amazon.awssdk.utils.StringUtils;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.preferences.AmazonQPreferencePage;

public final class ProxyUtil {

    private static boolean hasSeenInvalidProxyNotification;

    private ProxyUtil() {
        // Prevent initialization
    }

    public static String getHttpsProxyUrl() {
        return getHttpsProxyUrl(System.getenv("HTTPS_PROXY"),
                Activator.getDefault().getPreferenceStore().getString(AmazonQPreferencePage.HTTPS_PROXY));
    }

    protected static String getHttpsProxyUrl(final String envVarValue, final String prefValue) {
        String httpsProxy = envVarValue;
        if (!StringUtils.isEmpty(prefValue)) {
            httpsProxy = prefValue;
        }
        try {
            if (!StringUtils.isEmpty(httpsProxy)) {
                URI.create(httpsProxy);
            }
        } catch (IllegalArgumentException e) {
            if (!hasSeenInvalidProxyNotification) {
                hasSeenInvalidProxyNotification = true;
                Display.getDefault().asyncExec(() -> {
                    AbstractNotificationPopup notification = new ToolkitNotification(Display.getCurrent(),
                            Constants.INVALID_PROXY_CONFIGURATION_TITLE,
                            Constants.INVALID_PROXY_CONFIGURATION_BODY);
                    notification.open();
                });
            }
            return null;
        }
        return httpsProxy;
    }

    public static SSLContext getCustomSslContext() {
        try {
            String customCertPath = System.getenv("NODE_EXTRA_CA_CERTS");
            String caCertPreference = Activator.getDefault().getPreferenceStore().getString(AmazonQPreferencePage.CA_CERT);
            if (!StringUtils.isEmpty(caCertPreference)) {
                customCertPath = caCertPreference;
            }

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

                SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
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
