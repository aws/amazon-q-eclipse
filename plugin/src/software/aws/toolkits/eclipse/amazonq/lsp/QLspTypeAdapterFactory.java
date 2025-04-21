// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.InitializeResult;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import software.aws.toolkits.eclipse.amazonq.lsp.model.AwsExtendedInitializeResult;
import software.aws.toolkits.eclipse.amazonq.lsp.model.LspServerConfigurations;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.views.model.Configuration;
import software.aws.toolkits.eclipse.amazonq.views.model.Customization;
import software.aws.toolkits.eclipse.amazonq.views.model.QDeveloperProfile;

public class QLspTypeAdapterFactory implements TypeAdapterFactory {

    @Override
    @SuppressWarnings("unchecked")
    public final <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
        if (type.getRawType() == LspServerConfigurations.class) {
            return (TypeAdapter<T>) new TypeAdapter<LspServerConfigurations<Configuration>>() {
                @Override
                public void write(final JsonWriter out, final LspServerConfigurations<Configuration> value)
                        throws IOException {
                    gson.toJson(value.getConfigurations(), new TypeToken<List<Configuration>>() {
                    }.getType(), out);
                }

                @Override
                public LspServerConfigurations<Configuration> read(final JsonReader in) throws IOException {
                    JsonElement rootElement = JsonParser.parseReader(in);
                    Activator.getLogger().info("Raw LSP message: " + rootElement);

                    List<Configuration> customizations = new ArrayList<>();

                    if (rootElement.isJsonArray()) {
                        JsonArray array = rootElement.getAsJsonArray();
                        Type listType = TypeToken.getParameterized(List.class, QDeveloperProfile.class).getType();

                        if (!array.isEmpty() && array.get(0).isJsonObject()
                                && array.get(0).getAsJsonObject().has("description")) {
                            listType = TypeToken.getParameterized(List.class, Customization.class).getType();
                        }

                        customizations = gson.fromJson(rootElement.getAsJsonArray(), listType);
                    }

                    return new LspServerConfigurations<>(customizations);
                }
            }.nullSafe();
        }

        if (type.getRawType() == InitializeResult.class) {
            final TypeAdapter<InitializeResult> delegate = (TypeAdapter<InitializeResult>) gson.getDelegateAdapter(this,
                    type);

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
