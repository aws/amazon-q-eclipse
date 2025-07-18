// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.lsp4j.ClientInfo;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.Launcher.Builder;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.jsonrpc.messages.Message;
import org.eclipse.lsp4j.jsonrpc.messages.RequestMessage;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage;

import com.google.gson.ToNumberPolicy;

import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommand;
import software.aws.toolkits.eclipse.amazonq.lsp.model.AwsExtendedInitializeResult;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.telemetry.metadata.ClientMetadata;
import software.aws.toolkits.eclipse.amazonq.telemetry.metadata.PluginClientMetadata;

public class AmazonQLspServerBuilder extends Builder<AmazonQLspServer> {

    private static final String USER_AGENT_CLIENT_NAME = "AmazonQ For Eclipse";
    private static Launcher<AmazonQLspServer> launcher;

    @Override
    public final Launcher<AmazonQLspServer> create() {
        super.setRemoteInterface(AmazonQLspServer.class);
        super.configureGson(builder -> {
           builder.registerTypeAdapterFactory(new QLspTypeAdapterFactory());
           builder.setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE);
        });
        launcher = super.create();
        return launcher;
    }

    private Map<String, Object> getInitializationOptions(final ClientMetadata metadata) {
        Map<String, Object> initOptions = new HashMap<>();
        Map<String, Object> awsInitOptions = new HashMap<>();
        Map<String, Object> extendedClientInfoOptions = new HashMap<>();
        Map<String, String> extensionOptions = new HashMap<>();
        Map<String, Object> awsClientCapabilities = new HashMap<>();
        Map<String, Object> qOptions = new HashMap<>();
        extensionOptions.put("name", USER_AGENT_CLIENT_NAME);
        extensionOptions.put("version", metadata.getPluginVersion());
        extendedClientInfoOptions.put("extension", extensionOptions);
        extendedClientInfoOptions.put("clientId", metadata.getClientId());
        extendedClientInfoOptions.put("version", metadata.getIdeVersion());
        extendedClientInfoOptions.put("name", metadata.getIdeName());
        awsInitOptions.put("clientInfo", extendedClientInfoOptions);
        qOptions.put("developerProfiles", true);
        qOptions.put("customizationsWithMetadata", true);
        qOptions.put("mcp", true);
        qOptions.put("pinnedContextEnabled", true);
        qOptions.put("modelSelection", true);
        awsClientCapabilities.put("q", qOptions);
        Map<String, Object> window = new HashMap<>();
        window.put("showSaveFileDialog", true);
        awsClientCapabilities.put("window", window);
        awsInitOptions.put("awsClientCapabilities", awsClientCapabilities);
        initOptions.put("aws", awsInitOptions);
        return initOptions;
    }

    @Override
    protected final MessageConsumer wrapMessageConsumer(final MessageConsumer consumer) {
        return super.wrapMessageConsumer((final Message message) -> {
            if (message instanceof RequestMessage && ((RequestMessage) message).getMethod().equals("initialize")) {
                InitializeParams initParams = (InitializeParams) ((RequestMessage) message).getParams();
                ClientMetadata metadata = PluginClientMetadata.getInstance();
                initParams.setClientInfo(new ClientInfo(USER_AGENT_CLIENT_NAME, metadata.getPluginVersion()));
                initParams.setInitializationOptions(getInitializationOptions(metadata));
            }
            if (message instanceof ResponseMessage && ((ResponseMessage) message).getResult() instanceof AwsExtendedInitializeResult) {
                AwsExtendedInitializeResult result = (AwsExtendedInitializeResult) ((ResponseMessage) message).getResult();
                var command = ChatUIInboundCommand.createCommand("chatOptions", result.getAwsServerCapabilities().chatOptions());
                Activator.getEventBroker().post(ChatUIInboundCommand.class, command);
                Activator.getLspProvider().setServer(AmazonQLspServer.class, launcher.getRemoteProxy());
            }
            consumer.consume(message);
        });
    }

}
