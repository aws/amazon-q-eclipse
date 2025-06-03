// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import software.aws.toolkits.eclipse.amazonq.chat.models.ReferenceTrackerInformation;

public record ChatCodeReference(
        @JsonProperty("references") ReferenceTrackerInformation[] references
) { }
