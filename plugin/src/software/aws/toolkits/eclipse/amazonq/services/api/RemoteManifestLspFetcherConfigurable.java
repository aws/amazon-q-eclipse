package software.aws.toolkits.eclipse.amazonq.services.api;

import java.net.http.HttpClient;
import org.osgi.framework.VersionRange;
import com.fasterxml.jackson.databind.ObjectMapper;

public interface RemoteManifestLspFetcherConfigurable {

    String getManifestUrl();
    VersionRange getVersionRange();
    boolean isIntegrityCheckingEnabled();
    HttpClient getHttpClient();
    ObjectMapper getObjectMapper();

}
