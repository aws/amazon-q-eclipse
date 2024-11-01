// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.encryption;

import java.io.OutputStream;

public interface LspEncryptionManager {
    String encrypt(Object data);
    String decrypt(String jwt);
    void initializeEncrypedCommunication(OutputStream serverStdin);
}
