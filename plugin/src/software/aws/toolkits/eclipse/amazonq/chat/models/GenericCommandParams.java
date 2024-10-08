// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GenericCommandParams(
        @JsonProperty("tabId") String tabId,
        @JsonProperty("selection") String selection,
        @JsonProperty("triggerType") String triggerType, // {@link TriggerType}
        @JsonProperty("genericCommand") String genericCommand // {@link GenericCommandVerb}
) { };

