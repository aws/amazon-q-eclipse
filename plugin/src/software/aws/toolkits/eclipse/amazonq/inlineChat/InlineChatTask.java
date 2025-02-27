package software.aws.toolkits.eclipse.amazonq.inlineChat;

import org.eclipse.ui.texteditor.ITextEditor;

import software.aws.toolkits.eclipse.amazonq.chat.models.CursorState;

public class InlineChatTask {
    private String prompt;
    private CursorState cursorState;
    private String originalCode;
    private int offset;
    private String partialResult;
    private ITextEditor editor;

    // Getters
    public String getPrompt() {
        return prompt;
    }

    public CursorState getCursorState() {
        return cursorState;
    }

    public String getOriginalCode() {
        return originalCode;
    }

    public ITextEditor getEditor() {
        return editor;
    }

    public void setEditor(final ITextEditor editor) {
        this.editor = editor;
    }

    public String getPartialResult() {
        return partialResult;
    }

    public void setPartialResult(final String partialResult) {
        this.partialResult = partialResult;
    }

    public int getOffset() {
        return offset;
    }

    // Setters
    public void setPrompt(final String prompt) {
        this.prompt = prompt;
    }

    public void setCursorState(final CursorState cursorState) {
        this.cursorState = cursorState;
    }

    public void setOriginalCode(final String originalCode) {
        this.originalCode = originalCode;
    }

    public void setOffset(final int offset) {
        this.offset = offset;
    }
}

