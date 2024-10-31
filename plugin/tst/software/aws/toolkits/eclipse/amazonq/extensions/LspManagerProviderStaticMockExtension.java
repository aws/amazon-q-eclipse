package software.aws.toolkits.eclipse.amazonq.extensions;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspInstallResult;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspManager;
import software.aws.toolkits.eclipse.amazonq.providers.LspManagerProvider;
import static org.mockito.Mockito.mockStatic;

import java.util.Map;
import java.util.Optional;

public final class LspManagerProviderStaticMockExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback,
        AfterAllCallback {

    private static MockedStatic<LspManagerProvider> lspManagerProviderStaticMock = null;
    @Mock private static LspManager lspManagerMock;
    @Mock private static LspInstallResult lspInstallResultMock;

    private static final Map<Class<?>, Object> MOCKS_MAP;

    static {
        lspManagerMock = Mockito.mock(LspManager.class);
        lspInstallResultMock = Mockito.mock(LspInstallResult.class);

        MOCKS_MAP = Map.of(
                LspManager.class, lspManagerMock,
                LspInstallResult.class, lspInstallResultMock
        );
    }

    public static MockedStatic<LspManagerProvider> getStaticMock() {
        return lspManagerProviderStaticMock;
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> getMock(final Class<T> type) {
        return Optional.ofNullable(MOCKS_MAP.get(type)).map(mock -> (T) mock);
    }

    @Override
    public void beforeAll(final ExtensionContext context) {
        lspManagerProviderStaticMock = mockStatic(LspManagerProvider.class);
        lspManagerProviderStaticMock.when(LspManagerProvider::getInstance).thenReturn(lspManagerMock);
    }

    @Override
    public void beforeEach(final ExtensionContext context) {
        Mockito.when(lspManagerMock.getLspInstallation()).thenReturn(lspInstallResultMock);
    }

    @Override
    public void afterEach(final ExtensionContext context) {
        Mockito.reset(lspManagerMock, lspInstallResultMock);
    }

    @Override
    public void afterAll(final ExtensionContext context) {
        if (lspManagerProviderStaticMock != null) {
            lspManagerProviderStaticMock.close();
        }
    }

}
