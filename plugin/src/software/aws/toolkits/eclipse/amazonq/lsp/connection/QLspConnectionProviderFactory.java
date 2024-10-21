package software.aws.toolkits.eclipse.amazonq.lsp.connection;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

public final class QLspConnectionProviderFactory {

    private QLspConnectionProviderFactory() { }

    public static QLspConnectionProvider createProvider() {
        BundleContext bundleContext = FrameworkUtil.getBundle(QLspConnectionProviderFactory.class).getBundleContext();
        ServiceReference<QLspConnectionProvider> serviceReference = bundleContext.getServiceReference(QLspConnectionProvider.class);
        if (serviceReference != null) {
            return bundleContext.getService(serviceReference);
        }
        return null;
    }

}

