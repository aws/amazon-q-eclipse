// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import java.io.IOException;
import java.net.URL;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import software.aws.toolkits.eclipse.amazonq.util.ClientMetadata;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;

public class FeedbackDialog extends Dialog {

    private static final String TITLE = "Share Feedback";
    private static final int MAX_CHAR_LIMIT = 2000;
    private Composite container;
    private Text commentBox;
    private Font magnifiedFont;
    private Image loadedImage;
    private Label characterRemainingLabel;
    private Sentiment selectedSentiment = Sentiment.POSITIVE;

    public class CustomRadioButton extends Composite {
        private Label iconLabel;
        private Label textLabel;
        private Button radioButton;

        public CustomRadioButton(final Composite parent, final Image image, final String text, final int style) {
            super(parent, style);

            Composite contentComposite = new Composite(parent, SWT.NONE);
            contentComposite.setLayout(new GridLayout(1, false));

            iconLabel = new Label(contentComposite, SWT.NONE);
            iconLabel.setImage(image);
            iconLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

            textLabel = new Label(contentComposite, SWT.NONE);
            textLabel.setText(text);
            textLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));

            radioButton = new Button(contentComposite, SWT.RADIO);
            radioButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
        }

        public final Button getRadioButton() {
            return radioButton;
        }
    }

    enum Sentiment {
        POSITIVE,
        NEGATIVE
    }

    public FeedbackDialog(final Shell parentShell) {
        super(parentShell);
    }

    private Image loadImage(final String imagePath) {
        Image loadedImage = null;
        try {
            URL imageUrl = PluginUtils.getResource(imagePath);
            if (imageUrl != null) {
                // TODO: Need to add disposing logic for images
                loadedImage = new Image(Display.getCurrent(), imageUrl.openStream());
                this.loadedImage = loadedImage;
            }
        } catch (IOException e) {
            PluginLogger.warn(e.getMessage(), e);
        }
        return loadedImage;
    }

    @Override
    protected final void createButtonsForButtonBar(final Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, "Share", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected final void okPressed() {
        Sentiment selectedSentiment = this.selectedSentiment;
        String comment = commentBox.getText();
        // TODO: to remove the info log post call to telemetry feedback api implementation
        PluginLogger.info(String.format("Selected sentiment: %s and comment: %s", selectedSentiment.toString(), comment));
        super.okPressed();
    }

    private Font magnifyFontSize(final Font originalFont, final int fontSize) {
        FontData[] fontData = originalFont.getFontData();
        for (int i = 0; i < fontData.length; i++) {
            fontData[i].setHeight(fontSize);
        }
        Font magnifiedFont = new Font(getShell().getDisplay(), fontData);
        // Dispose the previous magnifiedFont, if it exists
        if (this.magnifiedFont != null && !this.magnifiedFont.isDisposed()) {
            this.magnifiedFont.dispose();
        }
        this.magnifiedFont = magnifiedFont;
        return magnifiedFont;
    }

    private void handleTextModified(final ModifyEvent event) {
        Text text = (Text) event.widget;
        if (text.getText().length() > MAX_CHAR_LIMIT) {
            text.setText(text.getText().substring(0, MAX_CHAR_LIMIT));
            text.setSelection(MAX_CHAR_LIMIT); // Move the caret to the end of the text
        }
        updateCharacterRemainingCount();
    }

    @Override
    protected final Control createDialogArea(final Composite parent) {
        container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(1, false));

        createHeaderSection(container);
        createJoinUsOnGithubSection(container);
        createReportRequestContributeSection(container);
        createShareFeedbackSection(container);
        createQuestionSection(container);

        return container;
    }

    private void createHeaderSection(final Composite container) {
        Composite headlineContainer = new Composite(container, SWT.NONE);
        RowLayout rowLayout = new RowLayout();
        rowLayout.spacing = 0; // to reduce space between two labels
        headlineContainer.setLayout(rowLayout);

        createLabelWithFontSize(headlineContainer, "Looking for help? View the", 14);
        createLinkLabel(headlineContainer, "Getting Started Guide", 14, "https://aws.amazon.com/q/getting-started/");
        createLabelWithFontSize(headlineContainer, "or search our", 14);
        createLinkLabel(headlineContainer, "Documentation", 14, "https://docs.aws.amazon.com/amazonq/latest/qdeveloper-ug/what-is.html");
    }

    private void createJoinUsOnGithubSection(final Composite container) {
        Composite joinUsOnGithubContainer = new Composite(container, SWT.NONE);
        GridLayout joinUsOnGithubLayout = new GridLayout(2, false);
        joinUsOnGithubLayout.horizontalSpacing = 5;
        joinUsOnGithubContainer.setLayout(joinUsOnGithubLayout);
        joinUsOnGithubContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        createLabelWithFontSize(joinUsOnGithubContainer, "Join us on GitHub", 14);
        createSeparator(joinUsOnGithubContainer);
    }

    private void createReportRequestContributeSection(final Composite container) {
        Composite reportRequestContributeContainer = new Composite(container, SWT.NONE);
        GridLayout reportRequestContributeContainerLayout = new GridLayout(2, false);
        reportRequestContributeContainerLayout.horizontalSpacing = 10;
        reportRequestContributeContainerLayout.marginLeft = 10;
        reportRequestContributeContainer.setLayout(reportRequestContributeContainerLayout);

        createImageLabel(reportRequestContributeContainer, "icons/ReportAnIssue.png");
        createLinkLabel(reportRequestContributeContainer, "Report an issue", SWT.NONE,
            String.format("https://github.com/aws/amazon-q-eclipse/issues/new?body=%s", getBodyMessageForReportIssueOrRequestFeature()));

        createImageLabel(reportRequestContributeContainer, "icons/RequestFeature.png");
        createLinkLabel(reportRequestContributeContainer, "Request a feature", SWT.NONE,
            String.format("https://github.com/aws/amazon-q-eclipse/issues/new?body=%s", getBodyMessageForReportIssueOrRequestFeature()));

        createImageLabel(reportRequestContributeContainer, "icons/ViewCode.png");
        createLinkLabel(reportRequestContributeContainer, "View source code and contribute", SWT.NONE, "https://github.com/aws/amazon-q-eclipse/");
    }

    private void createShareFeedbackSection(final Composite container) {
        Composite shareFeedbackTextContainer = new Composite(container, SWT.NONE);
        GridLayout shareFeedbackTextContainerLayout = new GridLayout(2, false);
        shareFeedbackTextContainerLayout.horizontalSpacing = 5;
        shareFeedbackTextContainer.setLayout(shareFeedbackTextContainerLayout);
        shareFeedbackTextContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        createLabelWithFontSize(shareFeedbackTextContainer, "Share feedback", 14);
        createSeparator(shareFeedbackTextContainer);
    }

    private void createQuestionSection(final Composite container) {
        Composite questionsContainer = new Composite(container, SWT.NONE);
        GridLayout questionsContainerLayout = new GridLayout(1, false);
        questionsContainerLayout.marginLeft = 10;
        questionsContainer.setLayout(questionsContainerLayout);
        GridData questionsContainerLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
        questionsContainer.setLayoutData(questionsContainerLayoutData);

        createLabel(questionsContainer, "How satisified are you with the AWS Toolkit?");

        Composite sentimentContainer = new Composite(questionsContainer, SWT.NONE);
        RowLayout sentimentContainerLayout = new RowLayout(SWT.HORIZONTAL);
        sentimentContainerLayout.spacing = 0;
        sentimentContainer.setLayout(sentimentContainerLayout);
        CustomRadioButton positiveSentimentButton = createCustomRadioButton(sentimentContainer, "icons/HappyFace.png", "Satisfied", SWT.NONE, true);
        CustomRadioButton negativeSentimentButton = createCustomRadioButton(sentimentContainer, "icons/FrownyFace.png", "Unsatisfied", SWT.NONE, false);
        positiveSentimentButton.getRadioButton().addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                negativeSentimentButton.getRadioButton().setSelection(false);
                selectedSentiment = Sentiment.POSITIVE;
            }
        });
        negativeSentimentButton.getRadioButton().addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                positiveSentimentButton.getRadioButton().setSelection(false);
                selectedSentiment = Sentiment.NEGATIVE;
            }
        });

        createLabel(questionsContainer, "What do you like about the AWS Toolkit? What can we improve?");

        commentBox = new Text(questionsContainer, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        GridData commentBoxLayout = new GridData(SWT.FILL, SWT.FILL, true, true);
        commentBoxLayout.heightHint = 200;
        commentBoxLayout.widthHint = 0;
        commentBox.setLayoutData(commentBoxLayout);
        commentBox.addModifyListener(this::handleTextModified);

        characterRemainingLabel = new Label(questionsContainer, SWT.NONE);
        characterRemainingLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        updateCharacterRemainingCount();
    }

    private void createLabel(final Composite parent, final String text) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(text);
    }

    private void createLabelWithFontSize(final Composite parent, final String text, final int fontSize) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(text);
        label.setFont(magnifyFontSize(label.getFont(), fontSize));
    }

    private void createLinkLabel(final Composite parent, final String text, final int fontSize, final String url) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(text);
        label.setFont(magnifyFontSize(label.getFont(), fontSize));
        label.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_BLUE));
        label.setCursor(parent.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(final MouseEvent e) {
                PluginUtils.openWebpage(url);
            }
        });
    }

    private void createImageLabel(final Composite parent, final String imagePath) {
        Label label = new Label(parent, SWT.NONE);
        label.setImage(loadImage(imagePath));
    }

    private void createSeparator(final Composite parent) {
        Label separatorLabel = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridData separatorGithubLayout = new GridData(SWT.FILL, SWT.CENTER, true, false);
        separatorLabel.setLayoutData(separatorGithubLayout);
    }

    private CustomRadioButton createCustomRadioButton(final Composite parent, final String imagePath,
            final String text, final int style, final boolean isSelected) {
        CustomRadioButton button = new CustomRadioButton(parent, loadImage(imagePath), text, style);
        button.getRadioButton().setSelection(isSelected);
        return button;
    }

    private String getBodyMessageForReportIssueOrRequestFeature() {
        return String.format(
            "--- \n"
            + "Toolkit: Amazon Q for %s\n"
            + "OS: %s %s\n"
            + "IDE: %s %s", ClientMetadata.getIdeName(), ClientMetadata.getOSName(),
            ClientMetadata.getOSVersion(), ClientMetadata.getIdeName(),
            ClientMetadata.getIdeVersion()).stripIndent();
    }

    private void updateCharacterRemainingCount() {
        int currentLength = commentBox.getText().length();
        int remainingCharacters = MAX_CHAR_LIMIT - currentLength;
        characterRemainingLabel.setText(remainingCharacters + " characters remaining");
    }

    @Override
    protected final void configureShell(final Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(TITLE);
    }

    @Override
    protected final Point getInitialSize() {
        return new Point(800, 600);
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
        if (this.loadedImage != null && !this.loadedImage.isDisposed()) {
            this.loadedImage.dispose();
        }
        if (this.magnifiedFont != null && !this.magnifiedFont.isDisposed()) {
            this.magnifiedFont.dispose();
        }
    }
}
