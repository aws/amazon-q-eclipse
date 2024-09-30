// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class ChatRequestParams {
    private final String tabId;
    private final ChatPrompt prompt;
    private String partialResultToken;

    public ChatRequestParams(
        @JsonProperty("tabId") final String tabId,
        @JsonProperty("prompt") final ChatPrompt prompt
    ) {
        this.tabId = tabId;
        this.prompt = prompt;
    }

    public String getTabId() {
        return tabId;
    }

    public ChatPrompt getPrompt() {
        return prompt;
    }

    public String getPartialResultToken() {
        return partialResultToken;
    }

    public void setPartialResultToken(final String partialResultToken) {
        this.partialResultToken = partialResultToken;
    }
}

