// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.lsp;

import java.util.Arrays;
import java.util.Collections;

import software.aws.toolkits.eclipse.amazonq.lsp.model.AwsServerCapabilities;
import software.aws.toolkits.eclipse.amazonq.lsp.model.ChatOptions;
import software.aws.toolkits.eclipse.amazonq.lsp.model.Command;
import software.aws.toolkits.eclipse.amazonq.lsp.model.QuickActions;
import software.aws.toolkits.eclipse.amazonq.lsp.model.QuickActionsCommandGroup;

public final class AwsServerCapabiltiesProvider {
    private static AwsServerCapabiltiesProvider instance;
    private AwsServerCapabilities serverCapabilties;
    private static final ChatOptions DEFAULT_CHAT_OPTIONS = new ChatOptions(
            new QuickActions(
                Collections.singletonList(
                    new QuickActionsCommandGroup(
                        Arrays.asList(
                            new Command("/help", "Learn more about Amazon Q"),
                            new Command("/clear", "Clear this session")
                        )
                    )
                )
            )
        );

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
        return DEFAULT_CHAT_OPTIONS;
    }
}
