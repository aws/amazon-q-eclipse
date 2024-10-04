// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class QuickActionParams {
    private final String tabId;
    private final String quickAction;
    private final String prompt;
    private String partialResultToken;

    public QuickActionParams(
        @JsonProperty("tabId") final String tabId,
        @JsonProperty("quickAction") final String quickAction,
        @JsonProperty("prompt") final String prompt
    ) {
        this.tabId = tabId;
        this.quickAction = quickAction;
        this.prompt = prompt;
    }

    public String getTabId() {
        return tabId;
    }

    public String getQuickAction() {
        return quickAction;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getPartialResultToken() {
        return partialResultToken;
    }

    public void setPartialResultToken(final String partialResultToken) {
        this.partialResultToken = partialResultToken;
    }
}

