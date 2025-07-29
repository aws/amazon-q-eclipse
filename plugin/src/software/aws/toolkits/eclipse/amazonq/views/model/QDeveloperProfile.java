// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.model;

import software.amazon.awssdk.utils.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import software.amazon.awssdk.arns.Arn;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

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
        if (identityDetails != null && !StringUtils.isBlank(identityDetails.region())) {
            return identityDetails.region();
        }
        try {
            return Arn.fromString(getArn()).region().orElse(null);
        } catch (Exception e) {
            Activator.getLogger().error("Error parsing arn for region", e);
            return null;
        }
    }
}
