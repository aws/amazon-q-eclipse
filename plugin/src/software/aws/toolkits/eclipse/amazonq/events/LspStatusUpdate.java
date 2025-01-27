// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.events;

public record LspStatusUpdate(Status status) {
    public enum Status {
        INITIALIZING("Language Server Initializing"), READY("Language Server Ready"), ERROR("Language Server Error"),
        STOPPED("Language Server Stopped");

        private final String value;

        Status(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

}
