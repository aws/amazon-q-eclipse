package software.aws.toolkits.eclipse.amazonq.lsp.model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.swt.graphics.Image;

public class TextCompareInput implements ITypedElement, IStreamContentAccessor {
    private final String content;

    public TextCompareInput(String content) {
        this.content = content;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public Image getImage() {
        return null;
    }

    @Override
    public String getType() {
        return ITypedElement.TEXT_TYPE;
    }

    @Override
    public InputStream getContents() {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
