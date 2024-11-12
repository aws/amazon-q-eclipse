// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth.model;

import com.google.gson.annotations.SerializedName;

public record ProfileSettings(@SerializedName("region") String region, @SerializedName("sso_session") String ssoSession) { }
