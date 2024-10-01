// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InfoLinkClickParams {
    @JsonProperty("tabId")
    private String tabId;

    @JsonProperty("link")
    private String link;

    @JsonProperty("eventId")
    private String eventId;

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
}
