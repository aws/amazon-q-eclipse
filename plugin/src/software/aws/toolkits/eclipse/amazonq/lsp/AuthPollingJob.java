// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp;


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.DefaultLoginService;

/*
 * The Auth Polling Job periodically checks if a user is authenticated. The main purpose is to update the toolbar
 * with the proper menu (see "software.aws.toolkits.eclipse.amazonq.toolbar" in plugin.xml). If the user
 * is authenticated, display the toolbar with a normal Q icon. If the user is unauthenticated, display the
 * toolbar with a disconnected Q icon.
 */
public final class AuthPollingJob extends Job {
    private static final String JOB_NAME = "Auth Polling Job";
    private static final Object FAMILY = new Object();
    private static final long POLLING_INTERVAL = 5000; // 5 seconds
    private static final long SHORT_POLLING_INTERVAL = 1000; // 1 second

    public AuthPollingJob() {
        super(JOB_NAME);
        setSystem(true); // Run in the background. Prevent job from being displayed in Progress view.
    }

    @Override
    public boolean belongsTo(final Object family) {
        return family == FAMILY;
    }

    @Override
    protected IStatus run(final IProgressMonitor monitor) {
        if (monitor.isCanceled()) {
            return Status.CANCEL_STATUS;
        }

        AuthSourceProvider authSourceProvider = AuthSourceProvider.getProvider();

        if (authSourceProvider == null) {
            schedule(SHORT_POLLING_INTERVAL);
            return Status.warning("Unable to update authentication status. No AuthSourceProvider instance retrieved.");
        }

        Boolean isAuthenticated;
        try {
            isAuthenticated = checkUserIsAuthenticated();
        } catch (Exception e) {
            Activator.getLogger().error("Failed to retrieve authentication status", e);
            scheduleNextOccurence();
            return Status.error("Failed to retrieve authentication status.");
        }

        Display.getDefault().asyncExec(() -> {
            authSourceProvider.setIsAuthenticated(isAuthenticated);
        });

        scheduleNextOccurence();
        return Status.OK_STATUS;
    }

    public void start() {
        cancelExistingJobs();  // ensure only one job is scheduled
        schedule();
    }

    public void stop() {
        cancelExistingJobs();
    }

    private void scheduleNextOccurence() {
        cancelExistingJobs();  // ensure only one job is scheduled
        schedule(POLLING_INTERVAL);
    }

    private void cancelExistingJobs() {
        Job.getJobManager().cancel(FAMILY);
    }

    private Boolean checkUserIsAuthenticated() {
        try {
            return DefaultLoginService.getInstance().getLoginDetails()
                .thenApply(loginDetails -> loginDetails.getIsLoggedIn()).get();
        } catch (Exception e) {
            throw new AmazonQPluginException("Error occurred while retrieving authentications status", e);
        }
    }
}
