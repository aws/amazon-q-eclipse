// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.jface.dialogs.MessageDialog;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

/**
 * Utility class for ABAP/ADT-related constants and helper methods. Centralizes
 * hardcoded values to improve maintainability.
 */
public final class AbapUtil {

    // ABAP/ADT related constants
    public static final String SEMANTIC_FS_SCHEME = "semanticfs:/";
    public static final String ADT_CLASS_NAME_PATTERN = "com.sap.adt";
    public static final String SEMANTIC_BUNDLE_ID = "org.eclipse.core.resources.semantic";
    public static final String SEMANTIC_CACHE_FOLDER = ".cache";
    private static final Set<String> ABAP_EXTENSIONS = Set.of("asprog", "aclass", "asinc", "aint", "assrvds",
            "asbdef", "asddls", "astablds", "astabldt", "amdp", "apack", "asrv", "aobj", "aexit", "abdef",
            "acinc", "asfugr", "apfugr", "asfunc", "asfinc", "apfunc", "apfinc");

    private AbapUtil() {
        // Prevent instantiation
    }

    /**
     * Checks if the given class name indicates an ADT editor.
     * ADT editors match pattern:
     * - SAP package prefix (com.sap.adt) for all SAP editors
     * such as: com.sap.adt.abapClassEditor,
     * com.sap.adt.abapInterfaceEditor,
     * com.sap.adt.functions.ui.internal.groups.editors.FunctionGroupEditor
     * com.sap.adt.functions.ui.internal.includes.editors.FunctionGroupIncludeEditor
     * com.sap.adt.ddic.tabletype.ui.internal.editors.TableTypeEditor
     * @param className the class name to check
     * @return true if it's likely an ADT editor
     */
    public static boolean isAdtEditor(final String className) {
        return className != null && className.contains(ADT_CLASS_NAME_PATTERN);
    }

    /**
     * Converts a semantic filesystem URI to a file system path.
     * @param semanticUri the semantic URI starting with "semanticfs:/"
     * @return the converted file system path
     */
    public static String convertSemanticUriToPath(final String semanticUri) {
        if (StringUtils.isBlank(semanticUri) || !semanticUri.startsWith(SEMANTIC_FS_SCHEME)) {
            return semanticUri;
        }
        String folderName = semanticUri.substring(SEMANTIC_FS_SCHEME.length());
        IPath cachePath = Platform.getStateLocation(Platform.getBundle(SEMANTIC_BUNDLE_ID))
                .append(SEMANTIC_CACHE_FOLDER)
                .append(folderName);
        return cachePath.toFile().toURI().toString();
    }

    /**
     * Checks if a file is an ABAP ADT file referenced via the semantic cache.
     * @param file the file to check
     * @return true if it's an ABAP ADT file
     */
    public static boolean isAbapFile(final String filePath) {
        if (StringUtils.isBlank(filePath)) {
            return false;
        }
        // checks whether a given file is an abap file present in cache by looking for certain strings in the path
        return StringUtils.containsIgnoreCase(filePath, AbapUtil.SEMANTIC_BUNDLE_ID) && StringUtils.containsIgnoreCase(filePath, ".adt");
    }


    /**
     * Gets the semantic cache path for a given workspace-relative path.
     * @param workspaceRelativePath the workspace-relative path
     * @return the full semantic cache path
     */
    public static String getSemanticCachePath(final String workspaceRelativePath) {
        if (StringUtils.isBlank(workspaceRelativePath)) {
            throw new IllegalArgumentException("Relative path for ADT plugin file does not exist");
        }
        IPath cachePath = Platform.getStateLocation(Platform.getBundle(SEMANTIC_BUNDLE_ID))
                .append(SEMANTIC_CACHE_FOLDER)
                .append(workspaceRelativePath);
        if (!cachePath.toFile().exists()) {
            throw new IllegalArgumentException("Semantic cache file does not exist: " + cachePath.toString());
        }
        return cachePath.toString();
    }

    public static String convertCachePathToWorkspaceRelativePath(final String cachePath) {
        IPath semanticCacheBase = Platform.getStateLocation(Platform.getBundle(SEMANTIC_BUNDLE_ID))
                .append(SEMANTIC_CACHE_FOLDER);

        String cacheBasePath = semanticCacheBase.toString().toLowerCase();
        String normalizedCachePath = cachePath.replace("\\", "/").toLowerCase();

        if (normalizedCachePath.startsWith(cacheBasePath)) {
            String relativePath = cachePath.substring(cacheBasePath.length());
            if (relativePath.startsWith("/") || relativePath.startsWith("\\")) {
                relativePath = relativePath.substring(1);
            }
            return relativePath;
        }
        return null;
    }

    /**
     * For an ABAP ADT file, update the remote server with the file update by triggering a save.
     * @param filePath
     */
    public static void updateAdtServer(final String filePath) {
        Display.getDefault().asyncExec(() -> {
            try {
                if (!AbapUtil.isAbapFile(filePath)) {
                    return;
                }

                IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                if (window == null) {
                    return;
                }

                IWorkbenchPage page = window.getActivePage();
                if (page == null) {
                    return;
                }

                IFile workspaceFile = getWorkspaceFileFromCache(filePath);
                if (workspaceFile == null || !workspaceFile.exists()) {
                    Activator.getLogger().info("File not found in workspace, new file may need to be configured with the workspace: " + filePath);
                    MessageDialog.openError(window.getShell(), "File Not Found in Workspace",
                        "File not found in workspace, new file may need to be configured with the workspace. See file updates at:  " + filePath);
                    return;
                }

                // Check if file is already open in an editor
                var existingEditor = findOpenEditor(page, workspaceFile);
                if (existingEditor != null) {
                    saveOpenEditor(existingEditor);
                } else {
                    // File not open, open it temporarily in an invisible editor and save to update remote
                    var descriptor = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(workspaceFile.getName());
                    var editor = page.openEditor(new FileEditorInput(workspaceFile),
                            descriptor != null ? descriptor.getId() : "org.eclipse.ui.DefaultTextEditor", false);
                    if (editor != null && AbapUtil.isAdtEditor(editor.getClass().getName())) {
                        saveOpenEditor(editor);
                        page.closeEditor(editor, false);
                    }
                }
            } catch (Exception e) {
                Activator.getLogger().error("Failed to update ABAP ADT editor", e);
            }
        });
    }

    private static IEditorPart findOpenEditor(final IWorkbenchPage page, final IFile file) {
        for (IEditorReference editorRef : page.getEditorReferences()) {
            var editor = editorRef.getEditor(false);
            if (editor != null && editor.getEditorInput() instanceof FileEditorInput) {
                var input = (FileEditorInput) editor.getEditorInput();
                if (file.equals(input.getFile())) {
                    return editor;
                }
            }
        }
        return null;
    }

    private static IFile getWorkspaceFileFromCache(final String cachePath) {
        try {
            String workspaceRelativePath = AbapUtil.convertCachePathToWorkspaceRelativePath(cachePath);
            if (workspaceRelativePath != null) {
                for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
                    if (project.isOpen()) {
                        String projectRelativePath = workspaceRelativePath;
                        if (StringUtils.startsWithIgnoreCase(workspaceRelativePath, project.getName())) {
                            projectRelativePath = projectRelativePath.substring(project.getName().length() + 1);
                        }
                        IFile file = project.getFile(projectRelativePath);
                        if (file.exists()) {
                            return file;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Activator.getLogger().error("Error finding workspace file associated with the cache", e);
        }
        return null;
    }

    private static void saveOpenEditor(final IEditorPart editor) {
        try {
            ITextEditor textEditor = editor.getAdapter(ITextEditor.class);
            if (textEditor == null) {
                return;
            }

            var provider = textEditor.getDocumentProvider();
            if (provider != null) {
                IEditorInput editorInput = editor.getEditorInput();
                provider.resetDocument(editorInput);
                IDocument document = provider.getDocument(editorInput);
                if (document != null) {
                    var content = document.get();
                    document.set(content); // Mark as dirty
                }
            }

            editor.doSave(new NullProgressMonitor());
        } catch (Exception e) {
            Activator.getLogger().error("Failed to save editor", e);
        }
    }

}
