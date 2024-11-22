// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.telemetry.metadata;

public final class ExceptionMetadata {
    private ExceptionMetadata() {
        //prevent instantiation
    }
    public static String scrubException(final Exception e) {
        /*TODO: add logic to scrub exception method of any senstive data or PII
         * Will return exception class name until scrubbing logic is implemented
         */
        return e.getClass().getName();
    }
    public static String scrubException(final String prefixString, final Exception e) {
        return prefixString + ". Error: " + scrubException(e);
    }
}
