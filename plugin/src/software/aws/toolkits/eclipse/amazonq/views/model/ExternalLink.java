// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.model;

public enum ExternalLink {
    GitHubRepo("https://github.com/aws/amazon-q-eclipse"),
    GitHubIssues("https://github.com/aws/amazon-q-eclipse/issues"),
    QInIdeGuide("https://docs.aws.amazon.com/amazonq/latest/qdeveloper-ug/q-in-IDE.html");

    private String value;

    ExternalLink(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
