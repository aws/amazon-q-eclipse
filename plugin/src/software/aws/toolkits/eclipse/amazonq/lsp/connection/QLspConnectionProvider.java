// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.connection;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspManager;
import software.aws.toolkits.eclipse.amazonq.providers.LspManagerProvider;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;

public class QLspConnectionProvider extends AbstractLspConnectionProvider {

    public QLspConnectionProvider() throws IOException {
        super();
        LspManager lspManager = LspManagerProvider.getInstance();
        List<String> commands = new ArrayList<>();
        commands.add(lspManager.getLspInstallation().nodeExecutable().toString());
        commands.add(lspManager.getLspInstallation().lspJs().toString());
        commands.add("--nolazy");
        commands.add("--inspect=5599");
        commands.add("--stdio");
        commands.add("--set-credentials-encryption-key");
        setCommands(commands);
    }

    @Override
    protected final void addEnvironmentVariables(final Map<String, String> env) {
        env.put("ENABLE_INLINE_COMPLETION", "true");
        env.put("ENABLE_TOKEN_PROVIDER", "true");
    }

    @Override
    public final void start() throws IOException {
        super.start();

        OutputStream outputStream = getOutputStream();

        PluginLogger.info("Q SERVER HAS STARTED. OutputStream retrieved: " + outputStream.toString());

        sendMessageToServer(outputStream, getEncyptionInitializationMessage());
    }

    private String getEncyptionInitializationMessage() {
        return String.format("{\"version\": \"1.0\",\"key\":\"%s\",\"mode\":\"JWT\"}", base64Encode(generateRandomKey()));
    }

    private String base64Encode(String str) {
        byte[] encodedBytes = Base64.getEncoder().encode(str.getBytes(StandardCharsets.UTF_8));
        return new String(encodedBytes, StandardCharsets.UTF_8);
    }

    private String generateRandomKey() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[128]; // 128 bytes = 256 hex characters
        secureRandom.nextBytes(randomBytes);

        return HexFormat.of().formatHex(randomBytes);
    }

    private void sendMessageToServer(OutputStream serverStdin, String message) throws IOException {
        if (serverStdin != null) {
            PluginLogger.info("Sending message to server: " + message);
            serverStdin.write((message + "\n").getBytes());
            serverStdin.flush();
        } else {
            throw new IllegalStateException("Server stdin is not available. Did you start the server?");
        }
    }

}
