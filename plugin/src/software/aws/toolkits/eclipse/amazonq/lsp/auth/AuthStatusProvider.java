// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth;

import java.util.ArrayList;
import java.util.List;

import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;

public final class AuthStatusProvider {
    private static final List<AuthStatusChangedListener> LISTENERS = new ArrayList<>();
    private static AuthState prevAuthState;

    private AuthStatusProvider() {
        //prevent instantiation
    }

    public static void addAuthStatusChangeListener(final AuthStatusChangedListener listener) {
        LISTENERS.add(listener);
    }

    public static void removeAuthStatusChangeListener(final AuthStatusChangedListener listener) {
        LISTENERS.remove(listener);
    }

    public static void notifyAuthStatusChanged(final AuthState authState) {
        if (prevAuthState != null && prevAuthState.equals(authState)) {
            return;
        }

        prevAuthState = authState;

        for (AuthStatusChangedListener listener : LISTENERS) {
            listener.onAuthStatusChanged(authState);
        }
    }
}
