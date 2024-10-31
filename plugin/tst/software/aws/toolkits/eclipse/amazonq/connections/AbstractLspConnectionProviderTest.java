package software.aws.toolkits.eclipse.amazonq.connections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.aws.toolkits.eclipse.amazonq.lsp.connection.AbstractLspConnectionProvider;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class AbstractLspConnectionProviderTest {

    private static class TestLspConnectionProvider extends AbstractLspConnectionProvider {

        private final Map<String, String> expectedEnvVars;

        TestLspConnectionProvider() throws IOException {
            super();
            expectedEnvVars = Map.of();
        }

        protected void addEnvironmentVariables(final Map<String, String> env) {
            env.putAll(expectedEnvVars);
        }
    }

    @Test
    void testStartWithValidArgumentsCreatesProcessAndStreams() throws IOException {
        var provider = new TestLspConnectionProvider();

        provider.setWorkingDirectory(System.getProperty("user.dir"));

        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            provider.setCommands(List.of("cmd", "/c", "dir"));
        } else {
            provider.setCommands(List.of("cat"));
        }

        Assertions.assertDoesNotThrow(() -> provider.start());

        Assertions.assertNotNull(provider.getInputStream());
        Assertions.assertNotNull(provider.getOutputStream());
        Assertions.assertNotNull(provider.getErrorStream());

        provider.stop();
    }

}