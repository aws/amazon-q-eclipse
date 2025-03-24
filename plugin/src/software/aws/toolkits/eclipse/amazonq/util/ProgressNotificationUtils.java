// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.lsp4j.ProgressParams;

import com.google.gson.Gson;
import com.google.gson.JsonElement;


public final class ProgressNotificationUtils {
    private ProgressNotificationUtils() {
        // Prevent instantiation
    }

    /*
     * Get the token from the ProgressParams value
     * @return The token as a String
     */
    public static String getToken(final ProgressParams params) {
        String token;

        if (params.getToken().isLeft()) {
            token = params.getToken().getLeft();
        } else {
            token = params.getToken().getRight().toString();
        }

        return token;
    }

    /*
     * Get the object from the ProgressParams value
     * @param cls The class of the object to be deserialized
     * @return The deserialized object, or null if the value is not a JsonElement
     */
    public static <T> T getObject(final ProgressParams params, final Class<T> cls) {
        Object val = params.getValue().getRight();

        if (!(val instanceof JsonElement)) {
            return null;
        }

        Gson gson = new Gson();
        JsonElement element = (JsonElement) val;
        T obj = gson.fromJson(element, cls);

        return obj;
    }
}
