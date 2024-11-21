// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
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
import software.aws.toolkits.eclipse.amazonq.telemetry.AwsTelemetryProvider;
import software.aws.toolkits.eclipse.amazonq.telemetry.UiTelemetryProvider;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;

public class AmazonQPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
    public static final String PREFERENCE_STORE_ID = "software.aws.toolkits.eclipse.preferences";
    public static final String CODE_REFERENCE_OPT_IN = "codeReferenceOptIn";
    public static final String TELEMETRY_OPT_IN = "telemetryOptIn";
    public static final String Q_DATA_SHARING = "qDataSharing";

    private boolean isTelemetryOptInChecked;
    private boolean isQDataSharingOptInChecked;

    private IPreferenceStore preferenceStore;

    public AmazonQPreferencePage() {
        super(GRID);
        preferenceStore = Activator.getDefault().getPreferenceStore();
    }

    @Override
    public final void init(final IWorkbench workbench) {
        isTelemetryOptInChecked = preferenceStore.getBoolean(TELEMETRY_OPT_IN);
        isQDataSharingOptInChecked = preferenceStore.getBoolean(Q_DATA_SHARING);
        setPreferenceStore(preferenceStore);
    }

    @Override
    protected final void createFieldEditors() {
        createHorizontalSeparator();
        createHeading("Inline Suggestions");
        createCodeReferenceOptInField();
        createHeading("Data Sharing");
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

    private void createHeading(final String text) {
        Label dataSharing = new Label(getFieldEditorParent(), SWT.NONE);
        dataSharing.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.HEADER_FONT));
        dataSharing.setText(text);
        dataSharing.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        new Label(getFieldEditorParent(), SWT.HORIZONTAL | SWT.SEPARATOR).setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    private void createCodeReferenceOptInField() {
        Composite codeReferenceOptInComposite = new Composite(getFieldEditorParent(), SWT.NONE);
        codeReferenceOptInComposite.setLayout(new GridLayout(2, false));
        GridData telemetryOptInCompositeData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        telemetryOptInCompositeData.horizontalIndent = 20;
        codeReferenceOptInComposite.setLayoutData(telemetryOptInCompositeData);

        BooleanFieldEditor codeReferenceOptIn = new BooleanFieldEditor(CODE_REFERENCE_OPT_IN,
                "Show inline code suggestions with code references", codeReferenceOptInComposite);
        addField(codeReferenceOptIn);

        Link codeReferenceLink = createLink("""
                Amazon Q creates a code reference when you insert a code suggestion from Amazon Q that is similar to training data.\
                \nWhen unchecked, Amazon Q will not show code suggestions that have code references. If you authenticate through IAM\
                \nIdentity Center, this setting is controlled by your Amazon Q administrator. \
                \n<a href=\"https://docs.aws.amazon.com/amazonq/latest/qdeveloper-ug/code-reference.html\">Learn more</a>
                """, 20, codeReferenceOptInComposite);
        codeReferenceLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent event) {
                UiTelemetryProvider.emitClickEventMetric("preferences_codeReferences");
                PluginUtils.openWebpage(event.text);
            }
        });
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
                UiTelemetryProvider.emitClickEventMetric("preferences_telemetryLink");
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
                When checked, your content processed by Amazon Q may be used for service improvement (except for content processed\
                \nby the Amazon Q Developer Pro tier). Unchecking this box will cause AWS to delete any of your content used for that\
                \npurpose. The information used to provide the Amazon Q service to you will not be affected.\
                \nSee the <a href="https://aws.amazon.com/service-terms/">Service Terms</a> for more detail.
                """, 20, qDataSharingComposite);
        dataSharingLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent event) {
                UiTelemetryProvider.emitClickEventMetric("preferences_dataSharingLink");
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

    @Override
    protected final void performDefaults() {
        super.performDefaults();
        sendUpdatedPreferences();
    }

    @Override
    protected final void performApply() {
        super.performApply();
        sendUpdatedPreferences();
    }

    @Override
    public final boolean performOk() {
        boolean result = super.performOk();
        sendUpdatedPreferences();
        return result;
    }

    private void sendUpdatedPreferences() {
        Boolean newIsTelemetryOptInChecked = preferenceStore.getBoolean(TELEMETRY_OPT_IN);
        if (newIsTelemetryOptInChecked != isTelemetryOptInChecked) {
            AwsTelemetryProvider.emitModifySettingEvent("amazonQ.telemetry", newIsTelemetryOptInChecked.toString());
            isTelemetryOptInChecked = newIsTelemetryOptInChecked;
        }

        Boolean newIsQDataSharingOptInChanged = preferenceStore.getBoolean(Q_DATA_SHARING);
        if (newIsQDataSharingOptInChanged != isQDataSharingOptInChecked) {
            AwsTelemetryProvider.emitModifySettingEvent("amazonQ.dataSharing", newIsQDataSharingOptInChanged.toString());
            isQDataSharingOptInChecked = newIsQDataSharingOptInChanged;
        }
    }

}

