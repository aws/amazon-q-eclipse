// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.telemetry.models;

public class UITelemetryEventRecord extends TelemetryEventRecord {

    private final String elementId;

    protected UITelemetryEventRecord(final String elementId) {
        if (elementId == null || elementId.isEmpty()) {
            throw new IllegalArgumentException("elementId cannot be null or empty");
        }
        this.elementId = elementId;
    }

    public final String elementId() {
        return elementId;
    }

    public static class Builder extends TelemetryEventBuilder<UITelemetryEventRecord, Builder> {

        private String elementId;

        public final Builder withElementId(final String elementId) {
            this.elementId = elementId;
            return this;
        }

        /**
         * Overrides the default implementation to return this instance, as required by the base class.
         *
         * @return this builder
         */
        @Override
        protected Builder self() {
            return this;
        }


        /**
         * Overrides the build method to return an instance of the TelemetryEvent class.
         *
         * @return a new instance of TelemetryEvent
         */
        @Override
        public UITelemetryEventRecord build() {
            return new UITelemetryEventRecord(elementId);
        }

    }

    public static Builder builder() {
        return new Builder();
    }

}
