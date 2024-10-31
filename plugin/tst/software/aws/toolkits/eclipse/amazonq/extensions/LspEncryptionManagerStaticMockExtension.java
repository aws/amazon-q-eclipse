package software.aws.toolkits.eclipse.amazonq.extensions;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import software.aws.toolkits.eclipse.amazonq.lsp.encryption.LspEncryptionManager;

import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.mockStatic;

public final class LspEncryptionManagerStaticMockExtension implements BeforeAllCallback, AfterAllCallback, AfterEachCallback {

    private static MockedStatic<LspEncryptionManager> lspEncryptionManagerStaticMock = null;
    @Mock private static LspEncryptionManager lspEncryptionManagerMock;

    private static final Map<Class<?>, Object> MOCKS_MAP;

    static {
        lspEncryptionManagerMock = Mockito.mock(LspEncryptionManager.class);

        MOCKS_MAP = Map.of(
                LspEncryptionManager.class, lspEncryptionManagerMock
        );
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> getMock(final Class<T> type) {
        return Optional.ofNullable(MOCKS_MAP.get(type)).map(mock -> (T) mock);
    }

    public static MockedStatic<LspEncryptionManager> getStaticMock() {
        return lspEncryptionManagerStaticMock;
    }

    @Override
    public void beforeAll(final ExtensionContext context) {
        lspEncryptionManagerStaticMock = mockStatic(LspEncryptionManager.class);
        lspEncryptionManagerStaticMock.when(LspEncryptionManager::getInstance).thenReturn(lspEncryptionManagerMock);
    }

    @Override
    public void afterEach(final ExtensionContext context) {
        Mockito.reset(lspEncryptionManagerMock);
    }

    @Override
    public void afterAll(final ExtensionContext context) {
        if (lspEncryptionManagerStaticMock != null) {
            lspEncryptionManagerStaticMock.close();
        }
    }

}
