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
import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage;

import com.google.gson.ToNumberPolicy;

import software.aws.toolkits.eclipse.amazonq.lsp.model.AwsExtendedInitializeResult;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;
import software.aws.toolkits.eclipse.amazonq.telemetry.metadata.ClientMetadata;
import software.aws.toolkits.eclipse.amazonq.telemetry.metadata.PluginClientMetadata;

public class AmazonQLspServerBuilder extends Builder<AmazonQLspServer> {

    private static final String USER_AGENT_CLIENT_NAME = "AmazonQ-For-Eclipse";

    @Override
    public final Launcher<AmazonQLspServer> create() {
        super.setRemoteInterface(AmazonQLspServer.class);
        super.configureGson(builder -> {
           builder.registerTypeAdapterFactory(new QLspTypeAdapterFactory());
           builder.setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE);
        });
        Launcher<AmazonQLspServer> launcher = super.create();
        LspProvider.setServer(AmazonQLspServer.class, launcher.getRemoteProxy());
        return launcher;
    }

    @Override
    protected final MessageConsumer wrapMessageConsumer(final MessageConsumer consumer) {
        return super.wrapMessageConsumer((Message message) -> {
            if (message instanceof RequestMessage && ((RequestMessage) message).getMethod().equals("initialize")) {
                InitializeParams initParams = (InitializeParams) ((RequestMessage) message).getParams();
                ClientMetadata metadata = PluginClientMetadata.getInstance();
                initParams.setClientInfo(new ClientInfo(USER_AGENT_CLIENT_NAME, metadata.getPluginVersion()));
            }
            if (message instanceof ResponseMessage && ((ResponseMessage) message).getResult() instanceof AwsExtendedInitializeResult) {
                AwsExtendedInitializeResult result = (AwsExtendedInitializeResult) ((ResponseMessage) message).getResult();
                var awsServerCapabiltiesProvider = AwsServerCapabiltiesProvider.getInstance();
                awsServerCapabiltiesProvider.setAwsServerCapabilties(result.getAwsServerCapabilities());
            }
            consumer.consume(message);
        });
    }

}
