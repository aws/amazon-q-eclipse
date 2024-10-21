// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.connection;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import software.amazon.awssdk.utils.StringUtils;
import software.aws.toolkits.eclipse.amazonq.lsp.encryption.LspEncryptionManager;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspManager;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.ProxyUtil;

@Component(service = QLspConnectionProvider.class)
public class QLspConnectionProvider extends AbstractLspConnectionProvider {

    @Activate
    public QLspConnectionProvider(@Reference final LspManager lspManager) throws IOException {
        super();
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
        if (!StringUtils.isEmpty(ProxyUtil.getHttpsProxyUrl())) {
            env.put("HTTPS_PROXY", ProxyUtil.getHttpsProxyUrl());
        }
    }

    @Override
    public final void start() throws IOException {
        super.start();

        Activator.getLogger().info("Initializing encrypted communication with Amazon Q Lsp Server");

        try {
            LspEncryptionManager lspEncryption = LspEncryptionManager.getInstance();
            OutputStream serverStdIn = getOutputStream();

            lspEncryption.initializeEncrypedCommunication(serverStdIn);
        } catch (Exception e) {
            Activator.getLogger().error("Error occured while initializing encrypted communication with Amazon Q Lsp Server", e);
        }
    }
}
