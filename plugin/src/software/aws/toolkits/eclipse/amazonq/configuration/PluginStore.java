// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.configuration;

import java.nio.charset.StandardCharsets;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import com.google.gson.Gson;

import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;

public final class PluginStore {

    private static final Preferences PREFERENCES = Preferences.userRoot().node("software.aws.toolkits.eclipse");

    private static final Gson GSON = new Gson();

    private PluginStore() {
        // Prevent instantiation
    }

    public static void put(final String key, final String value) {
        PREFERENCES.put(key, value);
        try {
            PREFERENCES.flush();
        } catch (BackingStoreException e) {
            PluginLogger.warn(String.format("Error while saving entry to a preference store - key: %s, value: %s", key, value), e);
        }
    }

    public static String get(final String key) {
        return PREFERENCES.get(key, null);
    }

    public static void remove(final String key) {
        PREFERENCES.remove(key);
    }

    public static <T> void putObject(final String key, final T value) {
        String jsonValue = GSON.toJson(value);
        byte[] byteValue = jsonValue.getBytes(StandardCharsets.UTF_8);
        PREFERENCES.putByteArray(key, byteValue);
        try {
            PREFERENCES.flush();
        } catch (BackingStoreException e) {
            PluginLogger.warn(String.format("Error while saving entry to a preference store - key: %s, value: %s", key, value), e);
        }
    }

    public static <T> T getObject(final String key, final Class<T> type) {
        byte[] byteValue = PREFERENCES.getByteArray(key, null);
        if (byteValue == null) {
            return null;
        }
        String jsonValue = new String(byteValue, StandardCharsets.UTF_8);
        return GSON.fromJson(jsonValue, type);
    }
}
