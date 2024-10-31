// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import software.aws.toolkits.eclipse.amazonq.lsp.model.GetConfigurationFromServerParams;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;

public class AmazonQPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
    public static final String PREFERENCE_STORE_ID = "software.aws.toolkits.eclipse.preferences";
    public static final String TELEMETRY_OPT_IN = "telemtryOptIn";
    public static final String Q_DATA_SHARING = "qDataSharing";

    public AmazonQPreferencePage() {
        super(GRID);
    }

    @Override
    public final void init(final IWorkbench workbench) {
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
    }

    @Override
    protected final void createFieldEditors() {
        createHorizontalSeparator();
        createDataSharingLabel();
        createTelemetryOptInField();
        createHorizontalSeparator();
        createQDataSharingField();
        adjustGridLayout();

        GetConfigurationFromServerParams params = new GetConfigurationFromServerParams();
        params.setSection("aws.q");
        Activator.getLspProvider().getAmazonQServer().thenCompose(server -> server.getConfigurationFromServer(params));
    }

    private void createHorizontalSeparator() {
        new Label(getFieldEditorParent(), SWT.HORIZONTAL);
    }

    private void createDataSharingLabel() {
        Label dataSharing = new Label(getFieldEditorParent(), SWT.NONE);
        dataSharing.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.HEADER_FONT));
        dataSharing.setText("Data Sharing");
        dataSharing.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        new Label(getFieldEditorParent(), SWT.HORIZONTAL | SWT.SEPARATOR).setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    private void createTelemetryOptInField() {
        Composite telemetryOptInComposite = new Composite(getFieldEditorParent(), SWT.NONE);
        telemetryOptInComposite.setLayout(new GridLayout(2, false));
        GridData telemetryOptInCompositeData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        telemetryOptInCompositeData.horizontalIndent = 20;
        telemetryOptInComposite.setLayoutData(telemetryOptInCompositeData);

        BooleanFieldEditor telemetryOptIn = new BooleanFieldEditor(TELEMETRY_OPT_IN, "Send usage metrics to AWS", telemetryOptInComposite);
        addField(telemetryOptIn);

        Link telemetryLink = createLink("See <a href=\"https://docs.aws.amazon.com/sdkref/latest/guide/overview.html\">here</a> for more detail.",
                20, telemetryOptInComposite);
        telemetryLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent event) {
                PluginUtils.openWebpage(event.text);
            }
        });
    }

    private void createQDataSharingField() {
        Composite qDataSharingComposite = new Composite(getFieldEditorParent(), SWT.NONE);
        qDataSharingComposite.setLayout(new GridLayout(2, false));
        GridData qDataSharingCompositeData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        qDataSharingCompositeData.horizontalIndent = 20;
        qDataSharingComposite.setLayoutData(qDataSharingCompositeData);

        BooleanFieldEditor qDataSharing = new BooleanFieldEditor(Q_DATA_SHARING, "Share Amazon Q Content with AWS", qDataSharingComposite);
        addField(qDataSharing);

        Link dataSharingLink = createLink("""
                When checked, your content processed by Amazon Q may be used for service improvement (except for content processed by the \
                Amazon Q Developer Pro tier).\nUnchecking this box will cause AWS to delete any of your content used for that purpose. The \
                information used to provide the Amazon Q service to you will not be affected.\nSee the \
                <a href="https://aws.amazon.com/service-terms/">Service Terms</a> for more detail.
                """, 20, qDataSharingComposite);
        dataSharingLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent event) {
                PluginUtils.openWebpage(event.text);
            }
        });
    }

    private Link createLink(final String text, final int horizontalIndent, final Composite parent) {
        Link link = new Link(parent, SWT.NONE);
        link.setText(text);
        GridData linkData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        linkData.horizontalIndent = horizontalIndent;
        link.setLayoutData(linkData);
        return link;
    }
}

