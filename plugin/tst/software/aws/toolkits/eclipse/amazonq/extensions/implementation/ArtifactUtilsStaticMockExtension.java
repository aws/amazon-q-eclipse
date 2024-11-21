// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.extensions.implementation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mockStatic;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;

import software.aws.toolkits.eclipse.amazonq.extensions.api.StaticMockExtension;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher.ArtifactUtils;

public final class ArtifactUtilsStaticMockExtension extends StaticMockExtension<ArtifactUtils>
        implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
    private MockedStatic<ArtifactUtils> artifactUtilsStaticMock = null;

    @Override
    public MockedStatic<ArtifactUtils> getStaticMock() {
        return artifactUtilsStaticMock;
    }

    @Override
    public void beforeAll(final ExtensionContext context) {
        artifactUtilsStaticMock = mockStatic(ArtifactUtils.class);
    }

    @Override
    public void beforeEach(final ExtensionContext context) {
        artifactUtilsStaticMock.reset();
        artifactUtilsStaticMock.when(() -> ArtifactUtils.deleteFile(any(Path.class))).thenCallRealMethod();
        artifactUtilsStaticMock.when(() -> ArtifactUtils.deleteDirectory(any(Path.class))).thenCallRealMethod();
        artifactUtilsStaticMock.when(() -> ArtifactUtils.getFilenameWithoutExtension(any(Path.class)))
                .thenCallRealMethod();
        artifactUtilsStaticMock.when(() -> ArtifactUtils.extractFile(any(Path.class), any(Path.class)))
                .thenCallRealMethod();
        artifactUtilsStaticMock.when(() -> ArtifactUtils.copyDirectory(any(Path.class), any(Path.class)))
                .thenCallRealMethod();
        artifactUtilsStaticMock.when(() -> ArtifactUtils.parseVersion(any(String.class))).thenCallRealMethod();
        artifactUtilsStaticMock.when(
                () -> ArtifactUtils.validateHash(any(Path.class), ArgumentMatchers.<List<String>>any(), anyBoolean()))
                .thenCallRealMethod();
        artifactUtilsStaticMock.when(() -> ArtifactUtils.calculateHash(any(Path.class))).thenCallRealMethod();
        artifactUtilsStaticMock.when(() -> ArtifactUtils.copyMissingFilesFromZip(any(Path.class), any(Path.class)))
                .thenCallRealMethod();
        artifactUtilsStaticMock.when(() -> ArtifactUtils.hasPosixFilePermissions(any(Path.class))).thenCallRealMethod();
    }

    @Override
    public void afterAll(final ExtensionContext context) throws Exception {
        if (artifactUtilsStaticMock != null) {
            artifactUtilsStaticMock.close();
        }
    }

}
