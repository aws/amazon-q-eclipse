// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public class AmazonQPreferenceInitializer extends AbstractPreferenceInitializer {

    @Override
    public final void initializeDefaultPreferences() {
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        store.setDefault(AmazonQPreferencePage.TELEMETRY_OPT_IN, true);
    }

}
