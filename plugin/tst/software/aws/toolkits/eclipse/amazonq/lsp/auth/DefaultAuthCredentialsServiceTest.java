// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import software.aws.toolkits.eclipse.amazonq.lsp.AmazonQLspServer;
import software.aws.toolkits.eclipse.amazonq.lsp.model.UpdateCredentialsPayload;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;

public class DefaultAuthCredentialsServiceTest {
    private static DefaultAuthCredentialsService authCredentialsService;
    private static LspProvider mockLspProvider;
    private static AmazonQLspServer mockedAmazonQServer;

    @BeforeEach
    public final void setUp() {
        mockLspProvider = mock(LspProvider.class);
        mockedAmazonQServer = mock(AmazonQLspServer.class);

        resetAuthTokenService();

        when(mockLspProvider.getAmazonQServer())
            .thenReturn(CompletableFuture.completedFuture(mockedAmazonQServer));
    }

    @Test
    void updateTokenCredentialsUnencryptedSuccess() {
        String accessToken = "accessToken";
        boolean isEncrypted = false;

        when(mockedAmazonQServer.updateTokenCredentials(any()))
            .thenReturn(CompletableFuture.completedFuture(new ResponseMessage()));

        authCredentialsService.updateTokenCredentials(new UpdateCredentialsPayload(accessToken, isEncrypted));

        verify(mockedAmazonQServer).updateTokenCredentials(any());
        verifyNoMoreInteractions(mockedAmazonQServer);
    }

    @Test
    void updateTokenCredentialsEncryptedSuccess() {
        boolean isEncrypted = true;

        when(mockedAmazonQServer.updateTokenCredentials(any()))
            .thenReturn(CompletableFuture.completedFuture(new ResponseMessage()));

        authCredentialsService.updateTokenCredentials(new UpdateCredentialsPayload("encryptedToken", isEncrypted));

        verify(mockedAmazonQServer).updateTokenCredentials(any());
        verifyNoMoreInteractions(mockedAmazonQServer);
    }

    @Test
    void deleteTokenCredentialsSuccess() {
        authCredentialsService.deleteTokenCredentials();

        verify(mockedAmazonQServer).deleteTokenCredentials();
        verifyNoMoreInteractions(mockedAmazonQServer);
    }

    private void resetAuthTokenService() {
        authCredentialsService = DefaultAuthCredentialsService.builder()
                .withLspProvider(mockLspProvider)
                .build();
        authCredentialsService = spy(authCredentialsService);
      }
}
