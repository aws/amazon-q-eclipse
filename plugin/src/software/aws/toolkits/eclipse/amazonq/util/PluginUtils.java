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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

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
                Activator.getLogger().warn("Unable to retreive Q icon", e);
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
            Activator.getLogger().warn("Error while trying to open an external web page:", ex);
        }
    }

    private static boolean showConfirmDialog(final String title, final String message) {
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

    public static void showView(final String viewId) {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                try {
                    page.showView(viewId);
                    Activator.getLogger().info("Showing view " + viewId);
                } catch (PartInitException e) {
                    Activator.getLogger().error("Error occurred while opening view " + viewId, e);
                }
            }
        }
    }
}
