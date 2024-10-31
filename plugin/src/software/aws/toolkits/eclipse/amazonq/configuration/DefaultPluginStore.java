// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.configuration;

import java.nio.charset.StandardCharsets;
import java.util.prefs.BackingStoreException;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import com.google.gson.Gson;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public final class DefaultPluginStore implements PluginStore {
    private static final Preferences PREFERENCES = Preferences.userRoot().node("software.aws.toolkits.eclipse");
    private static final Gson GSON = new Gson();

    private static DefaultPluginStore instance;

    private DefaultPluginStore() {
        // Prevent instantiation
    }

    public static synchronized DefaultPluginStore getInstance() {
        if (instance == null) {
            instance = new DefaultPluginStore();
        }
        return instance;
    }

    @Override
    public void put(final String key, final String value) {
        PREFERENCES.put(key, value);
        try {
            PREFERENCES.flush();
        } catch (BackingStoreException e) {
            Activator.getLogger().warn(String.format("Error while saving entry to a preference store - key: %s, value: %s", key, value), e);
        }
    }

    @Override
    public String get(final String key) {
        return PREFERENCES.get(key, null);
    }

    @Override
    public void remove(final String key) {
        PREFERENCES.remove(key);
    }

    @Override
    public void addChangeListener(final PreferenceChangeListener prefChangeListener) {
        PREFERENCES.addPreferenceChangeListener(prefChangeListener);
    }

    @Override
    public <T> void putObject(final String key, final T value) {
        String jsonValue = GSON.toJson(value);
        byte[] byteValue = jsonValue.getBytes(StandardCharsets.UTF_8);
        PREFERENCES.putByteArray(key, byteValue);
        try {
            PREFERENCES.flush();
        } catch (BackingStoreException e) {
            Activator.getLogger().warn(String.format("Error while saving entry to a preference store - key: %s, value: %s", key, value), e);
        }
    }

    @Override
    public <T> T getObject(final String key, final Class<T> type) {
        byte[] byteValue = PREFERENCES.getByteArray(key, null);
        if (byteValue == null) {
            return null;
        }
        String jsonValue = new String(byteValue, StandardCharsets.UTF_8);
        return GSON.fromJson(jsonValue, type);
    }

}