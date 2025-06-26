// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.editor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import software.aws.toolkits.eclipse.amazonq.lsp.AmazonQLspServer;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

/**
 * Listens for active editor changes and notifies the language server
 * with debouncing to avoid excessive notifications.
 */
public class ActiveEditorChangeListener implements IPartListener2 {
    private static final long DEBOUNCE_DELAY_MS = 100L;

    private final AmazonQLspServer languageServer;
    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> debounceTask;

    public ActiveEditorChangeListener(final AmazonQLspServer languageServer, final ScheduledExecutorService executor) {
        this.languageServer = languageServer;
        this.executor = executor;
    }

    @Override
    public void partActivated(final IWorkbenchPartReference partRef) {
        if (partRef.getPart(false) instanceof ITextEditor) {
            handleEditorChange((ITextEditor) partRef.getPart(false));
        }
    }

    @Override
    public void partBroughtToTop(final IWorkbenchPartReference partRef) {
        if (partRef.getPart(false) instanceof ITextEditor) {
            handleEditorChange((ITextEditor) partRef.getPart(false));
        }
    }

    @Override
    public void partClosed(final IWorkbenchPartReference partRef) {
        // When editor is closed, send notification with null values
        handleEditorChange(null);
    }

    @Override
    public void partDeactivated(final IWorkbenchPartReference partRef) {
        // No action needed
    }

    @Override
    public void partOpened(final IWorkbenchPartReference partRef) {
        if (partRef.getPart(false) instanceof ITextEditor) {
            handleEditorChange((ITextEditor) partRef.getPart(false));
        }
    }

    @Override
    public void partHidden(final IWorkbenchPartReference partRef) {
        // No action needed
    }

    @Override
    public void partVisible(final IWorkbenchPartReference partRef) {
        if (partRef.getPart(false) instanceof ITextEditor) {
            handleEditorChange((ITextEditor) partRef.getPart(false));
        }
    }

    @Override
    public void partInputChanged(final IWorkbenchPartReference partRef) {
        if (partRef.getPart(false) instanceof ITextEditor) {
            handleEditorChange((ITextEditor) partRef.getPart(false));
        }
    }

    private void handleEditorChange(final ITextEditor editor) {
        // Cancel any pending notification
        if (debounceTask != null) {
            debounceTask.cancel(false);
        }

        // Schedule a new notification after the debounce period
        debounceTask = executor.schedule(() -> {
            try {
                Map<String, Object> params = createActiveEditorParams(editor);

                // Send notification to language server
                languageServer.activeEditorChanged(params);
                Activator.getLogger().info("Active editor changed notification sent: "
                    + (editor != null ? editor.getTitle() : "no editor"));

            } catch (Exception e) {
                Activator.getLogger().error("Failed to send active editor changed notification", e);
            }
        }, DEBOUNCE_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private Map<String, Object> createActiveEditorParams(final ITextEditor editor) {
        Map<String, Object> params = new HashMap<>();

        if (editor != null) {
            IEditorInput editorInput = editor.getEditorInput();

            if (editorInput instanceof IFileEditorInput) {
                IFileEditorInput fileInput = (IFileEditorInput) editorInput;
                IFile file = fileInput.getFile();

                // Create textDocument with workspace-relative path if in workspace, absolute otherwise
                Map<String, String> textDocument = new HashMap<>();
                if (file.getWorkspace().getRoot().exists(file.getFullPath())) {
                    // workspace-relative path
                    textDocument.put("uri", file.getFullPath().toString());
                } else {
                    // absolute path
                    textDocument.put("uri", file.getLocation().toString());
                }
                params.put("textDocument", textDocument);

                // Add cursor state if available
                try {
                    ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
                    if (selection != null) {
                        Map<String, Object> cursorState = new HashMap<>();
                        Map<String, Integer> range = new HashMap<>();
                        range.put("start", selection.getOffset());
                        range.put("end", selection.getOffset() + selection.getLength());
                        cursorState.put("range", range);
                        params.put("cursorState", cursorState);
                    }
                } catch (Exception e) {
                    // Cursor state is optional, continue without it
                    Activator.getLogger().info("Could not get cursor state: " + e.getMessage());
                }
            }
        } else {
            // Editor is null (closed), send null values
            params.put("textDocument", null);
            params.put("cursorState", null);
        }

        return params;
    }

    /**
     * Register the listener with the workbench.
     */
    public static ActiveEditorChangeListener register(final AmazonQLspServer languageServer, final ScheduledExecutorService executor) {
        ActiveEditorChangeListener listener = new ActiveEditorChangeListener(languageServer, executor);

        // Register with all workbench windows
        PlatformUI.getWorkbench().addWindowListener(new org.eclipse.ui.IWindowListener() {
            @Override
            public void windowActivated(final org.eclipse.ui.IWorkbenchWindow window) {
                window.getPartService().addPartListener(listener);
            }

            @Override
            public void windowDeactivated(final org.eclipse.ui.IWorkbenchWindow window) {
                // Keep listener active
            }

            @Override
            public void windowClosed(final org.eclipse.ui.IWorkbenchWindow window) {
                window.getPartService().removePartListener(listener);
            }

            @Override
            public void windowOpened(final org.eclipse.ui.IWorkbenchWindow window) {
                window.getPartService().addPartListener(listener);
            }
        });

        // Register with current active window if available
        if (PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null) {
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().addPartListener(listener);
        }

        return listener;
    }

    /**
     * Unregister the listener.
     */
    public void dispose() {
        if (debounceTask != null) {
            debounceTask.cancel(true);
        }

        // Remove from all workbench windows
        for (org.eclipse.ui.IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
            window.getPartService().removePartListener(this);
        }
    }
}