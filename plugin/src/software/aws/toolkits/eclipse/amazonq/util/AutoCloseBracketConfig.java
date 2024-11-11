// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

public record AutoCloseBracketConfig(boolean isParenAutoClosed, boolean isAngleBracketAutoClosed,
        boolean isStringAutoClosed, boolean isBracesAutoClosed) {

}
