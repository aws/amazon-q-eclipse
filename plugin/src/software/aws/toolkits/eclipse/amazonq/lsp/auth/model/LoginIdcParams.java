// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LoginIdcParams {
    @JsonProperty("url")
    private String url;

    @JsonProperty("region")
    private String region;

    @JsonProperty("feature")
    private String feature;

    public final String getUrl() {
        return url;
    }

    public final void setUrl(final String url) {
        this.url = url;
    }

    public final String getRegion() {
        return region;
    }

    public final void setRegion(final String region) {
        this.region = region;
    }

    public final String getFeature() {
        return feature;
    }

    public final void setFeature(final String feature) {
        this.feature = feature;
    }
}
