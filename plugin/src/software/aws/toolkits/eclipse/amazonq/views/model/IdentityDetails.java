// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.model;

import com.google.gson.annotations.SerializedName;

public record IdentityDetails(@SerializedName("region") String region) {

}
