// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.model;

public class QDeveloperProfile extends Configuration {
    private IdentityDetails identityDetails;

    public QDeveloperProfile(final String arn, final String name, final IdentityDetails identityDetails) {
        super(arn, name);
        this.identityDetails = identityDetails;
    }

    public final IdentityDetails getIdentityDetails() {
        return this.identityDetails;
    }

}
