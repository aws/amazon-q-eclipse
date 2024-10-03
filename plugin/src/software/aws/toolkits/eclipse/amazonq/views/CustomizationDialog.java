// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import software.amazon.awssdk.utils.StringUtils;
import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;
import software.aws.toolkits.eclipse.amazonq.customization.CustomizationUtil;
import software.aws.toolkits.eclipse.amazonq.util.Constants;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;
import software.aws.toolkits.eclipse.amazonq.util.ThreadingUtils;
import software.aws.toolkits.eclipse.amazonq.views.model.Customization;

public final class CustomizationDialog extends Dialog {

    private static final String TITLE = "Amazon Q Customization";
    private Composite container;
    private Combo combo;
    private Font magnifiedFont;
    private Font boldFont;
    private List<Customization> customizationsResponse;
    private ResponseSelection responseSelection;
    private String selectedCustomisationArn;

    public enum ResponseSelection {
        AMAZON_Q_FOUNDATION_DEFAULT,
        CUSTOMIZATION
    }

    private final class CustomRadioButton extends Composite {
        private Button radioButton;
        private Label textLabel;
        private Label subtextLabel;

        CustomRadioButton(final Composite parent, final String text, final String subText, final int style) {
            super(parent, style);
            Composite contentComposite = new Composite(parent, SWT.FILL);
            GridLayout gridLayout = new GridLayout(2, false);
            contentComposite.setLayout(gridLayout);
            contentComposite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));

            radioButton = new Button(contentComposite, SWT.RADIO);
            radioButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));

            textLabel = createLabelWithFontSize(contentComposite, text, 16);
            textLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));

            new Label(contentComposite, SWT.NONE);

            subtextLabel = createLabelWithFontSize(contentComposite, subText, 16);
            subtextLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));
            subtextLabel.setForeground(contentComposite.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
        }

        public Button getRadioButton() {
            return radioButton;
        }
    }

    public CustomizationDialog(final Shell parentShell) {
        super(parentShell);
    }

    public void setCustomisationResponse(final List<Customization> customizationsResponse) {
        this.customizationsResponse = customizationsResponse;
    }

    public List<Customization> getCustomizationResponse() {
        return this.customizationsResponse;
    }

    public void setResponseSelection(final ResponseSelection responseSelection) {
        this.responseSelection = responseSelection;
    }

    public ResponseSelection getResponseSelection() {
        return this.responseSelection;
    }

    public void setSelectedCustomizationArn(final String arn) {
        this.selectedCustomisationArn = arn;
    }

    public String getSelectedCustomizationArn() {
        return this.selectedCustomisationArn;
    }

    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, "Select", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void okPressed() {
        PluginLogger.info(String.format("Select pressed with responseSelection:%s and selectedArn:%s", this.responseSelection, this.selectedCustomisationArn));
        if (this.responseSelection.equals(ResponseSelection.AMAZON_Q_FOUNDATION_DEFAULT)) {
            PluginStore.remove(Constants.CUSTOMIZATION_STORAGE_INTERNAL_KEY);
        } else {
            PluginStore.put(Constants.CUSTOMIZATION_STORAGE_INTERNAL_KEY, this.selectedCustomisationArn);
            Map<String, Object> updatedSettings = new HashMap<>();
            Map<String, String> internalMap = new HashMap<>();
            internalMap.put(Constants.LSP_CUSTOMIZATION_CONFIGURATION_KEY, this.selectedCustomisationArn);
            updatedSettings.put(Constants.LSP_CONFIGURATION_KEY, internalMap);
            ThreadingUtils.executeAsyncTask(() -> CustomizationUtil.triggerChangeConfigurationNotification(updatedSettings));
        }
        super.okPressed();
    }

    private Font magnifyFontSize(final Font originalFont, final int fontSize) {
        FontData[] fontData = originalFont.getFontData();
        for (int i = 0; i < fontData.length; i++) {
            fontData[i].setHeight(fontSize);
        }
        Font magnifiedFont = new Font(getShell().getDisplay(), fontData);
        if (this.magnifiedFont != null && !this.magnifiedFont.isDisposed()) {
            this.magnifiedFont.dispose();
        }
        this.magnifiedFont = magnifiedFont;
        return magnifiedFont;
    }

    private Font boldFont(final Font originalFont) {
        FontData[] fontData = originalFont.getFontData();
        for (FontData data : fontData) {
            data.setStyle(SWT.BOLD);
        }
        Font boldFont = new Font(getShell().getDisplay(), fontData);
        if (this.boldFont != null && !this.boldFont.isDisposed()) {
            this.boldFont.dispose();
        }
        this.boldFont = boldFont;
        return boldFont;
    }

    private static void addFormattedOption(final Combo combo, final String name, final String description) {
        String formattedText = name + " (" + description + ")";
        combo.add(formattedText);
    }

    private void createDropdownForCustomizations(final Composite parent) {
        Composite contentComposite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        layout.marginLeft = 18;
        contentComposite.setLayout(layout);
        GridData layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        layoutData.horizontalSpan = 2;
        contentComposite.setLayoutData(layoutData);
        combo = new Combo(contentComposite, SWT.READ_ONLY);
        combo.setLayout(new GridLayout());
        GridData comboGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        comboGridData.horizontalAlignment = GridData.FILL;
        comboGridData.grabExcessHorizontalSpace = true;
        comboGridData.horizontalSpan = 2;
        combo.setLayoutData(comboGridData);
        List<Customization> customizations = this.customizationsResponse;
        int defaultSelectedDropdownIndex = -1;
        for (int index = 0; index < customizations.size(); index++) {
            addFormattedOption(combo, customizations.get(index).getName(), customizations.get(index).getDescription());
            combo.setData(String.format("%s", index), customizations.get(index).getArn());
            if (this.responseSelection.equals(ResponseSelection.CUSTOMIZATION)
                    && StringUtils.isNotBlank(this.selectedCustomisationArn)
                    && this.selectedCustomisationArn.equals(customizations.get(index).getArn())) {
                defaultSelectedDropdownIndex = index;
            }
        }
        combo.select(defaultSelectedDropdownIndex);
        if (this.responseSelection.equals(ResponseSelection.AMAZON_Q_FOUNDATION_DEFAULT)) {
            combo.setEnabled(false);
        }
        combo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                int selectedIndex = combo.getSelectionIndex();
                String selectedOption = combo.getItem(selectedIndex);
                String selectedCustomizationArn = (String) combo.getData(String.valueOf(selectedIndex));
                CustomizationDialog.this.selectedCustomisationArn = selectedCustomizationArn;
                PluginLogger.info(String.format("Selected option:%s with arn:%s", selectedOption, selectedCustomizationArn));
            }
        });
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        container = (Composite) super.createDialogArea(parent);
        GridLayout gridLayout = new GridLayout(2, false);
        gridLayout.marginLeft = 10;
        gridLayout.marginRight = 10;
        gridLayout.marginTop = 10;
        container.setLayout(gridLayout);
        container.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));
        Label heading = createLabelWithFontSize(container, "Select an Amazon Q Customization", 18);
        GridData layoutWithHorizontalSpan = new GridData();
        layoutWithHorizontalSpan.horizontalSpan = 2;
        heading.setLayoutData(layoutWithHorizontalSpan);
        heading.setFont(boldFont(heading.getFont()));
        Boolean isDefaultAmazonQFoundationSelected = this.responseSelection.equals(ResponseSelection.AMAZON_Q_FOUNDATION_DEFAULT);
        CustomRadioButton defaultAmazonQFoundationButton = createCustomRadioButton(container, "Amazon Q foundation (Default)",
                "Receive suggestions from Amazon Q base model.", SWT.NONE, isDefaultAmazonQFoundationSelected);
        defaultAmazonQFoundationButton.setLayoutData(layoutWithHorizontalSpan);
        CustomRadioButton customizationButton = createCustomRadioButton(container, "Customization",
                "Receive Amazon Q suggestions based on your company's codebase.", SWT.NONE, !isDefaultAmazonQFoundationSelected);
        customizationButton.setLayoutData(layoutWithHorizontalSpan);
        defaultAmazonQFoundationButton.getRadioButton().addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                customizationButton.getRadioButton().setSelection(false);
                responseSelection = ResponseSelection.AMAZON_Q_FOUNDATION_DEFAULT;
                selectedCustomisationArn = null;
                combo.setEnabled(false);
            }
        });
        customizationButton.getRadioButton().addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                defaultAmazonQFoundationButton.getRadioButton().setSelection(false);
                responseSelection = ResponseSelection.CUSTOMIZATION;
                combo.setEnabled(true);
            }
        });
        createDropdownForCustomizations(container);
        createSeparator(container);
        return container;
    }

    private Label createLabelWithFontSize(final Composite parent, final String text, final int fontSize) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(text);
        label.setFont(magnifyFontSize(label.getFont(), fontSize));
        return label;
    }

    private void createSeparator(final Composite parent) {
        Label separatorLabel = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridData separatorLayout = new GridData(SWT.FILL, SWT.CENTER, true, false);
        separatorLabel.setLayoutData(separatorLayout);
    }

    private CustomRadioButton createCustomRadioButton(final Composite parent, final String text,
        final String subtext, final int style, final boolean isSelected) {
        CustomRadioButton button = new CustomRadioButton(parent, text, subtext, style);
        button.getRadioButton().setSelection(isSelected);
        return button;
    }

    @Override
    protected void configureShell(final Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(TITLE);
    }

    @Override
    protected Point getInitialSize() {
        return new Point(600, 400);
    }

    @Override
    public boolean close() {
        disposeAllComponents(container);
        disposeIndependentElements();
        return super.close();
    }

    private void disposeAllComponents(final Composite container) {
        for (Control control : container.getChildren()) {
            if (control instanceof Composite) {
                disposeAllComponents((Composite) control);
            } else {
                control.dispose();
            }
        }
    }

    public void disposeIndependentElements() {
        if (this.magnifiedFont != null && !this.magnifiedFont.isDisposed()) {
            this.magnifiedFont.dispose();
        }
        if (this.boldFont != null && !this.boldFont.isDisposed()) {
            this.boldFont.dispose();
        }
    }
}
