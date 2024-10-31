package software.aws.toolkits.eclipse.amazonq.extensions;

import java.util.Map;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;
import software.aws.toolkits.eclipse.amazonq.telemetry.service.TelemetryService;
import software.aws.toolkits.eclipse.amazonq.util.LoggingService;
import software.aws.toolkits.eclipse.amazonq.util.LoginService;

import java.util.Optional;

import static org.mockito.Mockito.mockStatic;

public final class ActivatorStaticMockExtension implements BeforeAllCallback, AfterAllCallback, AfterEachCallback {

    private static MockedStatic<Activator> activatorStaticMock = null;
    @Mock private static TelemetryService telemetryServiceMock;
    @Mock private static LoggingService loggingServiceMock;
    @Mock private static Activator activatorMock;
    @Mock private static LspProvider lspProviderMock;
    @Mock private static LoginService loginServiceMock;
    @Mock private static PluginStore pluginStoreMock;

    private static final Map<Class<?>, Object> MOCKS_MAP;

    static {
        telemetryServiceMock = Mockito.mock(TelemetryService.class);
        loggingServiceMock = Mockito.mock(LoggingService.class);
        activatorMock = Mockito.mock(Activator.class);
        lspProviderMock = Mockito.mock(LspProvider.class);
        loginServiceMock = Mockito.mock(LoginService.class);
        pluginStoreMock = Mockito.mock(PluginStore.class);

        MOCKS_MAP = Map.of(
            TelemetryService.class, telemetryServiceMock,
            LoggingService.class, loggingServiceMock,
            Activator.class, activatorMock,
            LspProvider.class, lspProviderMock,
            LoginService.class, loginServiceMock,
            PluginStore.class, pluginStoreMock
        );
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> getMock(final Class<T> type) {
        return Optional.ofNullable(MOCKS_MAP.get(type)).map(mock -> (T) mock);
    }

    public static MockedStatic<Activator> getStaticMock() {
        return activatorStaticMock;
    }

    @Override
    public void beforeAll(final ExtensionContext context) {
        activatorStaticMock = mockStatic(Activator.class);
        activatorStaticMock.when(Activator::getLogger).thenReturn(loggingServiceMock);
        activatorStaticMock.when(Activator::getTelemetryService).thenReturn(telemetryServiceMock);
        activatorStaticMock.when(Activator::getDefault).thenReturn(activatorMock);
        activatorStaticMock.when(Activator::getLspProvider).thenReturn(lspProviderMock);
        activatorStaticMock.when(Activator::getLoginService).thenReturn(loginServiceMock);
        activatorStaticMock.when(Activator::getPluginStore).thenReturn(pluginStoreMock);
    }

    @Override
    public void afterEach(final ExtensionContext context) {
        Mockito.reset(telemetryServiceMock, loggingServiceMock, activatorMock);
    }

    @Override
    public void afterAll(final ExtensionContext context) throws Exception {
        if (activatorStaticMock != null) {
            activatorStaticMock.close();
        }
    }

}
