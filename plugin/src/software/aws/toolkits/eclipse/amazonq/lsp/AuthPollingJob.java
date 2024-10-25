// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

/*
 * The Auth Polling Job periodically checks if a user is authenticated. The main purpose is to update the toolbar
 * with the proper menu (see "software.aws.toolkits.eclipse.amazonq.toolbar" in plugin.xml). If the user 
 * is authenticated, display the toolbar with a normal Q icon. If the user is unauthenticated, display the 
 * toolbar with a disconnected Q icon.
 */
public class AuthPollingJob extends Job {
	private static final String JOB_NAME = "Auth Polling Job";
	private static final Object FAMILY = new Object();
	private static final long POLLING_INTERVAL = 5000; // 5 seconds
	private static final long SHORT_POLLING_INTERVAL = 1000; // 1 second
	
    public AuthPollingJob() {
        super(JOB_NAME);
		setSystem(true); // Run in the background. Prevent job from being displayed in Progress view.
    }
    
    @Override
    public boolean belongsTo(Object family) {
        return family == FAMILY;
    }
    
    @Override
	protected IStatus run(IProgressMonitor monitor) {
        if (monitor.isCanceled()) {
            return Status.CANCEL_STATUS;
        }
        
        AuthSourceProvider authSourceProvider = AuthSourceProvider.getProvider();
        
        if (authSourceProvider == null) {
        	schedule(SHORT_POLLING_INTERVAL);
        	return Status.warning("Unable to update authentication status. No AuthSourceProvider instance retrieved.");
        }
		
		Boolean isAuthenticated = checkUserIsAuthenticated();
		
		Display.getDefault().asyncExec(() -> {
			authSourceProvider.setIsAuthenticated(isAuthenticated);
		});
		
		// Schedule next occurence
		cancelExistingJobs();  // ensure only one job is scheduled
		schedule(POLLING_INTERVAL);
		
		return Status.OK_STATUS;
	}
    
	public final void start() {
		cancelExistingJobs();  // ensure only one job is scheduled
    	schedule(); 
    }
	
	public final void stop() {
		cancelExistingJobs();
    }
	
	private void cancelExistingJobs() {
		Job.getJobManager().cancel(FAMILY);
	}

    private Boolean checkUserIsAuthenticated() {
        // TODO: Replace with logic that verifies user is authenticated
		AuthSourceProvider authProvider = AuthSourceProvider.getProvider();
		boolean isAuthenticated = (boolean) authProvider.getCurrentState().get(AuthSourceProvider.IS_AUTHENTICATED_VARIABLE_ID);
		return !isAuthenticated;
	}
}
