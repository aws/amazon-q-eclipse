// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QDeveloperProfile extends Configuration {

    @SerializedName("identityDetails")
    private IdentityDetails identityDetails;

    public QDeveloperProfile(final String arn, final String name, final IdentityDetails identityDetails) {
        super(arn, name);
        this.identityDetails = identityDetails;
    }

    public QDeveloperProfile() {
        super();
        this.identityDetails = null;
    }

    public final void setIdentityDetails(final IdentityDetails identityDetails) {
        this.identityDetails = identityDetails;
    }

    public final IdentityDetails getIdentityDetails() {
        return this.identityDetails;
    }

    public final String getRegion() {
        return identityDetails.region();
    }

}
