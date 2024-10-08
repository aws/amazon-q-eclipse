package software.aws.toolkits.eclipse.amazonq.util;

import java.util.concurrent.CompletableFuture;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

public final class EditorUtils {

    private EditorUtils() {
        // Prevent instantiation
    }

    public static CompletableFuture<String> getSelectedText() {
        CompletableFuture<String> future = new CompletableFuture<>();

        Display.getDefault().asyncExec(() -> {
            try {
                IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
                if (editor instanceof ITextEditor) {
                    ITextEditor textEditor = (ITextEditor) editor;
                    ISelection selection = textEditor.getSelectionProvider().getSelection();
                    if (selection instanceof ITextSelection) {
                        ITextSelection textSelection = (ITextSelection) selection;
                        future.complete(textSelection.getText());
                        return;
                    }
                }
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }
}
