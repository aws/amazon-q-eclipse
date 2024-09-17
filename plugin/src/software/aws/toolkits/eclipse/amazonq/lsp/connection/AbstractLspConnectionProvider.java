// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.connection;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.eclipse.lsp4e.server.ProcessStreamConnectionProvider;
import org.eclipse.lsp4e.server.StreamConnectionProvider;

public abstract class AbstractLspConnectionProvider extends ProcessStreamConnectionProvider implements StreamConnectionProvider {

    protected AbstractLspConnectionProvider() throws IOException {
        setWorkingDirectory(System.getProperty("user.dir"));
    }

    @Override
    protected final ProcessBuilder createProcessBuilder() {
        final var builder = new ProcessBuilder(getCommands());
        if (getWorkingDirectory() != null) {
            builder.directory(new File(getWorkingDirectory()));
        }
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        addEnvironmentVariables(builder.environment());
        return builder;
    }

    protected abstract void addEnvironmentVariables(Map<String, String> env);

}
