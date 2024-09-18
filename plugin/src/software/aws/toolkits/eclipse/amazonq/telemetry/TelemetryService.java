// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.telemetry;

import java.io.InputStream;
import java.net.URI;

import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentity.CognitoIdentityClient;
import software.amazon.awssdk.services.toolkittelemetry.ToolkitTelemetryClient;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

public final class TelemetryService {
    private static final ToolkitTelemetryClient CLIENT = createDefaultTelemetryClient();
    private static final Region TELEMETRY_REGION = Region.US_EAST_1;
    private static final String TELEMETRY_ENDPOINT = "https://client-telemetry.us-east-1.amazonaws.com";
    private static final String TELEMETRY_IDENTITY_POOL = "us-east-1:820fd6d1-95c0-4ca4-bffb-3f01d32da842";

    // prevent instantiation
    private TelemetryService() { }

    public static ToolkitTelemetryClient getClient() {
        return CLIENT;
    }

    private static ClientOverrideConfiguration.Builder nullDefaultProfileFile(final ClientOverrideConfiguration.Builder builder) {
        return builder.defaultProfileFile(ProfileFile.builder()
                .content(InputStream.nullInputStream())
                .type(ProfileFile.Type.CONFIGURATION)
                .build());
    }

    private static ToolkitTelemetryClient createDefaultTelemetryClient() {
        CognitoIdentityClient cognitoClient = CognitoIdentityClient.builder()
                .credentialsProvider(AnonymousCredentialsProvider.create())
                .region(TELEMETRY_REGION)
                .httpClient(ApacheHttpClient.create())
                .overrideConfiguration(builder -> nullDefaultProfileFile(builder))
                .build();
        AwsCredentialsProvider credentialsProvider = new AwsCognitoCredentialsProvider(TELEMETRY_IDENTITY_POOL, cognitoClient);
        return ToolkitTelemetryClient.builder()
                .credentialsProvider(credentialsProvider)
                .endpointOverride(URI.create(TELEMETRY_ENDPOINT))
                .build();
    }
}
