// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.notification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.lsp.AmazonQLspClient;

/**
 * Maintains client state information for the notification system.
 * 
 * This class tracks both static and dynamic information about the Eclipse environment
 * that the notification server uses to determine which notifications to show.
 */
public class NotificationConfiguration{
	
	// Singleton, needed one for whole eclipse session
	private static NotificationConfiguration instance;
	
	private final AmazonQLspClient lspClient;
	
	private final List<String> contexts = new ArrayList<>();
	
	private final Map<String, List<String>> clientStates = new HashMap<>();
	
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	private final AtomicBoolean pendingUpdate = new AtomicBoolean(false);
	private static final int DEBOUNCE_DELAY_MS = 1000; //1 sec debounce
	
	/**
     * Gets the singleton instance of NotificationConfiguration.
     * 
     * @param lspClient The LSP client to use for communication with the server
     * @return The NotificationConfiguration instance
     */
	public static synchronized NotificationConfiguration getInstance(AmazonQLspClient lspClient) {
		if (instance == null) {
			instance = new NotificationConfiguration(lspClient);
		}
		return instance;
	}
	
	/**
     * Private constructor to enforce singleton pattern.
     * 
     * @param lspClient The LSP client to use for communication
     */
	private NotificationConfiguration(AmazonQLspClient lspClient) {
		this.lspClient = lspClient;
		initializeStaticClientStates();
	}
	
	/**
     * Initialize static client states that won't change during the Eclipse session.
     */
	private void initializeStaticClientStates() {
		clientStates.put("IDE/VERSION", Collections.singletonList(getEclipseVersion()));
		
        // Add other static values as needed
        // clientStates.put("ANOTHER_STATIC_VALUE", getSomeOtherStaticValue());
	}
	
	private String getEclipseVersion() {
        //placeholder - need to use the actual Eclipse API to get the version
        return org.eclipse.core.runtime.Platform.getProduct().getDefiningBundle().getVersion().toString();
    }
	
	/**
     * Add a context to the active contexts list.
     * 
     * @param context The context to add
     */
	public synchronized void addContext(String context) {
		if  (!contexts.contains(context)) {
			contexts.add(context);
			notifyConfigurationChanged();
		}
	}
	
	/**
     * Remove a context from the active contexts list.
     * 
     * @param context The context to remove
     */
	public synchronized void removeContext(String context) {
		if (contexts.contains(context)) {
			contexts.remove(context);
			notifyConfigurationChanged();
		}
	}

	/**
     * Set SSO scopes.
     * 
     * @param scopes The list of SSO scopes
     */
	public synchronized void setSsoScopes(List<String> scopes) {
		List<String> scopesCopy = new ArrayList<>(scopes);
		clientStates.put("SSO_SCOPES", scopesCopy);
		notifyConfigurationChanged();
	}
	
	/**
     * Get the current notification configuration.
     * 
     * @return A map representing the current notification configuration
     */
	public synchronized Map<String, Object> getConfiguration() {
		Map<String, Object> config = new HashMap<>();
		config.putAll(clientStates);
		config.put("CONTEXT", new ArrayList<>(contexts));
		
		return config;
	}
	
	/**
     * Notify the server of a configuration change in a try catch block using didChangeConfiguration
     */
	private void notifyConfigurationChanged() {
		if (pendingUpdate.compareAndSet(false, true)) {
			scheduler.schedule(() -> {
				try {
					lspClient.didChangeConfiguration(null);
					Activator.getLogger().info("sent didChangeConfiguration notification");
				} catch  (Exception e) {
					Activator.getLogger().error("Error sending didChangeConfiguration notification", e);
				} finally {
					pendingUpdate.set(false);
				}
			}, DEBOUNCE_DELAY_MS, TimeUnit.MILLISECONDS);
		}
	}
	
	/**
     * Handles the getConfiguration request from the server.
     * 
     * @param section The configuration section requested
     * @return The requested configuration section
     */
	public Object handleGetConfiguration(String section) {
		if ("notifications".equals(section)) {
			return getConfiguration();
		}
		
		// Handle other sections or return null if not recognized
        return null;
	}
	
}