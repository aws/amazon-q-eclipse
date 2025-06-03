// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import java.util.List;

public record ShowSaveFileDialogParams(List<String> supportedFormats, String defaultUri) { }
