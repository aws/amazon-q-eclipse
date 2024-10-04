// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.lsp;

import software.aws.toolkits.eclipse.amazonq.lsp.model.AwsServerCapabilities;
import software.aws.toolkits.eclipse.amazonq.lsp.model.ChatOptions;

public final class AwsServerCapabiltiesProvider {
    private static AwsServerCapabiltiesProvider instance;
    private AwsServerCapabilities serverCapabilties;

    public static synchronized AwsServerCapabiltiesProvider getInstance() {
        if (instance == null) {
            instance = new AwsServerCapabiltiesProvider();
        }
        return instance;
    }

    public void setAwsServerCapabilties(final AwsServerCapabilities serverCapabilties) {
        this.serverCapabilties = serverCapabilties;
    }

    public ChatOptions getChatOptions() {
        if (serverCapabilties != null) {
            return serverCapabilties.chatOptions();
        }
        return null;
    }
}
