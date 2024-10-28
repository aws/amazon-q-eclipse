package software.aws.toolkits.eclipse.amazonq.extensions;

import java.util.Map;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.telemetry.service.TelemetryService;
import software.aws.toolkits.eclipse.amazonq.util.LoggingService;

import java.util.Optional;

import static org.mockito.Mockito.mockStatic;

public final class ActivatorStaticMockExtension implements BeforeAllCallback, AfterAllCallback, AfterEachCallback {

    private static MockedStatic<Activator> activatorStaticMock = null;
    @Mock private static TelemetryService telemetryServiceMock;
    @Mock private static LoggingService loggingServiceMock;
    @Mock private static Activator activatorMock;

    private static final Map<Class<?>, Object> MOCKS_MAP;

    static {
        telemetryServiceMock = Mockito.mock(TelemetryService.class);
        loggingServiceMock = Mockito.mock(LoggingService.class);
        activatorMock = Mockito.mock(Activator.class);

        MOCKS_MAP = Map.of(
            TelemetryService.class, telemetryServiceMock,
            LoggingService.class, loggingServiceMock,
            Activator.class, activatorMock
        );
    }

    @Override
    public void beforeAll(final ExtensionContext context) {
        activatorStaticMock = mockStatic(Activator.class);
        activatorStaticMock.when(Activator::getLogger).thenReturn(loggingServiceMock);
        activatorStaticMock.when(Activator::getTelemetryService).thenReturn(telemetryServiceMock);
        activatorStaticMock.when(Activator::getDefault).thenReturn(activatorMock);
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> getMock(final Class<T> type) {
        return Optional.ofNullable(MOCKS_MAP.get(type)).map(mock -> (T) mock);
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
