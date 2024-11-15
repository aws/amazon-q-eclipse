// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.actions;

import software.aws.toolkits.eclipse.amazonq.views.model.ExternalLink;

public final class OpenUserGuideAction extends OpenUrlAction {

    public OpenUserGuideAction() {
        super("Open User Guide", "ellipses_openUserGuide", ExternalLink.QInIdeGuide);
    }
}
