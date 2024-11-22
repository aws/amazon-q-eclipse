// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.telemetry.metadata;

public class ExceptionMetadata {
    private ExceptionMetadata() {
        //prevent instantiation
    }
    public static String scrubException(String prefixString, Exception e) {
        /*TODO: add logic to scrub exception method of any senstive data or PII
         * Will return exception class name until scrubbing logic is implemented
         */
        return prefixString + ". Error: " + e.getClass().getName();
    }
}
