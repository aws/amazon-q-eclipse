package software.aws.toolkits.eclipse.amazonq.extensions;

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

    static {
        telemetryServiceMock = Mockito.mock(TelemetryService.class);
        loggingServiceMock = Mockito.mock(LoggingService.class);
        activatorMock = Mockito.mock(Activator.class);
    }

    @Override
    public void beforeAll(final ExtensionContext context) {
        activatorStaticMock = mockStatic(Activator.class);
        activatorStaticMock.when(Activator::getLogger).thenReturn(loggingServiceMock);
        activatorStaticMock.when(Activator::getTelemetryService).thenReturn(telemetryServiceMock);
        activatorStaticMock.when(Activator::getDefault).thenReturn(activatorMock);
    }

    public static <T> Optional<T> getMock(final Class<T> type) {
        if (type.equals(TelemetryService.class)) {
            return Optional.of(type.cast(telemetryServiceMock));
        } else if (type.equals(LoggingService.class)) {
            return Optional.of(type.cast(loggingServiceMock));
        } else if (type.equals(Activator.class)) {
            return Optional.of(type.cast(activatorMock));
        }

        return Optional.empty();
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
