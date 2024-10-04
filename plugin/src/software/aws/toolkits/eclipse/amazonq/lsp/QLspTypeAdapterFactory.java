// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp;

import java.io.IOException;

import org.eclipse.lsp4j.InitializeResult;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import software.aws.toolkits.eclipse.amazonq.lsp.model.AwsExtendedInitializeResult;

public class QLspTypeAdapterFactory implements TypeAdapterFactory {

    @Override
    @SuppressWarnings("unchecked")
    public final <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
        if (type.getRawType() == InitializeResult.class) {
            final TypeAdapter<InitializeResult> delegate = (TypeAdapter<InitializeResult>) gson.getDelegateAdapter(this, type);

            return (TypeAdapter<T>) new TypeAdapter<InitializeResult>() {
                @Override
                public void write(final JsonWriter out, final InitializeResult value) throws IOException {
                    delegate.write(out, value);
                }

                @Override
                public InitializeResult read(final JsonReader in) throws IOException {
                    return gson.fromJson(in, AwsExtendedInitializeResult.class);
                }
            };
        }
        return null;
    }

}
