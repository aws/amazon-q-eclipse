//Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
//SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import java.util.ArrayList;
import java.util.List;
import software.aws.toolkits.eclipse.amazonq.views.model.CodeReferenceLogItem;

public final class CodeReferenceLoggedProvider {
 private static final List<CodeReferenceLoggedListener> LISTENERS = new ArrayList<>();

 private CodeReferenceLoggedProvider() {
     //prevent instantiation
 }

 public static void addCodeReferenceLoggedListener(final CodeReferenceLoggedListener listener) {
     LISTENERS.add(listener);
 }

 public static void removeCodeReferenceLoggedListener(final CodeReferenceLoggedListener listener) {
     LISTENERS.remove(listener);
 }

 protected static void notifyCodeReferenceLogged(final CodeReferenceLogItem codeReferenceLogItem) {
     for (CodeReferenceLoggedListener listener : LISTENERS) {
         listener.onCodeReferenceLogged(codeReferenceLogItem);
     }
 }
}
