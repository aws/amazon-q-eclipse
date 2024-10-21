package software.aws.toolkits.eclipse.amazonq.services.providers;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import software.aws.toolkits.eclipse.amazonq.lsp.manager.DefaultLspManager;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspConstants;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspManager;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher.LspFetcher;

@Component(service = LspManager.class,
        scope = ServiceScope.SINGLETON)
public final class DefaultLspManagerProvider {

    private final LspFetcher lspFetcher;

    @Reference
    public DefaultLspManagerProvider(final LspFetcher lspFetcher) {
        this.lspFetcher = lspFetcher;
    }

    public LspManager getLspManager() {
        return DefaultLspManager.builder()
                .withLspExecutablePrefix(LspConstants.CW_LSP_FILENAME)
                .withFetcher(lspFetcher)
                .build();
    }

}
