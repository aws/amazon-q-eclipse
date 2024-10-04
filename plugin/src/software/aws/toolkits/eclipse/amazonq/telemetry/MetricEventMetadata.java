// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.telemetry;

import software.amazon.awssdk.services.toolkittelemetry.model.AWSProduct;

public record MetricEventMetadata(
    String awsAccount,
    String awsRegion,
    AWSProduct awsProduct,
    String awsVersion
) {
    public MetricEventMetadata() {
        this(
            null,
            null,
            AWSProduct.AMAZON_Q_FOR_ECLIPSE,
            null
        );
    }
}
