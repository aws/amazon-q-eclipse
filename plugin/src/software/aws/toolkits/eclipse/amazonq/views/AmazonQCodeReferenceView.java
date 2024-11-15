package software.aws.toolkits.eclipse.amazonq.views;


import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.ViewPart;

import software.aws.toolkits.eclipse.amazonq.util.CodeReferenceLoggedListener;
import software.aws.toolkits.eclipse.amazonq.util.CodeReferenceLoggedProvider;
import software.aws.toolkits.eclipse.amazonq.views.model.CodeReferenceLogItem;

public final class AmazonQCodeReferenceView extends ViewPart implements CodeReferenceLoggedListener {

    public static final String ID = "software.aws.toolkits.eclipse.amazonq.views.AmazonQCodeReferenceView";

    private Composite parentComposite;
    private StyledText textArea;

    public AmazonQCodeReferenceView() {
        CodeReferenceLoggedProvider.addCodeReferenceLoggedListener(this);
    }

    @Override
    public void createPartControl(final Composite parent) {
        this.parentComposite = parent;

        if (textArea == null) {
            textArea = new StyledText(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        }

        textArea.append(
                "Your organization controls whether suggestions include code with references. To update these settings, please contact your admin.\n");
    }

    @Override
    public void setFocus() {
        parentComposite.setFocus();
    }

    @Override
    public void onCodeReferenceLogged(final CodeReferenceLogItem logItem) {
        appendLog(logItem.message());
    }

    private void appendLog(final String message) {
        Display.getDefault().asyncExec(() -> {
            int boldStart = textArea.getCharCount();
            int boldLength = message.split("\n", 2)[0].length();

            StyleRange boldFirstLineStyleRange = new StyleRange();
            boldFirstLineStyleRange.start = boldStart;
            boldFirstLineStyleRange.length = boldLength;
            boldFirstLineStyleRange.fontStyle = SWT.BOLD;

            textArea.append(message);
            textArea.append("\n");
            textArea.setStyleRange(boldFirstLineStyleRange);
        });
    }

    @Override
    public void dispose() {
        CodeReferenceLoggedProvider.removeCodeReferenceLoggedListener(this);
    }
}
