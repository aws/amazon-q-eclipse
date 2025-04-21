// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth.model;

import software.aws.toolkits.eclipse.amazonq.lsp.model.ConnectionMetadata;
import software.aws.toolkits.eclipse.amazonq.lsp.model.SsoProfileData;
import software.aws.toolkits.eclipse.amazonq.lsp.model.UpdateCredentialsPayload;

public record GetSsoTokenResult(SsoToken ssoToken, UpdateCredentialsPayload updateCredentialsParams) {

    public UpdateCredentialsPayload getdUpdateCredentialsPayloadHydratedWithStartUrl(final String startUrl) {
        SsoProfileData ssoProfileData = new SsoProfileData();
        ssoProfileData.setStartUrl(startUrl);
        ConnectionMetadata metadata = new ConnectionMetadata();
        metadata.setSso(ssoProfileData);
        return new UpdateCredentialsPayload(updateCredentialsParams.data(), metadata,
                updateCredentialsParams.encrypted());
    }

}
