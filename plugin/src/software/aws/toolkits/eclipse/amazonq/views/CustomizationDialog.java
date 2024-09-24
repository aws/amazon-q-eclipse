// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import java.util.List;
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
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;
import software.aws.toolkits.eclipse.amazonq.views.model.Customization;

public class CustomizationDialog extends Dialog {

    private static final String TITLE = "Amazon Q Customization";
    public static final String CUSTOMIZATION_STORAGE_INTERNAL_KEY = "aws.q.customization.eclipse";
    private static final String CUSTOMIZATION_STORAGE_LSP_KEY = "aws.q.customization";
    private Composite container;
    private Font magnifiedFont;
    private Font boldFont;
    private List<Customization> customizationsResponse;
    private ResponseSelection responseSelection;
    private String selectedCustomisationArn;

    public enum ResponseSelection {
    	AMAZON_Q_FOUNDATION_DEFAULT,
    	CUSTOMIZATION
    }

    private class CustomRadioButton extends Composite {
        private Button radioButton;
        private Label textLabel;
        private Label subtextLabel;

        public CustomRadioButton(final Composite parent, final String text, final String subText, final int style) {
            super(parent, style);
            Composite contentComposite = new Composite(parent, SWT.NONE);
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
            subtextLabel.setForeground(contentComposite.getDisplay().getSystemColor(SWT.COLOR_GRAY));
        }

        public final Button getRadioButton() {
            return radioButton;
        }
    }

    public CustomizationDialog(final Shell parentShell) {
        super(parentShell);
    }

    public void setCustomisationResponse(final List<Customization> customizationsResponse) {
    	this.customizationsResponse = customizationsResponse;
    }

    public final List<Customization> getCustomizationResponse() {
    	return this.customizationsResponse;
    }

    public void setResponseSelection(final ResponseSelection responseSelection) {
    	this.responseSelection = responseSelection;
    }

    public final ResponseSelection getResponseSelection() {
    	return this.responseSelection;
    }

    public void setSelectedCustomizationArn(final String arn) {
    	this.selectedCustomisationArn = arn;
    }

    public final String getSelectedCustomizationArn() {
    	return this.selectedCustomisationArn;
    }

    @Override
    protected final void createButtonsForButtonBar(final Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, "Select", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected final void okPressed() {
        PluginLogger.info(String.format("Select pressed with responseSelection:%s and selectedArn:%s", this.responseSelection, this.selectedCustomisationArn));
        if (this.responseSelection.equals(ResponseSelection.AMAZON_Q_FOUNDATION_DEFAULT)) {
        	PluginStore.remove(CUSTOMIZATION_STORAGE_INTERNAL_KEY);
        } else {
        	// TODO: Add the logic to trigger notification to LSP server regarding change of configuration
        	PluginStore.put(CUSTOMIZATION_STORAGE_INTERNAL_KEY, this.selectedCustomisationArn);
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
    
    private static void addFormattedOption(Combo combo, String name, String description) {
        String formattedText = name + " (" + description + ")";
        combo.add(formattedText);
    }
    
    private void createDropdownForCustomizations(final Composite parent) {
    	Composite contentComposite = new Composite(parent, SWT.NONE);
    	GridLayout layout = new GridLayout(1, false);
    	layout.marginLeft = 15;
        contentComposite.setLayout(layout);
        contentComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        Combo combo = new Combo(contentComposite, SWT.READ_ONLY);
        GridData comboGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        comboGridData.horizontalAlignment = GridData.FILL;
        comboGridData.grabExcessHorizontalSpace = true;
        combo.setLayoutData(comboGridData);
        List<Customization> customizations = this.customizationsResponse;
        int defaultSelectedDropdownIndex = -1;
        for (int index=0; index<customizations.size();index++) {
        	addFormattedOption(combo, customizations.get(index).getName(), customizations.get(index).getDescription());
        	combo.setData(String.format("%s", index), customizations.get(index).getArn());
        	if (this.responseSelection.equals(ResponseSelection.CUSTOMIZATION) 
        			&& StringUtils.isNotBlank(this.selectedCustomisationArn)
        			&& this.selectedCustomisationArn.equals(customizations.get(index).getArn())) {
        		defaultSelectedDropdownIndex = index;
        	}
        }
        combo.select(defaultSelectedDropdownIndex);
        combo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int selectedIndex = combo.getSelectionIndex();
                String selectedOption = combo.getItem(selectedIndex);
                String selectedCustomizationArn = (String) combo.getData(String.valueOf(selectedIndex));
                CustomizationDialog.this.selectedCustomisationArn = selectedCustomizationArn;
                PluginLogger.info(String.format("Selected option:%s with arn:%s", selectedOption, selectedCustomizationArn));
            }
        });
    }

    @Override
    protected final Control createDialogArea(final Composite parent) {
        container = (Composite) super.createDialogArea(parent);
        GridLayout gridLayout = new GridLayout(1, false);
        container.setLayout(gridLayout);
        container.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));
        Label heading = createLabelWithFontSize(container, "Select an Amazon Q customization", 18);
        boldFont(heading.getFont());
        Boolean isDefaultAmazonQFoundationSelected = this.responseSelection.equals(ResponseSelection.AMAZON_Q_FOUNDATION_DEFAULT);
        CustomRadioButton defaultAmazonQFoundationButton = createCustomRadioButton(container, "Amazon Q foundation (Default)", 
        		"Receive suggestions from Amazon Q base model.", SWT.NONE, isDefaultAmazonQFoundationSelected);
        CustomRadioButton customizationButton = createCustomRadioButton(container, "Customization", 
        		"Receive Amazon Q suggestions based on your company's codebase.", SWT.NONE, !isDefaultAmazonQFoundationSelected);
        defaultAmazonQFoundationButton.getRadioButton().addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            	customizationButton.getRadioButton().setSelection(false);
            	responseSelection = ResponseSelection.AMAZON_Q_FOUNDATION_DEFAULT;
            	selectedCustomisationArn = null;
            }
        });
        customizationButton.getRadioButton().addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            	defaultAmazonQFoundationButton.getRadioButton().setSelection(false);
            	responseSelection = ResponseSelection.CUSTOMIZATION;
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

    private CustomRadioButton createCustomRadioButton(final Composite parent, final String text, final String subtext, final int style, final boolean isSelected) {
        CustomRadioButton button = new CustomRadioButton(parent, text, subtext, style);
        button.getRadioButton().setSelection(isSelected);
        return button;
    }

    @Override
    protected final void configureShell(final Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(TITLE);
    }

    @Override
    protected final Point getInitialSize() {
        return new Point(600, 450);
    }

    @Override
    public final boolean close() {
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

    public final void disposeIndependentElements() {
        if (this.magnifiedFont != null && !this.magnifiedFont.isDisposed()) {
            this.magnifiedFont.dispose();
        }
        if (this.boldFont != null && !this.boldFont.isDisposed()) {
            this.boldFont.dispose();
        }
    }
}
