// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;

public final class PluginUtils {

    private PluginUtils() {
        // Prevent instantiation
    }

    private static Image qIcon = null;

    public static java.nio.file.Path getPluginDir(final String directoryName) {
        Bundle bundle = FrameworkUtil.getBundle(PluginUtils.class);
        File pluginDir = bundle.getDataFile(directoryName);
        if (!pluginDir.exists()) {
            pluginDir.mkdirs();
        }
        return Paths.get(pluginDir.getAbsolutePath());
    }

    public static java.nio.file.Path getAwsDirectory(final String subdir) {
        java.nio.file.Path homeDir = Paths.get(System.getProperty("user.home"));
        java.nio.file.Path awsDir = homeDir.resolve(".aws");
        java.nio.file.Path subDir = awsDir.resolve(subdir);

        try {
            Files.createDirectories(subDir);
        } catch (IOException e) {
            throw new AmazonQPluginException("Failed to create directory: " + subDir, e);
        }

        return subDir;
    }

    public static URL getResource(final String path) throws IOException {
        Bundle bundle = FrameworkUtil.getBundle(PluginUtils.class);
        URL url = FileLocator.find(bundle, new Path(path), null);
        return FileLocator.toFileURL(url);
    }

    public static Image getQIcon() {
        if (qIcon == null) {
            try {
                var icon = getResource("icons/AmazonQ.png");
                qIcon = new Image(null, icon.openStream());
            } catch (IOException e) {
                PluginLogger.warn("Unable to retreive Q icon", e);
            }
        }
        return qIcon;
    }

    public static PluginPlatform getPlatform() {
        if (Platform.OS.isWindows()) {
            return PluginPlatform.WINDOWS;
        } else if (Platform.OS.isLinux()) {
            return PluginPlatform.LINUX;
        } else if (Platform.OS.isMac()) {
            return PluginPlatform.MAC;
        } else {
            throw new AmazonQPluginException("Detected unsupported platform: " + Platform.getOS());
        }
    }

    public static PluginArchitecture getArchitecture() {
        String osArch = Platform.getOSArch();
        if (osArch.equals(Platform.ARCH_X86_64)) {
            return PluginArchitecture.X86_64;
        } else if (osArch.equals(Platform.ARCH_AARCH64)) {
            return PluginArchitecture.ARM_64;
        } else {
            throw new AmazonQPluginException("Detected unsupported architecture: " + osArch);
        }
    }

    public static void openWebpage(final String url) {
        try {
            PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(url));
        } catch (Exception ex) {
            PluginLogger.warn("Error while trying to open an external web page:", ex);
        }
    }
}
