// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericLinkClickParams {
    @JsonProperty("tabId")
    private String tabId;

    @JsonProperty("link")
    private String link;

    @JsonProperty("eventId")
    private String eventId;

    @JsonProperty("messageId")
    private String messageId;

    public final String getTabId() {
        return tabId;
    }

    public final void setTabId(final String tabId) {
        this.tabId = tabId;
    }

    public final String getLink() {
        return link;
    }

    public final void setLink(final String link) {
        this.link = link;
    }

    public final String getEventId() {
        return eventId;
    }

    public final void setEventId(final String eventId) {
        this.eventId = eventId;
    }

    public final String getMessageId() {
        return messageId;
    }

    public final void setMessageId(final String messageId) {
        this.messageId = messageId;
    }
}
