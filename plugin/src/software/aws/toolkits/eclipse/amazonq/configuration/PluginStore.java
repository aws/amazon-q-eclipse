// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.configuration;

import java.util.prefs.PreferenceChangeListener;

public interface PluginStore {
    void put(String key, String value);
    String get(String key);
    void remove(String key);
    void addChangeListener(PreferenceChangeListener prefChangeListener);
    <T> void putObject(String key, T value);
    <T> T getObject(String key, Class<T> type);
}
