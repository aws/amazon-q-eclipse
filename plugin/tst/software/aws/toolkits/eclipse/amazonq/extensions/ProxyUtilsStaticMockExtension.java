package software.aws.toolkits.eclipse.amazonq.extensions;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.MockedStatic;
import software.aws.toolkits.eclipse.amazonq.util.ProxyUtil;

import static org.mockito.Mockito.mockStatic;

public final class ProxyUtilsStaticMockExtension implements BeforeAllCallback, AfterAllCallback {

    private static MockedStatic<ProxyUtil> proxyUtilStaticMock;

    public static MockedStatic<ProxyUtil> getStaticMock() {
        return proxyUtilStaticMock;
    }

    @Override
    public void beforeAll(final ExtensionContext context) {
        proxyUtilStaticMock = mockStatic(ProxyUtil.class);
    }

    @Override
    public void afterAll(final ExtensionContext context) {
        if (proxyUtilStaticMock != null) {
            proxyUtilStaticMock.close();
        }
    }

}
