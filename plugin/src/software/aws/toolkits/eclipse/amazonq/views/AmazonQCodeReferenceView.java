package software.aws.toolkits.eclipse.amazonq.views;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import software.aws.toolkits.eclipse.amazonq.util.QInvocationSession;

public final class AmazonQCodeReferenceView extends ViewPart {

    public static final String ID = "software.aws.toolkits.eclipse.amazonq.views.AmazonQCodeReferenceView";
    private static final String CR_TEMPLATE = """
            [%s] Accepted recommendation with code
            %s
            provided with reference under %s from repository %s. Added to %s (lines from %d to %d)
            """;

    private StyledText textArea;

    @Override
    public void createPartControl(final Composite parent) {
        if (textArea == null) {
            textArea = new StyledText(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        }

        QInvocationSession qInvocationSessionInstance = QInvocationSession.getInstance();

        qInvocationSessionInstance.registerCallbackForCodeReference((item, startLine) -> {
            var references = item.getReferences();
            var editor = qInvocationSessionInstance.getEditor();
            String fqfn = editor.getTitle();
            if (references != null && references.length > 0) {
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm:ss a");
                String formattedDateTime = now.format(formatter);
                int suggestionTextDepth = item.getInsertText().split("\n").length;
                for (var reference : references) {
                    String itemToShow = String.format(CR_TEMPLATE, formattedDateTime, item.getInsertText(),
                            reference.getLicenseName(), reference.getReferenceUrl(),
                            fqfn, startLine, startLine + suggestionTextDepth);
                    int boldStart = textArea.getCharCount();
                    int boldLength = itemToShow.split("\n", 2)[0].length();

                    StyleRange styleRange = new StyleRange();
                    styleRange.start = boldStart;
                    styleRange.length = boldLength;
                    styleRange.fontStyle = SWT.BOLD;

                    textArea.append(itemToShow);
                    textArea.append("\n");
                    textArea.setStyleRange(styleRange);

                }
            }
        });

        textArea.append(
                "Your organization controls whether suggestions include code with references. To update these settings, please contact your admin.\n");
    }

    @Override
    public void setFocus() {
        return;
    }
}
