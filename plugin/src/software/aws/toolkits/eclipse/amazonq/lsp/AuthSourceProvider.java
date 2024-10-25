// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.ISourceProviderService;

public class AuthSourceProvider extends AbstractSourceProvider {
	public static final String IS_AUTHENTICATED_VARIABLE_ID = "is_authenticated";
	private boolean isAuthenticated = false;

	@Override
	public Map<String, Object> getCurrentState() {
		Map<String, Object> state = new HashMap<>();
		state.put(IS_AUTHENTICATED_VARIABLE_ID, isAuthenticated);
		return state;
	}

	@Override
	public void dispose() {
		// Reset the authenticated state
		isAuthenticated = false;

		// Notify listeners that this provider is being disposed
		fireSourceChanged(ISources.WORKBENCH, IS_AUTHENTICATED_VARIABLE_ID, null);
	}

	@Override
	public String[] getProvidedSourceNames() {
		return new String[] { IS_AUTHENTICATED_VARIABLE_ID };
	}

	public void setIsAuthenticated(Boolean isAuthenticated) {
		this.isAuthenticated = isAuthenticated;
		fireSourceChanged(ISources.WORKBENCH, IS_AUTHENTICATED_VARIABLE_ID, isAuthenticated);
	}
	
	public static AuthSourceProvider getProvider() {
    	IWorkbench workbench = PlatformUI.getWorkbench();
        ISourceProviderService sourceProviderService = (ISourceProviderService) workbench.getService(ISourceProviderService.class);
        AuthSourceProvider provider = (AuthSourceProvider) sourceProviderService.getSourceProvider(AuthSourceProvider.IS_AUTHENTICATED_VARIABLE_ID);
		return provider;
	}
}