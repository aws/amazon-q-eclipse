// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp;

import org.eclipse.lsp4j.ClientInfo;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.Launcher.Builder;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.jsonrpc.messages.Message;
import org.eclipse.lsp4j.jsonrpc.messages.RequestMessage;

import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;
import software.aws.toolkits.eclipse.amazonq.util.ClientMetadata;

public class AmazonQLspServerBuilder extends Builder<AmazonQLspServer> {

    @Override
    public final Launcher<AmazonQLspServer> create() {
        super.setRemoteInterface(AmazonQLspServer.class);
        Launcher<AmazonQLspServer> launcher = super.create();
        LspProvider.setServer(AmazonQLspServer.class, launcher.getRemoteProxy());
        return launcher;
    }

    @Override
    protected final MessageConsumer wrapMessageConsumer(final MessageConsumer consumer) {
        return super.wrapMessageConsumer((Message message) -> {
            if (message instanceof RequestMessage && ((RequestMessage) message).getMethod().equals("initialize")) {
                InitializeParams initParams = (InitializeParams) ((RequestMessage) message).getParams();
                initParams.setClientInfo(new ClientInfo(ClientMetadata.getPluginName(), ClientMetadata.getPluginVersion()));
            }
            consumer.consume(message);
        });
    }

}
