// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.manager.model;

import java.util.List;

public record Content(String filename, String url, List<String> hashes, int bytes) { }
