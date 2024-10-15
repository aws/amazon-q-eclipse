// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

public final class PreferenceStoreUtil {
    private IEclipsePreferences preferences;

    public PreferenceStoreUtil(final String node) {
        preferences = InstanceScope.INSTANCE.getNode(node);
    }

    public boolean getBoolean(final String attr, final boolean defaultAns) {
        return preferences.getBoolean(attr, defaultAns);
    }

    public void putBoolean(final String attr, final boolean val) {
        preferences.putBoolean(attr, val);
    }

    public int getInt(final String attr, final int defaultAns) {
        return preferences.getInt(attr, defaultAns);
    }
}
