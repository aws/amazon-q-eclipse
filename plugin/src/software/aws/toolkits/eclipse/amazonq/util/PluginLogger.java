// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.FrameworkUtil;

public final class PluginLogger implements LoggingService {
    private static final PluginLogger INSTANCE = new PluginLogger();
    private ILog logger = null;

    private PluginLogger() {
        logger = Platform.getLog(FrameworkUtil.getBundle(PluginLogger.class));
    }

    public static PluginLogger getInstance() {
        return INSTANCE;
    }

    @Override
    public void info(final String message) {
        info(message, null);
    }

    @Override
    public void info(final String message, final Throwable ex) {
        logger.info(message, ex);
    }

    @Override
    public void warn(final String message) {
        warn(message, null);
    }

    @Override
    public void warn(final String message, final Throwable ex) {
        logger.warn(message, ex);
    }

    @Override
    public void error(final String message) {
        error(message, null);
    }

    @Override
    public void error(final String message, final Throwable ex) {
        logger.error(message, ex);
    }

}
