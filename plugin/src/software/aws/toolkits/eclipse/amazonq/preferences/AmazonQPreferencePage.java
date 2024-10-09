// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public class AmazonQPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final String PREFERENCE_STORE_ID = "software.aws.toolkits.eclipse.preferences";
    public static final String TELEMETRY_OPT_IN = "telemtryOptIn";

    public AmazonQPreferencePage() {
        super(GRID);
    }

    @Override
    public final void init(final IWorkbench workbench) {
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        setDescription("Amazon Q Preferences");
    }

    @Override
    protected final void createFieldEditors() {
        BooleanFieldEditor telemetryOptIn = new BooleanFieldEditor(TELEMETRY_OPT_IN, "&Send usage metrics to AWS", getFieldEditorParent());
        addField(telemetryOptIn);
    }

}
