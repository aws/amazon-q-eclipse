// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.eclipse.mylyn.commons.ui.dialogs.AbstractNotificationPopup;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Version;

import org.tukaani.xz.XZInputStream;

import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher.ArtifactUtils;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.telemetry.metadata.PluginClientMetadata;

public class UpdateUtils {
    //env for this link?
    private static final String requestURL = "https://amazonq.eclipsetoolkit.amazonwebservices.com/artifacts.xml.xz";
    private static Version mostRecentNotificationVersion;
    private static Version remoteVersion;
    private static Version localVersion;
    private static final UpdateUtils INSTANCE = new UpdateUtils();

    public static UpdateUtils getInstance() {
        return INSTANCE;
    }

    private UpdateUtils() {
        mostRecentNotificationVersion = Activator.getPluginStore().getObject(Constants.LAST_NOTIFIED_UPDATE_VERSION, Version.class);
        String localString = PluginClientMetadata.getInstance().getPluginVersion();
        localVersion = ArtifactUtils.parseVersion(localString.substring(0, localString.lastIndexOf(".")));
    }

    private boolean newUpdateAvailable() {
        //fetch artifact file containing version info from repo
        remoteVersion = fetchRemoteArtifactVersion(requestURL);

        //return early if either version is unavailable
        if (remoteVersion == null || localVersion == null) {
            return false;
        }

        //prompt should show if never previously displayed or remote version is greater
        boolean shouldShowNotification = mostRecentNotificationVersion == null || remoteVersionIsGreater(remoteVersion, mostRecentNotificationVersion);

        return remoteVersionIsGreater(remoteVersion, localVersion) && shouldShowNotification;
    }

    public void checkForUpdate() {
        if (newUpdateAvailable()) {
            //notify user
            showNotification();

            //update storage with notification version
            Activator.getPluginStore().putObject(Constants.LAST_NOTIFIED_UPDATE_VERSION, remoteVersion);
        }
    }

    private Version fetchRemoteArtifactVersion(String repositoryUrl) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(repositoryUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            // handle response codes
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new AmazonQPluginException("HTTP request failed with response code: " + responseCode);
            }

            // process XZ content from input stream
            try (InputStream inputStream = connection.getInputStream();
                 XZInputStream xzis = new XZInputStream(inputStream);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(xzis))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("<artifact classifier=\"osgi.bundle\"")) {
                        int versionStart = line.indexOf("version=\"") + 9;
                        int versionEnd = line.indexOf("\"", versionStart);
                        String fullVersion = line.substring(versionStart, versionEnd);
                        return ArtifactUtils.parseVersion(fullVersion.substring(0, fullVersion.lastIndexOf(".")));
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }

    private void showNotification() {
        Display.getDefault().asyncExec(() -> {
            AbstractNotificationPopup notification = new ToolkitNotification(Display.getCurrent(),
                    Constants.PLUGIN_UPDATE_NOTIFICATION_TITLE,
                    Constants.PLUGIN_UPDATE_NOTIFICATION_BODY);
            notification.open();
        });
    }

    private static boolean remoteVersionIsGreater(Version remote, Version local) {
        return (remote != null) && (local != null) && (remote.compareTo(local) > 0);
    }
}
