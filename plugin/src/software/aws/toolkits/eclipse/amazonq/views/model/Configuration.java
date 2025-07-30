// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.model;

import com.google.gson.annotations.SerializedName;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.amazon.awssdk.arns.Arn;

public class Configuration {
    @SerializedName("arn")
    private String arn;

    @SerializedName("name")
    private String name;

    @SerializedName("accountId")
    private String accountId;

    public Configuration(final String arn, final String name) {
        this.arn = arn;
        this.name = name;
        this.accountId = extractAccountId(this.arn);
    }

    public Configuration() {
        this.arn = null;
        this.name = null;
        this.accountId = null;
    }

    public final void setArn(final String arn) {
        this.arn = arn;
    }

    public final void setName(final String name) {
        this.name = name;
    }

    public final void setAccountId(final String accountId) {
        this.accountId = accountId;
    }

    public final String getArn() {
        return this.arn;
    }

    public final String getName() {
        return this.name;
    }

    public final String getAccountId() {
        if (this.accountId == null) {
            this.accountId = extractAccountId(this.arn);
        }
        return this.accountId;
    }

    private String extractAccountId(final String arn) {
        try {
            if (arn == null || arn.trim().isEmpty()) {
                return null;
            }
            return Arn.fromString(arn).accountId().orElse(null);
        } catch (Exception e) {
            Activator.getLogger().error("Error parsing arn for account Id", e);
            return null;
        }
    }

}
