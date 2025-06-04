// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import software.amazon.awssdk.utils.StringUtils;
import software.aws.toolkits.eclipse.amazonq.configuration.customization.CustomizationUtil;
import software.aws.toolkits.eclipse.amazonq.configuration.profiles.QDeveloperProfileUtil;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.Constants;
import software.aws.toolkits.eclipse.amazonq.util.PluginPlatform;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;
import software.aws.toolkits.eclipse.amazonq.util.ThreadingUtils;
import software.aws.toolkits.eclipse.amazonq.views.model.Customization;

public final class CustomizationDialog extends Dialog {

    private static final String TITLE = "Amazon Q Customization";
    private Composite container;
    private Combo combo;
    private Font titleFont;
    private List<Customization> customizationsResponse;
    private ResponseSelection responseSelection;
    private Customization selectedCustomization;
    private final int smallFont = PluginUtils.getPlatform().equals(PluginPlatform.WINDOWS) ? 8 : 12;
    private final int mediumFont = PluginUtils.getPlatform().equals(PluginPlatform.WINDOWS) ? 10 : 14;

    public enum ResponseSelection {
        AMAZON_Q_FOUNDATION_DEFAULT,
        CUSTOMIZATION
    }

    private final class RadioButtonWithDescriptor extends Composite {

        private Button radioButton;
        private Label textLabel;
        private Label subtextLabel;
        private Font textFont;
        private Font subtextFont;

        private RadioButtonWithDescriptor(final Composite parent, final String text, final String subtext,
                final int style) {
            super(parent, SWT.NONE);

            GridLayout layout = new GridLayout(1, false);
            layout.marginWidth = 0;
            layout.marginHeight = 0;
            layout.verticalSpacing = 2;
            this.setLayout(layout);

            Composite topRow = new Composite(this, SWT.NONE);
            GridLayout topRowLayout = new GridLayout(2, false);
            topRowLayout.marginWidth = 0;
            topRowLayout.marginHeight = 0;
            topRow.setLayout(topRowLayout);
            topRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

            radioButton = new Button(topRow, SWT.RADIO | style);
            radioButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

            textFont = createFont(smallFont, SWT.NORMAL);

            textLabel = new Label(topRow, SWT.WRAP);
            textLabel.setText(text);
            textLabel.setFont(textFont);
            GridData textData = new GridData(SWT.FILL, SWT.CENTER, true, false);
            textData.horizontalIndent = PluginUtils.getPlatform().equals(PluginPlatform.WINDOWS) ? 3 : 0;
            textLabel.setLayoutData(textData);

            subtextFont = createFont(smallFont, SWT.ITALIC);

            subtextLabel = new Label(this, SWT.WRAP);
            subtextLabel.setText(subtext);
            subtextLabel.setFont(subtextFont);
            subtextLabel.setForeground(getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
            GridData subtextData = new GridData(SWT.FILL, SWT.CENTER, true, false);
            subtextData.horizontalIndent = PluginUtils.getPlatform().equals(PluginPlatform.WINDOWS) ? 21 : 23; // Indent to align with text label
            subtextLabel.setLayoutData(subtextData);

            addDisposeListener(e -> {
                if (subtextFont != null && !subtextFont.isDisposed()) {
                    subtextFont.dispose();
                }
                if (textFont != null && !textFont.isDisposed()) {
                    textFont.dispose();
                }
            });
        }

        public void setSelection(final boolean isSelected) {
            radioButton.setSelection(isSelected);
        }

        private void addSelectionListener(final Runnable runnable) {
            radioButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent event) {
                    runnable.run();
                }
            });

            textLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseDown(final MouseEvent event) {
                    radioButton.setSelection(true);
                    runnable.run();
                }
            });
        }

    }

    public CustomizationDialog(final Shell parentShell) {
        super(parentShell);
    }

    public void setCustomizationResponse(final List<Customization> customizationsResponse) {
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

    public void setSelectedCustomization(final Customization customization) {
        this.selectedCustomization = customization;
    }

    public Customization getSelectedCustomization() {
        return this.selectedCustomization;
    }

    private List<Customization> getCustomizations() {
        List<Customization> customizations = new ArrayList<>();
        try {
            customizations = CustomizationUtil.listCustomizations().get();
        } catch (InterruptedException | ExecutionException e) {
            Activator.getLogger().error("Error occurred in getCustomizations", e);
        }
        return customizations;
    }

    private static void addFormattedOption(final Combo combo, final String name, final String profileName,
            final String description) {
        String formattedText = name + " (" + profileName + ") - " + description;
        combo.add(formattedText);
    }

    private void updateComboOnUIThread(final List<Customization> customizations) {
        combo.removeAll();
        int customizationsCount = 0;
        int selectedCustomizationIndex = 0;
        Customization currentCustomization = Activator.getPluginStore()
                .getObject(Constants.CUSTOMIZATION_STORAGE_INTERNAL_KEY, Customization.class);
        for (int index = 0; index < customizations.size(); index++) {
            if (customizations.get(index).getIsDefault()) {
                continue;
            }
            if (currentCustomization != null
                    && customizations.get(index).getArn().equals(currentCustomization.getArn())) {
                selectedCustomizationIndex = customizationsCount;
            }
            addFormattedOption(combo, customizations.get(index).getName(),
                    customizations.get(index).getProfile().getName(), customizations.get(index).getDescription());
            combo.setData(String.format("%s", customizationsCount), customizations.get(index));
            ++customizationsCount;
        }
        combo.select(selectedCustomizationIndex);
        if (this.responseSelection.equals(ResponseSelection.AMAZON_Q_FOUNDATION_DEFAULT) || customizations.isEmpty()) {
            combo.setEnabled(false);
        } else {
            combo.setEnabled(true);
        }
        combo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                int selectedIndex = combo.getSelectionIndex();
                String selectedOption = combo.getItem(selectedIndex);
                Customization selectedCustomization = (Customization) combo.getData(String.valueOf(selectedIndex));
                CustomizationDialog.this.setSelectedCustomization(selectedCustomization);
                Activator.getLogger().info(String.format("Selected option:%s with arn:%s", selectedOption,
                        selectedCustomization.getArn()));
            }
        });
    }

    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, "Select", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void configureShell(final Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(TITLE);

        newShell.addDisposeListener(e -> {
            if (titleFont != null && !titleFont.isDisposed()) {
                titleFont.dispose();
            }
        });
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        container = (Composite) super.createDialogArea(parent);

        GridLayout mainLayout = new GridLayout(1, false);
        mainLayout.marginWidth = 15;
        mainLayout.marginHeight = 15;
        mainLayout.verticalSpacing = 10;
        container.setLayout(mainLayout);

        titleFont = createFont(mediumFont, SWT.BOLD);

        Label titleLabel = new Label(container, SWT.NONE);
        titleLabel.setText("Select an Amazon Q Customization");
        titleLabel.setFont(titleFont);
        titleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Label separator = new Label(container, SWT.HORIZONTAL | SWT.SEPARATOR);
        separator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Boolean isDefaultAmazonQFoundationSelected = this.responseSelection
                .equals(ResponseSelection.AMAZON_Q_FOUNDATION_DEFAULT);

        RadioButtonWithDescriptor defaultAmazonQFoundationButton = createRadioButton(container,
                "Amazon Q foundation (Default)", "Receive suggestions from Amazon Q base model.", SWT.NONE,
                isDefaultAmazonQFoundationSelected);
        RadioButtonWithDescriptor customizationButton = createRadioButton(container, "Customization",
                "Receive Amazon Q suggestions based on your company's codebase.", SWT.NONE,
                !isDefaultAmazonQFoundationSelected);

        defaultAmazonQFoundationButton.addSelectionListener(() -> {
            customizationButton.setSelection(false);
            defaultAmazonQFoundationButton.setSelection(true);
            responseSelection = ResponseSelection.AMAZON_Q_FOUNDATION_DEFAULT;
            setSelectedCustomization(null);
            combo.setEnabled(false);
        });
        customizationButton.addSelectionListener(() -> {
            defaultAmazonQFoundationButton.setSelection(false);
            customizationButton.setSelection(true);
            responseSelection = ResponseSelection.CUSTOMIZATION;
            combo.setEnabled(true);
        });

        Composite comboComposite = new Composite(container, SWT.NONE);
        GridLayout comboLayout = new GridLayout(1, false);
        comboLayout.marginWidth = 0;
        comboLayout.marginHeight = 0;
        comboLayout.marginLeft = 20;
        comboLayout.verticalSpacing = 0;
        comboComposite.setLayout(comboLayout);

        GridData comboCompositeData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        comboCompositeData.verticalIndent = 0;
        comboComposite.setLayoutData(comboCompositeData);

        combo = new Combo(comboComposite, SWT.READ_ONLY | SWT.DROP_DOWN);

        GridData comboData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        comboData.verticalIndent = 0;
        comboData.grabExcessHorizontalSpace = true;
        comboData.horizontalSpan = 2;
        combo.setLayoutData(comboData);

        GridData containerData = new GridData(GridData.FILL_HORIZONTAL);
        containerData.heightHint = SWT.DEFAULT; // Let the height be determined by contents
        container.setLayoutData(containerData);

        combo.setItems(new String[]{"Loading..."});
        combo.select(0);
        combo.setEnabled(false);

        CompletableFuture.supplyAsync(() -> getCustomizations()).thenAcceptAsync(
                customizations -> updateComboOnUIThread(customizations), Display.getDefault()::asyncExec);

        return container;
    }

    @Override
    protected void okPressed() {
        if (this.responseSelection.equals(ResponseSelection.AMAZON_Q_FOUNDATION_DEFAULT)) {
            Activator.getPluginStore().remove(Constants.CUSTOMIZATION_STORAGE_INTERNAL_KEY);
            Display.getCurrent()
                    .asyncExec(() -> CustomizationUtil.showNotification(Constants.DEFAULT_Q_FOUNDATION_DISPLAY_NAME));
        } else if (Objects.nonNull(this.getSelectedCustomization())
                && StringUtils.isNotBlank(this.getSelectedCustomization().getName())) {
            try {
                QDeveloperProfileUtil.getInstance()
                        .setDeveloperProfile(this.getSelectedCustomization().getProfile(), false)
                        .get();
            } catch (InterruptedException | ExecutionException e) {
                Activator.getLogger().info("Failed to update profile: " + e);
            }
            Activator.getPluginStore().putObject(Constants.CUSTOMIZATION_STORAGE_INTERNAL_KEY,
                    this.getSelectedCustomization());
            Display.getCurrent().asyncExec(() -> CustomizationUtil.showNotification(
                    String.format("%s customization", this.getSelectedCustomization().getName())));
        }
        ThreadingUtils.executeAsyncTask(() -> CustomizationUtil.triggerChangeConfigurationNotification());
        super.okPressed();
    }

    private Font createFont(final int size, final int style) {
        FontData[] fontData = getShell().getDisplay().getSystemFont().getFontData();
        FontData newFontData = new FontData(fontData[0].getName(), size, // specify exact font size
                style); // SWT.NORMAL, SWT.BOLD, SWT.ITALIC, or SWT.BOLD | SWT.ITALIC
        return new Font(getShell().getDisplay(), newFontData);
    }

    private RadioButtonWithDescriptor createRadioButton(final Composite parent, final String text,
            final String subtext, final int style, final boolean isSelected) {
        RadioButtonWithDescriptor button = new RadioButtonWithDescriptor(parent, text, subtext, style);
        button.setSelection(isSelected);
        return button;
    }

}
