// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.telemetry.generator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public final class ResourceLoader {
    private static final String SCHEMA_PATH = "telemetrySchema.json";
    private static final String COMMON_DEFINITIONS = "definitions/commonDefinitions.json";

    private final File basePath;
    private final List<String> definitionsFiles;
    private final String schemaFile;

    public ResourceLoader(final File basePath) {
        this.basePath = basePath;
        this.definitionsFiles = new ArrayList<>();
        this.definitionsFiles.add(loadResource(COMMON_DEFINITIONS));
        this.schemaFile = loadResource(SCHEMA_PATH);
    }

    private String loadResource(final String path) {
        try (InputStream inputStream = new FileInputStream(new File(basePath, path));
             Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
            return scanner.useDelimiter("\\A").next();
        } catch (IOException e) {
            System.err.println("Error while loading resources: " + e);
            throw new RuntimeException(e);
        }
    }

    public List<String> getDefinitionsFiles() {
        return definitionsFiles;
    }

    public String getSchemaFile() {
        return schemaFile;
    }
}