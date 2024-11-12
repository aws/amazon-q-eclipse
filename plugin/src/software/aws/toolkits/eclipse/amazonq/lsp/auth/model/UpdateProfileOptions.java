// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth.model;

public record UpdateProfileOptions(
        boolean createNonexistentProfile,
        boolean createNonexistentSsoSession,
        boolean ensureSsoAccountAccessScope,
        boolean updateSharedSsoSession) { }
