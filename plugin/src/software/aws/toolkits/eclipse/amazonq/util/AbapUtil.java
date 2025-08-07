// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.apache.commons.lang3.StringUtils;

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
}
