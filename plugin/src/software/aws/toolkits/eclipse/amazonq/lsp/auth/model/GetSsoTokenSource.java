// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth.model;

import java.util.List;

public record GetSsoTokenSource(String kind, List<String> ssoRegistrationScopes, String profileName) { }
