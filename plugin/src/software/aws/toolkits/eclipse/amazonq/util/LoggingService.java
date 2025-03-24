// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

public interface LoggingService {
    void info(String message);
    void info(String message, Throwable ex);
    void warn(String message);
    void warn(String message, Throwable ex);
    void error(String message);
    void error(String message, Throwable ex);
}
