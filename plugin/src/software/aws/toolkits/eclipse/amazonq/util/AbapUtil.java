// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import java.util.Set;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Platform;

/**
 * Utility class for ABAP/ADT-related constants and helper methods. Centralizes
 * hardcoded values to improve maintainability.
 */
public final class AbapUtil {

    // ABAP/ADT related constants
    public static final String SEMANTIC_FS_SCHEME = "semanticfs:/";
    public static final String SAP_PACKAGE_PREFIX = "com.sap";
    public static final String ADT_CLASS_NAME_PATTERN = "Adt";
    public static final String SEMANTIC_BUNDLE_ID = "org.eclipse.core.resources.semantic";
    public static final String SEMANTIC_CACHE_FOLDER = ".cache";

    // ABAP file extensions that require semantic cache path
    private static final Set<String> ABAP_EXTENSIONS = Set.of("asprog", "aclass", "asinc", "aint", "assrvds", "asbdef",
            "asddls", "astablds", "astabldt", "amdp", "apack", "asrv", "aobj", "aexit", "abdef", "acinc", "asfugr", "apfugr", "asfunc", "asfinc", "apfunc", "apfinc");

    private AbapUtil() {
        // Prevent instantiation
    }

    /**
     * Checks if the given class name indicates an ADT editor.
     * @param className the class name to check
     * @return true if it's likely an ADT editor
     */
    public static boolean isAdtEditor(final String className) {
        return className != null
                && (className.contains(SAP_PACKAGE_PREFIX) || className.contains(ADT_CLASS_NAME_PATTERN));
    }

    /**
     * Converts a semantic filesystem URI to a file system path.
     * @param semanticUri the semantic URI starting with "semanticfs:/"
     * @return the converted file system path
     */
    public static String convertSemanticUriToPath(final String semanticUri) {
        if (!semanticUri.startsWith(SEMANTIC_FS_SCHEME)) {
            return semanticUri;
        }

        String folderName = semanticUri.substring(SEMANTIC_FS_SCHEME.length());
        String newUri = Platform.getStateLocation(Platform.getBundle(SEMANTIC_BUNDLE_ID)).toString() + "/"
                + SEMANTIC_CACHE_FOLDER + "/" + folderName;
        return "file:///" + newUri.replace("\\", "/");
    }

    /**
     * Checks if a file is an ABAP file requiring semantic cache.
     * @param file the file to check
     * @return true if it's an ABAP file
     */
    public static boolean isAbapFile(final IFile file) {
        if (file == null) {
            return false;
        }
        String extension = file.getFileExtension();
        return extension != null && ABAP_EXTENSIONS.contains(extension.toLowerCase());
    }

    /**
     * Gets the semantic cache path for a given workspace-relative path.
     * @param workspaceRelativePath the workspace-relative path
     * @return the full semantic cache path
     */
    public static String getSemanticCachePath(final String workspaceRelativePath) {
        String semanticStateLocation = Platform.getStateLocation(Platform.getBundle(SEMANTIC_BUNDLE_ID)).toString();
        return semanticStateLocation + "/" + SEMANTIC_CACHE_FOLDER + workspaceRelativePath;
    }
}
