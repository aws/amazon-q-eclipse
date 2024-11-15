// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public final class PluginUtils {

    private PluginUtils() {
        // Prevent instantiation
    }

    public static java.nio.file.Path getPluginDir(final String directoryName) {
        var stateLocation = new File(Activator.getDefault().getStateLocation().toOSString(), directoryName);
        if (!stateLocation.exists()) {
            stateLocation.mkdirs();
        }
        return Paths.get(stateLocation.getAbsolutePath());
    }

    public static URL getResource(final String path) throws IOException {
        Bundle bundle = FrameworkUtil.getBundle(PluginUtils.class);
        URL url = FileLocator.find(bundle, new Path(path), null);
        return FileLocator.toFileURL(url);
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
            Activator.getLogger().warn("Error while trying to open an external web page", ex);
        }
    }

    protected static boolean showConfirmDialog(final String title, final String message) {
        final boolean[] result = new boolean[] {false};
        try {
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    result[0] = MessageDialog.openConfirm(Display.getDefault().getActiveShell(), title, message);
                }
            });
        } catch (Exception ex) {
            Activator.getLogger().error(ex.getMessage());
        }
        return result[0];
    }

    public static void showErrorDialog(final String title, final String message) {
        try {
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    MessageDialog.openError(Display.getDefault().getActiveShell(), title, message);
                }
            });
        } catch (Exception ex) {
            Activator.getLogger().error("Failed to show error dialog", ex);
        }
    }

    public static void handleExternalLinkClick(final String link) {
        try {
            var result = showConfirmDialog("Amazon Q", "Do you want to open the external website?\n\n" + link);
            if (result) {
                openWebpage(link);
            }
        } catch (Exception ex) {
            Activator.getLogger().error("Failed to open url in browser", ex);
        }
    }
}
