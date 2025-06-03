// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.exception;

public enum LspError {
    /*
     * TODO: change this to be an exception class
     * usages inside LspError should throw LspException
     * with (reason, errorCode) construction
     */
    INVALID_VERSION_MANIFEST("Invalid Manifest file"),
    INVALID_REMOTE_SERVER("Invalid Remote Server"),
    INVALID_CACHE_SERVER("Invalid Cache Server"),
    MANIFEST_FETCH_ERROR("Error fetching manifest"),
    MANIFEST_REMOTE_FETCH_ERROR("Error fetching manifest from remote location"),
    SERVER_REMOTE_FETCH_ERROR("Error fetching server from remote"),
    UNEXPECTED_MANIFEST_CACHE_ERROR("Error while caching Manifest file"),
    NO_COMPATIBLE_LSP("No LSP version found matching requirements"),
    NO_VALID_SERVER_FALLBACK("No valid server version to fallback to."),
    ARTIFACT_VALIDATION_ERROR("Error validating artifact"),
    INVALID_LAUNCH_PROPERTIES("Invalid launch properties"),
    INVALID_WORKING_DIRECTORY("Invalid working directory"),
    SERVER_ZIP_EXTRACTION_ERROR("Error extracting server zip files"),
    UNKNOWN("Unknown");

    private final String value;

    LspError(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
