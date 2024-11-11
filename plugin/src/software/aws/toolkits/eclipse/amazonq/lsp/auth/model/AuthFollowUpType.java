// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth.model;

public enum AuthFollowUpType {
        FULL_AUTH("full-auth"),
        RE_AUTH("re-auth"),
        MISSING_SCOPES("missing_scopes"),
        USE_SUPPORTED_AUTH("use-supported-auth");

        private final String value;

        AuthFollowUpType(final String value) {
                this.value = value;
        }

        public String getValue() {
                return value;
        }
};
