// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

public final class FileClickParams {
    @JsonProperty("tabId")
    private String tabId;

    @JsonProperty("filePath")
    private String filePath;

    @JsonProperty("action")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String action;

    @JsonProperty("messageId")
    private String messageId;

    @JsonProperty("fullPath")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String fullPath;

    // Getters and Setters
    public String getTabId() {
        return tabId;
    }

    public void setTabId(final String tabId) {
        this.tabId = tabId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(final String filePath) {
        this.filePath = filePath;
    }

    public String getAction() {
        return action;
    }

    public void setAction(final String action) {
        this.action = action;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(final String messageId) {
        this.messageId = messageId;
    }

    public String getFullPath() {
        return fullPath;
    }

    public void setFullPath(final String fullPath) {
        this.fullPath = fullPath;
    }
}

