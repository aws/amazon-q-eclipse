// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.configuration;

import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;

public interface PluginStore {
    void put(String key, String value);
    String get(String key);
    void remove(String key);
    void addChangeListener(IPreferenceChangeListener prefChangeListener);
    <T> void putObject(String key, T value);
    <T> T getObject(String key, Class<T> type);
}
