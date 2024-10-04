// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.views;

/*
 * Listener that listens to requests being made to send message to Chat UI
 */
public interface ChatUiRequestListener {
    void onSendToChatUi(String message);
}
