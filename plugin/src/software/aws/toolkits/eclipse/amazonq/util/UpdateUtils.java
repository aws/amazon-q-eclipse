// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.eclipse.mylyn.commons.ui.dialogs.AbstractNotificationPopup;
import org.eclipse.swt.widgets.Display;

import org.tukaani.xz.XZInputStream;

import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher.ArtifactUtils;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.telemetry.metadata.PluginClientMetadata;

public final class UpdateUtils {
    private static final String REQUEST_URL = "https://amazonq.eclipsetoolkit.amazonwebservices.com/artifacts.xml.xz";
    private static ArtifactVersion mostRecentNotificationVersion;
    private static ArtifactVersion remoteVersion;
    private static ArtifactVersion localVersion;
    private static final UpdateUtils INSTANCE = new UpdateUtils();

    public static UpdateUtils getInstance() {
        return INSTANCE;
    }

    private UpdateUtils() {
        mostRecentNotificationVersion = Activator.getPluginStore().getObject(Constants.DO_NOT_SHOW_UPDATE_KEY, ArtifactVersion.class);
        String localString = PluginClientMetadata.getInstance().getPluginVersion();
        localVersion = ArtifactUtils.parseVersion(localString.substring(0, localString.lastIndexOf(".")));
    }

    private boolean newUpdateAvailable() {
        //fetch artifact file containing version info from repo
        remoteVersion = fetchRemoteArtifactVersion(REQUEST_URL);

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
            showNotification();
        }
    }

    private ArtifactVersion fetchRemoteArtifactVersion(final String repositoryUrl) {
        HttpClient connection = HttpClientFactory.getInstance();
        try {
            HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(repositoryUrl))
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build();

            HttpResponse<InputStream> response = connection.send(request,
            HttpResponse.BodyHandlers.ofInputStream());

            // handle response codes
            if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                throw new AmazonQPluginException("HTTP request failed with response code: " + response.statusCode());
            }

            // process XZ content from input stream
            try (InputStream inputStream = response.body();
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
            Activator.getLogger().error("Error fetching artifact from remote location.", e);
        }
        return null;
    }

    private void showNotification() {
        Display.getDefault().asyncExec(() -> {
            AbstractNotificationPopup notification = new PersistentToolkitNotification(Display.getCurrent(),
                    Constants.PLUGIN_UPDATE_NOTIFICATION_TITLE,
                    String.format(Constants.PLUGIN_UPDATE_NOTIFICATION_BODY, remoteVersion.toString()),
                    (selected) -> {
                        if (selected) {
                            Activator.getPluginStore().putObject(Constants.DO_NOT_SHOW_UPDATE_KEY, remoteVersion);
                        } else {
                            Activator.getPluginStore().remove(Constants.DO_NOT_SHOW_UPDATE_KEY);
                        }
                    });
            notification.open();
        });
    }

    private static boolean remoteVersionIsGreater(final ArtifactVersion remote, final ArtifactVersion local) {
        return remote.compareTo(local) > 0;
    }
}
