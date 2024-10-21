package software.aws.toolkits.eclipse.amazonq.lsp.connection;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

public final class AuthLspConnectionProviderFactory {

    private AuthLspConnectionProviderFactory() { }

    public static AuthLspConnectionProvider createProvider() {
        BundleContext bundleContext = FrameworkUtil.getBundle(AuthLspConnectionProviderFactory.class).getBundleContext();
        ServiceReference<AuthLspConnectionProvider> serviceReference = bundleContext.getServiceReference(AuthLspConnectionProvider.class);
        if (serviceReference != null) {
            return bundleContext.getService(serviceReference);
        }
        return null;
    }

}
