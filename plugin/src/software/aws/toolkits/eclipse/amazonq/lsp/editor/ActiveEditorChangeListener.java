// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.editor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils;
import software.aws.toolkits.eclipse.amazonq.util.ThreadingUtils;

public final class ActiveEditorChangeListener implements IPartListener2 {
    private static ActiveEditorChangeListener instance;
    private static final long DEBOUNCE_DELAY_MS = 100L;
    private ScheduledFuture<?> debounceTask;
    private IWorkbenchWindow registeredWindow;

    private ActiveEditorChangeListener() { }

    public static ActiveEditorChangeListener getInstance() {
        if (instance == null) {
            instance = new ActiveEditorChangeListener();
        }
        return instance;
    }

    public void initialize() {
        Display.getDefault().asyncExec(() -> {
            try {
                registeredWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                if (registeredWindow != null) {
                    registeredWindow.getPartService().addPartListener(this);
                }
            } catch (Exception e) {
                Activator.getLogger().error("Failed to initialize ActiveEditorChangeListener", e);
            }
        });
    }

    public void stop() {
        if (debounceTask != null) {
            debounceTask.cancel(true);
        }
        try {
            if (registeredWindow != null) {
                registeredWindow.getPartService().removePartListener(this);
            }
        } catch (Exception e) {
            Activator.getLogger().error("Error stopping ActiveEditorChangeListener", e);
        }
    }

    @Override
    public void partActivated(final IWorkbenchPartReference partRef) {
        if (partRef.getPart(false) instanceof ITextEditor) {
            ITextEditor editor = (ITextEditor) partRef.getPart(false);
            handleEditorChange(editor);
        }
    }

    @Override
    public void partClosed(final IWorkbenchPartReference partRef) {
        if (partRef.getPart(false) instanceof ITextEditor) {
            handleEditorChange(null);
        }
    }

    private void handleEditorChange(final ITextEditor editor) {
        // Cancel any pending notification
        if (debounceTask != null) {
            debounceTask.cancel(false);
        }

        // Schedule a new notification after the debounce period
        debounceTask = (ScheduledFuture<?>) ThreadingUtils.scheduleAsyncTaskWithDelay(() -> {
            Display.getDefault().syncExec(() -> {
                try {
                    Map<String, Object> params = createActiveEditorParams(editor);
                    var lspServer = Activator.getLspProvider().getAmazonQServer().get();
                    lspServer.activeEditorChanged(params);
                } catch (Exception e) {
                    Activator.getLogger().error("Failed to send active editor changed notification", e);
                }
            });
        }, DEBOUNCE_DELAY_MS);
    }

    private Map<String, Object> createActiveEditorParams(final ITextEditor editor) {
        Map<String, Object> params = new HashMap<>();
        if (editor != null) {
            Optional<String> fileUri = QEclipseEditorUtils.getOpenFileUri(editor.getEditorInput());
            if (fileUri.isPresent()) {
                Map<String, String> textDocument = new HashMap<>();
                textDocument.put("uri", fileUri.get());
                params.put("textDocument", textDocument);
                QEclipseEditorUtils.getSelectionRange(editor).ifPresent(range -> {
                    Map<String, Object> cursorState = new HashMap<>();
                    cursorState.put("range", range);
                    params.put("cursorState", cursorState);
                });
            }
        } else {
            // Editor is null (closed), send null values
            params.put("textDocument", null);
            params.put("cursorState", null);
        }

        return params;
    }
}
