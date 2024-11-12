package software.aws.toolkits.eclipse.amazonq.util;

public interface LoggingService {
    void info(String message);
    void info(String message, Throwable ex);
    void warn(String message);
    void warn(String message, Throwable ex);
    void error(String message);
    void error(String message, Throwable ex);
}
