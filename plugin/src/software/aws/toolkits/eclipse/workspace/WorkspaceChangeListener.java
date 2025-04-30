package software.aws.toolkits.eclipse.workspace;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

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

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.preferences.AmazonQPreferencePage;
import software.aws.toolkits.eclipse.amazonq.util.ThreadingUtils;

public final class WorkspaceChangeListener implements IResourceChangeListener {

    private static volatile WorkspaceChangeListener instance;

    private WorkspaceChangeListener() { }

    public static WorkspaceChangeListener getInstance() {
        if (instance == null) {
            synchronized (WorkspaceChangeListener.class) {
                if (instance == null) {
                    instance = new WorkspaceChangeListener();
                }
            }
        }
        return instance;
    }

    public void start() {
        ResourcesPlugin.getWorkspace().addResourceChangeListener(
            this,
            IResourceChangeEvent.POST_CHANGE
        );
    }

    @Override
    public void resourceChanged(final IResourceChangeEvent event) {
        ThreadingUtils.executeAsyncTask(() -> {
            boolean indexingEnabled = Activator.getDefault().getPreferenceStore().getBoolean(AmazonQPreferencePage.WORKSPACE_INDEX);
            if (!indexingEnabled) {
                return;
            }

            IResourceDelta delta = event.getDelta();

            List<FileCreate> createdFiles = new ArrayList<>();
            List<FileDelete> deletedFiles = new ArrayList<>();
            List<FileRename> renamedFiles = new ArrayList<>();

            try {
                delta.accept(delta1 -> {
                    if (delta1.getResource().getType() != IResource.FILE) {
                        return true;
                    }

                    URI uri = delta1.getResource().getLocationURI();
                    String uriString = uri.toString();

                    switch (delta1.getKind()) {
                        case IResourceDelta.ADDED:
                            createdFiles.add(new FileCreate(uriString));
                            break;
                        case IResourceDelta.REMOVED:
                            deletedFiles.add(new FileDelete(uriString));
                            break;
                        case IResourceDelta.CHANGED:
                            if ((delta1.getFlags() & IResourceDelta.MOVED_FROM) != 0) {
                                URI oldUri = delta1.getMovedFromPath().toFile().toURI();
                                renamedFiles.add(new FileRename(oldUri.toString(), uriString));
                            }
                            break;
                        default:
                    }
                    return true;
                });
            } catch (CoreException e) {
                Activator.getLogger().error("Unable to process file change events", e);
            } catch (IllegalArgumentException e) {
                Activator.getLogger().error("Invalid resource path", e);
            }

            try {
                if (!createdFiles.isEmpty()) {
                    CreateFilesParams createParams = new CreateFilesParams(createdFiles);
                    Activator.getLspProvider().getAmazonQServer().get().getWorkspaceService().didCreateFiles(createParams);
                }

                if (!deletedFiles.isEmpty()) {
                    DeleteFilesParams deleteParams = new DeleteFilesParams(deletedFiles);
                    Activator.getLspProvider().getAmazonQServer().get().getWorkspaceService().didDeleteFiles(deleteParams);
                }

                if (!renamedFiles.isEmpty()) {
                    RenameFilesParams renameParams = new RenameFilesParams(renamedFiles);
                    Activator.getLspProvider().getAmazonQServer().get().getWorkspaceService().didRenameFiles(renameParams);
                }
            } catch (Exception e) {
                Activator.getLogger().error("Unable to update LSP with file change events: " + e.getMessage());
            }
        });
    }

    public void stop() {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
    }

}
