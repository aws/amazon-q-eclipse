// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.telemetry.metadata;

public final class ExceptionMetadata {
    private ExceptionMetadata() {
        //prevent instantiation
    }
    public static String scrubException(final Throwable e) {
        /*TODO: add logic to scrub exception method of any senstive data or PII
         * Will return exception class name until scrubbing logic is implemented
         */
        if (e == null) {
            return "";
        }
        String message = e.getClass().getName();
        if (e.getCause() == null) {
            return message;
        }
        return message + "-" + scrubException(e.getCause());
    }
    public static String scrubException(final String errorCode, final Throwable e) {
        if (e == null) {
            return errorCode;
        }
        return errorCode + "-" + scrubException(e);
    }
}
