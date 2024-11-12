// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.configuration;

import java.nio.charset.StandardCharsets;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

import com.google.gson.Gson;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public final class DefaultPluginStore implements PluginStore {
    private static final Gson GSON = new Gson();

    private static DefaultPluginStore instance;

    private IEclipsePreferences preferences;

    public DefaultPluginStore(final IEclipsePreferences preferences) {
        this.preferences = preferences != null ? preferences : InstanceScope.INSTANCE.getNode("software.aws.toolkits.eclipse");
        // Prevent instantiation
    }

    public static synchronized DefaultPluginStore getInstance() {
        if (instance == null) {
            instance = new DefaultPluginStore(null);
        }
        return instance;
    }

    @Override
    public void put(final String key, final String value) {
        preferences.put(key, value);
        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            Activator.getLogger().warn(String.format("Error while saving entry to a preference store - key: %s, value: %s", key, value), e);
        }
    }

    @Override
    public String get(final String key) {
        return preferences.get(key, null);
    }

    @Override
    public void remove(final String key) {
        preferences.remove(key);
        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            Activator.getLogger().warn(String.format("Error while removing entry from preference store - key: %s", key), e);
        }
    }

    @Override
    public void addChangeListener(final IPreferenceChangeListener prefChangeListener) {
        preferences.addPreferenceChangeListener(prefChangeListener);
    }

    @Override
    public <T> void putObject(final String key, final T value) {
        String jsonValue = GSON.toJson(value);
        byte[] byteValue = jsonValue.getBytes(StandardCharsets.UTF_8);
        preferences.putByteArray(key, byteValue);
        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            Activator.getLogger().warn(String.format("Error while saving entry to a preference store - key: %s, value: %s", key, value), e);
        }
    }

    @Override
    public <T> T getObject(final String key, final Class<T> type) {
        byte[] byteValue = preferences.getByteArray(key, null);
        if (byteValue == null) {
            return null;
        }
        String jsonValue = new String(byteValue, StandardCharsets.UTF_8);
        return GSON.fromJson(jsonValue, type);
    }

}
