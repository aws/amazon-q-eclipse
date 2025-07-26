// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

import software.aws.toolkits.eclipse.amazonq.lsp.model.SubscriptionDetails;
import software.aws.toolkits.eclipse.amazonq.util.PluginPlatform;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;

/**
 * Dialog that displays Amazon Q subscription details including usage statistics,
 * billing information, and upgrade options.
 */
public final class SubscriptionDetailsDialog extends Dialog {

    private static final String WINDOW_TITLE = "Account Details";
    private static final String UPGRADE_URL = "https://aws.amazon.com/q/developer/pricing/";

    private final SubscriptionDetails subscriptionDetails;
    private Font titleFont;
    private Font descriptionFont;
    private Font smallFont;

    private int smallFontSize = PluginUtils.getPlatform().equals(PluginPlatform.WINDOWS) ? 8 : 12;
    private int mediumFontSize = PluginUtils.getPlatform().equals(PluginPlatform.WINDOWS) ? 10 : 14;
    private int largeFontSize = PluginUtils.getPlatform().equals(PluginPlatform.WINDOWS) ? 12 : 16;

    public SubscriptionDetailsDialog(final Shell parentShell, final SubscriptionDetails subscriptionDetails) {
        super(parentShell);
        this.subscriptionDetails = subscriptionDetails;
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    @Override
    protected void configureShell(final Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(WINDOW_TITLE);
    }

    @Override
    protected Point getInitialSize() {
        return new Point(500, 600);
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        final Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(1, false));

        createFonts();
        createHeader(container);
        createSubscriptionInfo(container);
        createUsageSection(container);
        createBillingSection(container);

        return container;
    }

    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        // Only show upgrade button if not already on Pro tier
        // Use direct field access instead of deprecated getSubscriptionInfo() method
        final String subscriptionType = subscriptionDetails.getSubscriptionTier();
        if (!"Q_DEVELOPER_PRO".equals(subscriptionType)) {
            final Button upgradeButton = createButton(parent, 1001, "Upgrade", false);
            upgradeButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    openUpgradeUrl();
                }
            });
        }

        createButton(parent, Dialog.OK, "Close", true);
    }

    private void createFonts() {
        final Display display = Display.getCurrent();
        final FontData[] fontData = display.getSystemFont().getFontData();

        titleFont = new Font(display, fontData[0].getName(), largeFontSize, SWT.BOLD);
        descriptionFont = new Font(display, fontData[0].getName(), mediumFontSize, SWT.NORMAL);
        smallFont = new Font(display, fontData[0].getName(), smallFontSize, SWT.NORMAL);
    }

    private void createHeader(final Composite parent) {
        final Label titleLabel = new Label(parent, SWT.NONE);
        titleLabel.setText("Amazon Q Developer");
        titleLabel.setFont(titleFont);
        titleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        final Label descriptionLabel = new Label(parent, SWT.WRAP);
        descriptionLabel.setText("View your subscription details and usage information.");
        descriptionLabel.setFont(descriptionFont);
        final GridData descData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        descData.widthHint = 450;
        descriptionLabel.setLayoutData(descData);

        // Add spacing
        final Label spacer = new Label(parent, SWT.NONE);
        spacer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    private void createSubscriptionInfo(final Composite parent) {
        final Composite section = new Composite(parent, SWT.NONE);
        section.setLayout(new GridLayout(2, false));
        section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        final Label sectionTitle = new Label(section, SWT.NONE);
        sectionTitle.setText("Subscription");
        sectionTitle.setFont(titleFont);
        final GridData titleData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        titleData.horizontalSpan = 2;
        sectionTitle.setLayoutData(titleData);

        final Label typeLabel = new Label(section, SWT.NONE);
        typeLabel.setText("Plan:");
        typeLabel.setFont(descriptionFont);

        final Label typeValue = new Label(section, SWT.NONE);
        // Use direct field access instead of deprecated getSubscriptionInfo() method
        typeValue.setText(formatSubscriptionType(subscriptionDetails.getSubscriptionTier()));
        typeValue.setFont(descriptionFont);
        typeValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    private void createUsageSection(final Composite parent) {
        final Composite section = new Composite(parent, SWT.NONE);
        section.setLayout(new GridLayout(1, false));
        section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        final Label sectionTitle = new Label(section, SWT.NONE);
        sectionTitle.setText("Usage");
        sectionTitle.setFont(titleFont);
        sectionTitle.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Create usage bars for each limit type
        if (subscriptionDetails.getLimits() != null) {
            for (final SubscriptionDetails.UsageLimit limit : subscriptionDetails.getLimits()) {
                createUsageBar(section, limit);
            }
        }
    }

    private void createUsageBar(final Composite parent, final SubscriptionDetails.UsageLimit limit) {
        final Composite usageComposite = new Composite(parent, SWT.NONE);
        usageComposite.setLayout(new GridLayout(1, false));
        usageComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        // Usage type and numbers
        final Composite headerComposite = new Composite(usageComposite, SWT.NONE);
        headerComposite.setLayout(new GridLayout(2, false));
        headerComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        final Label typeLabel = new Label(headerComposite, SWT.NONE);
        typeLabel.setText(formatUsageType(limit.getType()));
        typeLabel.setFont(descriptionFont);

        final Label usageLabel = new Label(headerComposite, SWT.NONE);
        final String usageText = String.format("%s / %s",
            NumberFormat.getNumberInstance().format(limit.getCurrentUsage()),
            NumberFormat.getNumberInstance().format(limit.getTotalUsageLimit()));
        usageLabel.setText(usageText);
        usageLabel.setFont(smallFont);
        usageLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));

        // Progress bar
        final ProgressBar progressBar = new ProgressBar(usageComposite, SWT.HORIZONTAL);
        progressBar.setMaximum(100);
        progressBar.setSelection((int) Math.round(limit.getPercentUsed()));
        progressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Add spacing
        final Label spacer = new Label(usageComposite, SWT.NONE);
        spacer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    private void createBillingSection(final Composite parent) {
        // Always show billing section since we have direct field access
        final Composite section = new Composite(parent, SWT.NONE);
        section.setLayout(new GridLayout(2, false));
        section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        final Label sectionTitle = new Label(section, SWT.NONE);
        sectionTitle.setText("Billing Cycle");
        sectionTitle.setFont(titleFont);
        final GridData titleData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        titleData.horizontalSpan = 2;
        sectionTitle.setLayoutData(titleData);

        // Days until reset - use direct field access
        final Label daysLabel = new Label(section, SWT.NONE);
        daysLabel.setText("Days until reset:");
        daysLabel.setFont(descriptionFont);

        final Label daysValue = new Label(section, SWT.NONE);
        daysValue.setText(String.valueOf(subscriptionDetails.getDaysUntilReset()));
        daysValue.setFont(descriptionFont);

        // Next reset date - use direct field access
        if (subscriptionDetails.getSubscriptionPeriodReset() != null) {
            final Label resetLabel = new Label(section, SWT.NONE);
            resetLabel.setText("Next reset:");
            resetLabel.setFont(descriptionFont);

            final Label resetValue = new Label(section, SWT.NONE);
            resetValue.setText(formatDate(subscriptionDetails.getSubscriptionPeriodReset()));
            resetValue.setFont(descriptionFont);
        }

        // Overage information - use direct field access
        if (subscriptionDetails.getQueryOverage() > 0) {
            final Label overageLabel = new Label(section, SWT.NONE);
            overageLabel.setText("Current overages:");
            overageLabel.setFont(descriptionFont);

            final Label overageValue = new Label(section, SWT.NONE);
            overageValue.setText(String.valueOf(subscriptionDetails.getQueryOverage()));
            overageValue.setFont(descriptionFont);
        }
    }

    private String formatSubscriptionType(final String type) {
        if (type == null) {
            return "Unknown";
        }
        switch (type) {
            case "Q_DEVELOPER_STANDALONE_FREE":
                return "Amazon Q Developer Free";
            case "Q_DEVELOPER_STANDALONE":
                return "Amazon Q Developer Free";
            case "Q_DEVELOPER_PRO":
                return "Amazon Q Developer Pro";
            default:
                return type.replace("_", " ");
        }
    }

    private String formatUsageType(final String type) {
        if (type == null) {
            return "Unknown";
        }
        switch (type) {
            case "AI_EDITOR":
                return "AI Editor";
            case "AGENTIC_REQUEST":
                return "Agentic Requests";
            case "TRANSFORM":
                return "Code Transformations";
            case "CODE_COMPLETIONS":
                return "Code Completions";
            default:
                return type.replace("_", " ");
        }
    }

    private String formatDate(final String isoDate) {
        try {
            final Instant instant = Instant.parse(isoDate);
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
                .withZone(ZoneId.systemDefault());
            return formatter.format(instant);
        } catch (Exception e) {
            return isoDate;
        }
    }

    private void openUpgradeUrl() {
        try {
            Program.launch(UPGRADE_URL);
        } catch (Exception e) {
            // Fallback - could show error dialog or log
            System.err.println("Failed to open upgrade URL: " + e.getMessage());
        }
    }

    @Override
    public boolean close() {
        if (titleFont != null && !titleFont.isDisposed()) {
            titleFont.dispose();
        }
        if (descriptionFont != null && !descriptionFont.isDisposed()) {
            descriptionFont.dispose();
        }
        if (smallFont != null && !smallFont.isDisposed()) {
            smallFont.dispose();
        }
        return super.close();
    }
}
