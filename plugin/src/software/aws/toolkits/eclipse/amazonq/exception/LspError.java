// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
    SERVER_FETCH_ERROR("Error fetching server"),
    UNEXPECTED_CACHE_ERROR("Error while caching file"),
    NO_COMPATIBLE_LSP("No LSP version found matching requirements"),
    INVALID_LAUNCH_PROPERTIES("Invalid launch properties"),
    INVALID_WORKING_DIRECTORY("Invalid working directory"),
    EXTRACTION_ERROR("Error or invalid zip files"),
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
