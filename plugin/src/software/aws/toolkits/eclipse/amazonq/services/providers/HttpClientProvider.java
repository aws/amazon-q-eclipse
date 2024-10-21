package software.aws.toolkits.eclipse.amazonq.services.providers;

import java.net.http.HttpClient;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

@Component(service = HttpClient.class,
           scope = ServiceScope.SINGLETON)
public final class HttpClientProvider {
    private static final HttpClient INSTANCE = HttpClient.newHttpClient();

    public HttpClient get() {
        return INSTANCE;
    }
}
