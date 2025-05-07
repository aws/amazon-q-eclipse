// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.editor;

import org.eclipse.core.resources.IStorage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;

public final class InMemoryInput implements IStorageEditorInput {
    private final IStorage storage;

    public InMemoryInput(final IStorage storage) {
        this.storage = storage;
    }

    @Override
    public IStorage getStorage() {
        return storage;
    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public String getName() {
        return storage.getName();
    }

    @Override
    public String getToolTipText() {
        return getName();
    }

    @Override
    public IPersistableElement getPersistable() {
        return null;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Object getAdapter(final Class c) {
        return null;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return null;
    }
}
