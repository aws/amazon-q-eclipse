// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth.model;

public record GetSsoTokenParams(GetSsoTokenSource source, String clientName, GetSsoTokenOptions options) { }
