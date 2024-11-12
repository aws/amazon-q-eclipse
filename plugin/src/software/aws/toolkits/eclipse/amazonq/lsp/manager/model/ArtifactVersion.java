// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.manager.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

public record ArtifactVersion(
        @JsonProperty(required = true) String serverVersion,
        boolean isDelisted,
        Runtime runtime,
        List<Capability> capabilities,
        List<Protocol> protocol,
        String thirdPartyLicenses,
        @JsonProperty(required = false)  @JsonSetter(nulls = Nulls.AS_EMPTY) List<Target> targets) { }
