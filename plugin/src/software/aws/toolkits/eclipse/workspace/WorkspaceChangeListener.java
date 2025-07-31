// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.workspace;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.lsp4j.CreateFilesParams;
import org.eclipse.lsp4j.DeleteFilesParams;
import org.eclipse.lsp4j.FileCreate;
import org.eclipse.lsp4j.FileDelete;
import org.eclipse.lsp4j.FileRename;
import org.eclipse.lsp4j.RenameFilesParams;
import org.eclipse.lsp4j.DidChangeWorkspaceFoldersParams;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.WorkspaceFoldersChangeEvent;

import com.nimbusds.jose.shaded.gson.Gson;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.ThreadingUtils;

public final class WorkspaceChangeListener implements IResourceChangeListener {
    private static final AtomicReference<WorkspaceChangeListener> INSTANCE = new AtomicReference<>();

    private final FileChangeTracker fileChangeTracker;
    private static final Set<Integer> ALLOWED_RESOURCE_TYPES = Set.of(
            IResource.FILE,
            IResource.FOLDER,
            IResource.PROJECT);

    private WorkspaceChangeListener() {
        this.fileChangeTracker = new FileChangeTracker();
    }

    public static WorkspaceChangeListener getInstance() {
        INSTANCE.compareAndSet(null, new WorkspaceChangeListener());
        return INSTANCE.get();
    }

    public void start() {
        ResourcesPlugin.getWorkspace().addResourceChangeListener(
            this,
            IResourceChangeEvent.POST_CHANGE
        );
    }

    @Override
    public void resourceChanged(final IResourceChangeEvent event) {
        ThreadingUtils.executeAsyncTask(() -> processResourceChange(event));
    }

    private void processResourceChange(final IResourceChangeEvent event) {
        try {
            FileChanges changes = fileChangeTracker.trackChanges(event.getDelta());
            notifyLspServer(changes);
        } catch (Exception e) {
            Activator.getLogger().error("Error processing workspace changes", e);
        }
    }

    public void stop() {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
    }

    private record FileChanges(
            List<FileCreate> created,
            List<FileDelete> deleted,
            List<FileRename> renamed,
            List<WorkspaceFolder> addedFolders,
            List<WorkspaceFolder> removedFolders
        ) {
            FileChanges() {
                this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
            }
        }

    private static final class FileChangeTracker {
        FileChanges trackChanges(final IResourceDelta delta) throws CoreException {
            FileChanges changes = new FileChanges();

            delta.accept(resourceDelta -> {
                if (!ALLOWED_RESOURCE_TYPES.contains(resourceDelta.getResource().getType())) {
                    return true;
                }

                processResourceDelta(resourceDelta, changes);
                return true;
            });

            return changes;
        }

        private void processResourceDelta(final IResourceDelta delta, final FileChanges changes) {
            try {
                IResource resource = delta.getResource();
                if (resource.getType() == IResource.PROJECT) {
                    // Only notify for actual project add/remove, not when files inside change
                    if (delta.getKind() == IResourceDelta.ADDED || delta.getKind() == IResourceDelta.REMOVED) {
                        processProjectDelta(delta, changes);
                    }
                    return;
                }
                URI uri = resource.getLocationURI();
                if (uri == null) {
                 // Skip if URI is null which is project change
                    return; 
                }
                String uriString = uri.toString();

                switch (delta.getKind()) {
                    case IResourceDelta.ADDED:
                        changes.created.add(new FileCreate(uriString));
                        break;
                    case IResourceDelta.REMOVED:
                        changes.deleted.add(new FileDelete(uriString));
                        break;
                    case IResourceDelta.CHANGED:
                        processChangedResource(delta, changes, uriString);
                        break;
                    default:
                        throw new IllegalStateException("Unsupported resource delta type: " + delta.getKind());
                }
            } catch (IllegalArgumentException e) {
                Activator.getLogger().error("Invalid resource path", e);
            }
        }

        private void processProjectDelta(IResourceDelta delta, FileChanges changes) {
            IResource resource = delta.getResource();
            String name = resource.getName();
            
            URI uri = resource.getLocationURI();
            if (uri == null) {
                // For deleted projects, construct absolute URI from workspace root + project name
                URI workspaceRoot = ResourcesPlugin.getWorkspace().getRoot().getLocationURI();
                uri = workspaceRoot.resolve(name + "/");
            }
            
            String uriString = uri.toString().replaceFirst("^file:/(?!/)", "file:///");
            WorkspaceFolder folder = new WorkspaceFolder(uriString, name);
            
            switch (delta.getKind()) {
                case IResourceDelta.ADDED:
                    changes.addedFolders.add(folder);
                    break;
                case IResourceDelta.REMOVED:
                    changes.removedFolders.add(folder);
                    break;
            }            
        }

        private void processChangedResource(final IResourceDelta delta, final FileChanges changes, final String newUriString) {
            if ((delta.getFlags() & IResourceDelta.MOVED_FROM) != 0) {
                URI oldUri = delta.getMovedFromPath().toFile().toURI();
                changes.renamed.add(new FileRename(oldUri.toString(), newUriString));
            }
        }
    }

    private void notifyLspServer(final FileChanges changes) {
        try {
            var abc = Activator.getLspProvider().getAmazonQServer().get();
            var lspServer = abc.getWorkspaceService();
            Thread.sleep(10000);
            if (!changes.created.isEmpty()) {
                lspServer.didCreateFiles(new CreateFilesParams(changes.created));
            }

            if (!changes.deleted.isEmpty()) {
                lspServer.didDeleteFiles(new DeleteFilesParams(changes.deleted));
            }

            if (!changes.renamed.isEmpty()) {
                lspServer.didRenameFiles(new RenameFilesParams(changes.renamed));
            }
            if (!changes.addedFolders.isEmpty() || !changes.removedFolders.isEmpty()) {
                WorkspaceFoldersChangeEvent event = new WorkspaceFoldersChangeEvent(
                    changes.addedFolders, changes.removedFolders);
                DidChangeWorkspaceFoldersParams params = new DidChangeWorkspaceFoldersParams(event);
                try {
//                    Class<?> builderClass = software.aws.toolkits.eclipse.amazonq.lsp.AmazonQLspServerBuilder.class;
//                    java.lang.reflect.Field launcherField = builderClass.getDeclaredField("launcher");
//                    launcherField.setAccessible(true);
//                    var launcher = launcherField.get(null);
//                    
//                    if (launcher != null) {
//                        // Get the RemoteEndpoint from launcher
//                        java.lang.reflect.Method getRemoteEndpointMethod = launcher.getClass().getMethod("getRemoteEndpoint");
//                        var remoteEndpoint = getRemoteEndpointMethod.invoke(launcher);
//                        
//                        // Send notification using the RemoteEndpoint
//                        java.lang.reflect.Method notifyMethod = remoteEndpoint.getClass().getMethod("notify", String.class, Object.class);
//                        notifyMethod.invoke(remoteEndpoint, "workspace/didChangeWorkspaceFolders", params);
//                        Activator.getLogger().info("Params constsnt" + new Gson().toJson(params));
//                        Activator.getLogger().info("didChangeWorkspaceFolders notification sent successfully");
//                        notifyMethod.invoke(remoteEndpoint, "amazon/dummy", "hello from eclipse");
                    lspServer.didChangeWorkspaceFolders(params);
                    
                } catch (Exception e) {
                    Activator.getLogger().error("Failed to send notification via launcher", e);
                }
            }
        } catch (Exception e) {
            Activator.getLogger().error(
                "Unable to update LSP with file change events: " + e.getMessage()
            );
        }
    }
}
