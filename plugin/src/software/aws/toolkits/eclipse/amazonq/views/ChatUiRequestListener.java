// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

/*
 * Listener that listens to requests being made to send message to Chat UI
 */
public interface ChatUiRequestListener {
    void onSendToChatUi(String message);
}
