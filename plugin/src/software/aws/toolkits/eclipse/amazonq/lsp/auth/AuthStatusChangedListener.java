// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth;

import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;

public interface AuthStatusChangedListener {
    void onAuthStatusChanged(AuthState authState);
}
