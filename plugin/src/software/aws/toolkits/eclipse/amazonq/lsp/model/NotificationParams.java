// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.model;

import java.util.List;

import org.eclipse.lsp4j.MessageType;

public record NotificationParams(
    MessageType type,
    NotificationContent content,
    String id,
    List<NotificationAction> actions
) { }
