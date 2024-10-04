// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.telemetry.generator.util;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import java.io.File;
import java.io.IOException;

public final class FileWriter {
    private FileWriter() {
        // Prevent instantiation
    }

    public static void writeClassToFile(final TypeSpec classSpec, final File outputFolder, final String packageName) {
        try {
            JavaFile.builder(packageName, classSpec)
                    .indent("    ")
                    .addFileComment("Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.\n")
                    .addFileComment("SPDX-License-Identifier: Apache-2.0\n")
                    .addFileComment("THIS FILE IS GENERATED! DO NOT EDIT BY HAND!")
                    .build()
                    .writeTo(outputFolder);
        } catch (IOException e) {
            throw new RuntimeException("Error writing class " + classSpec.name, e);
        }
    }
}