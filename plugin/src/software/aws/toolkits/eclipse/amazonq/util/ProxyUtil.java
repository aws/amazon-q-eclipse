// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.core.net.proxy.IProxyData;
import software.amazon.awssdk.utils.StringUtils;

public final class ProxyUtil {

    private ProxyUtil() {
        // Prevent initialization
    }

    private static String proxyHttpsUrl = "";

    public static String getHttpsProxyUrl() {
        return proxyHttpsUrl;
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
}
