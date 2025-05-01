// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatUpdateParams(@JsonProperty("tabId") String tabId,
        @JsonProperty("state") TabState tabState,
        @JsonProperty("data") TabData tabData) {

}
