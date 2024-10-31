package software.aws.toolkits.eclipse.amazonq.extensions.api;

import org.mockito.MockedStatic;

import java.util.Optional;

public interface MockRetrievable<T> {

    <U> Optional<U> getMock(Class<U> type);
    MockedStatic<T> getStaticMock();

}
