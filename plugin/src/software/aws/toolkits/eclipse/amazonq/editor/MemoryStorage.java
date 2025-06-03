// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.editor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

public final class MemoryStorage implements IStorage {
    private final String path;
    private final byte[] bytes;

    public MemoryStorage(final String path, final String body) {
        this.path = path;
        this.bytes = body.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public InputStream getContents() {
        return new ByteArrayInputStream(bytes.clone());
    }

    @Override
    public IPath getFullPath() {
        return new Path(path);
    }

    @Override
    public String getName() {
        return path + " (preview)";
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public <T> T getAdapter(final Class<T> adapter) {
        return Platform.getAdapterManager().getAdapter(this, adapter);
    }

}
