package software.aws.toolkits.eclipse.amazonq.services.providers;

import java.net.http.HttpClient;

import org.osgi.framework.VersionRange;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import com.fasterxml.jackson.databind.ObjectMapper;

import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspConstants;
import software.aws.toolkits.eclipse.amazonq.services.api.RemoteManifestLspFetcherConfigurable;
import software.aws.toolkits.eclipse.amazonq.util.ObjectMapperFactory;

@Component(service = RemoteManifestLspFetcherConfigurable.class,
        scope = ServiceScope.SINGLETON)
public final class DefaultRemoteManifestLspFetcherConfiguration implements RemoteManifestLspFetcherConfigurable {

    @Reference private final HttpClient httpClient;
//    private final ObjectMapper objectMapper;

//    @Reference
//    public DefaultRemoteManifestLspFetcherConfiguration(final HttpClient httpClient, final ObjectMapper objectMapper) {
//        this.httpClient = httpClient;
//        this.objectMapper = objectMapper;
//    }

    @Override
    public String getManifestUrl() {
        return LspConstants.CW_MANIFEST_URL;
    }

    @Override
    public VersionRange getVersionRange() {
        return LspConstants.LSP_SUPPORTED_VERSION_RANGE;
    }

    @Override
    public boolean isIntegrityCheckingEnabled() {
        return true;
    }

    @Override
    public HttpClient getHttpClient() {
        return httpClient;
    }

    @Override
    public ObjectMapper getObjectMapper() {
        return ObjectMapperFactory.getInstance();
    }

}
