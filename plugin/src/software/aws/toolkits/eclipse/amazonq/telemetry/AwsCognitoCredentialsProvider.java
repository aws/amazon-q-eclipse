// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.telemetry;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.services.cognitoidentity.CognitoIdentityClient;
import software.amazon.awssdk.services.cognitoidentity.model.GetCredentialsForIdentityRequest;
import software.amazon.awssdk.services.cognitoidentity.model.GetCredentialsForIdentityResponse;
import software.amazon.awssdk.services.cognitoidentity.model.GetIdRequest;
import software.amazon.awssdk.services.cognitoidentity.model.GetIdResponse;

public class AwsCognitoCredentialsProvider implements AwsCredentialsProvider {
    private final String identityPoolId;
    private final CognitoIdentityClient cognitoIdentityClient;

    public AwsCognitoCredentialsProvider(final String identityPoolId, final CognitoIdentityClient cognitoIdentityClient) {
        this.identityPoolId = identityPoolId;
        this.cognitoIdentityClient = cognitoIdentityClient;
    }

    @Override
    public final AwsSessionCredentials resolveCredentials() {
        GetIdResponse getIdResponse = cognitoIdentityClient.getId(GetIdRequest.builder()
                .identityPoolId(identityPoolId)
                .build());

        GetCredentialsForIdentityResponse getCredentialsResponse = cognitoIdentityClient.getCredentialsForIdentity(GetCredentialsForIdentityRequest.builder()
                .identityId(getIdResponse.identityId())
                .build());

        return AwsSessionCredentials.create(
                getCredentialsResponse.credentials().accessKeyId(),
                getCredentialsResponse.credentials().secretKey(),
                getCredentialsResponse.credentials().sessionToken()
        );
    }
}
