// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.telemetry;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.services.cognitoidentity.CognitoIdentityClient;
import software.amazon.awssdk.services.cognitoidentity.model.Credentials;
import software.amazon.awssdk.services.cognitoidentity.model.GetCredentialsForIdentityRequest;
import software.amazon.awssdk.services.cognitoidentity.model.GetIdRequest;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.cache.CachedSupplier;
import software.amazon.awssdk.utils.cache.NonBlocking;
import software.amazon.awssdk.utils.cache.RefreshResult;

import java.time.temporal.ChronoUnit;

public class AwsCognitoCredentialsProvider implements AwsCredentialsProvider, SdkAutoCloseable {
    private final String identityPoolId;
    private final CognitoIdentityClient cognitoClient;
    private final CachedSupplier<AwsSessionCredentials> cacheSupplier;

    public AwsCognitoCredentialsProvider(final String identityPoolId, final CognitoIdentityClient cognitoClient) {
        this.identityPoolId = identityPoolId;
        this.cognitoClient = cognitoClient;
        this.cacheSupplier = CachedSupplier.builder(this::updateCognitoCredentials)
                .prefetchStrategy(new NonBlocking("Cognito Identity Credential Refresh"))
                .build();
    }

    @Override
    public final AwsCredentials resolveCredentials() {
        return cacheSupplier.get();
    }

    private RefreshResult<AwsSessionCredentials> updateCognitoCredentials() {
        String identityId = getIdentityId();
        Credentials cognitoCredentials = getCredentialsForIdentity(identityId);

        AwsSessionCredentials sessionCredentials = AwsSessionCredentials.create(
                cognitoCredentials.accessKeyId(),
                cognitoCredentials.secretKey(),
                cognitoCredentials.sessionToken()
        );

        return RefreshResult.builder(sessionCredentials)
                .staleTime(cognitoCredentials.expiration().minus(1, ChronoUnit.MINUTES))
                .prefetchTime(cognitoCredentials.expiration().minus(5, ChronoUnit.MINUTES))
                .build();
    }

    private String getIdentityId() {
        GetIdRequest request = GetIdRequest.builder()
                .identityPoolId(identityPoolId)
                .build();
        return cognitoClient.getId(request).identityId();
    }

    private Credentials getCredentialsForIdentity(final String identityId) {
        GetCredentialsForIdentityRequest request = GetCredentialsForIdentityRequest.builder()
                .identityId(identityId)
                .build();
        return cognitoClient.getCredentialsForIdentity(request).credentials();
    }

    @Override
    public final void close() {
        cognitoClient.close();
        cacheSupplier.close();
    }
}

