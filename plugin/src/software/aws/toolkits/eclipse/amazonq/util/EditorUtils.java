package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

public class EditorUtils {
	public static String getSelectedText() {
	    IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
	    if (editor instanceof ITextEditor) {
	        ITextEditor textEditor = (ITextEditor) editor;
	        ISelection selection = textEditor.getSelectionProvider().getSelection();
	        if (selection instanceof ITextSelection) {
	            ITextSelection textSelection = (ITextSelection) selection;
	            return textSelection.getText();
	        }
	    }
	    return null;
	}
}
