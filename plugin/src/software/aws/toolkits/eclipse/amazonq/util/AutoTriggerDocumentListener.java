// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import static software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils.getActiveTextEditor;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.texteditor.ITextEditor;

import software.aws.toolkits.eclipse.amazonq.editor.InMemoryInput;
import software.aws.toolkits.eclipse.amazonq.inlineChat.InlineChatSession;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public final class AutoTriggerDocumentListener implements IDocumentListener, IAutoTriggerListener {
    private static final String UNDO_COMMAND_ID = "org.eclipse.ui.edit.undo";

    // ADT editors keep a "live editing document" separate from the workspace file document that lsp4e tracks.
    // lsp4e only sends textDocument/didChange when the workspace file is saved, so the LSP server has stale
    // content while the user is typing. We track ADT document versions separately and manually push didChange
    // before each inline completion request to keep the server in sync.
    private static final int ADT_VERSION_START = 10000;
    private static final ConcurrentHashMap<String, AtomicInteger> ADT_DOC_VERSIONS = new ConcurrentHashMap<>();

    private final ThreadLocal<Boolean> isChangeInducedByUndo = ThreadLocal.withInitial(() -> false);
    private IExecutionListener commandListener;

    public AutoTriggerDocumentListener() {
    }

    @Override
    public void documentAboutToBeChanged(final DocumentEvent e) {
        return;
    }

    @Override
    public synchronized void documentChanged(final DocumentEvent e) {
        var qSes = QInvocationSession.getInstance();
        if (InlineChatSession.getInstance().isSessionActive()) {
            return;
        }
        if (!shouldSendQuery(e, qSes)) {
            return;
        }
        var editor = getActiveTextEditor();

        if (!qSes.isActive() && !(editor.getEditorInput() instanceof InMemoryInput)) {
            try {
                qSes.start(editor);
            } catch (ExecutionException e1) {
                return;
            }
        }
        if (!(editor.getEditorInput() instanceof InMemoryInput)) {
            syncLspDocumentIfAdt(e.getDocument(), editor);
            qSes.invoke(qSes.getViewer().getTextWidget().getCaretOffset(), e.getText().length());
        }
    }

    private boolean shouldSendQuery(final DocumentEvent e, final QInvocationSession session) {
        if (!Activator.getLoginService().getAuthState().isLoggedIn()) {
            return false;
        }

        if (e.getText().length() <= 0) {
            return false;
        }

        if (session.isPreviewingSuggestions() || session.isDecisionMade()) {
            return false;
        }

        if (isChangeInducedByUndo.get()) {
            isChangeInducedByUndo.set(false);
            return false;
        }

        // TODO: implement other logic to prevent unnecessary firing
        return true;
    }

    // For SAP ADT editors the viewer document (what users type into) is a different object from the file-buffer
    // document that lsp4e tracks. lsp4e's textDocument/didChange only fires when the file-buffer document changes,
    // which only happens on save. We bridge the gap by manually sending didChange with the current content before
    // each inline completion request. Because lsp4j serializes outgoing messages through a single writer queue,
    // didChange is guaranteed to reach the server before the inlineCompletion request that follows.
    private void syncLspDocumentIfAdt(final IDocument document, final ITextEditor editor) {
        if (Display.getCurrent() == null) {
            return; // ADT sync is only relevant for UI-thread-initiated changes (user typing)
        }
        try {
            var window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            if (window == null) {
                return;
            }
            var activePage = window.getActivePage();
            if (activePage == null) {
                return;
            }
            var activeEditor = activePage.getActiveEditor();
            if (activeEditor == null || !AbapUtil.isAdtEditor(activeEditor.getClass().getName())) {
                return;
            }

            var fileUri = QEclipseEditorUtils.getOpenFileUri(editor.getEditorInput());
            if (!fileUri.isPresent()) {
                return;
            }

            String uri = fileUri.get();
            int version = ADT_DOC_VERSIONS
                    .computeIfAbsent(uri, k -> new AtomicInteger(ADT_VERSION_START))
                    .incrementAndGet();

            var docId = new VersionedTextDocumentIdentifier(uri, version);
            var changeEvent = new TextDocumentContentChangeEvent(document.get());
            var params = new DidChangeTextDocumentParams(docId, List.of(changeEvent));

            // getNow(null) avoids blocking: if the server isn't ready yet we skip the sync
            // (the server can't serve completions either in that case)
            var server = Activator.getLspProvider().getAmazonQServer().getNow(null);
            if (server != null) {
                server.getTextDocumentService().didChange(params);
            }
        } catch (Exception ex) {
            Activator.getLogger().error("Failed to sync ADT document with LSP server", ex);
        }
    }

    @Override
    public void onStart() {
        ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
        commandListener = QEclipseEditorUtils.getAutoTriggerExecutionListener((commandId) -> undoCommandListenerCallback(commandId));
        commandService.addExecutionListener(commandListener);
        return;
    }

    @Override
    public void onShutdown() {
        if (commandListener != null) {
            ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
            commandService.removeExecutionListener(commandListener);
        }
        return;
    }

    private void undoCommandListenerCallback(final String commandId) {
        if (commandId.equals(UNDO_COMMAND_ID)) {
            isChangeInducedByUndo.set(true);
        }
    }
}
